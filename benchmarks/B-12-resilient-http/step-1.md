Build a Kotlin + Spring Boot price aggregator service that fetches prices from 3 external suppliers with full resilience patterns.

## Requirements

### External Suppliers (mock with WireMock stubs in tests)
- **Supplier A**: responds in ~100ms, reliable
- **Supplier B**: responds in ~500ms, sometimes returns 500 errors
- **Supplier C**: responds in ~2000ms, frequently times out

### Endpoint
- `GET /api/prices/{productId}` — returns the best (lowest) price from all suppliers that respond successfully

Response format:
```json
{
  "productId": "PROD-123",
  "bestPrice": {"supplierId": "A", "price": 29.99, "currency": "USD"},
  "allPrices": [
    {"supplierId": "A", "price": 29.99, "currency": "USD"},
    {"supplierId": "B", "price": 34.50, "currency": "USD"}
  ],
  "failedSuppliers": ["C"],
  "fetchedAt": "2024-01-15T10:30:00Z"
}
```

### Resilience Requirements

1. **Per-supplier timeout**: 1 second maximum. If a supplier doesn't respond in 1s, skip it.
2. **Retry**: Up to 2 retries on 5xx errors, with exponential backoff + jitter (not fixed delay!)
3. **Circuit breaker**: Open after 5 consecutive failures, half-open after 30 seconds. When open, don't even attempt the call.
4. **Fallback**: If a supplier is unavailable (timeout, error, circuit open), exclude it from results — don't fail the whole request.
5. **Total failure**: If ALL suppliers fail, return 503 with a meaningful error message.

### Metrics
- Per-supplier counters: `price_fetch_success_total{supplier="A"}`, `price_fetch_failure_total{supplier="A"}`
- Per-supplier timer: `price_fetch_duration_seconds{supplier="A"}`

### Tests with WireMock (REQUIRED)

1. **All succeed**: All 3 suppliers respond → lowest price returned
2. **Partial timeout**: Supplier C has 3-second delay stub → result from A and B only, C in `failedSuppliers`
3. **Retry success**: Supplier B returns 500 first time, 200 second time → B included in results
4. **All fail**: All 3 stubs return 500 → endpoint returns 503
5. **Circuit breaker**: After 5 failures for Supplier C, next call doesn't invoke the stub (verify with WireMock request count)
6. **Jitter verification**: Retry delays are not identical (statistical check — run multiple retries and verify delays vary)

### Technical
- Gradle Kotlin DSL, Spring Boot 3.x, Kotlin
- Use Resilience4j or Spring Retry for resilience patterns
- Use WireMock for integration tests
- Concurrent supplier calls (don't call sequentially — use coroutines or CompletableFuture)

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
