Create a Kotlin + Spring Boot 2.7 project with intentionally legacy patterns that will break on upgrade.

## Requirements

### Build
- Gradle Kotlin DSL with Spring Boot 2.7.18, Kotlin 1.8.22
- Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, H2

### Entity
- `Task` entity with `javax.persistence.*` imports (NOT jakarta): @Entity, @Id, @GeneratedValue, @Column
- Fields: id (Long), title (String), description (String?), completed (Boolean), createdAt (LocalDateTime)

### Security (legacy style)
- Extend `WebSecurityConfigurerAdapter` (deprecated in 2.7, removed in 3.x)
- Override `configure(HttpSecurity)` to set up basic authentication
- Permit GET endpoints for all, require auth for POST/PUT/DELETE

### Validation
- Use `javax.validation.*` imports: @NotBlank, @Size
- DTO: `CreateTaskRequest` with validation annotations

### Configuration
- `spring.config.use-legacy-processing=true` in application.properties
- `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect` (explicit dialect, no longer needed in Hibernate 6)

### REST Controller
- Standard CRUD: GET /api/tasks, GET /api/tasks/{id}, POST /api/tasks, PUT /api/tasks/{id}, DELETE /api/tasks/{id}

### Tests
- `@WebMvcTest` for controller with security (use `@WithMockUser`)
- `@DataJpaTest` for repository
- Must pass on Spring Boot 2.7

The project must compile and all tests must pass with `./gradlew test`.
