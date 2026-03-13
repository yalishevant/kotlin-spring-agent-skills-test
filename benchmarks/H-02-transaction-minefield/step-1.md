This Spring Boot project compiles and basic tests pass. However, there are verification tests in `PaymentVerificationTest.kt` that are `@Disabled` because they fail.

Your task:
1. Remove all `@Disabled` annotations from the verification tests
2. Analyze WHY each verification test fails
3. Fix the production code so ALL tests pass (both basic and verification)

Do NOT delete or modify the verification test assertions. Fix the production code only.

Run `./gradlew test` to verify all tests pass.
