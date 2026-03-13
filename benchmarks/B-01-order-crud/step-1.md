Build a Kotlin + Spring Boot order management service from scratch.

## Requirements

- Gradle Kotlin DSL build with Spring Boot 3.3, Kotlin 1.9+, Spring Data JPA, H2 (test/dev) + PostgreSQL (runtime), Flyway, Jackson, Jakarta Validation

### Entities

- `Order`: id (Long, generated), customerEmail (String, validated), status (enum: CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED), items (one-to-many), totalAmount (BigDecimal, computed), createdAt (Instant), updatedAt (Instant)
- `OrderItem`: id (Long, generated), productName (String), quantity (Int, > 0), unitPrice (BigDecimal, > 0)
- Relationship: Order has many OrderItems (cascade ALL, orphan removal)

### REST API

- `POST /api/orders` — create order with items. Validate: customerEmail is valid email, items non-empty, quantity > 0, unitPrice > 0. Return 201 + created order.
- `GET /api/orders/{id}` — return order with items. Return 404 if not found.
- `GET /api/orders?status={status}&page=0&size=20` — list orders filtered by optional status, paginated.
- `PATCH /api/orders/{id}/cancel` — cancel order. Only allowed if status is CREATED or CONFIRMED. Return 409 if already shipped/delivered/cancelled.

### Error Handling

- Unified error response format for all errors (400 validation, 404 not found, 409 conflict, 500 server error):
  ```json
  {"status": 400, "error": "Validation Error", "message": "...", "details": [...]}
  ```
- Use `@ControllerAdvice` with `@ExceptionHandler`

### Database

- Flyway migration V1 creates both tables with constraints and indexes

### Tests (REQUIRED)

- Unit tests for service logic (OrderService): create, cancel business rules.
- `@WebMvcTest` for controller: test each endpoint, validation errors, not-found, conflict.
- `@DataJpaTest` for repository: test save, find by status, pagination.
- Use backtick method names: `fun `should return 404 when order not found`()`

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
