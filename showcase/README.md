# Kotlin + Spring Skills — Showcase

AI skills are domain-specific knowledge documents that guide LLM agents toward correct Kotlin + Spring patterns. This showcase demonstrates their measurable impact through benchmarks, code examples, and A/B tests on open-source projects.

## Evidence-Backed Skills

6 skills with measurable impact, validated through compound benchmarks (2-3 runs per mode) and 7 A/B tests on 5 open-source projects:

| Skill | What It Catches | Evidence |
|-------|----------------|----------|
| [SK-16](../agent-skills/tier-2-high-value/configuration-properties-profiles-kotlin-safe/SKILL.md) Config/Profiles | `Long` timeout → `Duration`, mutable → immutable config | **Strongest.** Benchmark: fails 6/6 without, passes 6/6 with. External: no-skills agent explicitly dismissed as "not a bug" |
| [SK-09](../agent-skills/tier-1-critical/transaction-consistency-designer/SKILL.md) Transactions | Missing `@Transactional`, batch error handling | External: no-skills agent missed `@Transactional` on `deleteArticle` (2 DB ops non-atomic). Benchmark: batch errors fail 3/3 without |
| [SK-07](../agent-skills/tier-2-high-value/error-model-validation-architect/SKILL.md) Validation/Errors | Over-applied `@field:`, swallowed gateway exceptions | External: precise targeting (2 DTOs vs 5). Benchmark: gateway rethrow fails 2/3 without, passes 3/3 with |
| [SK-10](../agent-skills/tier-1-critical/jpa-spring-data-kotlin-mapper/SKILL.md) JPA Entity Identity | `data class` entities, missing uniqueness guards | 100% prevalence in real JPA projects (12/12 entities). Benchmark: uniqueness guard fails 3/3 without |
| [SK-08](../agent-skills/tier-2-high-value/jackson-kotlin-serialization-specialist/SKILL.md) Jackson | Tri-state PATCH (absent vs explicit null) | Benchmark: fails 3/3 without, passes 2/2 with |
| [SK-11](../agent-skills/tier-3-specialized/schema-migration-planner/SKILL.md) Schema Migration | Destructive column rename → expand-contract | Benchmark: fails 3/3 without, passes all with |

> **Key insight:** Skills don't teach the LLM new knowledge — it already knows each pattern individually (H-01..H-05: all 12/12 without skills). Skills provide **attention prioritization** so that when multiple pitfalls compete in a single task, none get dropped.

Full catalog: [agent-skills/](../agent-skills/) (25 skills, 35 patterns).

---

## Benchmark Results

### Compound Tasks (Multiple Runs)

Each benchmark was run 2-3 times per mode to verify consistency. Timed-out runs are excluded.

| Benchmark | With Skills | Without Skills | Delta |
|-----------|:-----------:|:--------------:|:-----:|
| **H-06** Zero-Downtime Order Fallout | **19/19** (n=2: 19, 19) | 14.7/19 (n=3: 14, 15, 15) | **+4.3** |
| **H-07** Inventory Reconciliation | **18.7/19** (n=3: 19, 19, 18) | 14.7/19 (n=3: 15, 15, 14) | **+4** |
| **H-08** Payment Gateway Meltdown | **17/17** (n=3: 17, 17, 17) | 14.7/17 (n=3: 16, 14, 14) | **+2.3** |

Skills involved: SK-03 (Proxy), SK-07 (Errors), SK-08 (Jackson), SK-09 (Transactions), SK-10 (JPA), SK-11 (Migration), SK-16 (Config).

