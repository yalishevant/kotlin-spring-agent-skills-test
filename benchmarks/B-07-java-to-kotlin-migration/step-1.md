Create a small Spring Boot + **Java** service (yes, Java, not Kotlin yet) for a product catalog.

## Requirements

### Entity
- Java entity: `Product` with Lombok `@Data`, `@Entity`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Fields: id (Long, generated), name (String), description (String), price (BigDecimal), category (enum: ELECTRONICS, CLOTHING, FOOD, BOOKS), active (boolean), createdAt (LocalDateTime)

### Service
- `ProductService` with:
  - `findAll()` — returns all active products
  - `findById(Long id)` — returns product or throws `ProductNotFoundException`
  - `create(CreateProductRequest)` — creates product
  - `deactivate(Long id)` — sets active=false
  - `@Transactional` on write methods
  - `@Cacheable("products")` on `findById`

### Controller
- Standard REST CRUD: GET /api/products, GET /api/products/{id}, POST /api/products, DELETE /api/products/{id}

### Tests
- JUnit 5 + Mockito for service tests
- `@WebMvcTest` for controller

### Build
- Gradle Kotlin DSL with Java source set + Lombok annotation processor
- Spring Boot 3.x, H2 for tests

The project must compile with `./gradlew compileJava` and all tests must pass with `./gradlew test`.
