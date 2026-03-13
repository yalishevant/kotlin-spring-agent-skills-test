Add production-grade observability to the URL shortener service.

## Requirements

### 1. Structured JSON logging
- Configure Logback for JSON output format (use `logstash-logback-encoder` or equivalent)
- Every log line includes: timestamp, level, logger, message, and MDC fields
- Log each request with: method, path, duration, status code

### 2. Correlation ID propagation
- Generate `X-Request-Id` UUID if not present in incoming request header
- Store in MDC (Mapped Diagnostic Context) so every log line includes it
- Include `X-Request-Id` in all response headers
- Use a servlet filter or Spring interceptor for this

### 3. Micrometer metrics
- `url_shortener_redirects_total` — counter, tagged by HTTP status only. IMPORTANT: do NOT use `shortCode` as a tag — that is high cardinality and will explode Prometheus storage!
- `url_shortener_create_total` — counter for URL creation
- `url_shortener_redirect_duration_seconds` — timer for redirect latency
- `url_shortener_active_urls` — gauge showing count of active short URLs in the database

### 4. Health indicators
- Custom health indicator that checks database connectivity
- Separate liveness and readiness probes if Spring Boot 3.x

### 5. Actuator configuration
- Expose ONLY: `/actuator/health`, `/actuator/prometheus`, `/actuator/info`
- All other actuator endpoints must be disabled/hidden
- Do NOT expose `/actuator/env`, `/actuator/beans`, `/actuator/configprops`

### Tests (REQUIRED)
- Metric test: after 5 redirects, `url_shortener_redirects_total` = 5
- No metric with `shortCode` as tag label (cardinality check)
- Correlation ID test: response contains `X-Request-Id` header
- Correlation ID test: if request sends `X-Request-Id`, same value in response
- Actuator test: `/actuator/health` returns 200
- Actuator test: `/actuator/env` returns 404 (not exposed)
- Health indicator: custom health check returns UP when DB is available

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
