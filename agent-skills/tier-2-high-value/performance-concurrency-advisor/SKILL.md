---
name: performance-concurrency-advisor
description: Analyze and improve performance, throughput, latency, and concurrency behavior in Kotlin plus Spring services using real evidence from metrics, traces, SQL, thread or heap signals, and code paths. Use when endpoints are slow, pools saturate, coroutines or reactive flows block unexpectedly, N+1 or contention appears, or caching and parallelism decisions need precise, non-generic guidance.
---

# Performance Concurrency Advisor

Source mapping: Tier 2 high-value skill derived from `Kotlin_Spring_Developer_Pipeline.md` (`SK-18`).

## Mission

Find the bottleneck that actually limits the system, then recommend the smallest high-leverage change.
Treat performance work as evidence-driven systems analysis, not as a bag of folklore.

## Read Evidence First

- Latency metrics, percentile breakdowns, throughput, and error rate.
- Traces for slow flows.
- SQL count or explain plans when persistence is involved.
- Hikari, thread pool, event loop, or queue metrics when available.
- Thread dumps, heap or GC signals, and recent code changes when the issue is severe.

## Diagnose By Layer

Check these layers explicitly:

- database query count and query latency
- connection pool contention
- downstream HTTP or messaging latency
- thread pool starvation or scheduler misuse
- event-loop blocking in reactive flows
- serialization or large payload overhead
- lock contention or transactional scope
- cache misses or invalidation churn
- CPU-bound work and allocation pressure

## Advanced Performance Heuristics

- N+1 is both a latency bug and a load amplifier. Fixing it often helps pool pressure and downstream CPU together.
- Bigger pools are not a universal fix. If the database is slow, increasing concurrency often worsens tail latency.
- Hikari tuning should follow database capacity and workload pattern, not only CPU count.
- Blocking calls inside WebFlux or coroutine event loops are catastrophic even when average latency looks acceptable.
- `Dispatchers.IO` is not a free performance button. It moves blocking but can also hide architectural mismatch and increase context switching.
- Parallelizing downstream calls can reduce mean latency while worsening saturation and timeouts under load.
- Caches need invalidation, warm-up, and cardinality discipline. A bad cache can improve benchmarks and hurt production correctness.
- Large object graphs, verbose JSON, and repeated mapping can dominate latency once the database is already optimized.
- Lock contention and long transactions often look like random latency spikes until you correlate them with pool saturation and retries.
- GC tuning is downstream of allocation patterns. Do not jump to JVM flags before understanding object churn.

## Measurement Nuances

- Average latency hides tail pain. Prefer percentiles and saturation correlation when user pain is bursty.
- Coordinated omission can make a load test look healthier than production. Treat benchmark tooling assumptions as part of the evidence.
- Container CPU limits, memory limits, and JDK container-awareness can dominate runtime behavior even when local profiling looks clean.
- Warm-up state, JIT compilation, cache fill, and connection pool priming matter. Separate cold-start behavior from steady-state behavior.
- If queueing exists anywhere in the path, small utilization increases can create nonlinear latency growth. Think in queueing terms, not only in raw CPU percentages.

## Expert Heuristics

- Prefer eliminating unnecessary work before parallelizing existing work.
- If one optimization helps throughput but worsens tail latency or operator clarity, call that tradeoff out explicitly.
- When reactive or coroutine code is slower than blocking code, first verify hidden blocking boundaries and context switches before blaming the paradigm.
- If the service is already SLO-bound by a dependency, optimize isolation and graceful degradation before micro-optimizing local code.

## Concurrency Rules

- Distinguish I/O-bound work from CPU-bound work before recommending async or parallel execution.
- Keep transaction scopes short when locks or connection usage are involved.
- Treat shared mutable in-memory state as a design smell in horizontally scaled services.
- For coroutine or reactive flows, verify context propagation for transactions, security, and MDC if debugging or tracing is part of the issue.
- Use backpressure, bulkheads, or bounded queues before unbounded parallelism.

## Concrete Pattern — N+1 Query Detection and Fix

### The Problem
```kotlin
@Entity
class Order(
    @Id @GeneratedValue val id: Long = 0,
    var customerName: String,
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)  // lazy is correct default
    val items: MutableList<OrderItem> = mutableListOf()
)

@Service
class ReportService(private val orderRepository: OrderRepository) {
    fun getOrderSummaries(): List<OrderSummary> {
        val orders = orderRepository.findAll()     // 1 query for orders
        return orders.map { order ->
            OrderSummary(
                orderId = order.id,
                customerName = order.customerName,
                itemCount = order.items.size        // BUG: triggers N lazy loads!
            )
        }
    }
}
// Total queries: 1 + N (one per order). With 1000 orders = 1001 queries.
```

