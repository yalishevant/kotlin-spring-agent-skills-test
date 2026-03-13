Refactor the Kotlin code to be fully idiomatic Kotlin.

## Requirements

1. Use `sealed class` or `sealed interface` for operation results instead of exceptions for expected failures:
   - `ProductResult.Success(product: Product)`
   - `ProductResult.NotFound(id: Long)`
   - `ProductResult.AlreadyInactive(id: Long)`
2. Use `@JvmInline value class ProductId(val value: Long)` for type-safe IDs
3. Use `when` expressions where appropriate (especially in controller mapping of sealed results to HTTP responses)
4. Extract DTO mapping to extension functions: `Product.toResponse()`, `CreateProductRequest.toEntity()`
5. Ensure null safety throughout — handle nullability idiomatically
6. Replace any remaining `lateinit var` with constructor injection or `lazy`
7. Use Kotlin scope functions where they improve readability (but don't overuse)

All tests must still pass. Add new tests for:
- Sealed class result patterns (Success, NotFound, AlreadyInactive)
- Value class serialization (ProductId serializes as plain Long)

Test count must be >= the step 2 test count (tests were added, not removed).

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
