---
name: integration-resilience-engineer
description: Design resilient HTTP, messaging, and scheduled integrations for Kotlin plus Spring services with explicit timeout budgets, retries, idempotency, circuit breakers, DLQ behavior, and failure observability. Use when integrating with unstable external systems, designing retry logic, handling duplicate delivery, preventing thundering herds, or making message-driven and scheduled workflows safe in production.
---

# Integration Resilience Engineer

Source mapping: Tier 2 high-value skill derived from `Kotlin_Spring_Developer_Pipeline.md` (`SK-12`).

## Mission

Make integrations behave predictably under partial failure, timeouts, retries, and duplicate delivery.
Treat every remote boundary as unreliable and every retry as a potential multiplier of load.

## Read First

- External API or messaging contract, including status codes or message schema.
- SLA, timeout budget, rate-limit rules, and idempotency guarantees.
- Current client configuration, retry logic, circuit breaker settings, and scheduling configuration.
- Business criticality of the operation and acceptable degradation behavior.
- Existing observability around request outcome, retry count, queue depth, and DLQ.

## Design Sequence

1. Classify the operation:
   - read-only or state-changing
   - idempotent or non-idempotent
   - synchronous user path or asynchronous background path
2. Set timeout budgets from the caller's SLO backward.
3. Decide which failures are retryable and which are terminal.
4. Decide where deduplication or idempotency must be enforced.
5. Add circuit breaking, bulkheading, or rate limiting where failure amplification is plausible.
6. Add metrics, tracing, and structured outcome logging.

## HTTP Client Rules

- Set connect and response timeouts explicitly.
- Retry only when the operation is safe to replay or explicitly idempotent.
- Use exponential backoff with jitter, not fixed-interval retries.
- Respect `429`, `Retry-After`, and dependency-specific throttling semantics.
- Distinguish timeout before request reached the server from timeout after side effects may already have happened.
- Map downstream errors into local semantics deliberately. Do not leak raw remote failures by accident.

## Messaging And Scheduling Rules

- Treat consumers as at-least-once unless a stronger guarantee is proven end to end.
- Design deduplication with stable keys and durable storage when duplicate delivery matters.
- Use DLQ or poison-message handling instead of infinite redelivery loops.
- Be explicit about ordering guarantees and partition-key strategy.
- For scheduled jobs, prevent overlap with distributed locking when multiple nodes can run the same task.
- Account for clock skew, long-running jobs, and partial completion when scheduling recurring work.

## Advanced Failure Modes

- Retries at multiple layers multiply explosively. One client retry policy plus platform retry plus queue retry can create storms.
- Circuit breakers without sensible fallback or operator visibility often only change the failure shape.
- Fallback data can become a stale correctness problem, not merely a degraded UX.
- A timeout on a state-changing call may leave the caller uncertain whether the remote side succeeded. This is an idempotency design problem, not just a timeout problem.
- DLQ without replay discipline creates permanent operational debt.
- Exactly-once is usually a system property claim, not an individual code property. Be skeptical and precise.
- If message schema evolution is expected, plan backward-compatible consumers before the first change arrives.

## Boundary-Specific Nuances

- HTTP connection pooling, DNS caching, TLS handshakes, and per-host limits can dominate latency long before business logic does. Client-level transport settings matter.
- A `202 Accepted` plus asynchronous completion model may be safer than a synchronous state-changing call when dependency latency is unpredictable.
- Outbox and inbox patterns solve different halves of reliability. Do not talk about one as if it covers both publish and consume idempotency.
- Kafka rebalance behavior, consumer lag, and partition skew are part of resiliency, not only throughput tuning.
- Schedulers need both overlap prevention and business-level idempotency. A distributed lock alone does not make the job semantically safe.

## Expert Heuristics

- Put a retry budget around each integration path. If retries exceed the budget, fail fast and surface the degradation clearly.
- If a dependency is slow but not down, circuit breaking may be less useful than tight timeouts plus bulkheads.
- If the caller cannot safely determine whether a remote write succeeded, design a reconciliation path, not only a retry policy.
- If a queue consumer is critical, define operator behavior for DLQ replay before the first poison message appears.

## Concrete Pattern — Resilient HTTP Client with Timeout, Retry, and Circuit Breaker

### The Problem
```kotlin
// BUG: no timeouts, no retry, no failure handling
@Service
class PaymentGatewayClient(private val restTemplate: RestTemplate) {
    fun charge(orderId: Long, amount: BigDecimal): PaymentResult {
        return restTemplate.postForObject(                     // BUG: default infinite timeout
            "https://payments.example.com/charge",
            ChargeRequest(orderId, amount),
            PaymentResult::class.java
        )!!                                                    // BUG: NPE if 204 No Content
    }
}
```

Without explicit timeouts, a slow downstream blocks the calling thread indefinitely. Without retries for transient failures, a single network blip fails the user. Without circuit breaking, a failing dependency drags the entire service down.

