## Security Audit

Perform a security audit of the task management API you just built. Check for the following:

### Checklist

1. **Endpoint exposure**: Are there any endpoints accessible without authentication that shouldn't be? (Check: actuator endpoints beyond health/prometheus, error endpoints, swagger-ui)
2. **JWT validation**: Does the JWT filter check expiration? Issuer? Is the signing key hardcoded or configurable?
3. **Authorization bypass**: Can a USER delete tasks by directly calling DELETE with a valid JWT? Is method-level security (`@PreAuthorize`) used in addition to URL-based rules?
4. **CORS**: Is the CORS policy restrictive enough? Does it allow arbitrary origins?
5. **Error information leakage**: Do 401/403 responses leak stack traces, class names, or internal details?
6. **CSRF**: Is CSRF disabled? If so, is there a comment explaining why (stateless API with token auth)?
7. **Security headers**: Are security headers configured? (X-Content-Type-Options, X-Frame-Options, Strict-Transport-Security)
8. **Actuator security**: Are sensitive actuator endpoints (env, beans, configprops) disabled or protected?

### Output Format

For each finding:
```
### Finding N: [Title]
- **Severity**: CRITICAL / HIGH / MEDIUM / LOW
- **Location**: [file:line]
- **Problem**: [what's wrong]
- **Risk**: [what could happen]
- **Fix**: [code change]
```

### Implementation

Fix all CRITICAL and HIGH findings. Add security regression tests for each fix.
