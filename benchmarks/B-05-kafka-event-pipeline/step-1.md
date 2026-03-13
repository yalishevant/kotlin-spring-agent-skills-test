Build a Kotlin + Spring Boot order event processing service.

## Requirements

- Gradle Kotlin DSL, Spring Boot 3.3, Kotlin, Spring Data JPA, Spring Kafka, H2, Flyway, Jackson, Validation, Micrometer
- For testing: `spring-kafka-test` with `EmbeddedKafkaBroker` or Testcontainers Kafka

### Kafka Consumer

Topic: `order-events`
Message format (JSON):
```json
{
  "eventId": "uuid-string",
  "eventType": "ORDER_CREATED | ORDER_PAID | ORDER_CANCELLED",
  "orderId": "uuid-string",
  "payload": { ... },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Processing Logic

- **ORDER_CREATED** -> create `OrderProjection` in DB (orderId, status=CREATED, customerEmail from payload, totalAmount from payload)
- **ORDER_PAID** -> update projection status to PAID
- **ORDER_CANCELLED** -> update projection status to CANCELLED, but ONLY if current status is NOT PAID (paid orders cannot be cancelled)

### Deduplication

- Store processed `eventId` values in a `processed_events` table
- Before processing, check if eventId already exists -> if yes, skip (log as duplicate)
- The deduplication check and business logic must be in the SAME transaction

### Dead Letter Queue

- If processing fails after 3 retries (exponential backoff: 1s, 2s, 4s), send to `order-events-dlq` topic
- Do not retry on deserialization errors (send to DLQ immediately)

### Transactional Outbox

- After successful processing, write a row to `outbox` table: id, aggregateId (orderId), eventType ("PROJECTION_UPDATED"), payload (JSON), createdAt, published (boolean, default false)
- The outbox write must be in the SAME transaction as the business logic
- Create a `@Scheduled` method that polls unpublished outbox entries every 5 seconds and publishes them to `processing-results` topic, then marks as published

### Metrics (Micrometer)

- `order_events_processed_total` -- counter, tag: `event_type`
- `order_events_duplicates_total` -- counter
- `order_events_dlq_total` -- counter

### Tests (REQUIRED)

- Unit tests: processing logic for each event type, duplicate detection, cancel-when-paid rejection
- Integration test with embedded Kafka:
  - Send ORDER_CREATED -> verify OrderProjection created
  - Send same eventId again -> verify no duplicate processing
  - Send ORDER_PAID -> verify status updated
  - Send poison message -> verify it ends up in DLQ
  - Verify outbox row created after successful processing
  - Verify metrics incremented

All tests must pass with `./gradlew test`.