### The Fix — RestClient with Resilience4j
```kotlin
@Configuration
class PaymentClientConfig {
    @Bean
    fun paymentRestClient(): RestClient = RestClient.builder()
        .baseUrl("https://payments.example.com")
        .requestFactory(ClientHttpRequestFactorySettings(
            connectTimeout = Duration.ofSeconds(2),
            readTimeout = Duration.ofSeconds(5)
        ).toRequestFactory())
        .build()
}

@Service
class PaymentGatewayClient(
    private val restClient: RestClient,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val retryRegistry: RetryRegistry
) {
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment")
    private val retry = retryRegistry.retry("payment")

    fun charge(orderId: Long, amount: BigDecimal): PaymentResult {
        return Decorators.ofSupplier {
            restClient.post()
                .uri("/charge")
                .body(ChargeRequest(orderId, amount))
                .retrieve()
                .body(PaymentResult::class.java)
                ?: throw PaymentException("Empty response from payment gateway")
        }
        .withRetry(retry)                    // retries transient 5xx/timeout
        .withCircuitBreaker(circuitBreaker)   // opens after threshold failures
        .decorate()
        .get()
    }
}
```

```yaml
# application.yml — Resilience4j config
resilience4j:
  retry:
    instances:
      payment:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2     # 500ms, 1s, 2s
        retry-exceptions:
          - java.net.SocketTimeoutException
          - org.springframework.web.client.HttpServerErrorException
  circuitbreaker:
    instances:
      payment:
        failure-rate-threshold: 50
        slow-call-duration-threshold: 3s
        sliding-window-size: 10
        wait-duration-in-open-state: 30s
```

### Common mistakes
- No explicit connect/read timeouts — threads block indefinitely on slow dependencies
- Retrying non-idempotent POST without idempotency keys — can duplicate charges
- Retrying 4xx errors — client errors won't fix themselves on retry
- Adding retries at multiple layers (client + gateway + queue) — multiplicative retry storms

## Concrete Pattern — Spring Retry Setup with @EnableRetry and @Retryable

### The Problem — spring-retry Dependency Without Activation
```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-starter-aop")
}

// GatewayClient.kt — no @Retryable, no retry behavior
@Service
class GatewayClient(private val restClient: RestClient) {
    fun charge(amount: BigDecimal, idempotencyKey: String): GatewayChargeResponse {
        // BUG: no retry on transient failures — single failure = user-visible error
        return restClient.post()
            .uri("/api/charges")
            .body(mapOf("amount" to amount.toString()))
            .retrieve()
            .body(GatewayChargeResponse::class.java)
            ?: throw GatewayException("Empty response")
    }
}

// AppConfig.kt — no @EnableRetry
@Configuration
@EnableCaching
class AppConfig  // BUG: missing @EnableRetry — @Retryable silently does nothing
```
Having `spring-retry` on the classpath does **nothing** without `@EnableRetry` on a `@Configuration` class. Similarly, `@EnableRetry` does nothing without `@Retryable` on the methods that should be retried. **Both are required together.**

### The Fix — Three-Part Setup
```kotlin
// 1. Dependencies in build.gradle.kts
dependencies {
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-starter-aop")  // required for retry proxy
}

// 2. @EnableRetry on a @Configuration class
@Configuration
@EnableCaching
@EnableRetry  // activates @Retryable proxy infrastructure
class AppConfig

// 3. @Retryable on the method that calls external service
@Service
class GatewayClient(private val restClient: RestClient) {
    @Retryable(
        retryFor = [GatewayTimeoutException::class, GatewayException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, multiplier = 2.0)
    )
    fun charge(amount: BigDecimal, idempotencyKey: String): GatewayChargeResponse {
        // Now retries up to 3 times with exponential backoff on gateway failures
        return restClient.post()
            .uri("/api/charges")
            .header("X-Idempotency-Key", idempotencyKey)
            .body(mapOf("amount" to amount.toString()))
            .retrieve()
            .body(GatewayChargeResponse::class.java)
            ?: throw GatewayException("Empty response")
    }

    @Recover
    fun chargeFallback(ex: GatewayTimeoutException, amount: BigDecimal, idempotencyKey: String): GatewayChargeResponse {
        throw GatewayTimeoutException("Gateway timeout after retries: ${ex.message}")
    }
}
```

### Checklist — Spring Retry requires ALL THREE:
1. **Dependency**: `spring-retry` + `spring-boot-starter-aop` in build.gradle.kts
2. **Enable annotation**: `@EnableRetry` on a `@Configuration` class
3. **Method annotation**: `@Retryable` on the target method with retry conditions

Missing any one of these three makes retry silently non-functional.

### Common mistakes
- Adding `spring-retry` dependency without `@EnableRetry` — retry infrastructure never created
- Adding `@EnableRetry` without putting `@Retryable` on any method — nothing to retry
- Using `@Retryable` without `spring-boot-starter-aop` dependency — proxy cannot be created
- Retrying non-idempotent operations without idempotency keys — creates duplicates
- Not specifying `retryFor` — defaults to retrying all exceptions including validation errors

## Output Contract

Return these sections:

- `Failure model`: what can go wrong at this boundary.
- `Resilience policy`: timeouts, retries, circuit breaking, bulkheads, fallback, and deduplication.
- `Idempotency rule`: how duplicate or uncertain delivery is handled.
- `Operational signals`: which metrics, logs, traces, and alerts are required.
- `Minimal implementation plan`: client, listener, scheduler, or config changes to make.
- `Verification`: chaos, integration, or replay tests that prove the design.

## Guardrails

- Do not retry non-idempotent operations by default.
- Do not add retries without jitter and a retry budget.
- Do not use fallbacks that silently violate business invariants.
- Do not leave outcome ambiguity unexplained for timed-out state changes.
- Do not build resilience policies that operators cannot observe.

## Quality Bar

A good run of this skill produces an integration policy that fails in controlled, explainable ways.
A bad run adds retries and circuit breakers everywhere while increasing duplication, latency, and operational confusion.
