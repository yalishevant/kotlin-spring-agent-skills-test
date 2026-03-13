Migrate the entire product catalog service from Java to Kotlin.

## Requirements

1. Convert ALL Java classes to Kotlin files
2. Remove Lombok completely — replace with Kotlin equivalents:
   - `@Data` → Kotlin properties
   - `@Builder` → default/named parameters
   - `@NoArgsConstructor`/`@AllArgsConstructor` → Kotlin constructors
   - `@Slf4j` → `companion object` with `LoggerFactory`
3. Make the code Kotlin-idiomatic: use `val` over `var`, null-safe operators, expression bodies where appropriate
4. Remove Lombok from build dependencies
5. Ensure all Spring annotations (`@Transactional`, `@Cacheable`, etc.) still work correctly after migration
6. Update the test framework to be Kotlin-idiomatic
7. Use backtick test method names: `` fun `should return 404 when product not found`() ``

All tests must pass after migration. Behavior must be identical to the Java version.

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
