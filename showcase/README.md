# Kotlin + Spring Skills — Showcase

AI skills are domain-specific knowledge documents that guide LLM agents toward correct Kotlin + Spring patterns. This showcase demonstrates their measurable impact through concrete before/after code examples.

## Results

Each benchmark is a realistic Kotlin + Spring project with hidden tests the agent never sees. We run the same task twice — with and without skills — and compare scores.

| Benchmark | Description | With Skills | Without Skills | Delta |
|-----------|-------------|:-----------:|:--------------:|:-----:|
| **H-06** Zero-Downtime Order Fallout | Migration, PATCH, cache, transactions | **19/19** | 14/19 | **+5** |
| **H-07** Inventory Reconciliation | Config, batch, uniqueness, N+1 | **19/19** | 15/19 | **+4** |
| **H-08** Payment Gateway Meltdown | Resilience, validation, error handling | **17/17** | 16/17 | **+1** |

Skills involved: SK-03 (Proxy), SK-08 (Jackson), SK-09 (Transactions), SK-10 (JPA), SK-11 (Migration), SK-16 (Config).

> **Note:** 15 standard benchmarks (B-01 through B-15) show no delta — both modes score 99-100%. Skills make a difference only on compound tasks where multiple Kotlin+Spring pitfalls interact.

---

## Cases

### Case 1: Immutable Config with Duration Type

**Skill:** [SK-16 Configuration Properties](../agent-skills/tier-2-high-value/config-profiles-specialist/SKILL.md)
**Benchmark:** H-07 (checks E2 + E3)

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

**Skill:** [SK-10 JPA Mapper](../agent-skills/tier-1-critical/jpa-spring-data-kotlin-mapper/SKILL.md)
**Benchmark:** H-07 (check A3)

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

**Skill:** [SK-09 Transaction Designer](../agent-skills/tier-1-critical/transaction-consistency-designer/SKILL.md)
**Benchmark:** H-07 (check D3)

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

**Skill:** [SK-11 Schema Migration Planner](../agent-skills/tier-3-specialized/schema-migration-planner/SKILL.md)
**Benchmark:** H-06 (checks A1 + A3)

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

**Skill:** [SK-08 Jackson Serialization](../agent-skills/tier-2-high-value/jackson-kotlin-serialization-specialist/SKILL.md)
**Benchmark:** H-06 (checks B2 + B3)

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

**Skill:** [SK-03 Proxy Compatibility](../agent-skills/tier-1-critical/kotlin-spring-proxy-compatibility/SKILL.md)
**Benchmark:** H-06 (check D3)

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

**Skill:** [SK-16 Configuration Properties](../agent-skills/tier-2-high-value/config-profiles-specialist/SKILL.md)
**Benchmark:** H-08 (check D2)

This is the same pattern as Case 1, but in a completely different project (Payment Gateway vs Inventory). It demonstrates that the skill generalizes across codebases.

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

```yaml
# application.yml
app:
  gateway:
    timeout: 5000
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

**Why it matters:** Same skill, different project — the pattern transfers. This case also shows that SK-16 is not benchmark-specific; it addresses a general Spring Boot configuration concern.

---

## How to Reproduce

```bash
cd benchmarks

# Run a benchmark with skills
./run.sh H-07 claude+skills

# Run the same benchmark without skills
./run.sh H-07 claude-skills

# Compare results
cat results/H-07-*_claude+skills_*/eval-step-1.json
cat results/H-07-*_claude-skills_*/eval-step-1.json
```

Requires: Claude API key, JDK 17+, ~15 minutes per run.

## Skills Used in This Showcase

| Skill | Tier | Cases | What It Teaches |
|-------|------|:-----:|-----------------|
| [SK-03](../agent-skills/tier-1-critical/kotlin-spring-proxy-compatibility/SKILL.md) | 1 | 6 | Self-invocation bypass, `@Enable*` annotations |
| [SK-08](../agent-skills/tier-2-high-value/jackson-kotlin-serialization-specialist/SKILL.md) | 2 | 5 | Tri-state PATCH, polymorphic JSON, `jackson-module-kotlin` |
| [SK-09](../agent-skills/tier-1-critical/transaction-consistency-designer/SKILL.md) | 1 | 3 | Batch error collection, transaction propagation |
| [SK-10](../agent-skills/tier-1-critical/jpa-spring-data-kotlin-mapper/SKILL.md) | 1 | 2 | Entity identity, uniqueness constraints |
| [SK-11](../agent-skills/tier-3-specialized/schema-migration-planner/SKILL.md) | 3 | 4 | Expand-contract migrations |
| [SK-16](../agent-skills/tier-2-high-value/config-profiles-specialist/SKILL.md) | 2 | 1, 7 | `Duration` binding, constructor binding, immutable config |

Full skill catalog: [agent-skills/](../agent-skills/) (25 skills, 35 patterns).

## Limitations

These results should be interpreted with the following caveats:

- **Co-development risk.** Skills and benchmarks were developed together over 9 iterative sessions. The skills were tuned based on benchmark failures. This means the measured delta may overestimate the benefit on unseen code.
- **Not validated externally.** No skill has been tested on a real production project or third-party codebase.
- **LLM non-determinism.** Results vary between runs (typically ±1-2 checks). The scores above are from the latest single run per mode, not averaged.
- **No delta on standard tasks.** The 15 standard benchmarks (B-01 through B-15) show 99-100% scores for both modes. Skills only help on compound tasks with multiple interacting pitfalls.
- **Generalizable core.** The patterns themselves (JPA identity, proxy bypass, Duration binding, expand-contract) are well-documented Kotlin+Spring pitfalls. The skill formulations may be more specific than necessary.
