Upgrade this project from Spring Boot 2.7.18 to Spring Boot 3.2.x.

## Required Changes

Handle ALL breaking changes:

1. **Namespace migration**: `javax.*` → `jakarta.*` for ALL imports:
   - `javax.persistence.*` → `jakarta.persistence.*`
   - `javax.validation.*` → `jakarta.validation.*`
   - `javax.servlet.*` → `jakarta.servlet.*` (if any)
2. **Security migration**: Remove `WebSecurityConfigurerAdapter`, replace with `SecurityFilterChain` @Bean method using the lambda DSL
3. **Remove legacy config**: Delete `spring.config.use-legacy-processing=true`
4. **Remove explicit Hibernate dialect**: Hibernate 6 auto-detects the dialect
5. **Update Kotlin**: Upgrade to Kotlin 1.9.x (compatible with Boot 3.x)
6. **Update Gradle plugins**: Spring Boot Gradle plugin 3.2.x, Kotlin Gradle plugin 1.9.x
7. **Fix deprecations**: Address any new deprecation warnings in Spring Boot 3.2

## Deliverables

1. Updated `build.gradle.kts` with all version changes
2. All source files updated with correct imports
3. Security configuration migrated to bean-based style
4. All tests pass on Spring Boot 3.2
5. A `MIGRATION_CHECKLIST.md` document listing every change made, why it was needed, and how to verify it (at least 5 items)

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
