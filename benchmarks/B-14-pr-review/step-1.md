Create a Kotlin + Spring Boot payment service with realistic code that contains subtle bugs. Write the code as if a junior developer wrote it — the bugs should be natural, not obvious.

## Requirements

### Entities
- `Payment` entity — deliberately make this a `data class` (this is Bug #1: breaks equals/hashCode with JPA proxies)
- Fields: id (Long), orderId (String), amount (BigDecimal), currency (String), status (enum: PENDING/COMPLETED/FAILED/REFUNDED), createdAt (Instant)

### Service: PaymentService

Implement with these specific patterns (each contains a planted bug):

**Bug #2**: Put `@Transactional` on a `private` method `private fun processInternal(payment: Payment)` — this won't be proxied.

**Bug #3**: `processPayment()` public method calls `this.saveAuditLog()` where `saveAuditLog()` has `@Transactional(propagation = Propagation.REQUIRES_NEW)`. The self-invocation means REQUIRES_NEW is bypassed.

**Bug #4**: Create a `PaymentRequest` data class DTO with validation annotations WITHOUT `@field:` target:
```kotlin
data class PaymentRequest(
    @NotBlank val orderId: String,  // Bug: should be @field:NotBlank
    @Positive val amount: BigDecimal  // Bug: should be @field:Positive
)
```

**Bug #5**: In SecurityFilterChain, use `permitAll()` on `/api/**` — this is too broad, should be specific paths only.

**Bug #6**: Add `@Cacheable("payments")` on a method that returns a mutable `Payment` entity — cache poisoning risk since callers can modify the cached object.

**Bug #7**: Call a Java library method that can return null (e.g., `java.util.Map.get()`) and assign the result to a non-null Kotlin type without null check.

**Bug #8**: Configure `ObjectMapper` bean manually but forget to register `KotlinModule`:
```kotlin
@Bean
fun objectMapper(): ObjectMapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    // Missing: .registerModule(KotlinModule.Builder().build())
```

**Bug #9**: Add a `@Scheduled(fixedRate = 60000)` method for payment reconciliation without any distributed lock — this will run on ALL instances in a cluster.

**Bug #10**: In the service, load related data in a loop (N+1 pattern in service layer):
```kotlin
fun getPaymentSummaries(orderIds: List<String>): List<PaymentSummary> {
    return orderIds.map { orderId ->
        val payments = paymentRepository.findByOrderId(orderId) // N+1!
        PaymentSummary(orderId, payments.sumOf { it.amount })
    }
}
```

### Controller
- POST /api/payments — create payment
- GET /api/payments/{id} — get payment
- GET /api/payments/summary?orderIds=X,Y,Z — get payment summaries (triggers bug #10)
- POST /api/payments/{id}/refund — refund payment

### Security
- JWT-based authentication (simplified — just the filter chain, not full login flow)
- ADMIN can refund, USER can create and view own payments

### Tests
- Write basic tests that PASS despite the bugs (the bugs are subtle runtime/production issues, not compilation errors)
- At least 5 tests covering the happy paths

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.

IMPORTANT: Write the code naturally. Do NOT add comments indicating bugs. The code should look like a real junior developer wrote it.