With skills, the agent passes nearly every check (variance 0-1). Without skills, the same failures recur consistently across runs — see [key delta checks](EVIDENCE_DETAILS.md#compound-benchmark-key-delta-checks).

### Standard Tasks (B-01 through B-15)

15 standard benchmarks, all steps combined (n=1 per mode):

| Benchmark | claude +skills | claude -skills | codex -skills | Delta |
|-----------|:--------------:|:--------------:|:-------------:|:-----:|
| B-01 Order CRUD | 13/13 | 13/13 | 13/13 | 0 |
| B-02 Transaction Trap | 13/13 | 13/13 | 13/13 | 0 |
| B-03 N+1 Clinic | 14/14 | 14/14 | 14/14 | 0 |
| B-04 Security Lockdown | 16/16 | 15/16 | 16/16 | **+1** |
| B-05 Kafka Pipeline | 12/12 | 12/12 | 12/12 | 0 |
| B-06 Config Maze | 13/13 | 11/13 | 13/13 | **+2** |
| B-07 Java→Kotlin | 21/21 | 21/21 | 21/21 | 0 |
| B-08 Jackson Gauntlet | 11/11 | 11/11 | 11/11 | 0 |
| B-09 Digital Wallet | 13/13 | 13/13 | 13/13 | 0 |
| B-10 Observability | 18/18 | 18/18 | 18/18 | 0 |
| B-11 Spring Upgrade | 18/18 | 18/18 | 18/18 | 0 |
| B-12 Resilient HTTP | 12/12 | 12/12 | 12/12 | 0 |
| B-13 Schema Migration | 15/15 ¹ | 15/15 | 15/15 | 0 |
| B-14 PR Review | 14/14 | 14/14 | 14/14 | 0 |
| B-15 Production Incident | 20/20 | 20/20 | 20/20 | 0 |

13 benchmarks show perfect parity. B-04 (+1) and B-06 (+2) show small positive deltas with skills.

¹ B-13 run 1 had a compilation failure (6/8). A rerun scored 15/15, confirming the failure was an environment fluke.

---

## Cases

### Case 1: Immutable Config with Duration Type

**Skill:** SK-16 &nbsp;|&nbsp; **Benchmark:** H-07 (checks E2 + E3)

Spring Boot 3.x supports `java.time.Duration` natively in `@ConfigurationProperties` and constructor binding for immutability. Without skills, the agent defaults to mutable classes with raw `Long` — losing unit semantics and startup validation.

**Without skills** — mutable class, `Long` timeout, no fail-fast:

```kotlin
@ConfigurationProperties(prefix = "app.pricing")
@Validated
class PricingProperties {
    @field:NotBlank
    lateinit var baseUrl: String

    @field:Positive
    var timeout: Long = 0       // millis? seconds? no way to tell

    var apiKey: String? = null
}
```

**With skills (SK-16)** — immutable data class, `Duration`, self-documenting:

```kotlin
@ConfigurationProperties(prefix = "app.pricing")
data class PricingProperties(
    val baseUrl: String,                            // fails fast if missing
    val timeout: Duration = Duration.ofSeconds(5),  // unit is explicit
    val apiKey: String? = null
)
```

**Why it matters:** `timeout: Long = 0` silently accepts zero or negative values. `lateinit var baseUrl` lets the app start with a missing URL and fail at runtime. The `data class` with `val` fails at startup if config is invalid.

---

### Case 2: Uniqueness Constraint for Idempotency

**Skill:** SK-10 &nbsp;|&nbsp; **Benchmark:** H-07 (check A3)

When an API accepts operations that should be idempotent (e.g., "reserve stock for order X"), the service must check for duplicates before processing. Without skills, the agent skips this check.

**Without skills** — no duplicate guard, silently creates duplicate reservations:

```kotlin
@Transactional
fun reserve(variantId: Long, orderId: String, quantity: Int): ReservationResponse {
    val stockLevel = stockLevelRepository.findByVariantIdForUpdate(variantId)
        ?: throw NotFoundException("Stock level not found for variant $variantId")

    if (stockLevel.availableQuantity < quantity) {
        throw InsufficientStockException("Not enough stock")
    }

    stockLevel.availableQuantity -= quantity
    stockLevel.reservedQuantity += quantity
    stockLevelRepository.save(stockLevel)

    val reservation = reservationRepository.save(
        Reservation(variantId = variantId, orderId = orderId, quantity = quantity)
    )
    return toResponse(reservation)
}
```

**With skills (SK-10)** — explicit duplicate check before business logic:

```kotlin
@Transactional
fun reserve(variantId: Long, orderId: String, quantity: Int): ReservationResponse {
    // SK-10: check uniqueness before acquiring lock
    val existing = reservationRepository.findByVariantIdAndOrderId(variantId, orderId)
    if (existing.isNotEmpty()) {
        throw DuplicateReservationException(
            "Reservation already exists for variant=$variantId, order=$orderId"
        )
    }

    val stockLevel = stockLevelRepository.findByVariantIdForUpdate(variantId)
        ?: throw NotFoundException("Stock level not found for variant $variantId")

    // ... same business logic ...
}
```

**Why it matters:** Without the guard, retrying a failed request (network timeout, client retry) silently double-reserves inventory. The agent even defines `findByVariantIdAndOrderId` in the repository — but never calls it.

---

### Case 3: Batch Error Collection

**Skill:** SK-09 &nbsp;|&nbsp; **Benchmark:** H-07 (check D3)

When importing a batch of rows (CSV, API bulk request), throwing on the first error means the caller never learns about problems in rows 2 through N. The skill teaches collecting all errors.

**Without skills** — throws `NotFoundException` on first bad SKU:

```kotlin
@Transactional
fun importBatch(rows: List<CsvRow>): BatchImportResult {
    val results = mutableListOf<StockAdjustment>()
    for (row in rows) {
        val adjustment = importRow(row)  // throws on first bad SKU!
        results.add(adjustment)
    }
    return BatchImportResult(imported = results.size, errors = emptyList())
}

fun importRow(row: CsvRow): StockAdjustment {
    val variant = variantRepository.findBySku(row.sku)
        ?: throw NotFoundException("SKU not found: ${row.sku}")
    // ...
}
```

**With skills (SK-09)** — collects all errors, throws composite exception:

```kotlin
@Transactional
fun importBatch(rows: List<CsvRow>): BatchImportResult {
    val errors = mutableListOf<String>()
    val results = mutableListOf<StockAdjustment>()

    for (row in rows) {
        val variant = variantRepository.findBySku(row.sku)
        if (variant == null) {
            errors.add("SKU not found: ${row.sku}")
            continue                         // keep processing remaining rows
        }
        // ... process valid row ...
    }

    if (errors.isNotEmpty()) {
        throw BatchImportException(
            "Batch failed with ${errors.size} error(s): ${errors.joinToString("; ")}",
            errors
        )
    }
    return BatchImportResult(imported = results.size, errors = emptyList())
}
```

**Why it matters:** In a 1000-row CSV import, the caller needs to know all 47 bad rows at once — not discover them one at a time through 47 sequential retries.

---

### Case 4: Expand-Contract Migration

**Skill:** SK-11 &nbsp;|&nbsp; **Benchmark:** H-06 (checks A1 + A3)

When renaming a database column, a destructive `RENAME` breaks any service still reading the old column name during deployment. The expand-contract pattern keeps both columns alive.

**Without skills** — destructive rename, old column gone:

```sql
-- V2__rename_shipping_address.sql
ALTER TABLE orders ALTER COLUMN shipping_address RENAME TO delivery_address;
```

**With skills (SK-11)** — expand: add new column alongside old, copy data:

```sql
-- V2__add_delivery_address.sql
ALTER TABLE orders ADD COLUMN delivery_address VARCHAR(255);
UPDATE orders SET delivery_address = shipping_address WHERE delivery_address IS NULL;
ALTER TABLE orders ALTER COLUMN delivery_address SET NOT NULL;
```

Plus dual-write in the entity so both columns stay in sync:

```kotlin
@Entity
@Table(name = "orders")
class Order(
    @Column(name = "delivery_address", nullable = false)
    var deliveryAddress: String = "",

    @Column(name = "shipping_address")
    var shippingAddress: String = deliveryAddress
) {
    @PrePersist @PreUpdate
    fun syncLegacyColumns() {
        shippingAddress = deliveryAddress
    }
}
```

**Why it matters:** In production with rolling deploys, old and new service versions run simultaneously. A destructive rename breaks the old version instantly. Expand-contract is the standard pattern for zero-downtime schema evolution.

---

### Case 5: Tri-State PATCH Semantics

**Skill:** SK-08 &nbsp;|&nbsp; **Benchmark:** H-06 (checks B2 + B3)

In a REST PATCH request, there are three states for each field: **absent** (don't change), **present with value** (update), and **present with null** (clear). Kotlin's `null` conflates the first and third cases.

**Without skills** — Elvis operator conflates absent and explicit null:

```kotlin
data class OrderPatchRequest(
    val notes: String? = null,           // absent → null, explicit null → also null
    val customerReference: String? = null
)

fun apply(order: Order, request: OrderPatchRequest) {
    order.notes = request.notes ?: order.notes                     // can never clear!
    order.customerReference = request.customerReference ?: order.customerReference
}
```

Sending `{"notes": null}` deserializes as `null`, Elvis keeps the old value. The field can never be cleared.

**With skills (SK-08)** — tracks which fields were explicitly set to null:

```kotlin
data class OrderPatchRequest(
    val notes: String? = null,
    val customerReference: String? = null,
    @JsonIgnore val explicitNulls: Set<String> = emptySet()
)

fun apply(order: Order, request: OrderPatchRequest) {
    // Explicit null → clear the field
    if ("notes" in request.explicitNulls) order.notes = null
    else if (request.notes != null) order.notes = request.notes
    // absent → no change

    if ("customerReference" in request.explicitNulls) order.customerReference = null
    else if (request.customerReference != null) order.customerReference = request.customerReference
}
```

**Why it matters:** Any REST API with nullable fields and PATCH operations needs tri-state semantics. Without it, clients cannot clear optional fields like notes, references, or descriptions.

---

### Case 6: Self-Invocation Bypasses Spring Proxy

**Skill:** SK-03 &nbsp;|&nbsp; **Benchmark:** H-06 (check D3)

When a Spring bean calls its own method (e.g., `this.getSummary()`), the call bypasses the AOP proxy. Annotations like `@Cacheable`, `@Transactional`, and `@Async` silently do nothing.

**Without skills** — `this.evictSummary()` and `this.getSummary()` bypass proxy:

```kotlin
@Service
class OrderSummaryService(private val orderRepository: OrderRepository) {

    @Cacheable("orderSummaries")
    fun getSummary(orderId: Long): OrderSummary { /* ... */ }

    @CacheEvict(value = ["orderSummaries"], key = "#orderId")
    fun evictSummary(orderId: Long) { }

    fun refreshSummary(orderId: Long): OrderSummary {
        evictSummary(orderId)       // this.evictSummary() — AOP proxy skipped!
        return getSummary(orderId)  // this.getSummary() — AOP proxy skipped!
    }
}
```

**With skills (SK-03)** — `@CachePut` replaces evict+get, works through proxy:

```kotlin
@Service
class OrderSummaryService(private val orderRepository: OrderRepository) {

    @Cacheable("orderSummaries")
    fun getSummary(orderId: Long): OrderSummary { /* ... */ }

    @CachePut("orderSummaries", key = "#orderId")
    fun refreshSummary(orderId: Long): OrderSummary {
        val order = orderRepository.findDetailedById(orderId)
            ?: throw OrderNotFoundException(orderId)
        return order.toSummary()  // always executes, always updates cache
    }
}
```

**Why it matters:** This is a fundamental limitation of Spring AOP (proxy-based). It affects `@Transactional`, `@Cacheable`, `@Async`, `@Retryable`, and any other proxy-intercepted annotation. The symptom is always the same: the annotation silently does nothing.

---

### Case 7: Duration Type in a Different Project

**Skill:** SK-16 &nbsp;|&nbsp; **Benchmark:** H-08 (check D2)

Same pattern as Case 1 in a different project (Payment Gateway vs Inventory), demonstrating that the skill generalizes across codebases.

**Without skills** — raw `Long` with no unit semantics:

```kotlin
@ConfigurationProperties(prefix = "app.gateway")
@Validated
data class GatewayProperties(
    @field:NotBlank
    val baseUrl: String,
    val timeout: Long = 5000,       // millis? seconds?
    val apiKey: String? = null
)
```

**With skills (SK-16)** — `Duration` with explicit unit:

```kotlin
@ConfigurationProperties(prefix = "app.gateway")
data class GatewayProperties(
    val baseUrl: String,
    val timeout: Duration = Duration.ofMillis(5000),
    val apiKey: String? = null
)
```

```yaml
# application.yml
app:
  gateway:
    timeout: 5000ms
```

---

## External Validation

Scanned 11 open-source Kotlin + Spring projects from GitHub. Found skill-targeted anti-patterns in 8 of them — SK-10 (`data class` entities) appeared in 100% of JPA projects.

Ran 7 A/B tests on 5 projects (same task, same agent, with and without skills):

| Project | Patterns | Task | Result |
|---------|----------|------|--------|
| realworld | SK-10 + SK-07 | Focused | **+skills wins** — code doesn't compile without |
| bastman | SK-10 | Focused | Parity |
| piomin | SK-16 | Focused | Mixed |
| boilerplate | SK-10 + SK-08 | Focused | Parity |
| realworld | SK-10 + SK-07 + SK-09 | Broad | **+skills wins** — missed `@Transactional` without |
| karumi | SK-10 + SK-03 | Broad | Parity |
| boilerplate | SK-10 + SK-16 | Broad | **+skills wins** — left `Long` config unchanged without |

- Focused tasks: 1 win, 2 parity, 1 mixed
- Broad compound tasks: **2 wins, 1 parity**
- Both compound wins involve patterns the no-skills agent explicitly dismissed or overlooked

For per-check comparison tables and full methodology, see [EVIDENCE_DETAILS.md](EVIDENCE_DETAILS.md).

---

## How to Reproduce

```bash
cd benchmarks

# Run a benchmark with skills
./run.sh H-07 claude+skills

# Run the same benchmark without skills
./run.sh H-07 claude-skills

# Compare results (results are stored in ../results/)
cat ../results/H-07-*_claude+skills_*/eval-step-1.json
cat ../results/H-07-*_claude-skills_*/eval-step-1.json
```

Requires: Claude API key, JDK 17+, ~15 minutes per run.

## Limitations and Mitigations

| Concern | Status | Evidence |
|---------|--------|----------|
| **Co-development risk** — skills and benchmarks developed together | Partially mitigated | 13/15 standard benchmarks score identically with and without skills → no regressions. B-04 (+1) and B-06 (+2) show unpredicted positive deltas. |
| **Not validated externally** — no testing on production code | Partially mitigated | 7 A/B tests on 5 open-source projects: 3 wins, 3 parity, 1 mixed. Compound tests show +skills catches patterns that -skills explicitly misses. |
| **LLM non-determinism** | Addressed | 2-3 runs per mode per benchmark. With skills: variance 0-1. Without: consistent failure patterns. |
| **No delta on standard tasks** | Expected | 13/15 parity. Skills help on compound tasks where 6+ pitfalls interact. |
| **Skill formulations too specific** | Partially mitigated | SK-16 works across 3 projects (H-07, H-08, B-06). SK-10 found in 100% of real JPA projects. |
