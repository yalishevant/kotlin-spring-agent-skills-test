## Performance Diagnosis

Enable Hibernate SQL logging by adding to `application.yml` (or test properties):
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

Write a test that:
1. Calls `GET /api/courses` (list all 50 courses with counts)
2. Captures or counts the number of SQL queries executed
3. If the count is > 5, you have an N+1 problem

Diagnose:
1. Which relationship causes the N+1?
2. Why does it happen? (lazy loading triggered during serialization or count computation)
3. How many queries are actually being executed? (expected: 1 for courses + 50 for lessons + 50 for enrollments = 101)

Propose at least 2 different fix approaches with trade-offs. Pick the best approach for this use case and implement it.

Document your diagnosis in a comment or a short report.
