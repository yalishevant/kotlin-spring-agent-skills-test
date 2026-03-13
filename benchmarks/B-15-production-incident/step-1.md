Build a Kotlin + Spring Boot inventory service.

## Requirements

### Entity
- `Product`: id (Long, generated), sku (String, unique), name (String), stockQuantity (Int), reservedQuantity (Int), lastRestockedAt (Instant?), version (Long — `@Version` for optimistic locking)

### Endpoints
- `POST /api/products` — create product with initial stock
- `GET /api/products/{id}/availability` — returns `{"sku": "...", "available": stockQuantity - reservedQuantity, "stockQuantity": N, "reservedQuantity": N}`
- `POST /api/products/{id}/reserve` — reserve N units. Body: `{"quantity": N}`. Returns 422 if insufficient available quantity. Uses optimistic locking.
- `POST /api/products/{id}/release` — release N reserved units. Body: `{"quantity": N}`. Returns 422 if releasing more than reserved.
- `POST /api/products/{id}/restock` — add stock. Body: `{"quantity": N}`. Updates `lastRestockedAt`.

### Business Rules
- `availableQuantity = stockQuantity - reservedQuantity`
- Cannot reserve more than available
- Cannot release more than currently reserved
- All stock operations use optimistic locking (`@Version`)

### Tests
- Reserve: success, insufficient quantity
- Release: success, over-release
- Availability: correct calculation

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
