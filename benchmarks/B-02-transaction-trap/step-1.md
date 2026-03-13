Build a Kotlin + Spring Boot money transfer service.

## Requirements

- Gradle Kotlin DSL, Spring Boot 3.3, Kotlin, Spring Data JPA, H2, Flyway, Jackson, Validation

### Entities

- `Account`: id (Long), ownerName (String), balance (BigDecimal), version (Long, `@Version` for optimistic locking)
- `TransferLog`: id (Long), fromAccountId (Long), toAccountId (Long), amount (BigDecimal), timestamp (Instant), status (enum: SUCCESS, FAILED, reason: String?)

### Service: TransferService

- Method `transfer(fromId: Long, toId: Long, amount: BigDecimal): TransferLog`
  - Validate: amount > 0
  - Load both accounts
  - Check sufficient balance on source
  - Deduct from source, add to destination
  - Create TransferLog with status SUCCESS
  - If transfer fails (insufficient funds), create TransferLog with status FAILED — **this log must NOT be rolled back** even if the transfer transaction rolls back

- Method `batchTransfer(transfers: List<TransferRequest>): List<TransferLog>`
  - Process each transfer independently
  - One failure must NOT roll back others

### REST API

- `POST /api/transfers` — single transfer, body: `{fromAccountId, toAccountId, amount}`
- `POST /api/transfers/batch` — batch transfer, body: `{transfers: [{fromAccountId, toAccountId, amount}, ...]}`
- `GET /api/accounts/{id}` — account detail with balance

### Tests (REQUIRED)

- Unit test: successful transfer, insufficient funds
- Integration test: after failed transfer, FAILED log entry exists in DB
- Integration test: batch of 3 where #2 fails → #1 and #3 succeeded, #2 has FAILED log
- Test: optimistic locking — concurrent transfers cause version conflict

All tests must pass with `./gradlew test`.
