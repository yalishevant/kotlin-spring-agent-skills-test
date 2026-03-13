This Kotlin + Spring Boot payment processing service compiles and its smoke tests pass, but the operations team has filed critical incident reports.

## Incident symptoms

- The `POST /api/payments` endpoint returns HTTP 200 instead of 201 for successful payment creation, which confuses API consumers that rely on standard HTTP semantics.
- API consumers report that requests with blank customer names or negative amounts are accepted instead of being rejected, even though validation annotations appear to be present on the request DTO.
- When a payment with the same idempotency key is submitted twice, the second request produces a 500 Internal Server Error with SQL constraint details leaked in the response body, instead of a clean 409 Conflict response.
- The external payment gateway sometimes times out, and when it does, the entire request hangs for 30+ seconds before eventually returning a generic 500 error — the error response format differs from the rest of the API.
- Transient gateway failures (brief network blips, momentary overload) immediately fail the payment permanently. The `spring-retry` library is already on the classpath, but retry behavior is not active — recoverable errors should be retried automatically before failing.
- The payment lookup endpoint is slow under repeated calls for the same payment, despite `@Cacheable` being present on the service method. Investigation suggests caching is not actually working.
- The service connected to a test payment gateway in production because the gateway URL configuration silently fell back to a default value instead of failing at startup.

## Your task

1. Fix the production code so that all reported issues are resolved.
2. Keep the existing smoke tests passing.
3. Additional verification will check runtime behavior around API status codes, error handling, resilience, configuration safety, and caching.

Do not delete tests or weaken assertions. Fix the production code only.

Run `./gradlew test` to verify the visible test suite.
