---
name: transaction-consistency-designer
description: Design safe transaction boundaries, rollback behavior, idempotency, locking, and consistency strategies for Kotlin + Spring business workflows. Use when a feature writes to the database, spans multiple repositories, publishes messages, calls external systems, suffers from partial commits or duplicate processing, or needs precise `@Transactional`, propagation, or isolation guidance.
---

# Transaction Consistency Designer

Source mapping: Tier 1 critical skill derived from `Kotlin_Spring_Developer_Pipeline.md` (`SK-09`).

## Mission

Place transaction boundaries where business invariants are actually enforced, not where annotations are easiest to type.
Prevent data loss, duplicate side effects, and hidden consistency bugs.

## Gather These Inputs

- The business workflow step by step.
- The repositories and tables touched by each step.
- Current `@Transactional` annotations, propagation, isolation, and exception handling.
- Any external HTTP, message broker, scheduler, or file I/O inside the workflow.
- Idempotency, retry, and concurrency requirements.

## Model The Workflow Explicitly

- Break the use case into state-changing steps and side effects.
- Mark which steps must be atomic together and which can be asynchronous.
- Mark where external systems are called.
- Mark where retries may happen.
- Mark the business invariant that must not be violated.

## Decision Rules

- Keep one database transaction focused on one consistency boundary.
- Avoid holding a database transaction open across external network calls.
- Prefer idempotency keys plus unique constraints for duplicate-request safety.
- Prefer the outbox pattern or post-commit publication for messages that must reflect committed state.
- Choose locking strategy based on contention and correctness needs:
  - optimistic locking for low-contention update races
  - unique constraints for duplicate prevention
  - pessimistic locking only when contention and correctness justify it

## Spring-Specific Checks

- Verify whether `@Transactional` is on a proxied public entry point.
- Verify whether self-invocation bypasses the transaction boundary.
- Verify rollback rules. By default, unchecked exceptions roll back, checked exceptions may not.
- Verify whether `readOnly = true` is used only where appropriate.
- Verify whether `REQUIRES_NEW` is truly required or is masking a design issue.
- Treat `NESTED` as database- and platform-dependent, not a universal escape hatch.

## Concrete Pattern — Propagation Choice: REQUIRED vs REQUIRES_NEW

### Scenario: Outbox + Audit in a patch operation

The order service patches an order. On success, it must publish an outbox event (same tx). On failure, it must record an audit entry (independent tx, survives rollback).

### BROKEN: Wrong propagation
```kotlin
@Service
class OutboxService(private val outboxRepository: OutboxRepository) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // BUG: orphan on rollback
    fun publishEvent(event: OutboxEvent) {
        outboxRepository.save(event)
    }
}

@Service
class AuditService(private val auditRepository: AuditRepository) {
    @Transactional  // BUG: REQUIRED joins caller → lost on rollback
    fun recordFailure(orderId: Long, reason: String) {
        auditRepository.save(AuditEntry(orderId, reason))
    }
}
```

If the outer transaction rolls back:
- OutboxService with REQUIRES_NEW: event is committed in its own tx → **orphaned event** with no corresponding order change
- AuditService with REQUIRED: audit joins the outer tx → **lost on rollback**

### CORRECT: Match propagation to intent
```kotlin
@Service
class OutboxService(private val outboxRepository: OutboxRepository) {
    @Transactional(propagation = Propagation.REQUIRED)  // joins caller tx → commits/rolls back together
    fun publishEvent(event: OutboxEvent) {
        outboxRepository.save(event)
    }
}

@Service
class AuditService(private val auditRepository: AuditRepository) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // independent tx → survives rollback
    fun recordFailure(orderId: Long, reason: String) {
        auditRepository.save(AuditEntry(orderId, reason))
    }
}
```

### Rule of thumb
- **REQUIRED** (default): use when the operation must succeed or fail with its caller
- **REQUIRES_NEW**: use when the operation must persist regardless of caller outcome (audit, logging, metrics)

## Concrete Pattern — Batch Processing with Atomic Error Collection

### The Problem — REQUIRES_NEW Per Row
```kotlin
@Service
class BatchImportService(private val stockRepository: StockLevelRepository) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // BUG: each row in its own tx
    fun importRow(row: CsvRow) {
        val stock = stockRepository.findBySku(row.sku) ?: throw IllegalArgumentException("Unknown SKU: ${row.sku}")
        stock.quantity += row.quantityChange
        stockRepository.save(stock)
    }

    fun importBatch(rows: List<CsvRow>): BatchImportResult {
        var success = 0
        rows.forEach { row ->
            try { importRow(row); success++ }
            catch (_: Exception) { /* silently skip */ }
        }
        return BatchImportResult(success, rows.size - success)
    }
}
```
**Bug**: Each row commits independently. If row 5 of 10 fails, rows 1-4 are already committed — **partial batch** in the database. Caller thinks "4 succeeded" but the batch is inconsistent.

