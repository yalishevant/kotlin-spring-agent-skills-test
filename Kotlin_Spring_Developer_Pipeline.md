# The Complete Kotlin + Spring Backend Developer Pipeline

> A comprehensive, hierarchical task list covering every stage of backend development with Kotlin and Spring Framework — from requirements to production support, including Java-to-Kotlin migration concerns.

---

## Stage 1. Requirements Analysis & API Contract Design

### Tasks

1. Decompose business requirements into use-cases, scenarios, business rules, and constraints (SLA, latency, throughput, consistency guarantees).
2. Define API contracts: REST endpoints (paths, methods, headers), request/response schemas, error codes, versioning strategy, idempotency requirements.
3. Specify non-functional requirements: security model, audit trail, logging policy, metrics/SLO targets, data retention.
4. Identify integration points: external services, message brokers, shared databases, file storage, third-party APIs.
5. Document key decisions as ADRs (Architecture Decision Records): trade-offs, rejected alternatives, rationale.
6. Agree on data models and bounded context boundaries (microservice vs. module vs. monolith).

### Kotlin-Specific Concerns

- Decide early whether DTOs will use `data class` (and what the nullability contract is on API boundaries).
- Choose serialization format strategy: Jackson with Kotlin module vs. kotlinx.serialization.
- Consider Kotlin coroutines support requirements if async/reactive endpoints are needed.

### Common Pain Points

- Requirements are implicit; API contracts drift without a single source of truth.
- Idempotency, timeout budgets, and error code taxonomy are forgotten until production.
- Mismatch between "nullable in JSON" and "nullable in Kotlin" surfaces late.

---

## Stage 2. Architecture & Module Design

### Tasks

1. Choose architectural style: layered (controller → service → repository), hexagonal/clean architecture, modular monolith, microservices, CQRS, event-driven.
2. Design package/module structure and define dependency rules (e.g., `web` must not import `persistence` directly).
3. Identify domain aggregates, transactional boundaries, and consistency strategies (strong vs. eventual).
4. Decide on synchronous vs. asynchronous communication: Spring MVC vs. WebFlux, Kotlin coroutines, message queues.
5. Plan cross-cutting concerns: logging, tracing, metrics, security, caching, rate limiting.
6. Define the error model: domain exceptions, HTTP error mapping, error response schema (e.g., RFC 7807 Problem Details).

### Kotlin-Specific Concerns

- Sealed classes/interfaces are natural for modeling domain errors, command results, and state machines.
- Extension functions can encapsulate mapping logic but must not break layer boundaries.
- Value classes (`@JvmInline value class`) for domain primitives (e.g., `UserId`, `OrderId`) — consider Spring/JPA/Jackson compatibility.

### Common Pain Points

- Mixing layers: business logic leaks into controllers, SQL leaks into services.
- Cyclic dependencies between modules.
- Over-engineering (hexagonal where layered suffices) or under-engineering (no boundaries at all).
- Anemic domain model: services do everything, entities are just data holders.

---

## Stage 3. Project Bootstrap & Build Configuration (Gradle Kotlin DSL)

### Tasks

1. Generate project skeleton (Spring Initializr or manual setup).
2. Configure `build.gradle.kts`: Kotlin JVM plugin, Spring Boot plugin, dependency management via Spring Boot BOM.
3. Add essential Kotlin compiler plugins:
   - `kotlin("plugin.spring")` — opens `@Configuration`, `@Service`, `@Controller`, `@Repository` classes for CGLIB proxying.
   - `kotlin("plugin.jpa")` — generates no-arg constructors for JPA entities (if using JPA).
   - `kotlin("plugin.serialization")` — if using kotlinx.serialization.
4. Configure JVM toolchain and Kotlin JVM target to match.
5. Set up code quality tools: ktlint, detekt, and optionally spotless.
6. Configure test framework: JUnit 5 platform, MockK, Testcontainers dependencies.
7. Set up `settings.gradle.kts` for multi-module projects: included builds, version catalogs.
8. Configure `gradle.properties`: Kotlin daemon settings, parallel builds, caching.
9. Verify the build: `./gradlew clean build` passes, `./gradlew bootRun` starts the app.

### Kotlin-Specific Concerns

