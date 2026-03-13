Build a Kotlin + Spring Boot course catalog service.

## Requirements

- Gradle Kotlin DSL, Spring Boot 3.3, Kotlin, Spring Data JPA, H2, Flyway, Jackson, Validation

### Entities

- `Course`: id (Long), title (String), description (String), category (String), instructor (String)
- `Lesson`: id (Long), title (String), durationMinutes (Int), orderIndex (Int). Relationship: Course has many Lessons, ordered by orderIndex.
- `Enrollment`: id (Long), studentName (String), enrolledAt (Instant). Relationship: Course has many Enrollments.

### REST API

- `GET /api/courses` — list ALL courses. Each item includes: id, title, category, instructor, lessonCount (Int), enrollmentCount (Int). Do NOT return full lesson/enrollment objects in the list.
- `GET /api/courses/{id}` — course detail with ALL lessons (ordered by orderIndex) and enrollmentCount (just the number).
- `GET /api/courses/{id}/students?page=0&size=20` — paginated list of enrolled students.

### Seed Data

- Flyway migration V1: create schema
- Flyway migration V2: insert 50 courses, each with 10 lessons and 20 enrollments (use SQL INSERT statements)

### Tests

- Test: `GET /api/courses` returns 50 items with correct lessonCount and enrollmentCount
- Test: `GET /api/courses/{id}` returns lessons ordered by orderIndex
- Test: `GET /api/courses/{id}/students` returns paginated results

All tests must pass with `./gradlew test`.
