Requirement: rename the `email_address` column to `email` in the `user_profiles` table.

Constraint: **zero-downtime deployment**. The old version of the app (reading `email_address`) and the new version (reading `email`) must coexist during rollout.

## Implement the expand/contract migration:

### Phase 1 — Expand (V2 migration + dual-write app)
1. **V2 Flyway migration**:
   - Add new `email` column
   - Backfill `email` from `email_address` for all existing rows
   - Add a database trigger OR document that the app will dual-write
2. **Application change**:
   - Update entity to read from `email` column
   - Write to BOTH `email` and `email_address` columns (dual-write for backward compatibility)
   - Update the `@Column` mapping

### Phase 2 — Contract (V3 migration + cleanup)
3. **V3 Flyway migration**:
   - Drop the `email_address` column (safe only after all app instances are on new version)
4. **Final application change**:
   - Remove dual-write logic
   - Entity reads/writes only `email` column

## Deliverables

Provide:
1. V2 migration SQL
2. V3 migration SQL
3. Updated entity class (showing the dual-write phase)
4. Final entity class (after V3, clean)
5. Tests verifying:
   - After V2: both columns have data, app reads from `email` column correctly
   - After V3: `email_address` column is gone, app still works
   - Data integrity: after V2 migration, all rows have matching `email` and `email_address` values

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
