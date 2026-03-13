Build a digital wallet microservice in Kotlin + Spring Boot.

## Domain

- `Wallet` (id: Long, userId: String unique, balance: BigDecimal, currency: String, status: enum [ACTIVE/FROZEN/CLOSED], createdAt: Instant, version: Long for optimistic locking)
- `Transaction` (id: Long, walletId: Long, type: enum [DEPOSIT/WITHDRAWAL/TRANSFER_IN/TRANSFER_OUT], amount: BigDecimal, referenceId: String unique for idempotency, description: String?, balanceBefore: BigDecimal, balanceAfter: BigDecimal, createdAt: Instant)

## Endpoints

- `POST /api/wallets` — create wallet (one per userId, enforced by unique constraint). Return 201 + wallet. Return 409 if userId already has a wallet.
- `GET /api/wallets/{id}` — wallet detail with last 10 transactions (eager loaded, not N+1). Return 404 if not found.
- `POST /api/wallets/{id}/deposit` — deposit funds. Body: `{referenceId, amount, description}`. Idempotent by referenceId — duplicate requests return the original result with 200, not 201.
- `POST /api/wallets/{id}/withdraw` — withdraw funds (check balance, idempotent by referenceId). Return 422 if insufficient balance. Return 409 if wallet is FROZEN or CLOSED.
- `POST /api/transfers` — transfer between two wallets. Body: `{fromWalletId, toWalletId, amount, referenceId, description}`. Atomic: debit source + credit destination in one transaction. Return 422 if insufficient balance.
- `GET /api/wallets/{id}/transactions?page=0&size=20` — transaction history with pagination, most recent first.

## Business Rules

- Cannot deposit/withdraw to FROZEN or CLOSED wallet — return 409 with explanation
- Cannot withdraw more than available balance — use optimistic locking with `@Version`
- Transfers are atomic — both sides succeed or neither does (single `@Transactional`)
- All money operations are idempotent by `referenceId` — duplicate requests return the original result, not a new transaction
- Transaction log is append-only — every balance change creates a Transaction record with balanceBefore and balanceAfter

## Technical Requirements

- Gradle Kotlin DSL, Spring Boot 3.x, Kotlin, Spring Data JPA, H2 (test), Flyway
- Flyway migrations for schema
- Unified error handling using Problem Details format (RFC 7807)
- Input validation on all endpoints

## Tests (REQUIRED)

- Unit tests: service logic (deposit, withdraw, transfer, idempotency check)
- Slice tests: `@WebMvcTest` for controller validation and error responses
- Integration tests (`@SpringBootTest` with H2):
  - Full flow: create wallet → deposit → transfer → check balance
  - Idempotency: same deposit referenceId twice → balance changes only once
  - Transfer atomicity: transfer with insufficient funds → neither wallet changes
  - Optimistic locking: simulate concurrent withdrawals (one succeeds, one fails with conflict, balance never goes negative)
  - Audit: every balance change has a Transaction record with correct before/after
- Query count: wallet detail with transactions executes ≤ 3 SQL queries (no N+1)

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
