The application fails to start with the following error:

```
***************************
APPLICATION FAILED TO START
***************************
Description:
Failed to bind properties under 'notification' to com.example.NotificationProperties

Reason: org.springframework.boot.context.properties.bind.BindException:
  Failed to bind properties under 'notification.sms' to com.example.SmsProperties

Action:
Update your application's configuration
```

The `application.yml` contains:
```yaml
notification:
  email:
    from: "noreply@example.com"
    smtp:
      host: localhost
      port: 1025
  sms:
    provider: TWILIO
  retry:
    max-attempts: 5
```

## Your Task

Diagnose and fix all issues:

1. Why is binding failing? (Examine the Kotlin class — is it using camelCase vs. kebab-case correctly? Are there nullability issues with `apiKey` when SMS is configured but key isn't provided?)
2. Why is the `retry.backoffMs` default not working? (Hint: `kebab-case` in YAML vs. `camelCase` in Kotlin — Spring uses relaxed binding but the default in Kotlin constructor may not apply correctly)
3. Why is `sms.apiKey` required but the conditional bean should make it optional when SMS is disabled?

Fix all issues and add tests that verify binding in all profiles.

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
