The following production incident report has been filed. You are the on-call engineer.

## Alert

Error rate on `POST /api/products/{id}/reserve` jumped from 0.1% to 47% at 14:32 UTC.

## Logs

```
2024-03-15 14:32:15.123 ERROR [req-id=a1b2c3] o.s.w.s.m.m.a.ExceptionHandlerExceptionResolver - Resolved [org.springframework.orm.ObjectOptimisticLockingFailureException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)]
2024-03-15 14:32:15.124 ERROR [req-id=d4e5f6] o.s.w.s.m.m.a.ExceptionHandlerExceptionResolver - Resolved [org.springframework.orm.ObjectOptimisticLockingFailureException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)]
2024-03-15 14:32:15.125 ERROR [req-id=g7h8i9] o.s.w.s.m.m.a.ExceptionHandlerExceptionResolver - Resolved [org.springframework.orm.ObjectOptimisticLockingFailureException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)]
... (200+ similar errors per second)
2024-03-15 14:31:58.001 INFO  Deployment v2.14.0 started
2024-03-15 14:32:00.000 INFO  Deployment v2.14.0 complete
2024-03-15 14:32:01.000 INFO  Flash sale started for product SKU-FLASH-001
```

## Context

A new feature was deployed at 14:31 that added a "flash sale" endpoint. The flash sale went live at 14:32, triggering ~100 simultaneous reservation requests for the SAME product.

## Your Task

### 1. Immediate Mitigation (first 5 minutes)
What do you do RIGHT NOW to restore service? Choose from:
- Rollback deployment
- Feature flag to disable flash sale
- Rate limit the reserve endpoint
- Add retry with backoff

Explain your choice. The correct answer is NOT "deploy a code fix" -- that takes too long during an active incident.

### 2. Root Cause Analysis
- Why is optimistic locking failing at 47%? (100 concurrent reservations on the same row = extreme contention)
- Why did this work in testing but fail in production?
- What is the relationship between concurrency level and optimistic locking failure rate?

### 3. Fix Option A: Retry Strategy
Implement retry logic for `ObjectOptimisticLockingFailureException`:
- Catch `ObjectOptimisticLockingFailureException` in the reserve method
- Retry up to 3 times with exponential backoff (100ms, 200ms, 400ms) + jitter
- If all retries fail, return 409 Conflict with a clear message

### 4. Fix Option B: Pessimistic Locking
Implement an alternative using pessimistic locking:
- Add a repository method with `@Lock(LockModeType.PESSIMISTIC_WRITE)` and `@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))`
- This eliminates optimistic locking failures at the cost of serialized access

### 5. Load Test
Add a test that simulates 50 concurrent reservation requests for the same product:
- All 50 should eventually succeed (with retries or pessimistic lock)
- Final reservedQuantity should be exactly 50
- No data inconsistency (reserved never exceeds stock)

### 6. Postmortem
Write a `POSTMORTEM.md` with:
- **Timeline**: When each event happened
- **Impact**: What users experienced
- **Root Cause**: Technical explanation of why optimistic locking fails under high contention
- **What Went Well**: What worked correctly (optimistic locking DID prevent data corruption)
- **What Went Wrong**: Flash sale wasn't load-tested with realistic concurrency
- **Action Items**: At least 3 concrete follow-ups with owners

Implement BOTH fix options (in separate service methods or behind a flag) and the load test.

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
