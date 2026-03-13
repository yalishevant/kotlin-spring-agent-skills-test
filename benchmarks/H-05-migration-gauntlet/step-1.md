This Spring Boot project compiles and basic tests pass. However, there are verification tests in `MigrationVerificationTest.kt` that are `@Disabled` because they fail.

The project uses Flyway for database migrations. The current migration renames a column (`product_name` → `display_name`).

Your task:
1. Remove all `@Disabled` annotations from the verification tests
2. Analyze WHY each verification test fails
3. Fix the migration scripts and production code so ALL tests pass

Do NOT delete or modify the verification test assertions. Fix the migration scripts and production code only.

Run `./gradlew test` to verify all tests pass.
