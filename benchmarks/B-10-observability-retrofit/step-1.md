Build a simple Kotlin + Spring Boot URL shortener service.

## Requirements

### Endpoints
- `POST /api/urls` — accepts `{"originalUrl": "https://example.com"}`, returns `{"shortCode": "abc123", "shortUrl": "http://localhost:8080/abc123"}`
- `GET /{shortCode}` — redirects (302) to the original URL. Returns 404 if shortCode not found.
- `GET /api/urls/{shortCode}/stats` — returns `{"shortCode": "abc123", "originalUrl": "...", "clickCount": 42, "createdAt": "..."}`

### Technical
- Entity: `ShortUrl` (id, shortCode unique index, originalUrl, clickCount, createdAt)
- ShortCode generation: 6-character alphanumeric random string, check for collisions
- H2 database, Flyway migration
- Gradle Kotlin DSL, Spring Boot 3.x, Kotlin

### Tests
- Unit: shortCode generation is 6 chars, alphanumeric
- Slice: `@WebMvcTest` for controller endpoints
- Integration: create URL → redirect → check stats shows clickCount=1

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
