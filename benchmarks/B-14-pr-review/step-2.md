Perform a thorough code review of the payment service built in step 1.

## Your Task

Review the entire codebase as if this were a pull request from a junior developer. For each issue found, provide:

1. **Severity**: CRITICAL / HIGH / MEDIUM / LOW
2. **Category**: security / correctness / performance / style / maintainability
3. **Location**: file and approximate location (class/method name)
4. **Problem**: What's wrong and why it's dangerous in production
5. **Fix**: The minimal code change to fix it (actual code, not just description)

## Review Focus Areas

Prioritize by risk to production. Focus on:
- Spring proxy and AOP issues (are `@Transactional`, `@Cacheable` actually working?)
- Security configuration (is anything accidentally exposed?)
- Data integrity (JPA entity patterns, transaction boundaries)
- Kotlin-specific traps (validation annotation targets, null safety at Java boundaries)
- Performance (N+1 queries, unnecessary database calls)
- Serialization (is Jackson configured correctly for Kotlin?)
- Distributed systems concerns (scheduling, caching with mutable objects)

Do NOT just check code style — focus on bugs that would cause production issues.

## Deliverable

Create a `REVIEW.md` file with all findings, ordered by severity (CRITICAL first).
For each finding, include the fix as a code diff or code snippet.
At the end, provide a summary: "Found X issues: Y critical, Z high, ..."