### The Fix — Single Transaction, Collect All Errors, Then Reject
```kotlin
@Service
class BatchImportService(private val stockRepository: StockLevelRepository) {
    @Transactional  // single transaction wraps entire batch
    fun importBatch(rows: List<CsvRow>): BatchImportResult {
        val errors = mutableListOf<String>()
        var success = 0

        rows.forEach { row ->
            val stock = stockRepository.findBySku(row.sku)
            if (stock == null) {
                errors.add("Unknown SKU: ${row.sku}")  // collect, don't throw yet
            } else {
                stock.quantity += row.quantityChange
                stockRepository.save(stock)
                success++
            }
        }

        if (errors.isNotEmpty()) {
            // Throw AFTER processing ALL rows — transaction rolls back everything
            throw BatchImportException("Batch failed with ${errors.size} errors: ${errors.joinToString("; ")}")
        }
        return BatchImportResult(success, 0)
    }
}
```
**Key**: Process ALL rows, collect ALL errors, then throw a single exception. The `@Transactional` rollback undoes all writes atomically. Caller gets a complete error report, not just the first failure.

### Rule of thumb
- **REQUIRES_NEW per item** = partial commits = data inconsistency on failure
- **Single @Transactional** + error collection + throw = atomic all-or-nothing with full diagnostics
- Always process the entire batch before deciding to reject — stopping at first error loses visibility

## Concrete Pattern — Optimistic Locking with Retry

### The Problem — @Version Without Retry
```kotlin
@Entity
class StockLevel(
    @Id @GeneratedValue val id: Long = 0,
    var availableQuantity: Int = 0,
    @Version var version: Long = 0  // optimistic lock
)

@Service
class ReservationService(private val stockRepo: StockLevelRepository) {
    @Transactional
    fun reserve(variantId: Long, quantity: Int) {
        val stock = stockRepo.findByVariantId(variantId)!!
        stock.availableQuantity -= quantity
        stockRepo.save(stock)
        // BUG: concurrent callers get OptimisticLockingFailureException with no retry!
    }
}
```

### The Fix — @Retryable for Automatic Retry
```kotlin
@Configuration
@EnableRetry  // Required! Enables @Retryable proxy
class RetryConfig

@Service
class ReservationService(private val stockRepo: StockLevelRepository) {
    @Transactional
    @Retryable(
        include = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 50)
    )
    fun reserve(variantId: Long, quantity: Int) {
        val stock = stockRepo.findByVariantId(variantId)!!
        if (stock.availableQuantity < quantity) throw InsufficientStockException(...)
        stock.availableQuantity -= quantity
        stockRepo.save(stock)
    }
}
// Requires: implementation("org.springframework.retry:spring-retry")
```

### Rule of thumb
- `@Version` alone prevents data corruption but causes valid requests to fail under contention
- Always pair `@Version` with retry logic — use `@Retryable(include = [OptimisticLockingFailureException::class])`
- Retry must create a fresh transaction (Spring Retry retries at proxy level, before @Transactional)
- Add `@EnableRetry` on a `@Configuration` class

## Anti-Patterns To Flag

- `@Transactional` on controllers by default.
- One transaction that does database writes and then performs slow HTTP calls.
- Catching exceptions inside the transaction and converting them to success-like flows.
- Publishing irreversible side effects before commit.
- Assuming retries are safe without idempotency.
- Using a bigger propagation setting to hide unclear boundaries.

## Advanced Consistency Nuances

- Distinguish duplicate prevention from concurrency control. A unique constraint solves one class of race, not lost updates or write skew.
- Remember that many integrity failures surface on flush or commit, not at the line that changed the entity. Design tests and exception handling accordingly.
- `UnexpectedRollbackException` often means an inner operation marked the transaction rollback-only even though the outer layer tried to return success.
- Isolation levels are database-specific in effect. The same setting on Postgres, MySQL, and SQL Server may protect different anomalies.
- Deadlock and serialization-failure retries belong at a carefully chosen outer boundary. Retrying a half-executed workflow with external side effects is dangerous.
- `@TransactionalEventListener` and `TransactionSynchronization` are phase-sensitive. Choose before-commit, after-commit, or after-rollback behavior deliberately.
- If the workflow crosses service boundaries, distinguish local transaction design from saga or orchestration design. Do not pretend one local transaction can guarantee distributed consistency.
- In reactive or coroutine transaction flows, verify which transaction manager and context propagation model is actually in use. Imperative assumptions often fail there.

## Expert Heuristics

- Start from the invariant, not from the annotation. Ask what must never be observably false to users or downstream systems.
- Prefer database-enforced invariants for uniqueness and impossible states, then use application logic to make violations rare and understandable.
- If a workflow mixes command and query steps, decide whether read-your-write guarantees are required immediately or whether eventual consistency is acceptable.
- If the team wants `REQUIRES_NEW`, ask whether they are isolating audit logging, masking rollback behavior, or compensating for a larger design problem.

## Output Contract

Return these sections:

- `Consistency goal`: the business invariant being protected.
- `Recommended boundary`: where the main transaction starts and ends.
- `Propagation and isolation`: only the settings that matter and why.
- `Idempotency and concurrency`: duplicate handling, locking, and retry safety.
- `External side effects`: what must happen outside the transaction or through outbox-style patterns.
- `Verification`: tests or scenarios that prove rollback, duplicate handling, and conflict behavior.

## Guardrails

- Do not put `@Transactional` on every service method by default.
- Do not recommend distributed 2PC or XA unless the project already uses it and truly requires it.
- Do not ignore the cost of holding database connections during external calls.
- Do not treat duplicate prevention as an application-only concern when the database can enforce it.

## Quality Bar

A good run of this skill turns a vague workflow into explicit consistency boundaries and testable invariants.
A bad run decorates methods with `@Transactional` without modeling failure paths, retries, and side effects.
