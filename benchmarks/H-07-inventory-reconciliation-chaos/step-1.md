This Kotlin + Spring Boot inventory management service compiles and its smoke tests pass, but the warehouse team has filed critical incident reports.

## Incident symptoms

- Concurrent stock reservation requests occasionally allow more items to be reserved than available, leading to overselling.
- The admin stock write-off endpoint appears to have no access control — any authenticated user can write off inventory.
- Product catalog listings are extremely slow when products have many variants (SKUs), and the database team reports thousands of queries per page load.
- The batch CSV stock adjustment import sometimes partially commits rows before failing, leaving inventory counts in an inconsistent state.
- A test environment connected to the wrong external pricing service because configuration binding silently fell back to defaults.
- Inventory adjustment requests with invalid quantities get through to the service layer instead of being rejected at the API boundary.

## Your task

1. Fix the production code so that all reported issues are resolved.
2. Keep the existing smoke tests passing.
3. Additional verification will check runtime behavior around concurrent reservations, authorization enforcement, query performance, batch atomicity, configuration safety, and input validation.

Do not delete tests or weaken assertions. Fix the production code only.

Run `./gradlew test` to verify the visible test suite.
