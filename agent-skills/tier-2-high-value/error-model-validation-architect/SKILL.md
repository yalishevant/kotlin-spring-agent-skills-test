---
name: error-model-validation-architect
description: Design and implement consistent API validation and error-handling behavior for Kotlin plus Spring services. Use when defining error payloads, mapping framework and domain exceptions, standardizing HTTP status codes, adding `@ControllerAdvice`, preventing internal-detail leakage, or ensuring clients can rely on stable machine-readable error semantics across endpoints.
---

# Error Model Validation Architect

Source mapping: Tier 2 high-value skill derived from `Kotlin_Spring_Developer_Pipeline.md` (`SK-07`).

## Mission

Turn ad-hoc exception handling into a deliberate public contract.
Treat validation and error mapping as part of API design, not a post-processing afterthought.

## Inputs To Read

- Endpoint contracts and sample payloads.
- Existing exception classes and error payloads.
- Validation annotations, custom validators, and business rule failures.
- Current `@ControllerAdvice`, security exception handling, and framework defaults.
- Logging and observability conventions, especially correlation ids and PII policy.

## Design The Error Taxonomy

Separate at least these categories:

- malformed request or unreadable JSON
- transport-level validation failure
- not found
- conflict or concurrency failure
- business rule rejection
- authentication and authorization failure
- downstream dependency failure
- unexpected internal failure

Do not collapse all non-success outcomes into one generic error envelope.

## Status Code Rules

- Use `400` for malformed input, type mismatch, or invalid transport shape.
- Use `401` for authentication failure and `403` for authorization failure.
- Use `404` when the addressed resource is absent.
- Use `409` for state conflicts, optimistic locking failures, duplicate idempotency keys, and uniqueness races when conflict semantics matter.
- Use `422` when the payload is structurally valid but violates business rules.
- Use `429`, `502`, `503`, or `504` deliberately when gateway or dependency behavior is part of the contract.
- Reserve `500` for genuinely unexpected internal failures.

## Advanced Contract Decisions

- Separate machine-readable error code from human-readable message. Clients should not parse prose.
- Decide whether field-level validation errors should be aggregated or first-failure only. Make the rule consistent.
- Preserve nested field paths for complex payloads and collections.
- Decide whether localization is a server concern, client concern, or documentation-only concern.
- Standardize correlation or trace identifiers in the error payload only if the platform can produce them consistently.
- Decide how unknown fields, enum mismatches, and unreadable date formats surface. These are common client-integration pain points.
- Map downstream failures carefully. Not every remote `500` should become your own `500`.
- If using RFC 7807 Problem Details, decide which extensions are stable parts of the contract and which are internal.

## Validation Rules

- Keep transport validation separate from domain validation even if both ultimately reject the request.
- Use Kotlin `@field:` targets for Bean Validation annotations on DTO constructor properties.
- Prefer dedicated validators or domain rules over abusing regex annotations for business semantics.
- Validate required configuration or invariants at startup when they are not truly request-scoped.

## Framework Exception Coverage

