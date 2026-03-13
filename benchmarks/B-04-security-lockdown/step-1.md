Build a Kotlin + Spring Boot task management API with JWT authentication.

## Requirements

- Gradle Kotlin DSL, Spring Boot 3.3, Kotlin, Spring Data JPA, Spring Security, H2, Flyway, Jackson, Validation

### Security Model

- Stateless JWT authentication (resource server mode, NOT login flow)
- JWT tokens contain: `sub` (userId), `role` (ADMIN or USER), `exp` (expiration), `iss` (issuer: "task-service")
- Provide a `JwtTestUtil` class that generates test tokens with configurable userId, role, and expiration

### Entities

- `User`: id (Long), email (String, unique), role (enum: ADMIN, USER)
- `Task`: id (Long), title (String), description (String?), status (enum: TODO, IN_PROGRESS, DONE), assignee (ManyToOne User), createdBy (ManyToOne User), createdAt (Instant)

### Endpoint Access Rules

| Endpoint | Anonymous | USER | ADMIN |
|----------|-----------|------|-------|
| POST /api/tasks | 401 | 201 (creates task, createdBy = current user) | 201 |
| GET /api/tasks | 401 | 200 (only tasks assigned to current user) | 200 (all tasks) |
| PATCH /api/tasks/{id} | 401 | 200 (only if assignee) | 200 (any task) |
| DELETE /api/tasks/{id} | 401 | 403 | 200 |
| GET /actuator/health | 200 | 200 | 200 |
| GET /actuator/prometheus | 200 | 200 | 200 |
| Any other path | 401 | 403 | 403 |

### CORS

- Allow origin: `http://localhost:3000`
- Allow credentials: true
- Allow methods: GET, POST, PATCH, DELETE, OPTIONS
- CORS must be processed BEFORE the security filter (preflight requests must not require auth)

### Error Handling

- 401 for unauthenticated requests (custom AuthenticationEntryPoint, not Spring's default)
- 403 for unauthorized requests (custom AccessDeniedHandler)
- Error responses in the same JSON format as the rest of the API

### Tests (REQUIRED -- this is a security-critical service)

For each endpoint x role combination (from the table above), write a test:
- At minimum 12 test cases (4 endpoints x 3 roles)
- Verify exact HTTP status code
- Verify body content where applicable
- Use `@WebMvcTest` with `@WithMockUser` or inject test JWT tokens

All tests must pass.
