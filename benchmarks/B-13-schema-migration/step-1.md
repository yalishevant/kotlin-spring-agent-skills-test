Build a Kotlin + Spring Boot user profile service.

## Requirements

### Entity
- `UserProfile`: id (Long, generated), userName (String, unique), emailAddress (String), bio (String?), createdAt (Instant)

### REST Endpoints
- `POST /api/profiles` — create profile, validate userName unique
- `GET /api/profiles/{id}` — get profile by id
- `GET /api/profiles?username={username}` — find by username
- `PUT /api/profiles/{id}` — update profile (bio, emailAddress)

### Database
- Flyway V1 migration: create `user_profiles` table with columns: id, user_name, email_address, bio, created_at
- Note the column is `email_address` (snake_case matching the entity field `emailAddress`)

### Tests
- CRUD operations work end-to-end
- Duplicate userName returns 409

The project must compile with `./gradlew compileKotlin` and all tests must pass with `./gradlew test`.
