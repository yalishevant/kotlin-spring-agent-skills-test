Build a Kotlin + Spring Boot notification service with complex configuration.

## Requirements

- `@ConfigurationProperties` class `NotificationProperties` with:
  - `email.from` (required, validated with `@NotBlank`)
  - `email.smtp.host`, `email.smtp.port` (required)
  - `sms.provider` (enum: TWILIO/VONAGE)
  - `sms.apiKey` (required, should come from env variable `SMS_API_KEY`)
  - `retry.maxAttempts` (default: 3), `retry.backoffMs` (default: 1000)
  - `features.smsEnabled` (default: false)
- Profiles: `local` (mock SMS, SMTP on localhost:1025), `staging` (real SMS, SMTP on mail.staging.internal), `prod` (real everything, strict validation)
- Conditional bean: `SmsNotifier` only created when `features.smsEnabled=true` and `sms.provider` is set
- Service: `NotificationService` that sends email and optionally SMS based on config
- Use `@ConstructorBinding` correctly in Kotlin with non-null types and defaults
- Validate required fields at startup with `@Validated`

### Tests (REQUIRED)

- Context loads successfully in each profile (`local`, `staging`, `prod`)
- Properties bind correctly in each profile
- Conditional bean: `SmsNotifier` present when `features.smsEnabled=true`, absent when false
- Validation: missing required property causes startup failure

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