- Cover `MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, missing-parameter exceptions, and unsupported media-type cases explicitly if the API claims a stable error contract.
- Decide how security exceptions participate in the shared error model. `AuthenticationEntryPoint` and `AccessDeniedHandler` often need explicit alignment with controller advice.
- Decide how async, scheduled, and messaging failures are reported differently from HTTP failures. One global envelope does not fit every boundary.
- If the platform uses Problem Details, verify framework-generated problem payloads do not diverge from custom ones during upgrades.

## Expert Heuristics

- Let validation errors help the client recover, but let internal logs help operators diagnose. These are different audiences and should not share the same detail level.
- If the service is public, keep error-code taxonomy versionable and stable even when internal exception types change.
- When multiple validation layers reject the same request, choose the most user-actionable signal instead of stacking redundant errors.
- If a downstream dependency failure is part of the business path, decide whether the contract should expose dependency semantics or normalize them to your own domain.

## Concrete Pattern — Kotlin DTO Validation with @field: Targets

### The Problem
```kotlin
// BROKEN: @NotBlank applies to the getter, not the constructor parameter
data class CreateOrderRequest(
    @NotBlank val customerName: String,  // validation may not trigger
    @Email val email: String
)
```
In Kotlin data classes, annotations on constructor parameters target the parameter by default. Spring's `@Valid` validates **fields** and **getters**, not constructor parameters. Without `@field:`, validation annotations may be silently ignored.

### The Fix
```kotlin
data class CreateOrderRequest(
    @field:NotBlank val customerName: String,
    @field:Email val email: String,
    @field:Size(min = 1, max = 100) val notes: String? = null
)
```

### In Controller
```kotlin
@PostMapping("/orders")
fun createOrder(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<Order> { ... }
```

### Status Code Mapping
```kotlin
@RestControllerAdvice
class ErrorHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ErrorResponse("VALIDATION_ERROR", errors))
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(ex: EntityNotFoundException) =
        ResponseEntity.status(404).body(ErrorResponse("NOT_FOUND", listOf(ex.message ?: "Not found")))

    // 409 for business conflicts (duplicate, state violation)
    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(ex: IllegalStateException) =
        ResponseEntity.status(409).body(ErrorResponse("CONFLICT", listOf(ex.message ?: "Conflict")))
}

data class ErrorResponse(val code: String, val errors: List<String>)
```

## Concrete Pattern — DataIntegrityViolationException to 409 Conflict

### The Problem
```kotlin
@Service
class UserService(private val userRepository: UserRepository) {
    fun register(request: RegisterRequest): User {
        // If email already exists, Spring throws DataIntegrityViolationException
        // Without explicit handling → 500 Internal Server Error with SQL details leaked
        return userRepository.save(User(email = request.email, name = request.name))
    }
}
```
When a unique constraint is violated, Spring wraps the database exception in `DataIntegrityViolationException`. Without explicit handling, the client sees a 500 error with internal SQL details — wrong status code, information leak.

### The Fix — Handle at Two Layers
```kotlin
// Layer 1: Service — check before save for friendly message
@Service
class UserService(private val userRepository: UserRepository) {
    fun register(request: RegisterRequest): User {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException("User with email '${request.email}' already exists")
        }
        return userRepository.save(User(email = request.email, name = request.name))
    }
}