### The Fix — JOIN FETCH or Entity Graph
```kotlin
// Option A: JPQL JOIN FETCH
interface OrderRepository : JpaRepository<Order, Long> {
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items")
    fun findAllWithItems(): List<Order>
}

// Option B: @EntityGraph
interface OrderRepository : JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = ["items"])
    override fun findAll(): List<Order>
}

// Option C: Projection (best for read-only summaries)
interface OrderSummaryProjection {
    val id: Long
    val customerName: String
    val itemCount: Long
}

interface OrderRepository : JpaRepository<Order, Long> {
    @Query("""
        SELECT o.id as id, o.customerName as customerName, COUNT(i) as itemCount
        FROM Order o LEFT JOIN o.items i GROUP BY o.id, o.customerName
    """)
    fun findAllSummaries(): List<OrderSummaryProjection>
}
// Total queries: 1 (single query regardless of order count)
```

### Critical — update BOTH repository AND service
When adding a new `findAllWithItems()` method to the repository, you MUST also update the service to call it:
```kotlin
// In the service — call the new method:
fun getOrderSummaries(): List<OrderSummary> {
    val orders = orderRepository.findAllWithItems()  // NOT findAll()
    return orders.map { ... }
}
```
If you override `findAll()` with `@EntityGraph`, the service automatically uses it without changes.

### Common mistakes
- Changing `FetchType.LAZY` to `FetchType.EAGER` globally — fixes one query but creates N+1 everywhere else
- Using `JOIN FETCH` with pagination — Hibernate fetches all rows and paginates in memory (warning: "HHH90003004")
- Forgetting `DISTINCT` with `JOIN FETCH` on collections — duplicate parent entities in result
- Not testing with realistic data volumes — N+1 is invisible with 5 rows, catastrophic with 5000
- Adding `findAllWithItems()` to repository but still calling `findAll()` in the service — fix is invisible

## Concrete Pattern — Per-Entity Loop to Batch Query

### The Problem — Loop with Per-Entity Repository Call
```kotlin
@Service
class InventoryService(
    private val productRepository: ProductRepository,
    private val stockLevelRepository: StockLevelRepository
) {
    fun getProductStockSummary(productId: Long): StockSummary {
        val product = productRepository.findById(productId).orElseThrow()
        var totalAvailable = 0
        var totalReserved = 0
        for (variant in product.variants) {                             // lazy load = 1 query
            val stock = stockLevelRepository.findByVariantId(variant.id) // BUG: 1 query per variant!
            if (stock != null) {
                totalAvailable += stock.availableQuantity
                totalReserved += stock.reservedQuantity
            }
        }
        return StockSummary(productId, totalAvailable, totalReserved)
    }
}
// With 10 variants: 1 (product) + 1 (variants) + 10 (stock per variant) = 12 queries
```

### The Fix — Batch Query with findByXIn()
```kotlin
@Service
class InventoryService(
    private val productRepository: ProductRepository,
    private val stockLevelRepository: StockLevelRepository
) {
    @Transactional(readOnly = true)
    fun getProductStockSummary(productId: Long): StockSummary {
        val product = productRepository.findById(productId).orElseThrow()
        val variantIds = product.variants.map { it.id }
        val stocks = stockLevelRepository.findByVariantIdIn(variantIds)  // 1 batch query
        return StockSummary(
            productId = productId,
            totalAvailable = stocks.sumOf { it.availableQuantity },
            totalReserved = stocks.sumOf { it.reservedQuantity }
        )
    }
}
// With 10 variants: 1 (product) + 1 (variants) + 1 (batch stock) = 3 queries
```

### Key rule — when a service loops and queries per item, replace with batch
- If you see `for (item in collection) { repo.findByX(item.id) }` → use `repo.findByXIn(ids)`
- Spring Data auto-generates `findByXIn(ids: List<Long>)` from method name — no @Query needed
- For parent + lazy children: use `@EntityGraph` or `JOIN FETCH` on the parent query
- For aggregate summaries: prefer `findByXIn()` over per-entity loops

## Output Contract

Return these sections:

- `Observed bottleneck`: the most likely limiting resource or contention point.
- `Evidence`: metrics, traces, code path, or SQL evidence supporting that conclusion.
- `Recommended change`: the smallest high-impact optimization.
- `Tradeoffs`: what gets better and what new risk appears.
- `Verification`: the benchmark, metric, or load-test signal that should improve.

## Guardrails

- Do not optimize without measurement.
- Do not recommend caching, async, or bigger pools as reflexes.
- Do not confuse throughput optimization with latency optimization; sometimes they move in opposite directions.
- Do not trust local benchmarks alone for highly concurrent or I/O-heavy paths.

## Quality Bar

A good run of this skill identifies the true constraint and gives a measurable improvement plan.
A bad run suggests generic tuning knobs with no evidence, no tradeoffs, and no way to confirm success.