- Forgetting `kotlin("plugin.spring")` causes `BeanCreationException` at runtime (final classes can't be proxied).
- Forgetting `kotlin("plugin.jpa")` causes `InstantiationException` for JPA entities (no no-arg constructor).
- kapt vs. KSP: prefer KSP where annotation processors support it (faster compilation).
- K2 compiler (default since Kotlin 2.0): faster compilation, but verify all compiler plugins are K2-compatible.
- JVM target mismatch between Kotlin and Java source sets causes `UnsupportedClassVersionError`.

### Common Pain Points

- Dependency version conflicts (Jackson, Hibernate, Netty, SLF4J) when overriding Spring Boot BOM versions.
- "Works locally, fails on CI" due to different JDK versions or Gradle daemon state.
- Slow Gradle sync in multi-module projects; poor cache hit rate.
- Confusion between `implementation`, `api`, `runtimeOnly`, and `compileOnly` configurations.

---

## Stage 4. Spring Core: Configuration, Dependency Injection & Profiles

### Tasks

1. Define configuration classes (`@Configuration`) and bean declarations (`@Bean`).
2. Implement constructor injection for all Spring-managed components (Kotlin's default and idiomatic style).
3. Set up `@ConfigurationProperties` classes with type-safe property binding.
4. Configure profiles (`application-{profile}.yml`) for local, test, staging, production environments.
5. Manage secrets: environment variables, Spring Cloud Config, Vault, or secret managers — never hardcode.
6. Understand and configure conditional beans: `@ConditionalOnProperty`, `@ConditionalOnMissingBean`, `@Profile`.
7. Configure bean lifecycle hooks: `@PostConstruct`, `@PreDestroy`, `SmartLifecycle`.
8. Set up AOP: understand proxy model (CGLIB vs. JDK dynamic proxies), transaction proxies, caching proxies, retry proxies.
9. Configure Jackson `ObjectMapper` for Kotlin: register `KotlinModule`, configure date/time serialization, null handling.

### Kotlin-Specific Concerns

- `@ConfigurationProperties` with Kotlin: use `data class` with `@ConstructorBinding` (implicit in Boot 3.x for single-constructor classes), handle default values and nullability carefully.
- Property name binding: `kebab-case` in YAML maps to `camelCase` in Kotlin fields — Spring handles this, but custom prefixes can cause confusion.
- Self-invocation trap: calling a `@Transactional` method from within the same class bypasses the proxy — this is a Spring issue, not Kotlin-specific, but Kotlin developers new to Spring hit it frequently.
- Kotlin `object` declarations (singletons) are not Spring-managed beans — don't mix them with DI.

### Common Pain Points

- `NoSuchBeanDefinitionException`: missing component scan, wrong profile, conditional not met.
- `BeanCurrentlyInCreationException`: circular dependencies — restructure or use `@Lazy`.
- Properties not binding: wrong prefix, missing `@EnableConfigurationProperties`, wrong data type.
- Unexpected beans from auto-configuration overriding custom ones.

---

## Stage 5. Web Layer: Spring MVC / WebFlux

### Tasks

1. Implement REST controllers with proper HTTP method mapping (`@GetMapping`, `@PostMapping`, etc.).
2. Design request/response DTOs as Kotlin `data class` with validation annotations.
3. Apply input validation using Jakarta Bean Validation (`@field:NotBlank`, `@field:Size`, `@field:Email`, etc.).
4. Implement global error handling with `@ControllerAdvice` and `@ExceptionHandler`.
5. Standardize error response format (Problem Details / RFC 7807 or custom schema).
6. Configure CORS policies.
7. Set up content negotiation and response serialization.
8. Generate API documentation: SpringDoc OpenAPI / Swagger UI.
9. Implement request/response logging and correlation ID propagation (MDC).
10. For WebFlux: implement handlers with `suspend` functions or `Flow`/`Mono`/`Flux` return types.

### Kotlin-Specific Concerns

- Validation annotation use-site targets: must use `@field:NotBlank` (not just `@NotBlank`) because Kotlin constructor parameters are not fields by default.
- Nullable vs. absent in JSON: a `val name: String?` field set to `null` is different from the field being absent in the payload. Configure Jackson's `FAIL_ON_NULL_FOR_PRIMITIVES`, inclusion rules.
- `data class` default parameter values interact with Jackson deserialization — requires `jackson-module-kotlin` to work correctly.
- WebFlux with coroutines: `suspend fun` in controllers, `Flow<T>` for streaming — requires `spring-boot-starter-webflux` and `kotlinx-coroutines-reactor`.

### Common Pain Points

- Validation silently not working because annotations target the wrong site (constructor parameter instead of field).
- `HttpMessageNotReadableException` due to Jackson failing to instantiate Kotlin classes (missing Kotlin module).
- Inconsistent error format: some errors return framework defaults, others return custom format.
- CORS misconfiguration: preflight requests fail, credentials not handled.
- `415 Unsupported Media Type` due to missing `Content-Type` header or wrong `consumes`/`produces` config.

---

## Stage 6. Serialization: Jackson & Kotlin

### Tasks

1. Register `jackson-module-kotlin` in the `ObjectMapper` (auto-configured by Spring Boot, but verify).
2. Configure date/time serialization: ISO 8601 format, timezone handling, `JavaTimeModule`.
3. Handle Kotlin nullability in JSON: decide on `null` inclusion vs. omission per field.
4. Configure naming strategy: `camelCase` (default), `snake_case`, or custom.
5. Handle polymorphic serialization: `@JsonTypeInfo` with sealed classes.
6. Configure custom serializers/deserializers where needed.
7. If using `kotlinx.serialization`: configure it for specific use-cases (e.g., Kafka messages) while keeping Jackson for Spring MVC.

### Kotlin-Specific Concerns

- Without `jackson-module-kotlin`, Jackson cannot instantiate classes with no default constructor — a frequent Kotlin-specific failure.
- `data class` with default parameter values: Jackson must use the Kotlin module to honor defaults; otherwise, missing JSON fields cause errors instead of using defaults.
- `val` properties: Jackson can deserialize into `val` fields via the constructor, but some custom deserializers expect `var` — avoid `var` in DTOs.
- Sealed classes for polymorphic types need explicit `@JsonSubTypes` or custom type resolver.
- Kotlin `enum class` with properties: ensure Jackson serializes by name (not ordinal) unless specifically configured otherwise.

### Common Pain Points

- "Cannot construct instance of..." — Kotlin module not registered or incompatible version.
- Date/time fields serialized as timestamps instead of ISO strings.
- Nullability mismatches: non-null Kotlin field receives `null` from JSON → runtime crash.
- Conflicting Jackson versions (Spring Boot BOM vs. explicit dependency).

---

## Stage 7. Business Logic & Service Layer

### Tasks

1. Implement service classes with use-case orchestration.
2. Define domain rules, validations, and invariants.
3. Handle transactional boundaries (delegate to Stage 8 for details).
4. Implement idempotency strategies: idempotency keys, unique constraints, conditional inserts.
5. Handle concurrency: optimistic locking (`@Version`), pessimistic locking, distributed locks.
6. Integrate with external services (delegate to Stage 10 for details).
7. Emit domain events for downstream processing (Spring `ApplicationEventPublisher` or message broker).
8. Implement caching where appropriate (`@Cacheable`, `@CacheEvict`).

### Kotlin-Specific Concerns

- Sealed classes for modeling operation results: `Success`, `NotFound`, `Conflict` — more idiomatic than exceptions for expected outcomes.
- Extension functions for domain operations — useful but should not hide business logic in hard-to-find places.
- Kotlin coroutines in service layer: if using WebFlux, services should be `suspend` functions; if using MVC, keep them blocking.
- `@Cacheable` on Kotlin functions: method must be `open` (handled by `plugin.spring` for `@Service` classes) and return type must be serializable by the cache provider.

### Common Pain Points

- Transaction boundaries too wide: holding database connections during external HTTP calls.
- Idempotency not implemented: retries create duplicate records.
- `@Cacheable` not working due to self-invocation or non-serializable return types.
- Race conditions in concurrent access to shared state.

---

## Stage 8. Data Access: JPA / Spring Data JDBC / R2DBC

### Tasks

1. Design database schema: tables, columns, types, constraints, indexes.
2. Implement JPA entities or Spring Data JDBC aggregates.
3. Define relationships: `@OneToMany`, `@ManyToOne`, `@ManyToMany` — choose fetch strategies.
4. Implement repositories: Spring Data derived queries, `@Query` (JPQL/SQL), specifications, projections.
5. Configure transaction management: `@Transactional` placement, propagation, isolation levels.
6. Handle `equals`/`hashCode` correctly for entities (especially with JPA).
7. Optimize query performance: avoid N+1, use fetch joins, entity graphs, batch fetching, DTO projections.
8. Implement pagination and sorting.
9. Configure connection pooling: HikariCP settings (pool size, timeouts, leak detection).
10. For R2DBC (reactive): use `DatabaseClient` or Spring Data R2DBC repositories with coroutines.

### Kotlin-Specific Concerns

- **JPA entities should NOT be `data class`**: `data class` generates `equals`/`hashCode` from all fields (including mutable state and lazy-loaded associations), which breaks JPA identity semantics. Use a regular `class` with manual `equals`/`hashCode` based on the business key or ID.
- `kotlin("plugin.jpa")` is required to generate no-arg constructors for entity classes.
- All entity properties involved in JPA proxying must be `open` (or use `plugin.spring` / `plugin.jpa` which handles this).
- Nullable fields in entities: Kotlin enforces null-safety at compile time, so lazy-loaded associations that might be null must be declared as `T?`.
- `lateinit var` can be used for JPA-assigned fields, but it's risky — prefer nullable types with explicit initialization.

### Common Pain Points

- N+1 queries: loading a collection triggers one query per element.
- `LazyInitializationException`: accessing a lazy-loaded field outside an open session/transaction.
- `data class` as JPA entity: broken `hashCode`/`equals`, issues with proxies and lazy loading.
- Transaction not rolling back: wrong exception type (checked vs. unchecked), `rollbackFor` not configured.
- Self-invocation: calling a `@Transactional` method from the same class bypasses the proxy.
- Deadlocks from inconsistent locking order.

---

## Stage 9. Database Schema Migrations (Flyway / Liquibase)

### Tasks

1. Choose migration tool: Flyway (SQL-based, simpler) or Liquibase (XML/YAML/SQL, more flexible).
2. Write versioned migration scripts: V1__create_tables.sql, V2__add_index.sql, etc.
3. Implement backward-compatible schema changes (expand/contract pattern for zero-downtime deployments):
   - Step 1: Add new column (nullable or with default).
   - Step 2: Deploy code that writes to both old and new columns.
   - Step 3: Backfill data.
   - Step 4: Switch reads to new column.
   - Step 5: Drop old column.
4. Test migrations: verify they run on an empty database and on a database with existing data.
5. Handle migration conflicts in team environments (concurrent branch merges).
6. Plan rollback strategies: reversible migrations, versioned rollback scripts.
7. Consider large table migrations: avoid long locks, use batched operations, `CREATE INDEX CONCURRENTLY`.

### Kotlin-Specific Concerns

- Migrations are SQL scripts — not Kotlin-specific. But the entities that map to the schema are Kotlin classes, so schema changes must be validated against entity definitions.
- Kotlin nullable types must match database column nullability (NOT NULL constraints).

### Common Pain Points

- "Migration checksum mismatch" — someone edited an already-applied migration.
- Long-running migrations locking tables in production.
- Forgetting to create indexes for foreign keys or frequently queried columns.
- Schema and entity model getting out of sync.

---

## Stage 10. Integrations & Resilience (HTTP, Messaging, Scheduling)

### Tasks

#### HTTP Client Integrations
1. Choose HTTP client: `RestClient` (Spring 6.1+), `WebClient` (reactive), `RestTemplate` (legacy).
2. Configure timeouts: connect, read, write — per-client or per-request.
3. Implement retry logic with exponential backoff and jitter (Resilience4j or Spring Retry).
4. Implement circuit breaker for unstable dependencies.
5. Add request/response logging with correlation ID propagation.
6. Implement idempotency for outgoing requests where needed.

#### Messaging (Kafka, RabbitMQ)
7. Implement message producers: schema, serialization, partitioning strategy.
8. Implement message consumers: deserialization, deduplication, error handling, DLQ (Dead Letter Queue).
9. Configure consumer retry policies and backoff.
10. Handle message ordering guarantees and exactly-once semantics where needed.
11. Implement the transactional outbox pattern for reliable event publishing.

#### Scheduling
12. Implement scheduled tasks (`@Scheduled`): cron expressions, fixed rate, fixed delay.
13. Handle distributed scheduling: use distributed locks (ShedLock) to prevent duplicate execution.

### Kotlin-Specific Concerns

- `WebClient` with coroutines: use `awaitBody()`, `awaitExchange()` from `kotlinx-coroutines-reactor`.
- Kafka message DTOs: ensure serialization/deserialization is configured for Kotlin classes (nullable fields, default values).
- `@Scheduled` methods must be `open` if proxied — `plugin.spring` handles this for `@Component` classes.

### Common Pain Points

- Retries without jitter causing thundering herd.
- Retrying non-idempotent operations, creating duplicates.
- Consuming a message, failing midway, and losing the event (no DLQ).
- `@Scheduled` executing on all instances in a cluster without distributed locking.
- Timeouts set too high, holding connections and threads.
- Circuit breaker not configured: one slow dependency degrades the entire service.

---

## Stage 11. Security (Spring Security)

### Tasks

1. Configure `SecurityFilterChain`: define public endpoints (health checks, Swagger UI), protected endpoints, authentication mechanism.
2. Implement authentication: JWT validation (resource server), OAuth2 login, session-based auth, API keys.
3. Implement authorization: role-based (`hasRole`), scope-based (`hasAuthority`), method-level security (`@PreAuthorize`, `@Secured`).
4. Configure CORS policy for browser-based clients.
5. Configure CSRF protection: enable for session-based apps, disable for stateless APIs.
6. Implement security headers: `Content-Security-Policy`, `X-Content-Type-Options`, `Strict-Transport-Security`.
7. Handle authentication/authorization errors: custom `AuthenticationEntryPoint` (401), `AccessDeniedHandler` (403).
8. Implement rate limiting for public or sensitive endpoints.
9. Audit security events: login attempts, access denials, privilege escalations.
10. Secure secrets management: never log tokens, mask sensitive fields.

### Kotlin-Specific Concerns

- Spring Security's Kotlin DSL for `SecurityFilterChain` configuration is concise and expressive — prefer it over the Java-style builder.
- `@PreAuthorize` SpEL expressions work the same in Kotlin but watch for method visibility (must be `public` and proxied).
- Kotlin lambda-based security configuration: ensure proper SAM (Single Abstract Method) conversion.

### Common Pain Points

- Overly permissive matchers: `permitAll()` on too many paths, accidentally exposing actuator endpoints.
- CORS preflight requests (OPTIONS) blocked because CORS is configured after authentication.
- 403 instead of 401: wrong order of filters or missing `AuthenticationEntryPoint`.
- JWT validation not checking expiration, issuer, or audience.
- `@PreAuthorize` silently not working because method security is not enabled (`@EnableMethodSecurity`).

---

## Stage 12. Testing (Unit / Slice / Integration / Contract)

### Tasks

#### Unit Tests
1. Test business logic in isolation (no Spring context): pure functions, domain rules, mappers.
2. Use MockK for mocking dependencies (Kotlin-idiomatic alternative to Mockito).
3. Test edge cases: nullability boundaries, empty collections, boundary values, error paths.

#### Web Layer (Slice) Tests
4. Use `@WebMvcTest` for Spring MVC controllers: MockMvc, request/response verification, validation.
5. Use `@WebFluxTest` for WebFlux handlers: WebTestClient.
6. Test error handling: validation errors (400), not found (404), business errors (409/422), server errors (500).

#### Data Layer (Slice) Tests
7. Use `@DataJpaTest` for JPA repositories: in-memory or Testcontainers database.
8. Test queries: derived queries, custom `@Query`, specifications.
9. Verify transactional behavior and constraint enforcement.

#### Integration Tests
10. Use `@SpringBootTest` for full application context tests.
11. Use Testcontainers for external dependencies: PostgreSQL, MySQL, Kafka, Redis, etc.
12. Test end-to-end flows: HTTP request → service → database → response.
13. Test security: authentication and authorization for different roles.

#### Contract Tests
14. Implement consumer-driven contract tests (Spring Cloud Contract or Pact).
15. Verify API contracts against OpenAPI specifications.

#### Test Infrastructure
16. Create test fixtures and factory methods for test data.
17. Configure test profiles and test-specific property overrides.
18. Optimize test execution time: minimize full context loads, use slice tests where possible.

### Kotlin-Specific Concerns

- MockK syntax: `every { mock.method() } returns value`, `verify { mock.method() }` — more natural in Kotlin than Mockito.
- Test classes and methods in Kotlin don't need to be `open` if using JUnit 5 (no subclassing needed).
- Backtick method names for readable test descriptions: `` fun `should return 404 when order not found`() ``.
- Coroutine testing: use `runTest` from `kotlinx-coroutines-test` for testing `suspend` functions.
- Kotlin `data class` for assertions: `assertEquals(expected, actual)` works well due to structural equality.

### Common Pain Points

- Slow tests: full `@SpringBootTest` where a slice test would suffice.
- Flaky tests: time-dependent logic, shared mutable state, non-deterministic ordering.
- Mocking too much: testing mocks instead of behavior.
- Missing negative test cases: only testing the happy path.
- Test data management: stale fixtures, shared database state between tests.

---

## Stage 13. Observability: Logging, Metrics & Tracing

### Tasks

#### Structured Logging
1. Configure structured JSON logging (Logback/Logstash encoder).
2. Propagate correlation IDs: `traceId`, `spanId`, `requestId` via MDC.
3. Define log levels per package/class: minimize noise, ensure critical paths are logged.
4. Avoid logging PII (Personally Identifiable Information) or secrets.

#### Metrics
5. Configure Micrometer with the appropriate registry (Prometheus, Datadog, CloudWatch, etc.).
6. Expose Spring Boot Actuator endpoints: `/actuator/health`, `/actuator/prometheus`, `/actuator/info`.
7. Add custom business metrics: counters (orders created), timers (processing duration), gauges (queue depth).
8. Control metric tag cardinality: avoid high-cardinality labels that explode storage.
9. Define SLIs and SLOs: latency percentiles, error rates, availability.

#### Distributed Tracing
10. Configure OpenTelemetry / Micrometer Tracing for distributed trace propagation.
11. Ensure trace context propagates across HTTP calls, message queues, and async boundaries.
12. For coroutines: verify context propagation works with `suspend` functions (Spring Boot 4 / Spring Framework 7 adds automatic propagation).

#### Health & Readiness
13. Configure health indicators for critical dependencies (database, message broker, external services).
14. Separate liveness (`/actuator/health/liveness`) and readiness (`/actuator/health/readiness`) probes.

### Kotlin-Specific Concerns

- MDC and coroutines: MDC is `ThreadLocal`-based, but coroutines can switch threads. Use `MDCContext` from `kotlinx-coroutines-slf4j` or Spring's built-in context propagation (Spring Framework 7+).
- Micrometer timers with coroutines: ensure you're measuring actual wall-clock time, not just thread time.

### Common Pain Points

- No correlation between logs and traces: separate request flows are impossible to follow.
- Metrics exist but don't answer operational questions (e.g., "which endpoint is slow?").
- High-cardinality metric labels causing storage/cost explosion.
- Actuator endpoints exposed publicly without authentication.
- Missing health checks for critical dependencies: service reports "healthy" while database is down.

---

## Stage 14. Debugging & Incident Triage

### Tasks

1. Analyze stack traces: identify root cause in `Caused by:` chain, distinguish symptoms from causes.
2. Classify errors by category: DI/bean lifecycle, configuration binding, serialization, database/SQL, security, timeouts, concurrency.
3. Reproduce issues locally: use same profiles, same data, same dependency versions.
4. Debug Spring context startup failures: missing beans, circular dependencies, conditional bean mismatches.
5. Debug transaction issues: unexpected commit/rollback, wrong propagation, self-invocation.
6. Debug Hibernate issues: N+1, lazy initialization, dirty checking surprises.
7. Debug security issues: `AccessDeniedException` analysis, filter chain inspection, authority mapping.
8. Debug configuration issues: property not binding, wrong profile active, environment variable override not applied.
9. Use Spring Boot DevTools for rapid feedback during local development.
10. Use JetBrains Spring Debugger plugin for visualizing bean graphs, request mappings, security filter chains.

### Kotlin-Specific Concerns

- Stack traces from coroutines can be hard to read — enable `-Dkotlinx.coroutines.debug` for enhanced stack traces.
- Kotlin inline functions don't appear in stack traces (inlined at call site).
- `NullPointerException` from platform types (`T!` from Java code): Kotlin doesn't enforce null checks on Java interop types — these manifest as runtime `NullPointerException` with no compile-time warning.

### Common Pain Points

- Long, nested Spring stack traces: the actual error is buried deep in `Caused by:`.
- "Works locally, fails in staging" — different profiles, different secrets, different database schema.
- Intermittent failures: race conditions, connection pool exhaustion, timeout-related issues.
- Treating symptoms instead of root causes: fixing the exception site instead of the actual source of the problem.

---

## Stage 15. CI/CD, Containerization & Release

### Tasks

#### Continuous Integration
1. Configure CI pipeline: checkout → compile → test → lint → static analysis → build artifact.
2. Run ktlint/detekt for code style and static analysis.
3. Run security scanning: dependency vulnerability checks (OWASP Dependency-Check, Snyk, Trivy).
4. Collect test coverage reports (JaCoCo) and enforce thresholds.
5. Cache Gradle dependencies and build outputs for faster builds.

#### Containerization
6. Create Dockerfile: multi-stage build, JRE-only runtime image, non-root user.
7. Use Spring Boot layered JARs for efficient Docker layer caching.
8. Optimize image size: use distroless or slim base images.
9. Consider GraalVM native image compilation for fast startup (Spring Boot 3.x+ support via Spring Native).

#### Release & Deployment
10. Version artifacts: semantic versioning, Git tags, changelog generation.
11. Configure CD pipeline: deploy to staging → smoke tests → deploy to production.
12. Implement deployment strategies: rolling update, blue-green, canary.
13. Handle database migrations during deployment: ensure migration runs before new code starts.
14. Configure rollback procedures: artifact rollback, database rollback strategy.
15. Set up environment-specific configuration: Kubernetes ConfigMaps/Secrets, environment variables.

### Kotlin-Specific Concerns

- GraalVM native image with Kotlin: requires reflection configuration for Kotlin-specific features. Spring Boot's AOT processing handles most cases, but custom reflection usage needs explicit hints.
- K2 compiler in CI: faster compilation, but ensure CI uses the same Kotlin version as local development.
- Kotlin incremental compilation in CI: may not be effective if the CI uses clean builds.

### Common Pain Points

- "Works locally, breaks on CI" — different JDK versions, missing environment variables.
- Slow CI builds: no caching, full re-download of dependencies.
- Docker image too large: using JDK instead of JRE, not using multi-stage builds.
- Migration runs after code deployment: new code fails because schema is old.
- No rollback plan: broken release requires forward-fix under pressure.

---

## Stage 16. Code Review, Refactoring & Technical Debt

### Tasks

1. Conduct pull request reviews: architecture adherence, Spring best practices, Kotlin idioms, security.
2. Check for common Spring anti-patterns: overly broad component scan, service-locator usage, field injection.
3. Check for common Kotlin anti-patterns: `!!` operator abuse, unnecessary `lateinit`, Java-style Kotlin.
4. Refactor package structure: enforce module boundaries, eliminate cyclic dependencies.
5. Extract shared logic into reusable modules or libraries (internal or open-source).
6. Reduce code duplication without premature abstraction.
7. Manage technical debt: identify, document, prioritize, and schedule repayment.
8. Review test coverage gaps and add missing tests before refactoring.

### Kotlin-Specific Concerns

- Refactoring to idiomatic Kotlin: replace `if-else` chains with `when` expressions, use `let`/`also`/`apply`/`run` appropriately (not excessively), prefer `val` over `var`, use `data class` for DTOs.
- Sealed classes for replacing type hierarchies: more exhaustive `when` matching, compiler-enforced completeness.
- Extension functions: powerful for readability, but can scatter logic if overused.
- Be cautious when refactoring classes that are Spring-proxied: changes to method visibility, class hierarchy, or final/open status can break AOP.

### Common Pain Points

- "Refactoring broke Spring wiring" — changed class hierarchy or package, broke component scan or AOP.
- Over-abstracting: creating generic frameworks where a simple direct implementation would suffice.
- Refactoring without sufficient test coverage: silent behavioral regressions.
- Large PRs that are impossible to review meaningfully.

---

## Stage 17. Java-to-Kotlin Migration

### Tasks

1. Assess the codebase: identify migration candidates (start with tests and utilities, then services, then core domain).
2. Use IntelliJ IDEA's automatic Java-to-Kotlin converter as a starting point (then clean up manually).
3. Migrate incrementally: one class/module at a time, keep Java and Kotlin coexisting.
4. Handle nullability at Java-Kotlin boundaries: add `@Nullable`/`@NonNull` (JSpecify) annotations to Java code before converting, or handle platform types carefully.
5. Replace Lombok patterns with Kotlin equivalents:
   - `@Data` → `data class`
   - `@Builder` → named/default parameters, `copy()`
   - `@Getter/@Setter` → Kotlin properties
   - `@Slf4j` → `companion object` with `LoggerFactory` or Kotlin logging library
6. Convert Java-style exception handling: replace checked exceptions with Kotlin idioms (sealed result types, `runCatching`).
7. Migrate collections: Java `List<String>` (mutable) → Kotlin `List<String>` (immutable) or `MutableList<String>`.
8. Update tests alongside production code: ensure behavioral equivalence.
9. Verify Spring compatibility after each conversion: context starts, AOP works, serialization unchanged.
10. Remove Lombok dependency when all Java code is migrated.

### Kotlin-Specific Concerns

- **Platform types (`T!`)**: Java code without nullability annotations appears as `T!` in Kotlin — neither nullable nor non-null. This is a source of runtime `NullPointerException`.
- **JPA entities**: do NOT convert to `data class` — use regular class with manual `equals`/`hashCode`.
- **Spring `@Configuration` classes**: after conversion to Kotlin, verify that `@Bean` methods work correctly (proxy requirements, return type inference).
- **`companion object` for static members**: Spring can inject into `companion object` `@Bean` factory methods, but the syntax differs from Java static methods.
- **Binary compatibility**: if the module has external consumers (other modules or services), changing Java to Kotlin can break binary compatibility. Plan for this.

### Common Pain Points

- Auto-converter produces unidiomatic Kotlin: `var` everywhere, Java-style null checks, unnecessary `!!`.
- Platform type traps: Kotlin code calling Java methods assumes non-null, crashes at runtime.
- Lombok removal: must convert all Lombok annotations before removing the dependency.
- `@Transactional` breaks: Kotlin class is `final` by default, forgot `plugin.spring`.
- Mixed Java/Kotlin modules: build order, annotation processing (kapt for Java annotations on Kotlin code), IDE confusion.

---

## Stage 18. Upgrades & Dependency Management

### Tasks

1. Plan upgrade path: read release notes and migration guides for major upgrades (Spring Boot 2.x → 3.x → 4.x, Kotlin 1.x → 2.x).
2. Identify breaking changes in your stack: `javax` → `jakarta` namespace (Boot 3), security configuration API changes, Hibernate 6 changes, JSpecify nullability (Boot 4).
3. Update Gradle wrapper version.
4. Update Kotlin version and compiler plugins.
5. Update Spring Boot version and dependency BOM.
6. Update third-party dependencies: check compatibility matrix.
7. Run full test suite after each version bump.
8. Handle deprecations: replace deprecated APIs before they're removed.
9. Address CVEs: monitor security advisories, patch vulnerable dependencies promptly.
10. Automate dependency updates: Dependabot, Renovate, or Gradle Version Catalog updates.

### Kotlin-Specific Concerns

- **Kotlin 1.x → 2.x**: K2 compiler is the default, some compiler plugin APIs changed, `kotlin-reflect` is lighter.
- **Spring Boot 4 / Spring Framework 7**: Kotlin 2.2 baseline, JSpecify nullability annotations enforced in Kotlin, new coroutine context propagation.
- `kotlinx.serialization` version must match Kotlin version.
- Kotlin Gradle plugin version must match Kotlin compiler version.

### Common Pain Points

- Upgrading Spring Boot without reading migration guide: auto-configuration breaks silently.
- Dependency version conflicts after upgrade: Jackson, Hibernate, Netty version mismatches.
- Deprecated APIs removed without warning: code compiles on old version, fails on new.
- Multi-step upgrade required but attempted as a single jump (e.g., Boot 2.7 → 4.0 instead of 2.7 → 3.0 → 3.3 → 4.0).
- CVE patching urgency conflicting with stability needs.

---

## Stage 19. Production Support & Evolution

### Tasks

#### Incident Response
1. Monitor alerts: error rate spikes, latency degradation, health check failures.
2. Triage incidents: quick mitigation first (feature flag, rollback, rate limit), then root cause analysis.
3. Analyze logs, metrics, and traces to localize the problem.
4. Implement hotfix with minimal risk, deploy, verify.
5. Write postmortem: timeline, root cause, impact, corrective actions, follow-ups.

#### Performance Optimization
6. Profile application: identify CPU, memory, I/O bottlenecks.
7. Optimize database queries: slow query log, EXPLAIN plans, index optimization.
8. Tune JVM: garbage collector selection, heap sizing, thread pool configuration.
9. Optimize connection pools: database, HTTP client, message broker connections.
10. Implement caching for hot paths: Spring Cache, Redis, local caches.

#### Continuous Improvement
11. Evolve the API: versioning strategy, backward compatibility, deprecation lifecycle.
12. Refactor based on production insights: simplify hot paths, remove unused features.
13. Improve observability based on incident learnings: add missing metrics, improve alert thresholds.
14. Update runbooks and operational documentation.

### Kotlin-Specific Concerns

- Coroutine debugging in production: `kotlinx-coroutines-debug` agent for enhanced stack traces (development only — has performance overhead).
- Kotlin inline functions and value classes: may behave differently at runtime than expected from source code (due to inlining/unboxing).
- Memory profiling: Kotlin object allocations (companion objects, lambda captures, coroutine state machines) may look different in profiler output compared to equivalent Java code.

### Common Pain Points

- Alert fatigue: too many noisy alerts, real issues get ignored.
- Incident without postmortem: same issue recurs.
- Performance optimization without measurement: guessing instead of profiling.
- "Hotfix" that introduces new bugs due to insufficient testing under time pressure.
- Knowledge silos: only one developer understands a critical service.

---

---

# Typical AI Requests Across the Pipeline

> A comprehensive catalog of real-world requests developers make to AI assistants at every pipeline stage. These are not abstract prompts — they represent the actual types of help that bring the most value during daily Kotlin + Spring development.

---

## Requests at Stage 1 — Requirements & API Contract Design

### Decomposition & Scoping
1. "Break down this business requirement into implementation sub-tasks for a Spring service: API layer, domain logic, data layer, integrations, tests. Flag where idempotency and retries matter."
2. "Given these user stories, identify the bounded contexts and propose which ones belong in the same module vs. separate services."
3. "What non-functional requirements am I likely forgetting for this use-case? (Think: SLA, rate limits, audit, data retention, GDPR.)"

### API Contract Design
4. "Design a REST API contract for an order service: POST `/orders`, GET `/orders/{id}`, PATCH `/orders/{id}/cancel`. Include request/response DTOs, error codes, idempotency key, versioning strategy, and example JSON payloads."
5. "Propose a unified error response format (RFC 7807 Problem Details or custom). Show how to implement it with `@ControllerAdvice` in Kotlin."
6. "Review this OpenAPI spec for consistency: are error codes correct? Are nullable fields aligned with Kotlin DTOs? Is versioning handled?"

### Architecture Decision Records
7. "Draft an ADR for choosing between transactional monolith and event-driven architecture for our payment processing module. Include trade-offs, risks, and the recommended option."

---

## Requests at Stage 2 — Architecture & Module Design

### Package Structure & Boundaries
8. "Propose a package structure for a Kotlin + Spring Boot application using feature-based organization. Show where controllers, services, repositories, DTOs, and config classes go."
9. "Define dependency rules between layers: what can import what? How do we enforce these boundaries (ArchUnit, module-info, or convention)?"
10. "Compare layered vs. hexagonal architecture for our use-case. When is hexagonal worth the overhead?"

### Domain Modeling
11. "Model the domain for a bonus accrual system using Kotlin sealed classes for operation results and value classes for domain IDs (`UserId`, `OrderId`). Show how these integrate with Spring and JPA."
12. "How should I model a state machine for order lifecycle (CREATED → PAID → SHIPPED → DELIVERED → CANCELLED) in Kotlin? Which pattern works best with Spring?"

---

## Requests at Stage 3 — Project Bootstrap & Gradle

### Build Configuration
13. "Generate a complete `build.gradle.kts` for a Spring Boot + Kotlin project with JUnit 5, MockK, Testcontainers, detekt, ktlint. Include `kotlin(\"plugin.spring\")` and `kotlin(\"plugin.jpa\")`."
14. "I'm getting `BeanCreationException` because my classes are final. Which Kotlin compiler plugins am I missing? Show the exact Gradle config."
15. "Set up a multi-module Gradle project with a shared `domain` module, a `web` module, and an `infrastructure` module. Show `settings.gradle.kts` and inter-module dependencies."

### Dependency & Version Issues
16. "My build fails with a Jackson version conflict after adding a new library. Here's `./gradlew dependencyInsight --dependency jackson-databind`. How do I resolve this?"
17. "Explain the difference between `implementation`, `api`, `runtimeOnly`, and `compileOnly` in Gradle. Which should I use for Spring Boot starters?"
18. "Should I use kapt or KSP for annotation processing? Which processors support KSP in 2025/2026?"

---

## Requests at Stage 4 — Spring Core Configuration & DI

### Bean Wiring & Lifecycle
19. "My app fails with `NoSuchBeanDefinitionException` for `PaymentClient`. Here's the stacktrace and my `@Configuration` classes. Diagnose the issue and propose a minimal fix."
20. "I have a circular dependency between `OrderService` and `InventoryService`. What are my options? (Restructure, `@Lazy`, events, interface extraction.)"
21. "Explain how Spring's conditional beans work: `@ConditionalOnProperty`, `@ConditionalOnMissingBean`, `@ConditionalOnClass`. When should I use each?"

### Configuration Properties
22. "Create a type-safe `@ConfigurationProperties` class in Kotlin for external API settings (baseUrl, connectTimeout, readTimeout, retryAttempts) with validation and sensible defaults."
23. "Why are my properties `null` at runtime even though they're in `application.yml`? Here's my Kotlin data class and YAML. Diagnose the binding issue."
24. "Set up `application.yml` with profiles for local, test, staging, and production. Secrets should come from environment variables, local dev uses Docker Compose defaults."

### AOP & Proxies
25. "Why is my `@Transactional` method not rolling back? I'm calling it from another method in the same class."
26. "Explain when Spring uses CGLIB vs. JDK dynamic proxies. How does `kotlin(\"plugin.spring\")` affect this?"

---

## Requests at Stage 5 — Web Layer (MVC / WebFlux)

### Controller Implementation
27. "Generate a Spring MVC controller in Kotlin for an order management API: create order, get by ID, cancel, list with pagination. Use constructor injection, DTOs as `data class`, Jakarta Validation."
28. "My validation annotations (`@NotBlank`, `@Email`) don't work on Kotlin data class fields. Show me the correct use-site targets (`@field:NotBlank`) and explain why."
29. "Implement a `@ControllerAdvice` that handles: validation errors (400), business exceptions (409/422), not-found (404), and unexpected errors (500) — all in a unified JSON format."

### Serialization Issues
30. "Jackson throws `HttpMessageNotReadableException` when deserializing my Kotlin DTO. I suspect it's the missing Kotlin module. Show me how to verify and fix."
31. "How do I distinguish between `null` and absent fields in a JSON PATCH request with Kotlin + Jackson? I need partial updates."
32. "Configure Jackson for ISO 8601 date/time serialization with timezone handling in a Spring Boot application."

### WebFlux & Coroutines
33. "Convert this Spring MVC controller to WebFlux with Kotlin coroutines: use `suspend fun` for handlers and `Flow` for streaming responses. What dependencies do I need?"
34. "How do I properly propagate MDC (logging context) in a coroutine-based WebFlux handler?"

---

## Requests at Stage 6 — Serialization (Jackson & Kotlin)

35. "My Kotlin data class has default parameter values, but Jackson ignores them when the field is missing from JSON. How do I fix this?"
36. "How do I serialize/deserialize Kotlin sealed classes with Jackson? Show the `@JsonTypeInfo` and `@JsonSubTypes` setup."
37. "Compare `jackson-module-kotlin` vs `kotlinx.serialization` for a Spring Boot project. When would I use each?"
38. "I have enum classes with properties in Kotlin. How do I ensure Jackson serializes them by name, not ordinal?"
39. "After upgrading Spring Boot, my Jackson configuration broke. How do I check which Jackson version Spring Boot manages and how to align custom modules?"

---

## Requests at Stage 7 — Business Logic & Service Layer

### Domain Logic
40. "Implement an `OrderService` that orchestrates: validate order → check inventory → reserve stock → create order → publish event. Show transaction boundaries and error handling."
41. "How should I model operation results in Kotlin services — exceptions, sealed classes (`Success`/`Failure`), or `Result<T>`? What works best with Spring's `@Transactional`?"
42. "Implement an idempotency check for order creation: use an idempotency key stored in the database. Show the service, the repository call, and the unique constraint."

### Caching
43. "Add `@Cacheable` to my product catalog service. Which cache provider should I use for development (Caffeine) vs. production (Redis)? Show the configuration."
44. "My `@Cacheable` doesn't work. The method is being called every time. Diagnose: is it self-invocation, missing proxy, or serialization issue?"

### Events
45. "How do I publish domain events from a Spring service? Compare `ApplicationEventPublisher` (in-process) vs. transactional outbox + Kafka (distributed). When to use which?"

---

## Requests at Stage 8 — Data Access (JPA / JDBC / R2DBC)

### Entity Modeling
46. "Create JPA entities for `Order` and `OrderItem` in Kotlin with correct relationships, fetch strategies, cascade settings, and `equals`/`hashCode` based on business key. Explain why `data class` is wrong here."
47. "My entity uses `lateinit var` for a field that's null during construction. Is this safe with JPA? What are the alternatives?"
48. "How do I use Kotlin value classes (`@JvmInline value class OrderId(val value: Long)`) with JPA? What about Spring Data repositories?"

### Queries & Performance
49. "I have N+1 queries when loading orders with their items. Here are my entities and repository. Propose fixes: fetch join, entity graph, batch size, DTO projection."
50. "Write a custom Spring Data JPA query using `@Query` with JPQL to fetch orders by status with pagination and sorting. Then show the equivalent native SQL approach."
51. "Explain `@EntityGraph` in Spring Data JPA. When is it better than `JOIN FETCH` in `@Query`?"

### Transactions
52. "Where should I place `@Transactional` in a use-case that creates an order, updates inventory, and sends a notification? What propagation level? What about the external HTTP call?"
53. "My transaction silently commits when an exception is thrown. Why? (Hint: checked vs. unchecked, `rollbackFor` rules.)"
54. "Explain the difference between `REQUIRED`, `REQUIRES_NEW`, and `NESTED` propagation. Give a concrete scenario for each."

---

## Requests at Stage 9 — Schema Migrations

55. "Write a Flyway migration to add a `status` column to the `orders` table with a default value, backfill existing rows, and add an index. Ensure zero-downtime compatibility."
56. "Plan an expand/contract migration for renaming a column: what are the steps across multiple deployments?"
57. "My Flyway migration fails with 'checksum mismatch'. What happened and how do I fix it safely?"
58. "How do I handle large table migrations (millions of rows) without locking the table? Show the batched approach."

---

## Requests at Stage 10 — Integrations & Resilience

### HTTP Clients
59. "Build a `RestClient` (Spring 6.1+) wrapper for an external payment API with configurable timeouts, retry on 5xx with exponential backoff + jitter, correlation ID propagation, and structured error handling."
60. "When should I use `RestClient` vs. `WebClient` vs. `RestTemplate`? My app is Spring MVC (blocking)."
61. "Configure a Resilience4j circuit breaker for the payment service call. Show the configuration, fallback behavior, and metrics."

### Messaging (Kafka)
62. "Implement a Kafka consumer for order events in Kotlin + Spring: deserialization, deduplication by event ID, DLQ on failure, retry with backoff, and metrics."
63. "Design a transactional outbox pattern: write events to the database in the same transaction as the domain change, then publish asynchronously."
64. "My Kafka consumer reprocesses the same message repeatedly. How do I implement deduplication?"

### Scheduling
65. "Implement a `@Scheduled` job that runs every 5 minutes but only on one instance in a cluster. Show ShedLock configuration."

---

## Requests at Stage 11 — Security

### Configuration
66. "Write a `SecurityFilterChain` configuration for a stateless JWT API: `/actuator/health` and `/actuator/prometheus` are public, all `/api/**` endpoints require authentication, `/api/admin/**` requires `ROLE_ADMIN`."
67. "Add method-level security with `@PreAuthorize` to the order cancellation endpoint: only the order owner or an admin can cancel."
68. "My app returns 403 instead of 401 for unauthenticated requests. Diagnose the filter chain and fix it."

### Security Review
69. "Review my Spring Security config for vulnerabilities: overly broad `permitAll`, missing CSRF protection, actuator endpoints exposed without auth, JWT without expiration/issuer check."
70. "How do I implement rate limiting for public API endpoints in Spring Boot? Compare Spring Cloud Gateway, Bucket4j, and Resilience4j RateLimiter."
71. "Configure CORS correctly for a React frontend on `localhost:3000` calling a Spring Boot API on `localhost:8080`. Ensure preflight requests work with JWT authentication."

---

## Requests at Stage 12 — Testing

### Unit Tests
72. "Generate unit tests for `OrderService` using JUnit 5 + MockK: test the happy path, idempotency duplicate handling, insufficient inventory error, and transaction rollback on failure."
73. "How do I test a Kotlin `suspend fun` in a service? Show `runTest` from `kotlinx-coroutines-test`."

### Web Layer (Slice) Tests
74. "Write a `@WebMvcTest` for `OrderController` with MockMvc: test successful creation (201), validation error (400), business error (409), and not-found (404)."
75. "How do I test a secured endpoint with `@WebMvcTest`? Show how to provide a mock JWT or security context."

### Data Layer (Slice) Tests
76. "Write a `@DataJpaTest` with Testcontainers PostgreSQL: apply Flyway migrations, test the repository, verify uniqueness constraint enforcement."

### Integration Tests
77. "Create a full `@SpringBootTest` integration test: HTTP request → service → database → response. Use Testcontainers for PostgreSQL and WireMock for external API stubs."
78. "My integration tests are slow because each test class starts a new Spring context. How do I share context across test classes?"

### Test Strategy
79. "For this use-case (order creation with payment and notification), propose which parts should be unit tested, which need slice tests, and which require full integration tests. Justify the split."
80. "My test is flaky — it fails intermittently. Here's the test code. Identify the non-deterministic element and fix it."

---

## Requests at Stage 13 — Observability

### Logging
81. "Set up structured JSON logging with Logback for a Spring Boot application. Include correlation ID propagation via MDC (`traceId`, `requestId`)."
82. "I need to log all incoming HTTP requests and outgoing responses, but without logging sensitive fields (passwords, tokens, PII). Show a filter-based approach."

### Metrics
83. "Add custom Micrometer metrics for order processing: a counter for orders created (by status), a timer for processing duration, and a gauge for pending orders in the queue."
84. "What metrics should I expose for a Spring Boot service to monitor SLOs? (Latency percentiles, error rates, saturation.) Show the Micrometer + Prometheus setup."
85. "My Prometheus storage is growing fast. I suspect high-cardinality labels. How do I find and fix them?"

### Tracing
86. "Configure OpenTelemetry distributed tracing in a Spring Boot + Kotlin application. Ensure trace context propagates across REST calls and Kafka messages."
87. "How do I propagate trace context in Kotlin coroutines? MDC is ThreadLocal-based, but coroutines switch threads."

### Health Checks
88. "Configure custom health indicators: check database connectivity, external API reachability, and Kafka broker status. Separate liveness and readiness probes."

---

## Requests at Stage 14 — Debugging & Incident Triage

### Stack Trace Analysis
89. "Here's a `LazyInitializationException` stack trace from production and the relevant code. Explain the root cause and propose 2–3 safe fixes with trade-offs."
90. "My app fails to start with a long Spring exception chain. Here's the full stack trace. Find the root cause in the `Caused by:` chain and propose a fix."
91. "I'm getting `NullPointerException` from a Java library call in Kotlin code. There's no `!!` in my code. Explain platform types and how to add null-safety at the boundary."

### Configuration & Wiring Issues
92. "My `@ConfigurationProperties` class has all null values at runtime even though `application.yml` has the correct keys. Diagnose: prefix mismatch? Missing `@EnableConfigurationProperties`? Wrong data type?"
93. "A bean that should be created by auto-configuration is missing. How do I debug which auto-configurations are active? (`--debug` flag, conditions evaluation report.)"

### Performance Issues
94. "An endpoint takes 5 seconds to respond. It was fast last week. Here are the recent code changes and current metrics. Help me localize the bottleneck."
95. "I suspect connection pool exhaustion. Here are the HikariCP metrics and thread dump. Diagnose and propose configuration changes."

### Transaction & Concurrency Bugs
96. "Data is partially saved after an error — the transaction should have rolled back but didn't. Here's the service code with `@Transactional`. Find the bug."
97. "I'm getting intermittent `ObjectOptimisticLockingFailureException`. Explain the cause and how to handle it gracefully."

---

## Requests at Stage 15 — CI/CD & Release

### CI Pipeline
98. "Set up a GitHub Actions CI pipeline for a Kotlin + Spring Boot project: compile → test → detekt → ktlint → build Docker image. Include Gradle caching."
99. "My CI build is slow (12 minutes). What can I do to speed it up? (Gradle caching, parallel test execution, test splitting.)"

### Containerization
100. "Write a multi-stage Dockerfile for a Spring Boot application: build with Gradle, run with JRE-only image, non-root user, layered JAR for efficient caching."
101. "How do I build a GraalVM native image for my Spring Boot + Kotlin application? What are the limitations and required configurations?"

### Deployment
102. "Write a Kubernetes deployment manifest for a Spring Boot application with liveness and readiness probes, resource limits, ConfigMap for properties, and Secret for credentials."
103. "How do I ensure database migrations run before the new application version starts serving traffic in a rolling deployment?"

---

## Requests at Stage 16 — Code Review & Refactoring

### Code Review
104. "Review this PR for a Spring + Kotlin service. Check for: transaction boundary correctness, proxy/AOP compatibility, Kotlin idiom violations, security risks, missing error handling."
105. "This code uses `!!` (not-null assertion) in 15 places. Is it justified? Propose safe alternatives for each occurrence."

### Refactoring
106. "Refactor this Java-style Kotlin code to be idiomatic: replace `if-null` checks with `?.let`, use `when` instead of `if-else` chains, extract DTOs to data classes, use sealed classes for errors."
107. "My service class is 500 lines long with 12 methods. Propose a decomposition strategy that respects Spring's DI and transaction model."
108. "I have cyclic dependencies between `OrderModule` and `PaymentModule`. Show how to break the cycle using events or interface extraction."

---

## Requests at Stage 17 — Java-to-Kotlin Migration

### File-by-File Migration
109. "Convert this Java service class (with `@Transactional`, `@Cacheable`, Lombok `@Data`, `@Builder`) to idiomatic Kotlin. Preserve behavior, remove Lombok, ensure AOP proxies still work."
110. "This Java class uses checked exceptions. How do I convert the exception handling to Kotlin? Show the sealed class result pattern and `runCatching` alternative."
111. "After converting a Java `@Configuration` class to Kotlin, my `@Bean` methods return inferred types. Is this safe? When do I need explicit return types?"

### Migration Strategy
112. "Plan a phased Java-to-Kotlin migration for a 200-class Spring Boot project. Where do I start? (Tests first, then utilities, then services, then domain.) What's the order of operations?"
113. "How do I handle platform types (`T!`) at the Java-Kotlin boundary? Where should I add JSpecify `@Nullable`/`@NonNull` annotations?"
114. "After migrating to Kotlin, my JPA entities broke: `equals`/`hashCode` issues, proxy failures, no-arg constructor missing. Show the correct Kotlin JPA entity pattern."

---

## Requests at Stage 18 — Upgrades & Dependency Management

### Spring Boot Upgrades
115. "Create a step-by-step migration checklist from Spring Boot 2.7 to 3.x: namespace changes (`javax` → `jakarta`), security config migration, Hibernate 6 changes, property renames."
116. "After upgrading Spring Boot, auto-configuration is broken. Here are the Gradle and runtime errors. Diagnose and propose a step-by-step fix."
117. "What changes does Spring Boot 4 (Spring Framework 7) introduce for Kotlin developers? (JSpecify nullability, Kotlin 2.2 baseline, coroutine context propagation.)"

### Kotlin & Gradle Upgrades
118. "I'm upgrading from Kotlin 1.9 to 2.x. What are the K2 compiler breaking changes I should watch for? Are all my compiler plugins compatible?"
119. "Upgrade Gradle from 7.x to 8.x in my project. What deprecated APIs need attention? Show the `settings.gradle.kts` changes."

### CVE & Dependency Patching
120. "Snyk reported a CVE in `log4j-core`. How do I check if my Spring Boot project is affected and force the patched version via Gradle?"
121. "I need to upgrade Jackson independently of the Spring Boot BOM due to a CVE. How do I override a single managed dependency without breaking the rest?"

---

## Requests at Stage 19 — Production Support & Evolution

### Incident Response
122. "Production shows a spike in 500 errors on `/api/orders`. Here are the logs from the last 30 minutes. Group the errors by root cause, propose a hotfix, and a long-term fix."
123. "Our service latency jumped from 50ms to 2 seconds after yesterday's deployment. Here are the metrics. Help me localize: is it DB, external API, or application code?"
124. "Write a postmortem template for our incident: timeline, impact, root cause, what went well, what went wrong, action items."

### Performance Optimization
125. "Profile this endpoint: it does 47 SQL queries per request. Show how to identify N+1 queries from logs and fix them."
126. "My service runs out of database connections under load. Here are HikariCP settings and metrics. Recommend pool size, timeout, and leak detection settings."
127. "Compare GC strategies for a Spring Boot service: G1GC vs. ZGC vs. Shenandoah. When does each make sense?"

### Operational Readiness
128. "Write a runbook for diagnosing common issues with this order service: 500 errors, high latency, database connection failures, Kafka consumer lag."
129. "What alerts should I set up for a Spring Boot microservice? (Error rate, latency p99, health check, connection pool utilization, Kafka consumer lag.) Show the Prometheus alerting rules."

---

## Cross-Cutting Requests (Applicable at Any Stage)

### Kotlin Idioms & Spring Compatibility
130. "Is it safe to use Kotlin `object` (singleton) in a Spring application? How does it interact with DI?"
131. "When should I use extension functions in a Spring project? Where do they help readability, and where do they scatter logic?"
132. "How do I use Kotlin coroutines with Spring MVC (not WebFlux)? Is it supported? What are the trade-offs?"

### Explaining Spring "Magic"
133. "How does Spring's auto-configuration actually work? Walk me through what happens when I add `spring-boot-starter-web` to my dependencies."
134. "Explain the complete lifecycle of an HTTP request in Spring MVC: from `DispatcherServlet` through filters, handler mapping, interceptors, controller, and back."
135. "What happens when I add `@Transactional` to a method? Trace the proxy creation, method interception, connection acquisition, commit/rollback."

### AI-Assisted Workflow Patterns
136. "I'm starting a new feature. Generate a task breakdown: API contract → DTOs → service interface → implementation → repository → migration → tests. For each, suggest what AI can generate and what I should review manually."
137. "Review this AI-generated code against Spring best practices. Check for: security holes, transaction correctness, Kotlin anti-patterns, missing edge cases."
138. "I have this AI-generated test. Is it actually testing behavior, or just mirroring the implementation? Identify mock-heavy tests that should be integration tests instead."

---

---

# AI Skills for Kotlin + Spring Development

> Each skill below is a **specialized behavioral mode** that improves AI assistant quality for a specific class of developer tasks. Skills are not isolated features — they compose and overlap. A single developer request often activates 2–3 skills simultaneously.
>
> **Cross-reference convention:** Each skill lists the AI requests it serves using `→ #N` notation, referencing the numbered requests in the [Typical AI Requests](#typical-ai-requests-across-the-pipeline) section above.

---

## SK-01. Project Context Ingestion

**Purpose:** Before answering any question, the AI must understand the project as a system — its modules, dependency graph, Spring version, Kotlin version, active profiles, and build configuration. This is the foundational skill that gates the quality of every other skill.

**What the AI does differently with this skill:**
- Reads `build.gradle.kts`, `settings.gradle.kts`, `application.yml`, and package structure before generating any code or diagnosis.
- Extracts version constraints (Kotlin, Spring Boot, Gradle, JDK) and uses them to filter out incompatible suggestions.
- Identifies the architectural style in use (layered, hexagonal, modular monolith) from package conventions.
- Detects which Kotlin compiler plugins are active (`plugin.spring`, `plugin.jpa`, `plugin.serialization`).

**Guardrails:**
- Never suggest APIs, annotations, or patterns from a different Spring Boot major version than the project uses.
- Never recommend dependencies without checking BOM compatibility.

**Serves requests:** → [#1–#3](#requests-at-stage-1--requirements--api-contract-design), [#8–#10](#requests-at-stage-2--architecture--module-design), [#13–#18](#requests-at-stage-3--project-bootstrap--gradle), [#133–#135](#cross-cutting-requests-applicable-at-any-stage)

---

## SK-02. Spring Context & DI Graph Reasoning

**Purpose:** Diagnose and resolve Spring application context failures — missing beans, circular dependencies, conditional bean mismatches, auto-configuration conflicts, profile issues.

**What the AI does differently with this skill:**
- Reconstructs the bean dependency graph from constructor parameters, `@Configuration` classes, component scan scopes, and `@Conditional` annotations.
- Traces `Caused by:` chains in stack traces to identify the root cause, not just the symptom.
- Generates ranked hypotheses (most probable first) and proposes the minimal fix for each.
- Understands the interplay between auto-configuration and custom `@Configuration`.

**Guardrails:**
- Prefer minimal fixes (add `@Qualifier`, fix profile, add missing `@ComponentScan` path) over broad changes.
- Never suggest `@ComponentScan("*")` or blanket `@SpringBootApplication(scanBasePackages = ...)` without justification.

**Serves requests:** → [#19–#21](#requests-at-stage-4--spring-core-configuration--di), [#25–#26](#requests-at-stage-4--spring-core-configuration--di), [#89–#93](#requests-at-stage-14--debugging--incident-triage), [#133–#135](#cross-cutting-requests-applicable-at-any-stage)

---

## SK-03. Kotlin ↔ Spring Proxy Compatibility

**Purpose:** Prevent and diagnose the class of issues where Kotlin's `final`-by-default semantics break Spring's proxy-based AOP (transactions, caching, async, security annotations).

**What the AI does differently with this skill:**
- Checks whether `kotlin("plugin.spring")` and `kotlin("plugin.jpa")` are present in the build.
- Detects self-invocation patterns where `@Transactional`/`@Cacheable` is called from within the same class (bypassing the proxy).
- Identifies methods or classes that need to be `open` for CGLIB proxying.
- Understands the difference between interface-based (JDK) and class-based (CGLIB) proxies.

**Guardrails:**
- Don't suggest making everything `open` manually — the compiler plugins handle this.
- Always explain *why* the proxy doesn't apply, not just *how* to fix it.

**Serves requests:** → [#14](#requests-at-stage-3--project-bootstrap--gradle), [#25–#26](#requests-at-stage-4--spring-core-configuration--di), [#44](#requests-at-stage-7--business-logic--service-layer), [#96](#requests-at-stage-14--debugging--incident-triage), [#109](#requests-at-stage-17--java-to-kotlin-migration), [#130](#cross-cutting-requests-applicable-at-any-stage)

---

## SK-04. Gradle Kotlin DSL Doctor

**Purpose:** Confidently generate, modify, and debug `build.gradle.kts` configurations without breaking the build or introducing version conflicts.

**What the AI does differently with this skill:**
- Distinguishes between plugin management, dependency management, BOMs, version catalogs, and build script DSL.
- Resolves dependency conflicts by analyzing `dependencyInsight` output and the Spring Boot BOM.
- Configures Kotlin compiler plugins, JVM toolchains, and test frameworks correctly.
- Generates minimal, correct diffs — not full file replacements.

**Guardrails:**
- Never guess dependency versions — derive them from the active BOM or explicit version catalog.
- Fix problems incrementally; don't rewrite the entire build file.

**Serves requests:** → [#13–#18](#requests-at-stage-3--project-bootstrap--gradle), [#99](#requests-at-stage-15--cicd--release), [#118–#121](#requests-at-stage-18--upgrades--dependency-management)

---

## SK-05. Dependency Conflict Resolver

**Purpose:** Diagnose and resolve classpath conflicts — especially the recurring version mismatches between Jackson, Hibernate, Netty, SLF4J, and Logback that arise when overriding Spring Boot BOM-managed versions.

**What the AI does differently with this skill:**
- Reads `./gradlew dependencies` and `dependencyInsight` output to find the conflict path.
- Understands Spring Boot's BOM version management and when it's safe to override.
- Distinguishes between `implementation`, `api`, `runtimeOnly`, `compileOnly`, `annotationProcessor`, and `testImplementation`.

**Guardrails:**
- Never propose random version numbers. Always trace versions to the active BOM or a known compatibility matrix.
- Prefer removing explicit versions (letting the BOM manage) over adding `force` or `exclude`.

**Serves requests:** → [#16–#17](#requests-at-stage-3--project-bootstrap--gradle), [#39](#requests-at-stage-6--serialization-jackson--kotlin), [#116](#requests-at-stage-18--upgrades--dependency-management), [#120–#121](#requests-at-stage-18--upgrades--dependency-management)

---

## SK-06. Spring MVC / WebFlux API Builder

**Purpose:** Generate correct, idiomatic Kotlin controller code with proper validation, error handling, serialization, and documentation — not just code that "looks right" but code that compiles and works.

**What the AI does differently with this skill:**
- Uses `@field:` use-site targets for Jakarta Validation annotations on Kotlin `data class` properties.
- Generates `@ControllerAdvice` with unified error response format and correct HTTP status mapping.
- Handles nullable vs. optional vs. absent fields in request DTOs.
- Generates OpenAPI-compatible endpoint signatures.
- For WebFlux: uses `suspend fun` and `Flow<T>` return types with proper coroutine setup.

**Guardrails:**
- Never mix MVC and WebFlux in the same module without explicit acknowledgment.
- Always verify `jackson-module-kotlin` is registered when generating Kotlin DTOs.

**Serves requests:** → [#4–#6](#requests-at-stage-1--requirements--api-contract-design), [#27–#34](#requests-at-stage-5--web-layer-mvc--webflux), [#40](#requests-at-stage-7--business-logic--service-layer)

---

## SK-07. Error Model & Validation Architect

**Purpose:** Design and implement a consistent error handling strategy across the entire application — from validation errors to business exceptions to unexpected failures.

**What the AI does differently with this skill:**
- Designs a unified error response schema (RFC 7807 or custom) with field-level validation details.
- Maps exception types to HTTP status codes systematically: 400 (validation), 404 (not found), 409 (conflict), 422 (business rule violation), 500 (unexpected).
- Ensures internal details (stack traces, class names, SQL errors) never leak to the client.
- Generates `@ControllerAdvice` that covers all exception paths, including framework-generated ones.

**Guardrails:**
- Never expose internal exception messages in production error responses.
- Always log the full error server-side while returning a sanitized version to the client.

**Serves requests:** → [#5](#requests-at-stage-1--requirements--api-contract-design), [#29](#requests-at-stage-5--web-layer-mvc--webflux), [#41](#requests-at-stage-7--business-logic--service-layer), [#68](#requests-at-stage-11--security)

---

## SK-08. Jackson & Kotlin Serialization Specialist

**Purpose:** Prevent and fix the serialization/deserialization issues that are uniquely problematic at the Kotlin + Jackson intersection — default parameter values, nullability, sealed classes, date/time handling.

**What the AI does differently with this skill:**
- Verifies `jackson-module-kotlin` presence and version compatibility.
- Correctly handles Kotlin `data class` default parameter values (which require the Kotlin module to work).
- Configures polymorphic serialization for sealed classes using `@JsonTypeInfo`/`@JsonSubTypes`.
- Distinguishes between `null` (field present, value null) and absent (field not in JSON) — a critical distinction for PATCH operations.

**Guardrails:**
- Never suggest `var` in DTOs just to satisfy Jackson — use the Kotlin module instead.
- Always specify date/time format explicitly rather than relying on defaults.

**Serves requests:** → [#30–#32](#requests-at-stage-5--web-layer-mvc--webflux), [#35–#39](#requests-at-stage-6--serialization-jackson--kotlin)

---

## SK-09. Transaction & Consistency Designer

**Purpose:** Help developers place `@Transactional` boundaries correctly, choose appropriate propagation/isolation levels, and avoid the most common transactional bugs.

**What the AI does differently with this skill:**
- Analyzes the use-case flow to identify where transactions should start and end.
- Detects anti-patterns: transactions spanning external HTTP calls, self-invocation bypassing proxies, wrong rollback rules.
- Recommends propagation levels with concrete scenarios (`REQUIRED` vs. `REQUIRES_NEW` vs. `NESTED`).
- Designs idempotency strategies: idempotency keys, unique constraints, conditional inserts.

**Guardrails:**
- Never put `@Transactional` on every method by default.
- Always flag when a transaction holds a database connection during an external call.

**Serves requests:** → [#42](#requests-at-stage-7--business-logic--service-layer), [#52–#54](#requests-at-stage-8--data-access-jpa--jdbc--r2dbc), [#96–#97](#requests-at-stage-14--debugging--incident-triage), [#135](#cross-cutting-requests-applicable-at-any-stage)

---

## SK-10. JPA / Spring Data Kotlin Mapper

**Purpose:** Generate correct JPA entity mappings in Kotlin, avoiding the traps that catch every Kotlin developer — `data class` as entity, broken `equals`/`hashCode`, lazy loading issues, N+1 queries.

**What the AI does differently with this skill:**
- Never generates JPA entities as `data class`. Uses regular classes with manual `equals`/`hashCode` based on business key.
- Configures fetch strategies to avoid N+1: `@EntityGraph`, `JOIN FETCH`, batch fetching, DTO projections.
- Handles Kotlin nullability in entity fields: nullable for optional associations, non-null for required columns.
- Validates that `kotlin("plugin.jpa")` is present for no-arg constructor generation.

**Guardrails:**
- Always explain *why* `data class` is wrong for JPA entities (broken identity semantics with mutable state and lazy proxies).
- Flag `lateinit var` usage in entities and suggest alternatives.

**Serves requests:** → [#46–#51](#requests-at-stage-8--data-access-jpa--jdbc--r2dbc), [#89](#requests-at-stage-14--debugging--incident-triage), [#114](#requests-at-stage-17--java-to-kotlin-migration)

---

## SK-11. Schema Migration Planner

**Purpose:** Generate safe, reversible database migrations and plan zero-downtime schema changes using the expand/contract pattern.

**What the AI does differently with this skill:**
- Plans multi-step migrations: add column → backfill → switch reads → drop old column.
- Considers table size and lock duration for production databases.
- Generates Flyway/Liquibase scripts with rollback strategies.
- Validates that entity model changes are consistent with migration scripts.

**Guardrails:**
- Never generate destructive migrations (DROP COLUMN, DROP TABLE) without an explicit multi-step plan.
- Always consider backward compatibility with the currently deployed code version.

**Serves requests:** → [#55–#58](#requests-at-stage-9--schema-migrations), [#103](#requests-at-stage-15--cicd--release)

---

## SK-12. Integration & Resilience Engineer

**Purpose:** Build reliable integrations with external services and message brokers — with proper timeouts, retries, circuit breakers, idempotency, and dead letter queues.

**What the AI does differently with this skill:**
- Configures HTTP clients (`RestClient`, `WebClient`) with explicit connect/read/write timeouts.
- Implements retry policies with exponential backoff + jitter (not fixed interval).
- Sets up circuit breakers (Resilience4j) with sensible thresholds and fallback behavior.
- Designs Kafka consumers with deduplication, DLQ, and transactional outbox patterns.
- Implements distributed scheduling with ShedLock.

**Guardrails:**
- Never retry non-idempotent operations without explicit confirmation.
- Never set retry count without also setting a backoff ceiling and jitter.
- Always flag when retries could cause a thundering herd.

**Serves requests:** → [#59–#65](#requests-at-stage-10--integrations--resilience), [#45](#requests-at-stage-7--business-logic--service-layer)

---

## SK-13. Spring Security Configurator & Auditor

**Purpose:** Generate secure, correct `SecurityFilterChain` configurations and audit existing security setups for common vulnerabilities.

**What the AI does differently with this skill:**
- Uses Spring Security's Kotlin DSL for concise configuration.
- Explicitly enumerates public endpoints (whitelist approach, not blacklist).
- Configures JWT validation with expiration, issuer, and audience checks.
- Separates authentication failures (401) from authorization failures (403).
- Verifies CORS configuration works with preflight requests and credentials.
- Audits for common mistakes: overly broad `permitAll`, actuator endpoints exposed, missing `@EnableMethodSecurity`.

**Guardrails:**
- Never disable CSRF without explaining why it's safe (stateless API with token auth).
- Never generate security configurations without a corresponding test.

**Serves requests:** → [#66–#71](#requests-at-stage-11--security), [#75](#requests-at-stage-12--testing), [#102](#requests-at-stage-15--cicd--release)

---

## SK-14. Test Suite Builder (Three-Layer Strategy)

**Purpose:** Generate tests at the right level of abstraction — unit tests for logic, slice tests for framework integration, full integration tests for end-to-end flows — using Kotlin-idiomatic tooling.

**What the AI does differently with this skill:**
- Proposes a test strategy before writing tests: what to unit test, what needs a slice test, what requires Testcontainers.
- Uses MockK (not Mockito) for Kotlin-idiomatic mocking: `every { ... } returns ...`, `verify { ... }`.
- Uses backtick method names for readable test descriptions: `` `should return 404 when order not found` ``.
- Generates `@WebMvcTest` / `@DataJpaTest` / `@SpringBootTest` with correct annotations and minimal context.
- Tests coroutines with `runTest` from `kotlinx-coroutines-test`.
- Generates test data factories, not inline object construction repeated in every test.

**Guardrails:**
- Don't default to `@SpringBootTest` when a slice test suffices.
- Don't test mocks — test behavior. Flag tests that only verify mock interactions.
- Make tests deterministic: no `System.currentTimeMillis()`, no random without seeds, no shared mutable state.

**Serves requests:** → [#72–#80](#requests-at-stage-12--testing), [#138](#cross-cutting-requests-applicable-at-any-stage)

---

## SK-15. Stacktrace & Log Triage

**Purpose:** Rapidly convert stack traces, error logs, and metric anomalies into root cause diagnoses with concrete, minimal fixes.

**What the AI does differently with this skill:**
- Classifies errors by category: DI/bean lifecycle, configuration binding, serialization, database/SQL, security, timeout, concurrency.
- Navigates Spring's deeply nested `Caused by:` chains to find the actual root cause.
- Distinguishes symptoms from causes (e.g., `BeanCreationException` is the symptom; missing dependency is the cause).
- Proposes a quick hotfix and a proper long-term fix separately.
- For Kotlin-specific issues: identifies platform type `NullPointerException`, coroutine stack trace issues, inline function trace gaps.

**Guardrails:**
- Clearly label hypotheses vs. confirmed diagnoses.
- Never propose risky fixes (schema changes, version upgrades) as hotfixes.

**Serves requests:** → [#89–#97](#requests-at-stage-14--debugging--incident-triage), [#122–#123](#requests-at-stage-19--production-support--evolution)

---

## SK-16. Configuration Properties & Profiles (Kotlin-safe)

**Purpose:** Make Spring configuration predictable — type-safe property binding, profile management, environment-specific overrides, and secret handling.

**What the AI does differently with this skill:**
- Generates `@ConfigurationProperties` classes in Kotlin with correct `@ConstructorBinding`, default values, and `@Validated` annotations.
- Diagnoses binding failures: prefix mismatch, `kebab-case` vs. `camelCase`, wrong type, missing `@EnableConfigurationProperties`.
- Designs profile hierarchy: `application.yml` → `application-{profile}.yml` → environment variables → secrets.
- Handles the Kotlin-specific trap: non-null fields without defaults fail silently if the property source is missing.

**Guardrails:**
- Never hardcode secrets in configuration files, even in examples.
- Always validate required properties at startup, not at first use.

**Serves requests:** → [#22–#24](#requests-at-stage-4--spring-core-configuration--di), [#92–#93](#requests-at-stage-14--debugging--incident-triage)

---

## SK-17. Observability Integrator

**Purpose:** Make services operationally self-explanatory — structured logging, actionable metrics, distributed tracing, health checks.

**What the AI does differently with this skill:**
- Configures structured JSON logging with MDC-based correlation IDs (`traceId`, `requestId`).
- Designs Micrometer metrics that answer operational questions: latency by endpoint, error rate by type, connection pool saturation.
- Controls metric tag cardinality to prevent storage explosion.
- Configures OpenTelemetry / Micrometer Tracing for cross-service trace propagation.
- For Kotlin coroutines: ensures MDC context propagation using `MDCContext` or Spring Framework 7's automatic propagation.
- Sets up health indicators with separate liveness and readiness semantics.

**Guardrails:**
- Never expose actuator endpoints publicly without authentication.
- Never log PII (personal data, tokens, passwords) even at DEBUG level.
- Don't add metrics "just in case" — each metric should tie to an alert or operational question.

**Serves requests:** → [#81–#88](#requests-at-stage-13--observability), [#128–#129](#requests-at-stage-19--production-support--evolution)

---

## SK-18. Performance & Concurrency Advisor

**Purpose:** Identify and resolve performance bottlenecks — database queries, connection pools, thread pools, JVM tuning — and Kotlin-specific concurrency concerns (coroutines, dispatchers, blocking bridges).

**What the AI does differently with this skill:**
- Analyzes slow endpoints: SQL query count, external call latency, connection pool contention.
- Tunes HikariCP settings: pool size, connection timeout, leak detection.
- Recommends caching strategies (Caffeine for local, Redis for distributed) with correct invalidation.
- For Kotlin coroutines: prevents blocking the event loop, selects correct dispatchers, manages context propagation.
- Advises on JVM GC selection: G1GC (default), ZGC (low latency), Shenandoah (large heaps).

**Guardrails:**
- Never optimize without measurement — always require profiling data or metrics before recommending changes.
- Never suggest "make everything async" without analyzing whether the bottleneck is I/O-bound or CPU-bound.

**Serves requests:** → [#43–#44](#requests-at-stage-7--business-logic--service-layer), [#49](#requests-at-stage-8--data-access-jpa--jdbc--r2dbc), [#94–#95](#requests-at-stage-14--debugging--incident-triage), [#125–#127](#requests-at-stage-19--production-support--evolution), [#132](#cross-cutting-requests-applicable-at-any-stage)

---

## SK-19. Java → Kotlin Migration Assistant

**Purpose:** Safely convert Java code to idiomatic Kotlin while preserving behavior, Spring compatibility, and API contracts — one class at a time without stopping development.

**What the AI does differently with this skill:**
- Converts constructors, fields, and accessors to Kotlin properties — not just mechanical translation.
- Replaces Lombok patterns with Kotlin equivalents: `@Data` → `data class`, `@Builder` → default/named params, `@Slf4j` → `companion object` logger.
- Handles platform types (`T!`) at Java-Kotlin boundaries by adding nullability annotations or Kotlin-side null checks.
- Verifies that AOP proxies, JPA entities, and serialization still work after conversion.
- Plans migration order: tests first → utilities → services → domain → entities.

**Guardrails:**
- Never convert JPA entities to `data class`.
- Never remove `open` modifiers needed for Spring proxying.
- Never change public API signatures during migration unless explicitly requested.
- Flag binary compatibility risks when the module has external consumers.

**Serves requests:** → [#109–#114](#requests-at-stage-17--java-to-kotlin-migration), [#106](#requests-at-stage-16--code-review--refactoring)

---

## SK-20. Kotlin Idiomatic Refactorer (Spring-Aware)

**Purpose:** Transform Java-flavored Kotlin into idiomatic Kotlin while respecting Spring's proxy, serialization, and DI constraints.

**What the AI does differently with this skill:**
- Replaces `if (x != null)` with `?.let`/`?.run` where appropriate (but not in every case — readability matters).
- Uses `when` expressions instead of `if-else` chains.
- Applies `sealed class`/`sealed interface` for domain error types and result models.
- Uses `data class` for DTOs, regular class for entities, `value class` for domain primitives.
- Verifies refactored code doesn't break: AOP proxies (class must be openable), serialization (Jackson compatibility), DI (constructor injection still works).

**Guardrails:**
- Don't refactor for "beauty" if it risks breaking proxies or serialization.
- Don't overuse scope functions (`let`, `also`, `apply`, `run`) — clarity beats conciseness.
- Verify with tests before and after every refactoring step.

**Serves requests:** → [#105–#108](#requests-at-stage-16--code-review--refactoring), [#131](#cross-cutting-requests-applicable-at-any-stage)

---

## SK-21. Code Review Agent (Spring + Kotlin)

**Purpose:** Review pull requests with Spring-aware and Kotlin-aware judgment — not just style comments, but architectural risks, transaction bugs, security holes, and proxy pitfalls.

**What the AI does differently with this skill:**
- Checks transaction boundaries: `@Transactional` placement, self-invocation, rollback rules, connection holding.
- Checks proxy compatibility: final classes, method visibility, AOP annotation placement.
- Checks serialization: Jackson/Kotlin compatibility, nullable fields, missing modules.
- Checks security: overly permissive matchers, missing auth on new endpoints, secrets in config.
- Checks Kotlin idioms: `!!` abuse, `lateinit` misuse, Java patterns in Kotlin.
- Checks for missing tests, especially for error paths and edge cases.

**Guardrails:**
- Distinguish between "must fix" (bugs, security) and "consider" (style, minor improvements).
- Don't nitpick style when the PR is focused on a critical bugfix.

**Serves requests:** → [#104–#105](#requests-at-stage-16--code-review--refactoring), [#137](#cross-cutting-requests-applicable-at-any-stage)

---

## SK-22. Upgrade & Breaking Change Navigator

**Purpose:** Plan and execute major version upgrades (Spring Boot, Kotlin, Gradle, JDK) with systematic breaking-change checklists and incremental migration steps.

**What the AI does differently with this skill:**
- Reads official migration guides and generates a project-specific checklist based on actual dependencies used.
- Plans multi-step upgrade paths (e.g., Boot 2.7 → 3.0 → 3.3 → 4.0, not a single jump).
- Identifies framework-specific changes: `javax` → `jakarta`, Security DSL migration, Hibernate 6 behavior changes, JSpecify nullability.
- For Kotlin upgrades: K2 compiler plugin compatibility, `kotlin-reflect` changes, `kotlinx.serialization` version matching.
- Proposes verification steps after each upgrade: compile → test → runtime smoke.

**Guardrails:**
- Never skip major versions in an upgrade sequence.
- Always run the full test suite between upgrade steps.
- Flag dependencies that lag behind the upgrade (e.g., a library not yet compatible with Jakarta).

**Serves requests:** → [#115–#119](#requests-at-stage-18--upgrades--dependency-management), [#117](#requests-at-stage-18--upgrades--dependency-management)

---

## SK-23. CI/CD & Containerization Advisor

**Purpose:** Design build pipelines, write Dockerfiles, and configure deployment strategies for Spring Boot + Kotlin applications.

**What the AI does differently with this skill:**
- Generates multi-stage Dockerfiles with layered JARs for efficient caching.
- Configures CI pipelines (GitHub Actions, GitLab CI) with Gradle caching, parallel test execution, and artifact publishing.
- Sets up Kubernetes manifests with health probes, resource limits, and config management.
- Coordinates database migrations with deployment (migrations run before new code).
- For GraalVM native images: generates reflection hints and AOT configuration.

**Guardrails:**
- Never use `latest` tags for base images in production Dockerfiles.
- Always run as non-root in containers.
- Ensure CI and local development use the same JDK version.

**Serves requests:** → [#98–#103](#requests-at-stage-15--cicd--release)

---

## SK-24. Production Incident Responder

**Purpose:** Guide incident triage from first alert to postmortem — prioritizing service restoration over root cause analysis, then producing actionable follow-ups.

**What the AI does differently with this skill:**
- Follows the incident response priority: **mitigate first** (rollback, feature flag, rate limit), **then** diagnose.
- Analyzes logs, metrics, and traces to localize the problem layer (app code, database, external dependency, infrastructure).
- Correlates timeline of changes (deployments, config changes) with the incident start.
- Generates postmortem structure: timeline, impact, root cause, contributing factors, action items.
- Proposes both quick mitigation and long-term fix as separate recommendations.

**Guardrails:**
- Never propose code changes as the initial incident response — start with reversible mitigations.
- Always distinguish confirmed root cause from working hypothesis.

**Serves requests:** → [#122–#124](#requests-at-stage-19--production-support--evolution), [#128–#129](#requests-at-stage-19--production-support--evolution)

---

## SK-25. Domain Decomposition & API Design Advisor

**Purpose:** Help decompose business requirements into bounded contexts, service boundaries, and well-designed API contracts before any code is written.

**What the AI does differently with this skill:**
- Breaks down business requirements into use-cases, identifying integration points, idempotency needs, and consistency boundaries.
- Proposes module/service boundaries based on domain coupling analysis.
- Designs REST API contracts with proper HTTP semantics, error codes, versioning, and idempotency keys.
- Drafts ADRs (Architecture Decision Records) with trade-offs and alternatives.
- Models domain concepts using Kotlin idioms: sealed classes for states, value classes for IDs, data classes for DTOs.

**Guardrails:**
- Don't design microservices where a module boundary suffices.
- Always surface non-functional requirements (SLA, GDPR, audit) that the developer may have omitted.

**Serves requests:** → [#1–#7](#requests-at-stage-1--requirements--api-contract-design), [#8–#12](#requests-at-stage-2--architecture--module-design), [#136](#cross-cutting-requests-applicable-at-any-stage)

---

## Summary: Skills at a Glance

| Code | Skill | Primary Focus | Key Requests |
|------|-------|--------------|--------------|
| SK-01 | [Project Context Ingestion](#sk-01-project-context-ingestion) | Read project before responding | #1–3, #8–10, #13–18, #133–135 |
| SK-02 | [Spring Context & DI Graph Reasoning](#sk-02-spring-context--di-graph-reasoning) | Bean wiring, startup failures | #19–21, #25–26, #89–93, #133–135 |
| SK-03 | [Kotlin ↔ Spring Proxy Compatibility](#sk-03-kotlin--spring-proxy-compatibility) | AOP, final classes, self-invocation | #14, #25–26, #44, #96, #109, #130 |
| SK-04 | [Gradle Kotlin DSL Doctor](#sk-04-gradle-kotlin-dsl-doctor) | Build config, plugins | #13–18, #99, #118–121 |
| SK-05 | [Dependency Conflict Resolver](#sk-05-dependency-conflict-resolver) | Version conflicts, BOM | #16–17, #39, #116, #120–121 |
| SK-06 | [Spring MVC / WebFlux API Builder](#sk-06-spring-mvc--webflux-api-builder) | Controllers, DTOs, validation | #4–6, #27–34, #40 |
| SK-07 | [Error Model & Validation Architect](#sk-07-error-model--validation-architect) | Unified errors, HTTP status mapping | #5, #29, #41, #68 |
| SK-08 | [Jackson & Kotlin Serialization](#sk-08-jackson--kotlin-serialization-specialist) | JSON, nullability, sealed classes | #30–32, #35–39 |
| SK-09 | [Transaction & Consistency Designer](#sk-09-transaction--consistency-designer) | @Transactional, idempotency | #42, #52–54, #96–97, #135 |
| SK-10 | [JPA / Spring Data Kotlin Mapper](#sk-10-jpa--spring-data-kotlin-mapper) | Entities, N+1, fetch strategies | #46–51, #89, #114 |
| SK-11 | [Schema Migration Planner](#sk-11-schema-migration-planner) | Flyway, zero-downtime DDL | #55–58, #103 |
| SK-12 | [Integration & Resilience Engineer](#sk-12-integration--resilience-engineer) | HTTP clients, Kafka, retries | #45, #59–65 |
| SK-13 | [Spring Security Configurator & Auditor](#sk-13-spring-security-configurator--auditor) | AuthN/AuthZ, CORS, JWT | #66–71, #75, #102 |
| SK-14 | [Test Suite Builder](#sk-14-test-suite-builder-three-layer-strategy) | Unit/slice/integration tests | #72–80, #138 |
| SK-15 | [Stacktrace & Log Triage](#sk-15-stacktrace--log-triage) | Root cause diagnosis | #89–97, #122–123 |
| SK-16 | [Configuration & Profiles](#sk-16-configuration-properties--profiles-kotlin-safe) | Property binding, profiles | #22–24, #92–93 |
| SK-17 | [Observability Integrator](#sk-17-observability-integrator) | Logging, metrics, tracing | #81–88, #128–129 |
| SK-18 | [Performance & Concurrency Advisor](#sk-18-performance--concurrency-advisor) | Bottlenecks, pools, coroutines | #43–44, #49, #94–95, #125–127, #132 |
| SK-19 | [Java → Kotlin Migration Assistant](#sk-19-java--kotlin-migration-assistant) | Incremental conversion, Lombok | #106, #109–114 |
| SK-20 | [Kotlin Idiomatic Refactorer](#sk-20-kotlin-idiomatic-refactorer-spring-aware) | Kotlin idioms, Spring-safe | #105–108, #131 |
| SK-21 | [Code Review Agent](#sk-21-code-review-agent-spring--kotlin) | PR review, risk detection | #104–105, #137 |
| SK-22 | [Upgrade & Breaking Change Navigator](#sk-22-upgrade--breaking-change-navigator) | Spring/Kotlin/Gradle upgrades | #115–119 |
| SK-23 | [CI/CD & Containerization Advisor](#sk-23-cicd--containerization-advisor) | Docker, pipelines, K8s | #98–103 |
| SK-24 | [Production Incident Responder](#sk-24-production-incident-responder) | Triage, mitigation, postmortem | #122–124, #128–129 |
| SK-25 | [Domain Decomposition & API Design](#sk-25-domain-decomposition--api-design-advisor) | Requirements, contracts, ADRs | #1–12, #136 |

---

## How Skills Compose: Typical Multi-Skill Activations

Most developer requests activate multiple skills simultaneously. Here are common compositions:

| Developer Scenario | Skills Activated |
|-------------------|-----------------|
| "App won't start — here's the stacktrace" | SK-01 → SK-15 → SK-02 (or SK-03) |
| "Build a new REST endpoint" | SK-01 → SK-06 → SK-07 → SK-08 → SK-14 |
| "My @Transactional doesn't work" | SK-03 → SK-09 → SK-15 |
| "Convert this Java class to Kotlin" | SK-01 → SK-19 → SK-03 → SK-14 |
| "Upgrade Spring Boot from 2.7 to 3.x" | SK-01 → SK-22 → SK-04 → SK-05 |
| "Production 500 errors spiked" | SK-24 → SK-15 → SK-17 |
| "Generate tests for this service" | SK-01 → SK-14 → SK-09 |
| "Add Kafka consumer with retries" | SK-01 → SK-12 → SK-14 |
| "Review this PR" | SK-21 → SK-03 → SK-09 → SK-13 |
| "N+1 queries in order loading" | SK-10 → SK-18 → SK-14 |

---

---

# Skill Prioritization: Frequency × Impact Matrix

> A two-factor prioritization of all 25 skills. Each skill is scored on two independent axes, and the scores are multiplied to produce a composite priority.

## Scoring Methodology

### Axis 1: Task Frequency — "How often does a Kotlin/Spring developer encounter this task?"

Based on industry survey data (IDC 2024, Stack Overflow 2025, SonarSource 2025, Tidelift/New Stack) and the three source specification documents:

| Score | Label | Meaning |
|-------|-------|---------|
| 5 | **Daily** | Happens every working day: writing code, debugging, running tests |
| 4 | **Several times/week** | Multiple times a week: reviewing PRs, fixing config, resolving build issues |
| 3 | **Weekly** | ~Once a week: security changes, new API endpoint, serialization issues |
| 2 | **Periodic** | Every few weeks: migration scripts, upgrade planning, new integrations |
| 1 | **Rare** | A few times per year: major Spring Boot upgrade, project bootstrap, architecture redesign |

### Axis 2: Skill Impact — "How much does a specialized skill improve AI quality vs. generic LLM behavior?"

Based on AI productivity research (METR 2025, Google RCT 2024, Faros AI 2025) and analysis of where generic LLMs fail most on Spring/Kotlin tasks:

| Score | Label | Meaning |
|-------|-------|---------|
| 5 | **Transformative** | Generic LLM gives wrong/dangerous answers without this skill; the skill turns failure into reliable output. Area where specialized knowledge produces verifiably correct results (compiles, tests pass, context starts). |
| 4 | **High** | Generic LLM gives incomplete or fragile answers; the skill closes critical gaps. Results are measurably better. |
| 3 | **Significant** | Generic LLM is roughly adequate but misses important nuances; the skill adds meaningful quality. |
| 2 | **Moderate** | Generic LLM handles the basics; the skill adds polish and consistency. |
| 1 | **Marginal** | Generic LLM already performs well; the skill adds minor refinements. |

### Composite Score

**Priority = Frequency × Impact** (range: 1–25)

---

## Priority Rankings

### Tier 1 — Critical (Score ≥ 16) — Build These First

| Rank | Skill | Freq | Impact | Score | Rationale |
|------|-------|------|--------|-------|-----------|
| **1** | [SK-15 Stacktrace & Log Triage](#sk-15-stacktrace--log-triage) | 5 | 5 | **25** | Debugging is 30–50% of developer time (IDC 2024). Spring stack traces are notoriously deep and misleading. Generic LLMs frequently misidentify root cause in `Caused by:` chains. A specialized skill produces correct diagnoses where generic fails — and the result is immediately verifiable. |
| **2** | [SK-02 Spring Context & DI Reasoning](#sk-02-spring-context--di-graph-reasoning) | 5 | 5 | **25** | DI/bean failures happen daily — every config change, every new bean, every profile switch can trigger them. The bean dependency graph is implicit and complex. Generic LLMs hallucinate bean names and give non-minimal fixes. Skill output is binary: context starts or it doesn't. |
| **3** | [SK-14 Test Suite Builder](#sk-14-test-suite-builder-three-layer-strategy) | 5 | 4 | **20** | Testing is ~12–15% of developer time, happens daily, and AI test generation is one of the highest-impact use-cases (40%+ faster test creation per studies). But generic LLMs over-use `@SpringBootTest`, generate mock-heavy tests that don't catch regressions, and don't know MockK idioms. |
| **4** | [SK-03 Kotlin ↔ Spring Proxy Compatibility](#sk-03-kotlin--spring-proxy-compatibility) | 4 | 5 | **20** | This is the #1 Kotlin-specific trap in Spring. Every `@Transactional`, `@Cacheable`, `@Async` can silently fail due to Kotlin's final-by-default. Generic LLMs almost never check for `plugin.spring` or diagnose self-invocation. The skill prevents production bugs that are invisible at compile time. |
| **5** | [SK-06 Spring MVC/WebFlux API Builder](#sk-06-spring-mvc--webflux-api-builder) | 4 | 5 | **20** | Building endpoints is the most common code generation task. Generic LLMs get Kotlin validation annotations wrong 80%+ of the time (missing `@field:` targets), generate broken Jackson DTOs, and mix MVC/WebFlux. Correctness is binary: validation works or it doesn't. |
| **6** | [SK-09 Transaction & Consistency Designer](#sk-09-transaction--consistency-designer) | 4 | 5 | **20** | Transaction bugs cause data loss and inconsistency in production. Generic LLMs slap `@Transactional` on everything without understanding propagation, self-invocation, or the danger of transactions spanning HTTP calls. Incorrect advice here is actively harmful. |
| **7** | [SK-10 JPA / Spring Data Kotlin Mapper](#sk-10-jpa--spring-data-kotlin-mapper) | 4 | 5 | **20** | Generic LLMs generate JPA entities as Kotlin `data class` ~90% of the time — which is wrong and causes subtle production bugs (`equals`/`hashCode` with lazy proxies). N+1 diagnosis requires understanding the fetch plan. This skill prevents a class of bugs that generic LLMs actively introduce. |
| **8** | [SK-01 Project Context Ingestion](#sk-01-project-context-ingestion) | 5 | 4 | **20** | Every AI response is only as good as the context it has. Without reading `build.gradle.kts` and `application.yml`, the AI suggests incompatible APIs, wrong versions, and missing plugins. This is the gating skill for all others. High frequency because it applies to every interaction. |
| **9** | [SK-04 Gradle Kotlin DSL Doctor](#sk-04-gradle-kotlin-dsl-doctor) | 4 | 4 | **16** | Build issues happen several times a week, especially in teams with multiple modules. Gradle Kotlin DSL has a steep learning curve. Generic LLMs frequently suggest Maven syntax in Gradle files or incompatible plugin configurations. The skill output is verifiable: `./gradlew build` passes or fails. |
| **10** | [SK-21 Code Review Agent](#sk-21-code-review-agent-spring--kotlin) | 4 | 4 | **16** | Code review happens daily in teams. AI review quality depends heavily on understanding Spring-specific risks (transaction boundaries, proxy pitfalls, security holes) — not just style. Generic LLMs do surface-level review. The skill adds architectural and Spring-aware depth. |

### Tier 2 — High Value (Score 10–15) — Build After Tier 1

| Rank | Skill | Freq | Impact | Score | Rationale |
|------|-------|------|--------|-------|-----------|
| **11** | [SK-08 Jackson & Kotlin Serialization](#sk-08-jackson--kotlin-serialization-specialist) | 4 | 4 | **16** | Jackson/Kotlin issues hit several times a week (every new DTO, every API change). "Cannot construct instance" is the most common Spring+Kotlin error. Generic LLMs don't understand `jackson-module-kotlin` requirements or null/absent distinction. |
| **12** | [SK-07 Error Model & Validation](#sk-07-error-model--validation-architect) | 3 | 4 | **12** | Happens when building new APIs (~weekly). Generic LLMs generate inconsistent error formats, miss exception paths, and leak internal details. The skill ensures a systematic approach rather than ad-hoc exception handling. |
| **13** | [SK-16 Configuration & Profiles](#sk-16-configuration-properties--profiles-kotlin-safe) | 4 | 3 | **12** | Config issues hit multiple times a week. Generic LLMs handle basic YAML but miss Kotlin-specific binding traps (non-null without defaults, `@ConstructorBinding`). Moderate impact because config issues are usually non-catastrophic and quick to diagnose manually. |
| **14** | [SK-20 Kotlin Idiomatic Refactorer](#sk-20-kotlin-idiomatic-refactorer-spring-aware) | 4 | 3 | **12** | Refactoring touches happen daily, but the Spring-aware part (don't break proxies, don't break serialization) is where generic LLMs miss. Moderate impact — most Kotlin idiom suggestions are correct; the skill mainly prevents the proxy/serialization edge cases. |
| **15** | [SK-18 Performance & Concurrency Advisor](#sk-18-performance--concurrency-advisor) | 3 | 4 | **12** | Performance issues are weekly for active services. Generic LLMs give generic advice ("use caching"); the skill adds specificity (HikariCP tuning, coroutine dispatcher selection, N+1 identification). High impact when it applies, but lower frequency than debugging. |
| **16** | [SK-12 Integration & Resilience Engineer](#sk-12-integration--resilience-engineer) | 3 | 4 | **12** | Integrations are ~weekly work. Generic LLMs generate HTTP clients without timeouts, retries without jitter, and Kafka consumers without DLQ. The skill prevents production outage patterns (thundering herd, missing circuit breaker). |
| **17** | [SK-13 Spring Security Configurator](#sk-13-spring-security-configurator--auditor) | 3 | 4 | **12** | Security config changes happen weekly. Generic LLMs generate insecure defaults (broad `permitAll`, missing CSRF rationale, JWT without expiration check). Wrong security config is actively dangerous — high impact per incident. |
| **18** | [SK-05 Dependency Conflict Resolver](#sk-05-dependency-conflict-resolver) | 3 | 3 | **9** | Dependency conflicts happen ~weekly but are usually solvable with `dependencyInsight`. The skill adds value by knowing the BOM hierarchy, but the task is partially mechanical. |
| **19** | [SK-24 Production Incident Responder](#sk-24-production-incident-responder) | 2 | 4 | **8** | Incidents are infrequent (every few weeks) but extremely high-stakes. Generic LLMs don't know the "mitigate first, diagnose second" discipline. When it applies, the impact is very high — but it applies rarely. |
| **20** | [SK-17 Observability Integrator](#sk-17-observability-integrator) | 2 | 4 | **8** | Setting up observability is periodic work (new service, new SLO, incident follow-up). Generic LLMs over-instrument or miss key metrics. The skill adds real value by designing actionable metrics, not just "add Prometheus." |

### Tier 3 — Valuable but Specialized (Score ≤ 7) — Build When Tier 1–2 Are Done

| Rank | Skill | Freq | Impact | Score | Rationale |
|------|-------|------|--------|-------|-----------|
| **21** | [SK-19 Java → Kotlin Migration](#sk-19-java--kotlin-migration-assistant) | 2 | 3 | **6** | Migration is periodic project work. Generic LLMs handle basic conversion but miss platform type traps and JPA entity pitfalls. Valuable when it applies, but most teams don't migrate daily. |
| **22** | [SK-25 Domain Decomposition & API Design](#sk-25-domain-decomposition--api-design-advisor) | 2 | 3 | **6** | Architecture happens at project start or major feature scope. Generic LLMs give reasonable high-level advice. The skill adds Kotlin-specific modeling (sealed classes, value classes) but the core design task is less AI-dependent. |
| **23** | [SK-22 Upgrade & Breaking Change Navigator](#sk-22-upgrade--breaking-change-navigator) | 1 | 5 | **5** | Major upgrades are rare (1–2x/year) but extremely painful. When it applies, the skill is transformative — generic LLMs don't know project-specific breaking changes. The low frequency keeps the composite score low despite maximum impact. |
| **24** | [SK-23 CI/CD & Containerization Advisor](#sk-23-cicd--containerization-advisor) | 1 | 3 | **3** | Pipeline setup is rare (project start or major infra change). Generic LLMs handle Dockerfiles and CI configs adequately. The skill adds Spring-specific layered JAR and GraalVM knowledge. |
| **25** | [SK-11 Schema Migration Planner](#sk-11-schema-migration-planner) | 2 | 3 | **6** | Migrations happen every few weeks. The expand/contract pattern is specialized knowledge but the actual SQL is often straightforward. Moderate impact — the skill mainly prevents the "dropped column in prod" catastrophe. |

---

## Visual Priority Map

```
                    IMPACT →
                Low (1)    Medium (3)    High (5)
              ┌──────────┬──────────────┬──────────────┐
  Rare (1)    │          │ SK-23        │ SK-22        │
              │          │              │              │
              ├──────────┼──────────────┼──────────────┤
  Periodic(2) │          │ SK-11,SK-19  │ SK-24,SK-17  │
              │          │ SK-25        │              │
  FREQUENCY   ├──────────┼──────────────┼──────────────┤
  ↓           │          │ SK-05,SK-16  │ SK-08,SK-12  │
  Weekly (3)  │          │ SK-20        │ SK-07,SK-13  │
              │          │              │ SK-18        │
              ├──────────┼──────────────┼──────────────┤
  Several/    │          │ SK-04,SK-21  │ SK-03,SK-06  │
  week (4)    │          │              │ SK-09,SK-10  │
              ├──────────┼──────────────┼──────────────┤
  Daily (5)   │          │ SK-01        │ SK-15,SK-02  │
              │          │              │ SK-14        │
              └──────────┴──────────────┴──────────────┘
                          BUILD ←─── THESE FIRST ───→
```

---

## Recommended Implementation Roadmap

Based on the priority matrix, here's a suggested order for building skills:

### Phase 1 — Foundation (Tier 1, Score ≥ 16)

The 10 skills that cover ~80% of daily developer-AI interactions:

1. **SK-15** Stacktrace & Log Triage — *highest daily demand, highest AI differentiation*
2. **SK-02** Spring Context & DI Graph Reasoning — *the "app won't start" skill*
3. **SK-03** Kotlin ↔ Spring Proxy Compatibility — *the #1 Kotlin-specific trap*
4. **SK-14** Test Suite Builder — *most measurable AI productivity gain*
5. **SK-06** Spring MVC/WebFlux API Builder — *most common code generation request*
6. **SK-09** Transaction & Consistency Designer — *prevents data loss*
7. **SK-10** JPA / Spring Data Kotlin Mapper — *prevents bugs that generic LLMs introduce*
8. **SK-01** Project Context Ingestion — *foundation for all other skills*
9. **SK-04** Gradle Kotlin DSL Doctor — *"build doesn't work" is a daily blocker*
10. **SK-21** Code Review Agent — *daily team activity, Spring-specific risks*

### Phase 2 — Extended Coverage (Tier 2, Score 8–15)

Skills for specialized but frequent scenarios:

11. **SK-08** Jackson & Kotlin Serialization
12. **SK-07** Error Model & Validation
13. **SK-13** Spring Security Configurator
14. **SK-12** Integration & Resilience Engineer
15. **SK-18** Performance & Concurrency Advisor
16. **SK-16** Configuration & Profiles
17. **SK-20** Kotlin Idiomatic Refactorer
18. **SK-05** Dependency Conflict Resolver
19. **SK-24** Production Incident Responder
20. **SK-17** Observability Integrator

### Phase 3 — Full Portfolio (Tier 3, Score ≤ 7)

Skills for less frequent but important tasks:

21. **SK-19** Java → Kotlin Migration
22. **SK-25** Domain Decomposition & API Design
23. **SK-22** Upgrade & Breaking Change Navigator
24. **SK-11** Schema Migration Planner
25. **SK-23** CI/CD & Containerization Advisor

---

---

# Skill Benchmark Suite: 15 Standard Evaluation Tasks

> A suite of 15 self-contained, multi-step benchmark tasks designed to measure AI assistant quality **with skills vs. without skills** on realistic Kotlin + Spring backend development scenarios.

> Repository status update: the repo now also contains `H-01` through `H-06` hard/compound calibration benchmarks under `benchmarks/`. The original 15-task suite below remains the standard benchmark spec; the hard-benchmark results and `H-06` calibration are documented in `BENCHMARK_REPORT.md`.

## How to Use This Benchmark

### Execution Model: Single-Shot + Fix Loop

1. Agent receives the full task prompt (no prior codebase).
2. Agent produces the complete solution autonomously.
3. Automated evaluation runs: `./gradlew compileKotlin`, `./gradlew test`, plus task-specific checks.
4. Agent sees test/check output and can iterate (up to 3 fix attempts).
5. Final state is scored.

### Evaluation Rubric (per step)

Each step is scored on two layers:

**Layer 1 — Automated (0–25 points, 5 dimensions × 0–5):**

| Dimension | 0 | 1–2 | 3 | 4–5 | How to measure |
|-----------|---|-----|---|-----|----------------|
| **Functional correctness** | Doesn't compile | Compiles but tests fail | Most tests pass | All tests pass + edge cases | Provided acceptance tests |
| **Kotlin idiomacy** | Java-in-Kotlin | Mixed style | Mostly idiomatic | Fully idiomatic, no anti-patterns | Detekt + custom rules: `data class` for DTOs, `val` over `var`, no `!!`, sealed classes where appropriate |
| **Spring correctness** | Broken DI/proxy/txn | Works but has anti-patterns | Correct patterns | Best practices throughout | Custom checklist: proxy safety, `@Transactional` placement, DI style, error handling |
| **Test quality** | No tests / won't run | Tests run but trivial | Good coverage, right level | Comprehensive: unit + slice + integration, deterministic, tests behavior | Coverage % + structural analysis |
| **Production readiness** | Missing basics | Partial (no error handling or logging) | Mostly ready | Full: timeouts, retries, logging, validation, no leaked internals | Checklist scan |

**Layer 2 — LLM-as-Judge (0–25 points, 5 dimensions × 0–5):**

| Dimension | What is evaluated |
|-----------|------------------|
| **Code readability** | Naming, structure, appropriate abstraction level, not over/under-engineered |
| **Architectural soundness** | Layer separation, dependency direction, single responsibility |
| **Task adherence** | Did the agent do exactly what was asked? No extra features, no missing requirements |
| **Error handling completeness** | All failure paths covered, meaningful error messages, no silent failures |
| **Overall impression** | Would a senior Kotlin/Spring developer approve this in a code review? |

**Total per step: 0–50.** Combined across steps: a benchmark with 3 steps has a maximum of 150.

### Skill Coverage Verification

Every Tier 1 skill is primary target in ≥ 2 benchmarks:

| Tier 1 Skill | Primary in benchmarks |
|---|---|
| SK-15 Stacktrace & Log Triage | B-02, B-06, B-15 |
| SK-02 Spring Context & DI Reasoning | B-06, B-15 |
| SK-14 Test Suite Builder | B-01, B-03, B-04, B-05, B-09, B-12 |
| SK-03 Kotlin ↔ Spring Proxy | B-02, B-07, B-14 |
| SK-06 Spring MVC/WebFlux API Builder | B-01, B-04, B-08, B-09 |
| SK-09 Transaction & Consistency | B-02, B-05, B-09, B-14 |
| SK-10 JPA / Spring Data Mapper | B-01, B-03, B-09, B-13 |
| SK-01 Project Context Ingestion | All (implicit — agent must read its own project) |
| SK-04 Gradle Kotlin DSL Doctor | B-01, B-11 |
| SK-21 Code Review Agent | B-14 |

---

## B-01. Order Management CRUD Service

**Skills tested:** [SK-04](#sk-04-gradle-kotlin-dsl-doctor), [SK-06](#sk-06-spring-mvc--webflux-api-builder), [SK-07](#sk-07-error-model--validation-architect), [SK-08](#sk-08-jackson--kotlin-serialization-specialist), [SK-10](#sk-10-jpa--spring-data-kotlin-mapper), [SK-14](#sk-14-test-suite-builder-three-layer-strategy)

**Why this benchmark matters:** This is the most common task a Spring developer does — build a CRUD REST service from scratch. It hits code generation, Kotlin JPA pitfalls, validation annotation traps, and test strategy all at once.

### Step 1 — Build (the foundation)

**Prompt to the agent:**

> Build a Kotlin + Spring Boot order management service from scratch.
>
> **Requirements:**
> - Gradle Kotlin DSL build with Spring Boot 3.x, Kotlin, JPA, H2 (test) + PostgreSQL (runtime), Flyway, Jackson, validation
> - Entity: `Order` (id, customerEmail, status [CREATED/CONFIRMED/SHIPPED/DELIVERED/CANCELLED], items list, totalAmount, createdAt, updatedAt)
> - Entity: `OrderItem` (id, productName, quantity, unitPrice)
> - Relationship: Order has many OrderItems (cascade, orphan removal)
> - REST endpoints: POST `/api/orders`, GET `/api/orders/{id}`, GET `/api/orders?status={status}&page=0&size=20`, PATCH `/api/orders/{id}/cancel`
> - Validation: customerEmail must be valid email, items must not be empty, quantity > 0, unitPrice > 0
> - Unified error response format for all errors (400, 404, 409, 500)
> - Cancel is only allowed if status is CREATED or CONFIRMED
> - Flyway migration for schema creation
> - Tests: unit tests for service logic, `@WebMvcTest` for controller, `@DataJpaTest` for repository
>
> The project must compile and all tests must pass with `./gradlew test`.

**What this step reveals (with vs. without skill):**
- **SK-06:** Does the agent use `@field:` validation targets on Kotlin data class DTOs? (Without skill: ~80% failure rate)
- **SK-10:** Does the agent use `data class` for JPA entities? (Without skill: ~90% incorrect)
- **SK-08:** Does the agent register `jackson-module-kotlin` and handle null/absent correctly?
- **SK-07:** Is the error format consistent across validation, not-found, and business errors?
- **SK-04:** Does the build include `kotlin("plugin.spring")` and `kotlin("plugin.jpa")`?

**Automated checks:**
- `./gradlew compileKotlin` passes
- `./gradlew test` passes
- Detekt scan: no `!!`, entities are not `data class`, DTOs use `@field:` targets
- Test count: ≥ 3 unit + ≥ 3 web slice + ≥ 2 data slice

### Step 2 — Extend (add serialization complexity)

**Prompt to the agent:**

> Extend the order service with the following:
>
> 1. Add a `discount` field to OrderItem: nullable, optional in JSON (absent ≠ null). When absent, no discount. When present and null, explicitly no discount. When present with a value, apply it.
> 2. Add polymorphic payment info to Order: `PaymentInfo` is a sealed class with subclasses `CreditCard(last4: String, brand: String)` and `BankTransfer(iban: String)`. Serialize as JSON with a `type` discriminator field.
> 3. Add a `GET /api/orders/{id}/receipt` endpoint that returns order data with calculated totals (subtotal, discount, total) as a formatted response.
> 4. Add date/time serialization: all dates in ISO 8601 format with UTC timezone.
> 5. Add tests for all new functionality.

**What this step reveals:**
- **SK-08:** Can the agent handle null vs. absent distinction? Polymorphic sealed class serialization? Date formatting?
- **SK-06:** Does the receipt endpoint follow REST semantics?
- **SK-14:** Are the new tests at the right level?

**Automated checks:**
- Tests pass
- JSON serialization tests verify: absent discount ≠ null discount, sealed class type discriminator present, dates in ISO 8601
- No new Detekt violations

---

## B-02. The Transaction Trap

**Skills tested:** [SK-03](#sk-03-kotlin--spring-proxy-compatibility), [SK-09](#sk-09-transaction--consistency-designer), [SK-15](#sk-15-stacktrace--log-triage)

**Why this benchmark matters:** Transaction bugs are invisible at compile time and often at test time. They only appear under specific runtime conditions. This benchmark tests whether the AI understands proxy mechanics and self-invocation — the #1 Kotlin-specific Spring trap.

### Step 1 — Build (with deliberate trap)

**Prompt to the agent:**

> Build a Kotlin + Spring Boot money transfer service.
>
> **Requirements:**
> - Entity: `Account` (id, ownerName, balance: BigDecimal, version: Long for optimistic locking)
> - Service: `TransferService` with method `transfer(fromId, toId, amount)`:
>   - Validates sufficient balance
>   - Deducts from source, adds to destination
>   - Logs the transfer in a `TransferLog` entity (fromId, toId, amount, timestamp, status)
>   - If transfer fails, the log entry should still be saved with status FAILED (i.e., the log save must NOT be rolled back)
> - Service method `batchTransfer(transfers: List<TransferRequest>)` that calls `transfer()` for each item. Each transfer should be independent — one failure shouldn't roll back others.
> - REST endpoint: POST `/api/transfers` (single), POST `/api/transfers/batch` (batch)
> - Tests verifying: successful transfer, insufficient funds, partial batch failure
>
> The project must compile and all tests must pass.

**What this step reveals:**
- **SK-09:** Does the agent use `REQUIRES_NEW` propagation for the audit log? Does `batchTransfer` call `transfer` through the proxy or via `this` (self-invocation trap)?
- **SK-03:** Are classes open for proxying? Is `plugin.spring` present?
- **Key trap:** If `batchTransfer` calls `this.transfer()`, the `@Transactional` on `transfer()` is bypassed. The agent must either inject the service into itself, extract to a separate bean, or use `TransactionTemplate`.

**Automated checks:**
- Tests pass
- Specific test: after a failed transfer, verify the FAILED log entry exists (proves audit log is not rolled back)
- Specific test: in a batch of 3 where #2 fails, verify #1 and #3 succeeded (proves independent transactions)
- Code scan: check for self-invocation pattern (`this.transfer(`) — if found, flag as incorrect

### Step 2 — Debug (the silent failure)

**Prompt to the agent:**

> The following bug report was filed:
>
> "In production, when a transfer fails due to insufficient funds, the TransferLog entry is also missing. It should be saved with status FAILED regardless of the transfer outcome. Additionally, in batch transfers, when transfer #2 fails, transfers #3 and #4 are also rolled back even though they should be independent."
>
> Diagnose the root cause(s) and fix the code. Explain:
> 1. Why the audit log is being rolled back (hint: transaction propagation)
> 2. Why batch transfers are not independent (hint: self-invocation)
> 3. The minimal fix for each issue
>
> Add regression tests that specifically verify these behaviors.

**What this step reveals:**
- **SK-15:** Can the agent diagnose transaction propagation and self-invocation issues from a bug report (no stack trace)?
- **SK-09:** Does the fix use the correct pattern? (`REQUIRES_NEW` for audit, proxy call or separate bean for batch)
- **SK-03:** Does the agent explain the proxy mechanism?

**Automated checks:**
- Regression tests pass
- Code review: no self-invocation of `@Transactional` methods remains
- Audit log test: failed transfer → FAILED log entry persists

---

## B-03. N+1 Query Clinic

**Skills tested:** [SK-10](#sk-10-jpa--spring-data-kotlin-mapper), [SK-18](#sk-18-performance--concurrency-advisor), [SK-14](#sk-14-test-suite-builder-three-layer-strategy)

**Why this benchmark matters:** N+1 is the most common JPA performance problem. This benchmark tests whether the AI can build efficient data access from the start, and whether it can diagnose and fix N+1 when it occurs.

### Step 1 — Build (seed the problem)

**Prompt to the agent:**

> Build a Kotlin + Spring Boot course catalog service.
>
> **Requirements:**
> - Entity: `Course` (id, title, description, category, instructor)
> - Entity: `Lesson` (id, title, durationMinutes, orderIndex) — Course has many Lessons
> - Entity: `Enrollment` (id, studentName, enrolledAt) — Course has many Enrollments
> - REST endpoints:
>   - GET `/api/courses` — list all courses with lesson count and enrollment count
>   - GET `/api/courses/{id}` — course detail with all lessons (ordered) and enrollment count
>   - GET `/api/courses/{id}/students` — list enrolled students with pagination
> - Flyway migration to create schema and seed 50 courses, each with 10 lessons and 20 enrollments
> - Tests for all endpoints

**What this step reveals:**
- **SK-10:** Does the agent use fetch joins or entity graphs for the detail endpoint? Or does it produce N+1?
- The list endpoint must return aggregate counts without loading all children — does the agent use a DTO projection or `@Query` with COUNT?

**Automated checks:**
- Tests pass
- **SQL query count test**: intercept Hibernate SQL logging, verify that `GET /api/courses` executes ≤ 3 queries (not 50+ N+1)
- **SQL query count test**: verify `GET /api/courses/{id}` executes ≤ 2 queries

### Step 2 — Diagnose (find the N+1)

**Prompt to the agent:**

> Enable Hibernate SQL logging (`spring.jpa.show-sql=true`, `logging.level.org.hibernate.SQL=DEBUG`).
>
> Run the test for `GET /api/courses` (list all 50 courses with counts). Count the SQL queries emitted.
>
> If the query count is > 5, you have an N+1 problem. Diagnose it:
> 1. Which relationship causes the N+1?
> 2. Why does it happen (lazy loading triggered during serialization)?
> 3. Propose 3 different fixes with trade-offs: fetch join, entity graph, DTO projection
>
> Implement the best fix and add a test that asserts the query count is ≤ 3.

### Step 3 — Fix and verify

**Prompt to the agent:**

> Implement a DTO projection approach for the list endpoint: create a `CourseSummaryDto` with `id, title, category, instructor, lessonCount, enrollmentCount` populated by a single JPQL query with COUNT subqueries.
>
> For the detail endpoint, use `@EntityGraph` to load lessons eagerly in a single query.
>
> Verify: all tests pass, query count tests pass, and the API response format hasn't changed.

**Automated checks:**
- All tests pass
- Query count assertion tests pass (≤ 3 for list, ≤ 2 for detail)
- No new N+1 detected

---

## B-04. Security Lockdown

**Skills tested:** [SK-13](#sk-13-spring-security-configurator--auditor), [SK-06](#sk-06-spring-mvc--webflux-api-builder), [SK-14](#sk-14-test-suite-builder-three-layer-strategy)

**Why this benchmark matters:** Security misconfigurations are the most dangerous type of AI-generated bug. This benchmark tests whether the AI produces secure defaults and can audit its own security setup.

### Step 1 — Build (secure API)

**Prompt to the agent:**

> Build a Kotlin + Spring Boot task management API with JWT authentication.
>
> **Requirements:**
> - Entities: `User` (id, email, role [ADMIN/USER]), `Task` (id, title, description, status, assignee: User, createdBy: User)
> - Security: stateless JWT authentication, tokens contain userId and role
> - Endpoint access rules:
>   - `POST /api/tasks` — authenticated users
>   - `GET /api/tasks` — authenticated users (returns only tasks assigned to the current user, unless ADMIN)
>   - `PATCH /api/tasks/{id}` — only the assignee or ADMIN
>   - `DELETE /api/tasks/{id}` — only ADMIN
>   - `GET /actuator/health` — public
>   - `GET /actuator/prometheus` — public
>   - Everything else — denied
> - CORS: allow `http://localhost:3000` with credentials
> - Error responses: 401 for unauthenticated, 403 for unauthorized (not the Spring default of swapping them)
> - Tests: security tests for each endpoint × each role (ADMIN, USER, anonymous)
>
> Provide a utility to generate test JWT tokens. Do NOT implement a full login flow — just the resource server side.

**What this step reveals:**
- **SK-13:** Does the security config use whitelist approach (deny all, permit specific)? Does it handle 401 vs. 403 correctly? Is CORS configured before the security filter?
- **SK-06:** Does the API follow REST semantics? Are method-level security annotations used?
- **SK-14:** Are security tests comprehensive? (Each endpoint × each role = at least 12 test cases)

**Automated checks:**
- Tests pass
- Security matrix test: verify each endpoint × role combination returns the expected status code
- Scan for: no `permitAll()` except health/prometheus, no disabled CSRF without comment, CORS config present

### Step 2 — Audit (find the holes)

**Prompt to the agent:**

> Perform a security audit of the application you just built. Check for:
>
> 1. Are there any endpoints accidentally exposed without authentication?
> 2. Does the JWT validation check expiration, issuer, and audience?
> 3. Can a USER delete tasks by guessing the endpoint?
> 4. Are actuator endpoints other than health/prometheus exposed?
> 5. Is there any PII leaking in error responses?
>
> For each finding, provide: severity (CRITICAL/HIGH/MEDIUM/LOW), the vulnerable code, and the fix.
> Add tests that specifically verify each vulnerability is closed.

**Automated checks:**
- Audit report covers ≥ 3 distinct findings
- Fixes compile and tests pass
- New security regression tests added

---

## B-05. Kafka Event Pipeline with Idempotency

**Skills tested:** [SK-12](#sk-12-integration--resilience-engineer), [SK-09](#sk-09-transaction--consistency-designer), [SK-14](#sk-14-test-suite-builder-three-layer-strategy)

**Why this benchmark matters:** Event-driven systems require idempotency, deduplication, and proper error handling — exactly where generic LLMs produce dangerous code.

### Step 1 — Build

**Prompt to the agent:**

> Build a Kotlin + Spring Boot order event processing service.
>
> **Requirements:**
> - Kafka consumer: listens to `order-events` topic, messages are JSON with fields: `eventId` (UUID), `eventType` (ORDER_CREATED/ORDER_PAID/ORDER_CANCELLED), `orderId`, `payload` (varies by type), `timestamp`
> - Deduplication: if the same `eventId` is received twice, skip processing (store processed event IDs in the database)
> - Processing logic:
>   - ORDER_CREATED → create an `OrderProjection` in the local database (id, status, customerEmail, totalAmount)
>   - ORDER_PAID → update status to PAID
>   - ORDER_CANCELLED → update status to CANCELLED, only if current status is not PAID
> - Dead letter queue: if processing fails after 3 retries, send to `order-events-dlq`
> - Transactional outbox: after processing, publish a `processing-results` event using the outbox pattern (write to `outbox` table in the same transaction, separate scheduler publishes)
> - Metrics: counter of events processed (by type), counter of duplicates skipped, counter of DLQ sends
> - Tests:
>   - Unit tests for processing logic
>   - Integration test with embedded Kafka (or Testcontainers) verifying: normal flow, deduplication, DLQ on failure, outbox publication

**What this step reveals:**
- **SK-12:** Does the agent implement retry with backoff? Is DLQ configured correctly? Is the outbox pattern correct (same transaction as business logic)?
- **SK-09:** Is deduplication checked inside the transaction? Is the outbox write in the same transaction as the state change?
- **SK-14:** Are integration tests with Kafka realistic and deterministic?

**Automated checks:**
- Tests pass (including embedded Kafka integration tests)
- Deduplication test: send same event twice → processed once
- DLQ test: poison message → appears in DLQ after retries
- Outbox test: successful processing → outbox row created in same transaction

---

## B-06. The Config Maze

**Skills tested:** [SK-16](#sk-16-configuration-properties--profiles-kotlin-safe), [SK-02](#sk-02-spring-context--di-graph-reasoning), [SK-15](#sk-15-stacktrace--log-triage)

**Why this benchmark matters:** Configuration issues are the second most common "app won't start" cause after DI failures. Kotlin adds unique traps with `@ConfigurationProperties` binding.

### Step 1 — Build (multi-profile app)

**Prompt to the agent:**

> Build a Kotlin + Spring Boot notification service with complex configuration.
>
> **Requirements:**
> - `@ConfigurationProperties` class `NotificationProperties` with:
>   - `email.from` (required, validated with `@NotBlank`)
>   - `email.smtp.host`, `email.smtp.port` (required)
>   - `sms.provider` (enum: TWILIO/VONAGE)
>   - `sms.apiKey` (required, should come from env variable `SMS_API_KEY`)
>   - `retry.maxAttempts` (default: 3), `retry.backoffMs` (default: 1000)
>   - `features.smsEnabled` (default: false)
> - Profiles: `local` (mock SMS, SMTP on localhost:1025), `staging` (real SMS, SMTP on mail.staging.internal), `prod` (real everything, strict validation)
> - Conditional bean: `SmsNotifier` only created when `features.smsEnabled=true` and `sms.provider` is set
> - Service: `NotificationService` that sends email and optionally SMS based on config
> - Tests: context loads in each profile, properties bind correctly, conditional bean present/absent as expected

**What this step reveals:**
- **SK-16:** Does the agent use `@ConstructorBinding` correctly in Kotlin? Handle defaults with non-null types? Validate required fields?
- **SK-02:** Is the conditional bean wired correctly?

### Step 2 — Debug (context won't start)

**Prompt to the agent:**

> The application fails to start with the following error:
>
> ```
> ***************************
> APPLICATION FAILED TO START
> ***************************
> Description:
> Failed to bind properties under 'notification' to com.example.NotificationProperties
>
> Reason: org.springframework.boot.context.properties.bind.BindException:
>   Failed to bind properties under 'notification.sms' to com.example.SmsProperties
>
> Action:
> Update your application's configuration
> ```
>
> The `application.yml` contains:
> ```yaml
> notification:
>   email:
>     from: "noreply@example.com"
>     smtp:
>       host: localhost
>       port: 1025
>   sms:
>     provider: TWILIO
>   retry:
>     max-attempts: 5
> ```
>
> Diagnose:
> 1. Why is binding failing? (Hint: examine the Kotlin class — is it using camelCase vs. kebab-case correctly? Are there nullability issues?)
> 2. Why is the `retry.backoffMs` default not working? (Hint: `kebab-case` in YAML vs. `camelCase` in Kotlin)
> 3. Why is `sms.apiKey` required but the conditional bean should make it optional when SMS is disabled?
>
> Fix all issues and add tests that verify binding in all profiles.

**Automated checks:**
- App context starts in all profiles
- Tests verify each property binds correctly
- Tests verify conditional bean behavior

---

## B-07. Java to Kotlin Migration

**Skills tested:** [SK-19](#sk-19-java--kotlin-migration-assistant), [SK-03](#sk-03-kotlin--spring-proxy-compatibility), [SK-20](#sk-20-kotlin-idiomatic-refactorer-spring-aware)

**Why this benchmark matters:** Migration is a real-world multi-step task where generic LLMs produce "Java in Kotlin syntax" and break Spring proxies and JPA entities.

### Step 1 — Build (the Java starting point)

**Prompt to the agent:**

> Create a small Spring Boot + Java service (yes, Java, not Kotlin yet) for a product catalog:
>
> - Java entity: `Product` with Lombok `@Data`, `@Entity`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` — fields: id, name, description, price (BigDecimal), category (enum), active (boolean), createdAt
> - Java service: `ProductService` with `@Transactional` on write methods, `@Cacheable` on findById
> - Java controller: standard REST CRUD
> - Java tests: JUnit 5 + Mockito
> - Gradle build with Java + Lombok
>
> The project must compile and tests must pass.

### Step 2 — Migrate (Java → Kotlin)

**Prompt to the agent:**

> Migrate the entire product catalog service from Java to Kotlin:
>
> 1. Convert all classes to Kotlin
> 2. Remove Lombok — replace with Kotlin equivalents
> 3. Entity: do NOT use `data class` — explain why
> 4. Replace Mockito with MockK in tests
> 5. Make the code idiomatic Kotlin (not Java-in-Kotlin)
> 6. Ensure `@Transactional` and `@Cacheable` still work after migration
> 7. Add `kotlin("plugin.spring")` and `kotlin("plugin.jpa")` to the build
>
> All tests must pass after migration. Behavior must be identical.

### Step 3 — Refactor (idiomatic Kotlin)

**Prompt to the agent:**

> Now refactor to be fully idiomatic Kotlin:
>
> 1. Use sealed class for operation results instead of exceptions for expected failures (ProductNotFound, InsufficientStock)
> 2. Use value class for `ProductId`
> 3. Use `when` expressions where appropriate
> 4. Extract DTO mapping to extension functions
> 5. Ensure no `!!` operators, no `lateinit var` where avoidable
>
> All tests must still pass. Add new tests for the sealed class result patterns.

**Automated checks (across all steps):**
- Each step: compile + tests pass
- Step 2: no Lombok annotations remain, `plugin.spring` and `plugin.jpa` present
- Step 2: entities are NOT `data class`
- Step 3: Detekt scan passes, no `!!`, sealed class used for results
- Step 3: test count ≥ step 2 test count (tests were added, not removed)

---

## B-08. Jackson Serialization Gauntlet

**Skills tested:** [SK-08](#sk-08-jackson--kotlin-serialization-specialist), [SK-06](#sk-06-spring-mvc--webflux-api-builder), [SK-07](#sk-07-error-model--validation-architect)

**Why this benchmark matters:** Serialization is the #1 runtime failure point in Kotlin + Spring APIs. This benchmark tests all the edge cases that generic LLMs get wrong.

### Step 1 — Build (edge case bonanza)

**Prompt to the agent:**

> Build a Kotlin + Spring Boot API that exercises all tricky Jackson/Kotlin serialization scenarios.
>
> **Requirements — a `POST /api/events` endpoint that accepts and returns event objects:**
>
> 1. **Default parameters:** `Event` data class with `priority: Int = 0` — absent in JSON → should use default, not fail
> 2. **Nullable vs. absent:** `description: String?` — null in JSON, absent in JSON, and present are three different states. Track which state using a custom wrapper or `Optional`-like approach.
> 3. **Sealed class hierarchy:** `EventPayload` sealed class with `ClickEvent(url: String, elementId: String)`, `PurchaseEvent(orderId: String, amount: BigDecimal)`, `CustomEvent(data: Map<String, Any>)` — serialized with `"type"` discriminator
> 4. **Enum with properties:** `EventSource` enum with `displayName: String` property — serialize by name, not ordinal
> 5. **Date/time:** all timestamps as ISO 8601 with UTC timezone, `Instant` for creation time, `LocalDate` for date-only fields
> 6. **Value class:** `EventId` as `@JvmInline value class EventId(val value: String)` — must serialize as plain string, not as object
> 7. **Strict mode:** unknown fields in JSON should be rejected (400), not silently ignored
>
> For each scenario, provide a test that verifies correct serialization AND deserialization.

**Automated checks:**
- All tests pass
- Test for each of the 7 scenarios verifies round-trip (serialize → deserialize → equal)
- Test: absent field uses default value
- Test: unknown field returns 400, not 200 with ignored data
- Test: sealed class discriminator present in JSON output

---

## B-09. Full Feature: Digital Wallet Service

**Skills tested:** [SK-25](#sk-25-domain-decomposition--api-design-advisor), [SK-06](#sk-06-spring-mvc--webflux-api-builder), [SK-09](#sk-09-transaction--consistency-designer), [SK-10](#sk-10-jpa--spring-data-kotlin-mapper), [SK-12](#sk-12-integration--resilience-engineer), [SK-14](#sk-14-test-suite-builder-three-layer-strategy)

**Why this benchmark matters:** This is the largest benchmark — a realistic, multi-concern service that tests whether the AI can build a production-grade feature end-to-end. It exercises nearly all Tier 1 skills simultaneously.

### Step 1 — Design and build

**Prompt to the agent:**

> Build a digital wallet microservice in Kotlin + Spring Boot.
>
> **Domain:**
> - `Wallet` (id, userId, balance: BigDecimal, currency, status [ACTIVE/FROZEN/CLOSED], createdAt)
> - `Transaction` (id, walletId, type [DEPOSIT/WITHDRAWAL/TRANSFER_IN/TRANSFER_OUT], amount, referenceId, description, balanceBefore, balanceAfter, createdAt)
>
> **Endpoints:**
> - `POST /api/wallets` — create wallet (one per userId, enforced by unique constraint)
> - `GET /api/wallets/{id}` — wallet detail with last 10 transactions
> - `POST /api/wallets/{id}/deposit` — deposit funds (idempotent by referenceId)
> - `POST /api/wallets/{id}/withdraw` — withdraw funds (check balance, idempotent)
> - `POST /api/transfers` — transfer between two wallets (atomic: debit source + credit destination in one transaction)
> - `GET /api/wallets/{id}/transactions?page=0&size=20` — transaction history with pagination
>
> **Business rules:**
> - Cannot deposit/withdraw to FROZEN or CLOSED wallet
> - Cannot withdraw more than available balance (optimistic locking)
> - Transfers are atomic — both sides succeed or neither does
> - All money operations are idempotent by `referenceId` — duplicate requests return the original result
> - Transaction log is append-only — every balance change creates a Transaction record with before/after balance
>
> **Technical requirements:**
> - Flyway migrations
> - Unified error handling (Problem Details format)
> - Input validation on all endpoints
> - Tests: unit (service logic), slice (controller + repository), integration (full flow: create wallet → deposit → transfer → check balance)
>
> The service must compile, all tests must pass, and the integration test must verify end-to-end idempotency.

**Automated checks:**
- Compile + all tests pass
- Idempotency test: same deposit referenceId twice → balance changes only once
- Transfer atomicity test: transfer with insufficient funds → neither wallet changes
- Optimistic locking test: concurrent withdrawals → one succeeds, one fails with conflict (not negative balance)
- Transaction audit: every balance change has a Transaction record with correct before/after
- Query count: wallet detail with transactions executes ≤ 3 SQL queries

---

## B-10. Observability Retrofit

**Skills tested:** [SK-17](#sk-17-observability-integrator), [SK-18](#sk-18-performance--concurrency-advisor)

**Why this benchmark matters:** Observability is rarely done right by AI — metrics are either missing or have exploding cardinality. This benchmark tests whether the AI can instrument a service meaningfully.

### Step 1 — Build (bare service)

**Prompt to the agent:**

> Build a simple Kotlin + Spring Boot URL shortener service:
>
> - `POST /api/urls` — accepts `{originalUrl}`, returns `{shortCode, shortUrl}`
> - `GET /{shortCode}` — redirects (302) to the original URL
> - `GET /api/urls/{shortCode}/stats` — returns click count
> - H2 database, Flyway migration
> - Basic tests

### Step 2 — Instrument (add observability)

**Prompt to the agent:**

> Add production-grade observability to the URL shortener:
>
> 1. **Structured JSON logging** with Logback — include requestId, method, path, duration, status code for every request
> 2. **Correlation ID propagation**: generate `X-Request-Id` if not present, propagate via MDC, include in all log lines and response headers
> 3. **Micrometer metrics:**
>    - `url_shortener_redirects_total` (counter, tags: shortCode is NOT a tag — it's high cardinality! Use only status as tag)
>    - `url_shortener_create_total` (counter)
>    - `url_shortener_redirect_duration_seconds` (timer)
>    - `url_shortener_active_urls` (gauge — count of active short URLs)
> 4. **Health indicators**: custom health check for database connectivity
> 5. **Actuator**: expose only `/actuator/health`, `/actuator/prometheus`, `/actuator/info` — all others disabled
> 6. **Tests**: verify metrics are recorded, verify correlation ID propagation, verify health endpoint

**What this step reveals:**
- **SK-17:** Does the agent avoid high-cardinality tags? (shortCode as tag would explode Prometheus)
- **SK-17:** Is MDC propagation correct? Is JSON structured logging configured?
- **SK-18:** Is the timer measuring the right thing?

**Automated checks:**
- Tests pass
- Metric test: after 5 redirects, `url_shortener_redirects_total` = 5
- No metric with `shortCode` as tag label (cardinality check)
- Correlation ID test: response contains `X-Request-Id` header
- Actuator test: `/actuator/health` returns 200, `/actuator/env` returns 404

---

## B-11. Spring Boot Upgrade (2.7 → 3.2)

**Skills tested:** [SK-22](#sk-22-upgrade--breaking-change-navigator), [SK-04](#sk-04-gradle-kotlin-dsl-doctor), [SK-05](#sk-05-dependency-conflict-resolver)

**Why this benchmark matters:** Major upgrades are rare but extremely painful. This benchmark tests whether the AI can navigate breaking changes systematically.

### Step 1 — Build (the legacy app)

**Prompt to the agent:**

> Create a Kotlin + Spring Boot 2.7 project with:
>
> - `javax` imports (validation, persistence, servlet)
> - WebSecurityConfigurerAdapter-based security (deprecated in 2.7, removed in 3.x)
> - `spring.config.use-legacy-processing=true` in properties
> - A REST controller + JPA entity + repository + security config + tests
> - Gradle with Spring Boot 2.7.x, Kotlin 1.8.x
>
> Must compile and tests must pass on Spring Boot 2.7.

### Step 2 — Upgrade

**Prompt to the agent:**

> Upgrade this project from Spring Boot 2.7 to 3.2. Handle:
>
> 1. `javax.*` → `jakarta.*` namespace migration
> 2. `WebSecurityConfigurerAdapter` → `SecurityFilterChain` bean-based configuration
> 3. Remove `spring.config.use-legacy-processing`
> 4. Update Kotlin to 1.9+ (compatible with Boot 3.x)
> 5. Update Gradle plugin versions
> 6. Fix any deprecation warnings
> 7. Verify: compile + all tests pass
>
> Provide a migration checklist of every change made and why.

**Automated checks:**
- Compile + tests pass on Spring Boot 3.2
- No `javax.` imports remain (all `jakarta.`)
- No `WebSecurityConfigurerAdapter` usage
- Kotlin version ≥ 1.9
- Migration checklist document present with ≥ 5 items

---

## B-12. Resilient HTTP Integration

**Skills tested:** [SK-12](#sk-12-integration--resilience-engineer), [SK-14](#sk-14-test-suite-builder-three-layer-strategy)

**Why this benchmark matters:** External HTTP integrations are where "works in development" turns into "production outage." This benchmark tests timeout, retry, and circuit breaker logic.

### Step 1 — Build (with resilience)

**Prompt to the agent:**

> Build a Kotlin + Spring Boot price aggregator service that fetches prices from 3 external suppliers.
>
> **Requirements:**
> - Service calls 3 external HTTP APIs (mock them with interfaces / WireMock stubs):
>   - Supplier A: responds in ~100ms, reliable
>   - Supplier B: responds in ~500ms, sometimes returns 500
>   - Supplier C: responds in ~2000ms, frequently times out
> - Endpoint: `GET /api/prices/{productId}` — returns the best (lowest) price from all suppliers that respond
> - Resilience requirements:
>   - Per-supplier timeout: 1 second
>   - Retry: up to 2 retries on 5xx, with exponential backoff + jitter
>   - Circuit breaker: open after 5 consecutive failures, half-open after 30 seconds
>   - Fallback: if a supplier is unavailable, exclude it from results (don't fail the whole request)
>   - If ALL suppliers fail, return 503 with a meaningful error
> - Metrics: per-supplier success/failure counters, response time histogram
> - Tests with WireMock:
>   - All suppliers respond → lowest price returned
>   - One supplier times out → result from other two
>   - One supplier returns 500 → retry succeeds on second attempt
>   - All suppliers fail → 503 response
>   - Circuit breaker opens after failures → subsequent calls don't attempt the failing supplier

**Automated checks:**
- Tests pass (all 5 scenarios)
- Timeout test: supplier C stub with 3-second delay → excluded from result (not blocking the request for 3s)
- Circuit breaker test: after 5 failures, next call doesn't invoke the stub (verified by WireMock request count)
- Jitter test: retry delays are not identical (statistical check on timing)

---

## B-13. Schema Migration: Zero-Downtime Column Rename

**Skills tested:** [SK-11](#sk-11-schema-migration-planner), [SK-10](#sk-10-jpa--spring-data-kotlin-mapper)

**Why this benchmark matters:** Destructive schema changes done wrong cause production outages. This benchmark tests whether the AI can plan an expand/contract migration.

### Step 1 — Build (initial schema)

**Prompt to the agent:**

> Build a Kotlin + Spring Boot user profile service:
>
> - Entity: `UserProfile` (id, userName, emailAddress, bio, createdAt)
> - REST: CRUD endpoints
> - Flyway V1 migration creates the table
> - Tests: CRUD operations work

### Step 2 — Migrate (rename column without downtime)

**Prompt to the agent:**

> Requirement: rename the `emailAddress` column to `email` in the `user_profiles` table.
>
> Constraint: zero-downtime deployment. The old version of the app (reading `emailAddress`) and the new version (reading `email`) must coexist during rollout.
>
> Implement the expand/contract migration:
> 1. **V2 migration**: add `email` column, backfill from `emailAddress`, add trigger or application-level dual-write
> 2. **Application change**: update entity to read from `email`, write to both `email` and `emailAddress`
> 3. **V3 migration**: drop `emailAddress` column (only after all instances are on new version)
> 4. **Final application change**: remove dual-write, entity reads/writes only `email`
>
> Provide all migrations, entity versions, and tests verifying:
> - After V2: both columns have data, app reads from new column
> - After V3: old column is gone, app still works
> - Backward compatibility: V2 app can coexist with V1 app

**Automated checks:**
- All migrations run without errors
- Tests pass at each migration step
- Data integrity test: after V2 migration, all rows have matching `email` and `emailAddress` values
- V3 migration drops the column successfully

---

## B-14. PR Review Simulation

**Skills tested:** [SK-21](#sk-21-code-review-agent-spring--kotlin), [SK-03](#sk-03-kotlin--spring-proxy-compatibility), [SK-09](#sk-09-transaction--consistency-designer), [SK-13](#sk-13-spring-security-configurator--auditor)

**Why this benchmark matters:** Code review is a daily activity where Spring-specific and Kotlin-specific knowledge produces dramatically different quality of feedback. This benchmark tests review depth.

### Step 1 — Build (the codebase with planted bugs)

**Prompt to the agent:**

> Create a Kotlin + Spring Boot payment service. Then introduce the following 10 bugs/issues WITHOUT mentioning them — just write the code naturally as if a junior developer wrote it:
>
> 1. JPA entity `Payment` is a `data class` (wrong: breaks equals/hashCode with proxies)
> 2. `@Transactional` on a private method (won't be proxied)
> 3. Self-invocation: `processPayment()` calls `this.saveAuditLog()` which has `@Transactional(propagation = REQUIRES_NEW)` — the REQUIRES_NEW is bypassed
> 4. Validation annotations without `@field:` target on Kotlin data class DTO
> 5. `SecurityFilterChain` with `permitAll()` on `/api/**` (too broad — should be specific paths)
> 6. `@Cacheable` on a method that returns a mutable object (cache poisoning risk)
> 7. No null-safety at Java interop boundary: calling a Java library method that can return null, assigned to non-null Kotlin type
> 8. Jackson `ObjectMapper` configured without `KotlinModule`
> 9. `@Scheduled` job without distributed lock (runs on all instances)
> 10. SQL query in a loop (N+1 pattern in service layer, not repository)
>
> The code should compile and basic tests should pass (the bugs are subtle, not compilation errors).

### Step 2 — Review

**Prompt to the agent:**

> Perform a thorough code review of this payment service. For each issue found:
>
> 1. **Severity**: CRITICAL / HIGH / MEDIUM / LOW
> 2. **Category**: security / correctness / performance / style / maintainability
> 3. **Location**: file and line
> 4. **Problem**: what's wrong and why it's dangerous
> 5. **Fix**: the minimal code change
>
> Prioritize by risk. Don't just check style — focus on bugs that would cause production issues.

**Automated checks:**
- Number of planted bugs found: count out of 10
- False positive rate: number of non-issues flagged as bugs
- Severity accuracy: were CRITICALs ranked higher than LOWs?
- Fix quality: do the proposed fixes compile and resolve the issues?

**Scoring:**
- Each correctly identified bug: +2 points
- Each correct fix that compiles: +1 point
- Each false positive: -1 point
- Max: 30 points (10 bugs × 3)

---

## B-15. Production Incident Triage

**Skills tested:** [SK-15](#sk-15-stacktrace--log-triage), [SK-24](#sk-24-production-incident-responder), [SK-02](#sk-02-spring-context--di-graph-reasoning)

**Why this benchmark matters:** Incident response is where generic LLMs fail most dramatically — they jump to code fixes instead of mitigation, misidentify root causes, and give dangerous advice under pressure. This benchmark tests the full incident response lifecycle.

### Step 1 — Build (the service)

**Prompt to the agent:**

> Build a Kotlin + Spring Boot inventory service:
>
> - Entity: `Product` (id, sku, name, stockQuantity, reservedQuantity, lastRestockedAt)
> - Endpoints: `POST /api/products/{id}/reserve` (reserve N units), `POST /api/products/{id}/release` (release N units), `GET /api/products/{id}/availability`
> - Business rule: `availableQuantity = stockQuantity - reservedQuantity`, cannot reserve more than available
> - Optimistic locking with `@Version`
> - Tests: basic CRUD + reservation logic

### Step 2 — Introduce the incident

**Prompt to the agent:**

> The following production incident report has been filed:
>
> **Alert**: Error rate on `/api/products/{id}/reserve` jumped from 0.1% to 47% at 14:32 UTC.
>
> **Logs** (provided):
> ```
> 2024-03-15 14:32:15.123 ERROR [req-id=a1b2] o.s.w.s.m.m.a.ExceptionHandlerExceptionResolver - Resolved [org.springframework.orm.ObjectOptimisticLockingFailureException: Row was updated or deleted by another transaction]
> 2024-03-15 14:32:15.124 ERROR [req-id=c3d4] o.s.w.s.m.m.a.ExceptionHandlerExceptionResolver - Resolved [org.springframework.orm.ObjectOptimisticLockingFailureException: Row was updated or deleted by another transaction]
> [... 200 more similar errors per second ...]
> 2024-03-15 14:31:58.001 INFO  Deployment v2.14.0 started
> 2024-03-15 14:32:00.000 INFO  Deployment v2.14.0 complete
> ```
>
> **Context**: A new feature was deployed at 14:31 that added a "flash sale" endpoint which reserves inventory for 100 customers simultaneously. The flash sale went live at 14:32.
>
> **Your task:**
> 1. **Immediate mitigation** (first 5 minutes): What do you do right now to restore service? (Options: rollback deployment, feature flag, rate limit, retry with backoff)
> 2. **Root cause analysis**: Why is optimistic locking failing at this rate? (Hint: 100 concurrent reservations for the same product)
> 3. **Fix**: Implement a retry strategy for `ObjectOptimisticLockingFailureException` with exponential backoff (3 attempts max)
> 4. **Alternative fix**: If contention is too high for optimistic locking, implement pessimistic locking with `SELECT ... FOR UPDATE` as an alternative
> 5. **Postmortem**: Write a brief postmortem with: timeline, impact, root cause, what went well, what went wrong, action items
>
> Implement both fixes (retry and pessimistic lock) and add a load test that simulates 50 concurrent reservations for the same product.

**Automated checks:**
- Both fixes compile and tests pass
- Mitigation recommendation is non-code-change (rollback or rate limit) — not "deploy a fix"
- Retry fix: `ObjectOptimisticLockingFailureException` is caught and retried up to 3 times
- Pessimistic lock fix: uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the repository query
- Load test: 50 concurrent reservations → all succeed (no data inconsistency), total reserved = 50
- Postmortem document present with all 5 sections

---

## Benchmark Summary

| # | Benchmark | Steps | Skills Tested | Max Score |
|---|-----------|-------|---------------|-----------|
| B-01 | Order CRUD Service | 2 | SK-04,06,07,08,10,14 | 100 |
| B-02 | Transaction Trap | 2 | SK-03,09,15 | 100 |
| B-03 | N+1 Query Clinic | 3 | SK-10,18,14 | 150 |
| B-04 | Security Lockdown | 2 | SK-13,06,14 | 100 |
| B-05 | Kafka Event Pipeline | 1 | SK-12,09,14 | 50 |
| B-06 | Config Maze | 2 | SK-16,02,15 | 100 |
| B-07 | Java→Kotlin Migration | 3 | SK-19,03,20 | 150 |
| B-08 | Jackson Gauntlet | 1 | SK-08,06,07 | 50 |
| B-09 | Digital Wallet (full) | 1 | SK-25,06,09,10,12,14 | 50 |
| B-10 | Observability Retrofit | 2 | SK-17,18 | 100 |
| B-11 | Spring Boot Upgrade | 2 | SK-22,04,05 | 100 |
| B-12 | Resilient HTTP Integration | 1 | SK-12,14 | 50 |
| B-13 | Schema Migration | 2 | SK-11,10 | 100 |
| B-14 | PR Review Simulation | 2 | SK-21,03,09,13 | 30+50 |
| B-15 | Production Incident | 2 | SK-15,24,02 | 100 |
| | | **Total** | **All 25 skills** | **~1330** |

---

## Summary: AI Request Categories at a Glance

| Category | Pipeline Stages | Examples |
|----------|----------------|----------|
| **Decomposition & Design** | 1, 2 | Requirements analysis, API contracts, ADRs, module boundaries |
| **Code Generation** | 3, 4, 5, 7, 8 | Controllers, DTOs, services, entities, config classes |
| **Build & Dependency Resolution** | 3, 18 | Gradle fixes, version conflicts, plugin setup |
| **Diagnosis & Debugging** | 4, 8, 14, 19 | Stack traces, DI issues, transaction bugs, config binding |
| **Optimization** | 8, 13, 19 | N+1 queries, connection pools, JVM tuning, caching |
| **Testing** | 12 | Unit/slice/integration tests, mocking strategy, flaky test fixes |
| **Security Configuration** | 11 | Filter chains, JWT, CORS, method security, vulnerability review |
| **Migration & Upgrades** | 17, 18 | Java→Kotlin conversion, Spring Boot upgrades, CVE patches |
| **Observability & Operations** | 13, 15, 19 | Metrics, logging, tracing, CI/CD, runbooks, incident response |
| **Explanation & Learning** | All | Spring internals, Kotlin idioms, architecture trade-offs |

---

## Summary: Pipeline at a Glance

| # | Stage | Key Activities |
|---|-------|---------------|
| 1 | Requirements & API Contracts | Use-cases, API schemas, NFRs, ADRs |
| 2 | Architecture & Module Design | Layers, boundaries, patterns, error model |
| 3 | Project Bootstrap | Gradle, plugins, dependencies, code quality |
| 4 | Spring Core Configuration | DI, profiles, properties, AOP, Jackson |
| 5 | Web Layer | Controllers, DTOs, validation, error handling |
| 6 | Serialization | Jackson + Kotlin, nullability, polymorphism |
| 7 | Business Logic | Services, domain rules, idempotency, caching |
| 8 | Data Access | JPA/JDBC, transactions, queries, optimization |
| 9 | Schema Migrations | Flyway/Liquibase, zero-downtime, rollback |
| 10 | Integrations & Resilience | HTTP clients, Kafka, retries, circuit breakers |
| 11 | Security | AuthN/AuthZ, CORS, CSRF, method security |
| 12 | Testing | Unit, slice, integration, contract tests |
| 13 | Observability | Logging, metrics, tracing, health checks |
| 14 | Debugging & Triage | Stack traces, DI issues, transactions, config |
| 15 | CI/CD & Release | Pipelines, Docker, deployment, rollback |
| 16 | Code Review & Refactoring | PR review, Kotlin idioms, tech debt |
| 17 | Java → Kotlin Migration | Incremental conversion, Lombok removal |
| 18 | Upgrades & Dependencies | Spring/Kotlin/Gradle upgrades, CVE fixes |
| 19 | Production Support | Incidents, performance, continuous improvement |

---

# How to Run the Benchmark Suite

> Practical guide for running the 15 standard benchmarks locally with Claude Code and/or GPT Codex.

## Prerequisites

- **Java 17+** and **Gradle 8+** (or Gradle wrapper will be downloaded by each project)
- **Claude Code CLI** (`claude`) — installed and authenticated
- **OpenAI Codex CLI** (`codex`) — installed and authenticated (optional, only for Codex mode)
- **ripgrep** (`rg`) — used by the evaluation script
- Clone the skills repo: `git clone https://github.com/Kotlin/kotlin-backend-agent-skills.git`

## Repository Structure

```
kotlin-backend-agent-skills/
├── agent-skills/                    # 25 skill definitions (3 tiers)
│   ├── tier-1-critical/             # 10 skills
│   ├── tier-2-high-value/           # 10 skills
│   └── tier-3-specialized/          # 5 skills
├── benchmarks/                      # 15 standard tasks + 6 hard/compound calibration benchmarks
│   ├── B-01-order-crud/             # Each has: meta.yaml, step-N.md, eval.yaml
│   ├── B-02-transaction-trap/
│   ├── ...
│   ├── B-15-production-incident/
│   ├── H-01-proxy-trap/             # Hard starter-fix benchmarks
│   ├── ...
│   ├── H-06-zero-downtime-order-fallout/
│   ├── run.sh                       # Main runner script
│   ├── eval-check.sh                # Automated evaluation
│   ├── compare.sh                   # Cross-mode comparison
│   └── llm-judge-prompt.md          # LLM-as-Judge template
├── CLAUDE.md                        # Claude Code project config (with skills summary)
├── AGENTS.md                        # OpenAI Codex project config
└── Kotlin_Spring_Developer_Pipeline.md  # This document
```

## Quick Start

```bash
cd benchmarks
chmod +x run.sh eval-check.sh compare.sh

# Run a single benchmark with Claude + skills
./run.sh B-01 claude+skills

# Run without skills (baseline)
./run.sh B-01 claude-skills

# Run all benchmarks
./run.sh all claude+skills

# Compare results
./compare.sh
```

## Four Execution Modes

| Mode | Agent | Skills | What happens |
|------|-------|--------|-------------|
| `claude+skills` | Claude Code | Yes | CLAUDE.md is populated with all skill definitions from agent-skills/ |
| `claude-skills` | Claude Code | No | CLAUDE.md is empty/minimal — baseline without specialized knowledge |
| `codex+skills` | GPT Codex | Yes | AGENTS.md is populated with skill definitions |
| `codex-skills` | GPT Codex | No | AGENTS.md is empty/minimal — baseline |

## How a Benchmark Run Works

1. **Workspace creation**: A fresh directory is created for the agent (no prior code).
2. **Skills injection** (if +skills mode): All SKILL.md files from `agent-skills/` are concatenated into the workspace's CLAUDE.md or AGENTS.md.
3. **Step execution**: For each step (step-1.md, step-2.md, ...):
   - The step prompt is sent to the agent.
   - The agent produces code autonomously.
   - Automated evaluation runs (`eval-check.sh`).
   - If checks fail, the agent sees the failure output and can retry (up to 3 fix loops).
4. **Scoring**: Final state is scored on two layers:
   - **Layer 1 — Automated** (0–25 per step): compilation, tests, grep checks, test count, Detekt
   - **Layer 2 — LLM-as-Judge** (0–25 per step): readability, architecture, task adherence, error handling, overall

## Running Manually (Without the Script)

You can run any benchmark manually for deeper investigation:

```bash
# 1. Create a workspace
mkdir /tmp/bench-B01 && cd /tmp/bench-B01

# 2. (Optional) Copy skills into CLAUDE.md
cat /path/to/agent-skills/tier-1-critical/*/SKILL.md > CLAUDE.md

# 3. Open Claude Code in the workspace
claude

# 4. Paste the prompt from benchmarks/B-01-order-crud/step-1.md
# 5. Let the agent work
# 6. Run evaluation:
/path/to/benchmarks/eval-check.sh /path/to/benchmarks/B-01-order-crud /tmp/bench-B01 1
```

## Evaluation Details

### Automated Checks (eval-check.sh)

For each benchmark step, the script:
1. **Gates**: Runs `./gradlew compileKotlin` and `./gradlew test` — both must pass
2. **Grep checks**: Verifies presence or absence of patterns (e.g., `data class` for entities = FAIL)
3. **Test count**: Ensures minimum number of test methods
4. **Output**: JSON report with pass/fail per check

### LLM-as-Judge (llm-judge-prompt.md)

After automated checks, feed the solution code + the judge prompt to a separate LLM call. The judge scores 5 dimensions (0-5 each) for a total of 0-25 per step.

### Comparison (compare.sh)

After running benchmarks in multiple modes:
```bash
./compare.sh ../results
```
Produces a markdown table comparing automated check pass rates across all modes.

## Expected Results

The hypothesis is that **+skills modes** will score significantly higher than **-skills modes** on:
- Spring-specific correctness (proxy safety, transaction patterns, JPA entity design)
- Kotlin idiom adherence (no `!!`, `@field:` targets, MockK usage)
- Production readiness (error handling, security config, observability)

The delta between +skills and -skills is the measured **skill impact**.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BENCHMARK_DIR` | Script's directory | Path to benchmarks/ |
| `SKILLS_DIR` | `../agent-skills` | Path to agent-skills/ |
| `RESULTS_DIR` | `../results` | Where to store results |
| `MAX_FIX_LOOPS` | `3` | Max retry iterations per step |