// Layer 2: ControllerAdvice — catch constraint violation as safety net (race condition)
@RestControllerAdvice
class ErrorHandler {
    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicate(ex: DuplicateResourceException) =
        ResponseEntity.status(409).body(ErrorResponse("DUPLICATE", listOf(ex.message ?: "Resource already exists")))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConstraintViolation(ex: DataIntegrityViolationException) =
        ResponseEntity.status(409).body(ErrorResponse("CONFLICT", listOf("Resource conflict — duplicate or constraint violation")))
}
```

### Why both layers
- The service-level check provides a **friendly, specific message** ("email already exists")
- The `@ControllerAdvice` handler catches **race conditions** where two requests pass the `existsBy` check simultaneously
- The database `@Table(uniqueConstraints = ...)` is the ultimate safety net — never rely on application-only checks

### Common mistakes
- Not handling `DataIntegrityViolationException` at all — client sees 500 with SQL fragments
- Only checking at application level without database constraint — race condition allows duplicates
- Only relying on database constraint without application check — error message is cryptic
- Catching `DataIntegrityViolationException` and returning 400 — it's a 409 Conflict, not bad input

## Concrete Pattern — Gateway Exception Rethrow from Service Layer

### The Problem
```kotlin
@Service
class PaymentService(private val gatewayClient: GatewayClient) {
    @Transactional
    fun createPayment(request: CreatePaymentRequest): PaymentResponse {
        val payment = paymentRepository.save(Payment(request.customerName, request.amount))
        try {
            val result = gatewayClient.charge(request.amount, request.idempotencyKey)
            payment.status = PaymentStatus.COMPLETED
            payment.gatewayTransactionId = result.transactionId
        } catch (e: Exception) {
            // BUG: catches EVERYTHING including GatewayTimeoutException
            payment.status = PaymentStatus.FAILED
            payment.failureReason = e.message
        }
        paymentRepository.save(payment)
        return toResponse(payment)
    }
}
```
The blanket `catch (e: Exception)` swallows `GatewayTimeoutException`. The `@RestControllerAdvice` handler that maps it to 502/504 **never fires** because the exception is consumed in the service layer. The client sees 200/201 with a FAILED payment instead of a proper 502/504 error.

### The Fix — Rethrow Gateway-Level Exceptions
```kotlin
@Service
class PaymentService(private val gatewayClient: GatewayClient) {
    @Transactional
    fun createPayment(request: CreatePaymentRequest): PaymentResponse {
        val payment = paymentRepository.save(Payment(request.customerName, request.amount))
        try {
            val result = gatewayClient.charge(request.amount, request.idempotencyKey)
            payment.status = PaymentStatus.COMPLETED
            payment.gatewayTransactionId = result.transactionId
        } catch (e: GatewayTimeoutException) {
            payment.status = PaymentStatus.FAILED
            payment.failureReason = e.message
            paymentRepository.save(payment)
            throw e  // rethrow — let @RestControllerAdvice map to 502/504
        } catch (e: GatewayException) {
            payment.status = PaymentStatus.FAILED
            payment.failureReason = e.message
            paymentRepository.save(payment)
            throw e  // rethrow — let @RestControllerAdvice map to 502
        } catch (e: Exception) {
            payment.status = PaymentStatus.FAILED
            payment.failureReason = e.message
        }
        paymentRepository.save(payment)
        return toResponse(payment)
    }
}
```

### Key rule
When a service calls an external gateway/API and catches exceptions to record failure state, it must **rethrow gateway-level exceptions** (timeout, circuit open, connection refused) so the `@RestControllerAdvice` can map them to proper HTTP status codes (502/504). Only catch and swallow **business-level** failures (validation, insufficient funds) that should not produce gateway error codes.

### The matching `@RestControllerAdvice` handler
```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(GatewayTimeoutException::class)
    fun handleGatewayTimeout(ex: GatewayTimeoutException) =
        ResponseEntity.status(504).body(ErrorResponse(504, "Gateway Timeout", "Downstream service timed out"))

    @ExceptionHandler(GatewayException::class)
    fun handleGatewayError(ex: GatewayException) =
        ResponseEntity.status(502).body(ErrorResponse(502, "Bad Gateway", "Downstream service error"))
}
```

### Common mistakes
- Blanket `catch (e: Exception)` that swallows gateway errors — controller advice never fires, client sees 200/201 instead of 502/504
- Catching `GatewayTimeoutException` without rethrowing — status code mapping becomes invisible
- Not saving the payment failure state before rethrowing — use `save()` before `throw` to persist the FAILED status

## Output Contract

Return these sections:

- `Error taxonomy`: the categories and stable codes.
- `Status mapping`: which exception or failure type maps to which status and why.
- `Payload shape`: the fields that belong in every error.
- `Framework coverage`: which framework exceptions must be handled explicitly.
- `Minimal implementation plan`: advice class, exception types, and tests to add.
- `Logging rule`: what to log server-side versus what to expose to clients.

## Guardrails

- Do not leak stack traces, SQL fragments, class names, tokens, or secrets to clients.
- Do not use one generic message for all failures if clients need actionable categories.
- Do not map every domain failure to `400`.
- Do not let security exceptions bypass the common contract unintentionally.
- Do not rely on the framework default error shape if the service claims to have a stable API contract.

## Quality Bar

A good run of this skill gives clients a predictable error language and gives operators enough server-side detail to debug safely.
A bad run produces a pretty error JSON that still conflates malformed input, business rejection, and internal failure.
