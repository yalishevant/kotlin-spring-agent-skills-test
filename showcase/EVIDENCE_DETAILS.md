# Evidence Details

Detailed methodology, scan results, and per-check A/B comparison tables supporting the [Showcase README](README.md).

## Projects Scanned

11 open-source Kotlin + Spring projects from GitHub:

| Project | Description | JPA? |
|---------|-------------|:----:|
| [gothinkster/kotlin-spring-realworld-example-app](https://github.com/gothinkster/kotlin-spring-realworld-example-app) | RealWorld spec (Medium clone) | Yes |
| [awakelife93/spring-boot-kotlin-boilerplate](https://github.com/awakelife93/spring-boot-kotlin-boilerplate) | Production-ready boilerplate | Yes |
| [bastman/kotlin-spring-jpa-examples](https://github.com/bastman/kotlin-spring-jpa-examples) | JPA examples playground | Yes |
| [callicoder/kotlin-spring-boot-jpa-rest-api-demo](https://github.com/callicoder/kotlin-spring-boot-jpa-rest-api-demo) | JPA REST API demo | Yes |
| [Karumi/SpringBootKotlin](https://github.com/Karumi/SpringBootKotlin) | Spring Boot + Kotlin demo | Yes |
| [spring-guides/tut-spring-boot-kotlin](https://github.com/spring-guides/tut-spring-boot-kotlin) | Official Spring guide | No (JDBC) |
| [piomin/sample-spring-kotlin-microservice](https://github.com/piomin/sample-spring-kotlin-microservice) | Microservice demo | No |
| [iokats/kotlin-microservices-spring-cloud](https://github.com/iokats/kotlin-microservices-spring-cloud) | Spring Cloud microservices | No (MongoDB) |
| [raeperd/realworld-springboot-kotlin](https://github.com/raeperd/realworld-springboot-kotlin) | RealWorld (clean impl) | — |
| [jkazama/sample-boot-kotlin](https://github.com/jkazama/sample-boot-kotlin) | DDD sample | Yes |
| [davidkiss/kotlin-spring-data-demo](https://github.com/davidkiss/kotlin-spring-data-demo) | Spring Data demo | — |

## Scan Findings

| Anti-Pattern | Skill | Findings | Projects Hit |
|-------------|-------|:--------:|:------------:|
| JPA entity as `data class` | SK-10 | **12+** | 5/5 JPA projects (100%) |
| Missing `@field:` on validation | SK-07 | **14** | 1/11 |
| Missing `@Transactional` (multi-op) | SK-09 | **1** | 1/11 |
| `Long` instead of `Duration` | SK-16 | 3 | 2/11 |
| Self-invocation bypassing proxy | SK-03 | 1 | 1/11 |
| `ObjectMapper()` without KotlinModule | SK-08 | 3 | 2/11 (tests only) |
| `!!` operator | — | 40+ | 5/11 |

**SK-10 details:** Every `@Entity` class in all JPA-using projects is `data class`:
- **realworld**: `Article`, `User`, `Comment`, `Tag` (4 entities)
- **boilerplate**: `User`, `Post` (2 entities)
- **bastman**: `Author`, `Tweet`, `Broker`, `Property`, `PropertyCluster`, `PropertyLink` (6 entities)

**SK-07 details:** 14 validation annotations in realworld missing `@field:` — `Register.kt` (9) and `Login.kt` (5). Without `@field:`, annotations target the constructor parameter instead of the backing field, so Bean Validation silently skips them.

**SK-09 details:** `deleteArticle` in realworld performs `commentRepository.deleteAll()` then `repository.delete()` without `@Transactional` — not atomic.

**SK-03 details:** karumi's `DeveloperController` calls `this.asyncHello()` — a `@Async` method on the same bean, bypassing the Spring proxy.

---

## A/B Test Methodology

For each test:
1. Clone the project into two identical copies
2. Give both agents the same task prompt
3. Agent A (no skills): has brief CLAUDE.md rules only
4. Agent B (with skills): has CLAUDE.md rules + detailed skill patterns with broken/correct examples
5. Compare results via `git diff`

All tests are n=1 (single run per mode).

**Limitation:** Both agents see brief CLAUDE.md rules ("JPA entities must NOT be data class", "Use @field: targets", etc.). The with-skills agent additionally receives detailed patterns with examples.

---

## Single-Pattern A/B Tests

### realworld (SK-10 + SK-07) — Focused Task

*"Review JPA entities and request DTOs. Fix bugs."*

| Check | Without skills | With skills |
|-------|:--------------:|:-----------:|
| `data class` → `class` on 4 entities | done | done |
| `hashCode` stable across lifecycle | `id.hashCode()` — **breaks** after persist | `javaClass.hashCode()` — stable |
| `@field:` on constructor-param DTOs (Register, Login) | done | done |
| `@field:` on body-property DTOs (NewArticle, NewComment, UpdateUser) | **over-applied** (unnecessary) | correctly skipped |
| `.copy()` call sites fixed after removing `data class` | **not fixed** | fixed in both handlers |
| **Code compiles?** | **No** | **Yes** |

Without skills, the agent removed `data class` but left `.copy()` calls in `UserHandler.kt` and `ArticleHandler.kt` — **compilation error**. With skills, the agent understood the full implications: correct `hashCode`, precise `@field:` targeting, and `.copy()` replacement.

### bastman (SK-10) — Focused Task

*"Review JPA entities. Fix bugs."* — 6 entities, complex relationships

| Check | Without skills | With skills |
|-------|:--------------:|:-----------:|
| `data class` → `class` on 6 entities | done | done |
| `hashCode = javaClass.hashCode()` | done | done |
| `.copy()` call sites fixed (4 files) | done | done |
| `@Embeddable` value objects left as `data class` | done | done |
| Copy-paste bug found (`lastName` → `firstName`) | found | found |

**Parity.** Both agents performed identically.

### piomin (SK-16) — Focused Task

*"Review controllers and configuration. Fix type safety issues."*

| Check | Without skills | With skills |
|-------|:--------------:|:-----------:|
| `Long delay` → `Duration` | done | done |
| `@Value` → `@ConfigurationProperties` class | kept `@Value` | created `AppProperties` data class |
| `@EnableConfigurationProperties` added | no | yes |
| Constructor binding (immutable `val`) | no (mutable `var`) | yes (`val` with default) |
| `application.yml`: `20` → `20ms` | done | done |
| Extra bugs found (EnvsController, Repository) | 2 fixes | not addressed |

**Mixed.** With-skills: more architecturally correct fix (`@ConfigurationProperties`, constructor binding, immutable `val`). Without-skills: simpler `@Value` approach but also fixed 2 unrelated bugs.

### boilerplate (SK-10 + SK-08) — Focused Task

*"Review JPA entities and test ObjectMapper. Fix bugs."*

| Check | Without skills | With skills |
|-------|:--------------:|:-----------:|
| `data class` → `class` on 2 entities | done | done |
| `hashCode = javaClass.hashCode()` | done | done |
| `ObjectMapper` + KotlinModule in 2 test files | done | done |
| Verified DTOs correctly use `@field:` already | done | done |

**Parity.** Both agents performed identically.

---

## Compound A/B Tests

Broad review tasks (not pattern-specific) to test whether skills help when multiple patterns compete for attention.

### realworld (SK-10 + SK-07 + SK-09) — Broad Task

*"Review the entire codebase for Kotlin + Spring bugs. Fix JPA entities, validation, transactions, null safety, thread safety. Apply all fixes."*

| Check | Without skills | With skills |
|-------|:--------------:|:-----------:|
| `data class` → `class` on 4 entities | done | done |
| `hashCode = javaClass.hashCode()` (stable) | `id.hashCode()` — **breaks** after persist | `javaClass.hashCode()` — stable |
| `@field:` on Register, Login (constructor params) | done | done |
| `@field:` on NewArticle, NewComment, UpdateUser (body properties) | **over-applied** (unnecessary) | correctly skipped |
| `.copy()` call sites fixed | done | done |
| `@Transactional` on `deleteArticle` (2 DB ops) | **missed** | **added** |
| BCrypt null password bug | found | found |
| **Distinct issues caught** | 5 of 6 patterns | **6 of 6 patterns** |

**Key delta:** Without skills, the agent missed `@Transactional` on `deleteArticle()` — which performs `commentRepository.deleteAll()` then `repository.delete()` without atomicity. With skills, SK-09 primed attention to transaction boundaries.

### karumi (SK-10 + SK-03) — Broad Task

*"Review the entire codebase. Fix JPA entities and Spring proxy issues."*

| Check | Without skills | With skills |
|-------|:--------------:|:-----------:|
| `data class` → `class` on DeveloperEntity | done | done |
| `hashCode = javaClass.hashCode()` | done | done |
| Self-invocation: `@Async asyncHello()` extracted to separate bean | done | done |
| `.copy()` call sites in tests fixed | done | done |
| Async service in separate file | inline in controller | **separate file** |

**Parity.** Both agents caught both patterns. Minor: with-skills extracted to a separate `@Service` file, without-skills inlined a `@Component` in the controller.

### boilerplate (SK-10 + SK-16) — Broad Task

*"Review the entire codebase. Fix JPA entities and configuration issues."*

| Check | Without skills | With skills |
|-------|:--------------:|:-----------:|
| `data class` → `class` on User, Post | done | done |
| `hashCode = javaClass.hashCode()` | done | done |
| `Long` expireTime → `Duration` | **explicitly left unchanged** ("not a bug") | **converted to Duration** |
| `@Value` → `@ConfigurationProperties` class | no | **created `JwtProperties`** |
| Constructor binding (immutable `val`) | no | **yes** |
| YAML updated (`1800000` → `30m`) | no | **yes** |

**Key delta:** Without skills, the agent explicitly concluded that `@Value` with `Long` expireTime is "not a bug." With skills, SK-16 identified this as the exact anti-pattern and created a proper `@ConfigurationProperties` class with `Duration` fields.

---

## Compound Benchmark Key Delta Checks

From H-06/H-07/H-08, each run 2-3 times per mode:

| Check | What it tests | Skill | Without skills | With skills |
|-------|--------------|-------|:--------------:|:-----------:|
| Duration binding | `timeout: Duration` instead of `Long` | SK-16 | fails 6/6 | **passes all** |
| Immutable config | `data class` with `val` | SK-16 | fails 5/6 | **passes all** |
| Uniqueness guard | Duplicate check before business logic | SK-10 | fails 3/3 | **passes all** |
| Batch errors | Collect all errors, don't throw on first | SK-09 | fails 3/3 | passes 2/3 |
| Tri-state PATCH | Absent vs explicit null | SK-08 | fails 3/3 | **passes all** |
| Expand-contract | Dual-write migration | SK-11 | fails 3/3 | **passes all** |
| Gateway rethrow | Don't swallow timeout in catch-all | SK-07 | fails 2/3 | **passes all** |
| Proxy bypass | `@CachePut` instead of self-invocation | SK-03 | fails 1/3 | **passes all** |
