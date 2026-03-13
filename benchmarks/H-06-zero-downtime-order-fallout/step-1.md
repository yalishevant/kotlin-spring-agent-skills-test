This Kotlin + Spring Boot order service compiles and its smoke tests pass, but the current rollout is failing in production.

## Incident symptoms

- Some `PATCH /api/orders/{id}` requests clear fields that clients did not send.
- A warehouse integration still reading the legacy shipping field started failing right after the latest migration.
- `GET /api/orders/{id}/summary` can return stale data after a successful patch.
- Failed patch attempts still show up in the outbound integration feed, while failure audit records are missing.
- Users can move orders backwards in status (e.g. from CONFIRMED back to DRAFT), causing confusion in the fulfillment pipeline.
- Notification records are being created for orders that failed to update.

## Your task

1. Fix the production code and database migrations so the rollout is safe.
2. Keep the existing smoke tests passing.
3. Additional verification will check runtime behavior around partial updates, rollout compatibility, transaction side effects, cached summaries, status transitions, and notification delivery.

Do not delete tests or weaken assertions. Fix the production code and migrations only.

Run `./gradlew test` to verify the visible test suite.
