Build a Kotlin + Spring Boot API that exercises all tricky Jackson/Kotlin serialization scenarios.

## Requirements — a `POST /api/events` endpoint that accepts and returns event objects:

### 1. Default parameters
`Event` data class with `priority: Int = 0` — absent in JSON should use the default, not fail deserialization.

### 2. Nullable vs. absent
`description: String?` — null in JSON, absent in JSON, and present in JSON are three different states. Track which state applies. Absent means "don't change," null means "explicitly clear," present means "set value."

### 3. Sealed class hierarchy
`EventPayload` sealed class with:
- `ClickEvent(url: String, elementId: String)`
- `PurchaseEvent(orderId: String, amount: BigDecimal)`
- `CustomEvent(data: Map<String, Any>)`
Serialized with a `"type"` discriminator field.

### 4. Enum with properties
`EventSource` enum with `displayName: String` property — serialize by enum name, not ordinal and not displayName.

### 5. Date/time
All timestamps as ISO 8601 with UTC timezone. `Instant` for creation time, `LocalDate` for date-only fields.

### 6. Value class
`EventId` as `@JvmInline value class EventId(val value: String)` — must serialize as plain string in JSON, not as `{"value": "..."}` object.

### 7. Strict unknown-field rejection
Unknown fields in incoming JSON should be rejected with 400 Bad Request, not silently ignored.

### Tests (REQUIRED)
For each of the 7 scenarios above, provide a test that verifies correct serialization AND deserialization:
- Round-trip test: serialize → deserialize → equals original
- Absent field uses default value (priority = 0)
- Unknown field returns 400, not 200 with ignored data
- Sealed class `type` discriminator present in JSON output
- Value class serializes as plain string
- Enum serializes as name string

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
