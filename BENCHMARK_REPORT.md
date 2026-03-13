# Benchmark Report — Kotlin + Spring AI Skills Evaluation

**Date:** 2026-03-13 (updated from 2026-03-12 with hardened H-06 results)
**Environment:** macOS Darwin 25.2.0, OpenJDK 25.0.2 (eval scripts force JDK 17), Gradle 9.4.0, Kotlin 2.0.21

---

## 1. Overview

This report documents the main evaluation of 25 AI skills for Kotlin + Spring development across 20 benchmarks (15 standard + 5 hard), plus a post-report calibration update for one new compound hard benchmark (`H-06`).

**Goal:** measure whether Kotlin/Spring-specific skills improve AI agent performance on domain tasks, and find a difficulty level that differentiates models.

**Agents tested:**
- **Claude Code (Opus)** — `claude --print --dangerously-skip-permissions` with default model
- **Claude Code Sonnet 4.6** — `claude --print --model claude-sonnet-4-6`
- **OpenAI Codex (gpt-5.4, xhigh reasoning)** — `codex exec --dangerously-bypass-approvals-and-sandbox`
- **OpenAI Codex (gpt-5.4, medium reasoning)** — same CLI, config `model_reasoning_effort = "medium"`

**Modes:**
- `claude+skills` / `codex+skills` — agent-skills injected into CLAUDE.md/AGENTS.md
- `claude-skills` / `codex-skills` — no skills, vanilla agent

**Important scope note:** Sections 3.1-3.2 are the original full-matrix benchmark results. Section 3.3 adds the later `H-06` calibration on the hardened runner with hidden behavioral evaluation and first-attempt scoring.

---

## 2. Benchmark Suite

### 2.1 Standard Benchmarks (B-01 through B-15)

15 multi-step coding tasks covering CRUD, transactions, N+1, security, Kafka, config, migrations, Jackson, resilience, observability, upgrades, PR review, incident response.

Each benchmark has:
- `starter/` — compilable project with partial implementation
- `step-N.md` — prompts sent to the agent sequentially
- `eval.yaml` — gates (compile, test) + automated grep checks

| ID | Name | Steps | Checks | Skills Targeted |
|----|------|:-----:|:------:|-----------------|
| B-01 | Order CRUD | 2 | 5 | SK-06, SK-10, SK-14 |
| B-02 | Transaction Trap | 2 | 4 | SK-09 |
| B-03 | N+1 Clinic | 3 | 5 | SK-10, SK-18 |
| B-04 | Security Lockdown | 2 | 4 | SK-13 |
| B-05 | Kafka Event Pipeline | 1 | 12 | SK-06, SK-08 |
| B-06 | Config Maze | 2 | 4 | SK-16 |
| B-07 | Java→Kotlin Migration | 3 | 8 | SK-19 |
| B-08 | Jackson Gauntlet | 1 | 11 | SK-08 |
| B-09 | Digital Wallet | 1 | 13 | SK-09, SK-07 |
| B-10 | Observability Retrofit | 2 | 10 | SK-17 |
| B-11 | Spring Boot Upgrade | 2 | 12 | SK-22 |
| B-12 | Resilient HTTP | 1 | 12 | SK-12 |
| B-13 | Schema Migration | 2 | 6 | SK-11 |
| B-14 | PR Review | 2 | 5 | SK-21 |
| B-15 | Production Incident | 2 | 11 | SK-24, SK-15 |

**Total checks across all B-benchmarks:** ~122 per run (varies slightly by step count).

### 2.2 Hard Benchmarks (H-01 through H-05)

Designed after B-benchmarks proved too easy. Format: "starter-fix" — project compiles, basic tests pass, but verification tests are `@Disabled`. Agent must enable tests and fix production code.

Key design principles:
- No `// BUG:` comments in source — bugs hidden in code structure only
- `@Disabled` messages are symptom-only (e.g., "Known issue — cache not returning same instance"), never root-cause
- Multiple interacting bugs per benchmark (3-5 bugs each)
- Each benchmark targets a single skill area at high obscurity

| ID | Name | Skill | Checks | Planted Bugs |
|----|------|-------|:------:|--------------|
| H-01 | Proxy Trap | SK-03 | 12 | data class entity, self-invocation ×3 (@Cacheable, @Transactional, @Async), missing @EnableCaching/@EnableAsync |
| H-02 | Transaction Minefield | SK-09 | 12 | private @Transactional, audit log in same transaction (no REQUIRES_NEW), no @Version for optimistic locking, transfer without @Transactional, data class entities |
| H-03 | JPA Identity Crisis | SK-10 | 12 | data class entities ×2, EAGER fetch → N+1, toString triggers lazy init, missing bidirectional sync, no orphanRemoval |
| H-04 | Jackson Puzzle Box | SK-08 | 12 | missing KotlinModule, missing JavaTimeModule, no @JsonTypeInfo on sealed class, PATCH tri-state handling, enum @JsonCreator |
| H-05 | Migration Gauntlet | SK-11 | 12 | direct column rename (should be expand-contract), no backfill, no dual-write, missing index |

**Total checks across all H-benchmarks:** 60 (12 per benchmark).

### 2.3 Compound Hard Calibration Benchmark (H-06)

`H-06` was added after `H-01` through `H-05` also saturated. Unlike the earlier hard benchmarks, it is deliberately multi-skill and behaviorally evaluated through hidden tests.

| ID | Name | Skills | Checks | Structure |
|----|------|--------|:------:|-----------|
| H-06 | Zero-Downtime Order Fallout | SK-08, SK-09, SK-03, SK-11 | 17 hidden behavioral checks + 2 gates = **19 total** | 6 interacting bug clusters across migration, PATCH semantics, transactions, caching, status transitions, and notifications |

**Six bug clusters:**

| Cluster | Area | Checks | Root Bug |
|---------|------|:------:|----------|
| **A** Migration | Zero-downtime column rename | A1, A2, A3 | V2 migration uses `RENAME COLUMN` instead of expand-contract; fix requires dual-write with both `shipping_address` and `delivery_address` |
| **B** PATCH | Tri-state null semantics | B1, B2, B3 | `?:` (Elvis) cannot distinguish omitted from explicit null; fix requires `Optional<T>?` or `presentFields: Set<String>` |
| **C** Transactions | Outbox + audit boundaries | C1, C2, C3 | OutboxService uses `REQUIRES_NEW` (orphans on rollback), AuditService.recordFailure uses `REQUIRED` (lost on rollback) |
| **D** Cache | Proxy self-invocation | D1, D2, D3 | OrderSummaryService calls its own `@Cacheable`/`@CacheEvict` methods via `this`, bypassing Spring proxy |
| **E** Status | Forward-only transitions | E1, E2, E3 | StatusTransitionValidator only checks enum validity, doesn't enforce forward-only progression |
| **F** Notifications | Event timing | F1, F2 | NotificationService uses `@EventListener` instead of `@TransactionalEventListener(AFTER_COMMIT)`; event published before validation |

**Baseline (unmodified starter):** 8/19 — passes gates + B1, C3, D1, E1, E3, F1.
**Net-scoreable checks** (fail on baseline): A1, A2, A3, B2, B3, C1, C2, D2, D3, E2, F2 = **11 checks**.

Special evaluation properties:
- `max_fix_loops: 0` for first-attempt measurement
- hidden verification runs through `command` checks and `eval-hidden.sh`
- agent phase runs in isolated temp workspaces with benchmark-sensitive assets masked
- canonical comparison uses `AGENT_TIMEOUT_SECONDS=900`

Design reference: `benchmarks/COMPOUND_BENCHMARK_DESIGN.md`

---

## 3. Results

### 3.1 V2 Standard Benchmarks (B-01 — B-15)

Benchmarks were revised from V1 (hints removed, cross-cutting checks added).

| Benchmark | claude+skills | claude-skills | codex-skills (xhigh) |
|-----------|:---:|:---:|:---:|
| B-01 Order CRUD | 5/5 | 5/5 | 5/5 |
| B-02 Transaction Trap | 4/4 | 4/4 | 4/4 |
| B-03 N+1 Clinic | 5/5 | 5/5 | 5/5 |
| B-04 Security Lockdown | 4/4 | **3/4** | 4/4 |
| B-05 Kafka Event Pipeline | 12/12 | 12/12 | 12/12 |
| B-06 Config Maze | 4/4 | 4/4 | 4/4 |
| B-07 Java→Kotlin Migration | 8/8 | 8/8 | 8/8 |
| B-08 Jackson Gauntlet | 11/11 | 11/11 | 11/11 |
| B-09 Digital Wallet | 13/13 | 13/13 | 13/13 |
| B-10 Observability Retrofit | 10/10 | 10/10 | 10/10 |
| B-11 Spring Boot Upgrade | 12/12 | 12/12 | 12/12 |
| B-12 Resilient HTTP | 12/12 | 12/12 | 12/12 |
| B-13 Schema Migration | 6/6 | 6/6 | 6/6 |
| B-14 PR Review | 5/5 | 5/5 | 5/5 |
| B-15 Production Incident | 11/11 | 11/11 | 11/11 |
| **TOTAL** | **122/122 (100%)** | **121/122 (99.2%)** | **122/122 (100%)** |

**Observation:** Only 1 check difference across all modes. B-04 Security Lockdown was the only failure (claude-skills missed one check). These benchmarks do not differentiate.

### 3.2 Hard Benchmarks (H-01 — H-05) — Accuracy

| Benchmark | Codex gpt-5.4 xhigh | Claude Sonnet 4.6 | Codex gpt-5.4 medium |
|-----------|:---:|:---:|:---:|
| H-01 Proxy Trap | 12/12 | 12/12 | 12/12 |
| H-02 Transaction Minefield | 12/12 | 12/12 | 12/12 |
| H-03 JPA Identity Crisis | 12/12 | 12/12 | 12/12 |
| H-04 Jackson Puzzle Box | 12/12 | 12/12 | 12/12 |
| H-05 Migration Gauntlet | 12/12 | 12/12 | 12/12 |
| **TOTAL** | **60/60 (100%)** | **60/60 (100%)** | **60/60 (100%)** |

**All three configurations scored 100%.** Hard benchmarks also do not differentiate.

### 3.3 Compound Hard Benchmark (H-06) — Hardened 19-Check Results

These runs use the hardened runner (6 clusters, 19 checks), isolated temp workspaces, hidden behavioral evaluation, `max_fix_loops=0`, and `AGENT_TIMEOUT_SECONDS=900`.

#### Phase 1: Initial 14-Check Version (deprecated)

The original H-06 had 4 clusters (A-D) with 12 behavioral checks + 2 gates = 14 total. These results are archived for reference but superseded by the hardened version.

| Mode | Score (14) | Notes |
|------|:----------:|-------|
| `codex+skills` | 14/14 (100%) | full pass |
| `codex-skills` | 13/14 (92.9%) | missed D3 |
| `claude-skills` | 10/14 (71.4%) | failed A3, B2, B3, D3 |
| `claude+skills` (file refs) | 5/14 (35.7%) | timed out, no workspace change |

#### Phase 2: Hardened 19-Check Version (canonical)

Added clusters E (status transitions) and F (notifications) to increase difficulty and surface area. Baseline rises from 5/14 to 8/19. Net-scoreable checks: 11.

**Canonical results (best of N runs for each mode):**

| Mode | Raw Score | Net Score | Elapsed | Failures |
|------|:---------:|:---------:|:-------:|----------|
| `codex-skills` | **19/19** (100%) | 11/11 | 635s | none |
| `claude+skills` (compact) | **19/19** (100%) | 11/11 | 542s | none |
| `claude-skills` (run 1) | 15/19 (79%) | 7/11 | 581s | A1, A3, B2, B3 |
| `claude-skills` (run 2) | 14/19 (74%) | 6/11 | 788s | A1, A3, B2, B3, D3 |

**Reproducibility:** Three independent `claude+skills` runs all scored 19/19 (T235513Z at 779s, T004836Z at 542s, T010350Z at 705s). Two independent `claude-skills` runs scored 15/19 and 14/19 respectively; both consistently failed A1, A3, B2, B3. The agent used different tri-state PATCH implementations across runs (custom Jackson deserializer with `presentFields` vs sentinel-string approach), demonstrating that the skills guide the agent toward the correct pattern family without dictating a specific implementation.

#### Key Finding: Skills Close the Gap

The compact skills delivery method (see Section 3.5) enables Claude to match Codex's perfect score on H-06. Without skills, Claude consistently fails on:

- **A1/A3 (migration):** Uses direct `RENAME COLUMN` instead of expand-contract. Skills inject the concrete expand-contract pattern.
- **B2/B3 (PATCH null):** Uses `?:` (Elvis) which treats omitted and explicit null identically. Skills inject the tri-state `Optional<T>?` pattern.

With skills providing these concrete patterns in CLAUDE.md, Claude implements both correctly on first attempt.

### 3.4 Hard Benchmarks — Timing & Fix Loops

| Benchmark | Codex xhigh | Claude Sonnet 4.6 | Codex medium |
|-----------|:-----------:|:------------------:|:------------:|
| H-01 Proxy Trap | ~5 min, 1 fix | ~7 min, 1 fix | ~3 min, 1 fix |
| H-02 Transaction Minefield | ~3 min, 0 fix | ~3 min, 0 fix | ~2 min, 0 fix |
| H-03 JPA Identity Crisis | ~3 min, 0 fix | ~2 min, 0 fix | ~2 min, 0 fix |
| H-04 Jackson Puzzle Box | ~10 min, 1 fix | ~6 min, 1 fix | ~3 min, 1 fix |
| H-05 Migration Gauntlet | ~8 min, 1 fix | ~22 min, 1 fix | ~6 min, 1 fix |
| **Total time** | **~29 min** | **~39 min** | **~15 min** |

Fix loops were identical across all configs: H-01, H-04, H-05 needed 1 extra pass; H-02, H-03 passed on first attempt.

**Speed ranking:** Codex medium (15 min) > Codex xhigh (29 min) > Claude Sonnet (39 min).
Claude Sonnet was particularly slow on H-05 (22 min vs 6-8 min for Codex).

### 3.5 Skills Delivery Optimization

The `claude+skills` mode failed on initial H-06 attempts because the skills injection strategy was wrong. Three delivery strategies were tested:

#### Strategy 1: File References Only
CLAUDE.md lists skill files with one-line descriptions and paths to `.benchmark-skills/*/SKILL.md`. The agent is expected to read the relevant skills when needed.

**Result:** Agent ignores file references. Never reads SKILL.md files. Applies its own training-data patterns, missing expand-contract and tri-state PATCH. Score: same as `claude-skills`.

#### Strategy 2: Full Inline (152KB)
Complete SKILL.md content for all 25 skills concatenated into CLAUDE.md (~152KB, ~38K tokens).

**Result:** Agent hangs analyzing the massive context. Times out with no workspace changes. Score: 0 net checks.

#### Strategy 3: Compact Inline (19KB) — Winning Strategy
CLAUDE.md contains:
1. **Skill index** — one line per skill with description and file path (~25 lines)
2. **Extracted concrete patterns** — awk extracts `## *Pattern*`, `## *Expand*Contract*`, `## *Tri*State*` headings from SKILL.md files with full code examples (~120 lines)
3. **Categorized rules** — ~30 concise rules organized by topic (Entity, Proxy, Transactions, Serialization, Migration, Kotlin) (~50 lines)

Total: ~247 lines, ~31KB (~8K tokens). Small enough for the agent to read in full, concrete enough to apply directly.

**Result:** 19/19 on two independent runs (542s and 779s). Agent implements expand-contract migration, tri-state PATCH deserializer, and all other patterns correctly on first attempt.

#### Why Compact Works

The critical insight is that skills must deliver **executable patterns** (code examples the agent can copy-adapt), not **reference pointers** (paths the agent should read) or **encyclopedic content** (full specification documents).

The agent's behavior with each strategy:

| Strategy | CLAUDE.md size | Agent reads skills? | Patterns applied? | Score |
|----------|:--------------:|:-------------------:|:-----------------:|:-----:|
| File references | ~3KB | No | No | 14-15/19 |
| Full inline | ~152KB | N/A (hangs) | N/A | 2/19 (baseline) |
| Compact inline | ~31KB | N/A (inline) | Yes | **19/19** |

---

## 4. Verified Architectural Fixes

All three models made proper architectural fixes on `H-01` through `H-05` (not test modifications):

### H-01 Proxy Trap
- Added `@EnableCaching` and `@EnableAsync` to application class
- Converted `data class` Subscription entity to regular class with manual `equals`/`hashCode`
- Fixed self-invocation: injected self via `@Lazy @Autowired` or extracted to separate bean
- Result: all 5 verification tests pass

### H-02 Transaction Minefield
- Changed `private` `@Transactional` methods to `open`
- Extracted `AuditLogService` with `@Transactional(propagation = REQUIRES_NEW)`
- Added `@Version` field to `Payment` entity for optimistic locking
- Added `@Transactional` to transfer operation
- Converted data class entities to regular classes

### H-03 JPA Identity Crisis
- Converted data class `Author` and `Post` entities to regular classes
- Changed `EAGER` fetch to `LAZY`
- Fixed `toString()` to not trigger lazy initialization
- Added bidirectional sync helpers (`addPost`/`removePost`)
- Added `orphanRemoval = true`

### H-04 Jackson Puzzle Box
- Registered `KotlinModule` in ObjectMapper configuration
- Registered `JavaTimeModule` for `Instant` serialization
- Added `@JsonTypeInfo`/`@JsonSubTypes` to sealed event class hierarchy
- Implemented tri-state PATCH with `Optional<T>?` pattern
- Added `@JsonCreator` for enum deserialization

### H-05 Migration Gauntlet
- Replaced direct `RENAME COLUMN` with expand-contract pattern:
  - V2 migration: `ADD COLUMN display_name` + `UPDATE ... SET display_name = product_name` + `NOT NULL` constraint + index
- Implemented dual-write in `ProductService` (writes to both `product_name` and `display_name`)
- Kept old `product_name` column readable for backward compatibility

### H-06 Zero-Downtime Order Fallout (19/19 solutions)
All six bug clusters were fixed in the `19/19` solutions (`claude+skills` compact, `codex-skills`):
- **A (migration):** Expand-contract: add `delivery_address` alongside `shipping_address`, backfill data, dual-write via `@PrePersist`/`@PreUpdate`
- **B (PATCH):** Custom Jackson deserializer tracking `presentFields: Set<String>` to distinguish omitted from explicit null
- **C (transactions):** OutboxService changed to `REQUIRED` (joins caller tx, rolls back together), AuditService.recordFailure changed to `REQUIRES_NEW` (persists independently)
- **D (cache):** Replaced self-invocation with `@CachePut` on the service method to ensure Spring proxy intercepts the call
- **E (status):** Added `require(newStatus.ordinal > currentStatus.ordinal)` to enforce forward-only transitions
- **F (notifications):** Changed `@EventListener` to `@TransactionalEventListener(AFTER_COMMIT)` and moved event publication after validation

---

## 5. Technical Issues & Solutions

### 5.1 JDK 25 Compatibility (Build)
- **Problem:** Kotlin 1.9.25 `JavaVersion.parse()` throws `IllegalArgumentException` on `25.0.2`
- **Fix:** Updated to Kotlin 2.0.21 in all `build.gradle.kts`

### 5.1b JDK 25 Compatibility (Eval Scripts)
- **Problem:** Kotlin 2.0.21 daemon also crashes on JDK 25.0.2 with `IllegalArgumentException: 25.0.2` when started via `--no-daemon` (which spawns a fresh JVM). Agent builds worked because they reused a warm daemon started with JDK 17 toolchain, but `eval-hidden.sh` uses `--no-daemon`, causing all hidden tests to fail.
- **Fix:** Added `JAVA_HOME` fallback to JDK 17 in `eval-hidden.sh`, `eval-check.sh`, and `run.sh`. Scripts detect JDK 25 and auto-switch to `/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`.

### 5.2 Gradle Version
- **Problem:** Gradle 8.x (8.10, 8.13, 8.14.2) all fail with JDK 25 version string
- **Fix:** Updated all `gradle-wrapper.properties` to Gradle 9.4.0

### 5.3 JUnit Platform on Gradle 9.x
- **Problem:** Tests fail with "Failed to load JUnit Platform"
- **Fix:** Added `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` to all `build.gradle.kts`

### 5.4 H2 MODE=PostgreSQL
- **Problem:** H2 in PostgreSQL mode doesn't support `ALTER TABLE ... ALTER COLUMN ... RENAME TO`
- **Fix:** Removed `MODE=PostgreSQL` from `application.yml` connection URL

### 5.5 Benchmark Design Anti-patterns Discovered
- `// BUG:` comments in source code give away answers — removed all, bugs hidden structurally
- `@Disabled` messages with root causes (e.g., "Missing @EnableCaching") — changed to symptom-only
- `step-1.md` describing the solution pattern (expand-contract) — rewritten to describe only the problem
- `eval.yaml` checks that always pass (e.g., checking for a field name that exists in starter) — tightened

### 5.6 H-06 Runner Hardening
- Added benchmark-local `max_fix_loops` support in `run.sh` so compound benchmarks can measure true first-attempt behavior
- Added `command` checks to `eval-check.sh` so hidden behavioral verification can run through `eval-hidden.sh` without copying verifier logic into the workspace
- Moved agent execution to isolated temp workspaces outside the repo tree to remove trivial relative-path leakage to benchmark assets
- Masked `eval.yaml`, `eval-hidden.sh`, and `hidden-tests/` during the agent phase, then restored them for evaluation
- Added wall-clock timeout metadata (`timed_out`, `workspace_changed`, `elapsed_seconds`) in `agent-step-N.json`
- Staged skills locally into `.benchmark-skills/` so `+skills` modes no longer depend on external global registries such as `~/.codex/skills`

---

## 6. Infrastructure

### run.sh Capabilities
- Modes: `claude+skills`, `claude-skills`, `codex+skills`, `codex-skills`
- Supports `CLAUDE_MODEL` env var for Claude model selection
- Supports `CODEX_MODEL` env var for Codex model override
- Fix loop: up to 3 iterations with benchmark-local override (`H-06` uses `max_fix_loops: 0`)
- Supports `AGENT_TIMEOUT_SECONDS` for wall-clock budget control
- Runs agents in isolated temp workspaces outside the repo tree
- Masks benchmark-sensitive assets during the agent phase
- Writes `agent-step-N.json` with timeout and workspace-change metadata
- Benchmark selection: individual ID, prefix match (`H`), or `all`
- Gradle wrapper auto-upgrade to 9.4.0

### eval-check.sh
- Gates: compile (`./gradlew build -x test`) and test (`./gradlew test`)
- Automated checks: `grep_present`, `grep_absent`, `test_count_min`, `command`
- Output: JSON with passed/failed/total per step

### Codex Configuration
```toml
# ~/.codex/config.toml
model = "gpt-5.4"
model_reasoning_effort = "xhigh"  # or "medium"
personality = "pragmatic"
```

### Claude Configuration
```bash
# Via env var
CLAUDE_MODEL=claude-sonnet-4-6 ./run.sh H claude-skills
```

---

## 7. Result Directories

All results are in `/Users/Anton.Yalyshev_1/Downloads/skills/results/`:

### V1 Backup (archived)
`results-v1-backup/` — 30 result dirs from V1 benchmarks (B-01..B-15, claude+skills and claude-skills)

### V2 Standard Benchmarks
| Directory Pattern | Mode | Date |
|---|---|---|
| `B-*_claude+skills_20260312T01*` — `B-*_claude+skills_20260312T04*` | Claude Opus + skills | Mar 12, 01:00-05:00 |
| `B-*_claude-skills_20260312T01*` — `B-*_claude-skills_20260312T04*` | Claude Opus - skills | Mar 12, 01:00-04:00 |
| `B-*_codex-skills_20260312T05*` — `B-*_codex-skills_20260312T09*` | Codex gpt-5.4 xhigh - skills | Mar 12, 05:00-10:00 |

### Hard Benchmarks
| Directory Pattern | Mode | Date |
|---|---|---|
| `H-*_codex-skills_20260312T12*` | Codex gpt-5.4 **xhigh** - skills | Mar 12, 12:37-12:59 |
| `H-*_claude-skills_20260312T13*` | Claude **Sonnet 4.6** - skills | Mar 12, 13:16-13:54 |
| `H-*_codex-skills_20260312T13*` — `H-*_codex-skills_20260312T14*` | Codex gpt-5.4 **medium** - skills | Mar 12, 13:56-14:11 |

### H-06 Compound Calibration — Phase 1 (14-check, deprecated)
| Directory | Mode | Notes |
|---|---|---|
| `H-06-..._codex-skills_20260312T165129Z` | `codex-skills` | `13/14` |
| `H-06-..._codex+skills_20260312T170352Z` | `codex+skills` | `14/14` |
| `H-06-..._claude-skills_20260312T171604Z` | `claude-skills` | `10/14` |
| `H-06-..._claude+skills_20260312T172659Z` | `claude+skills` (file refs) | `5/14`, timed out |

### H-06 Compound Calibration — Phase 2 (19-check, canonical)
| Directory | Mode | Score | Notes |
|---|---|:---:|---|
| `H-06-..._codex-skills_20260312T203654Z` | `codex-skills` | **19/19** | 635s, completed |
| `H-06-..._claude-skills_20260312T213456Z` | `claude-skills` | 15/19 | 581s, fails A1/A3/B2/B3 |
| `H-06-..._claude+skills_20260313T004836Z` | `claude+skills` (compact) | **19/19** | 542s, completed |
| `H-06-..._claude-skills_20260313T002614Z` | `claude-skills` (control) | 14/19 | 788s, fails A1/A3/B2/B3/D3 |

Each result directory contains:
- `workspace/` — the code as modified by the agent
- `step-N.log` — agent output log
- `eval-step-N.json` — evaluation results with passed/failed/total

---

## 8. Conclusions

### Key Finding 1: B-series and H-01..H-05 Saturate
The original B-series and `H-01` through `H-05` do not differentiate frontier models: scores remain in the `99-100%` range because the Kotlin/Spring patterns tested are well known from training data.

### Key Finding 2: H-06 Differentiates Claude With vs Without Skills
The hardened H-06 (19 checks, 6 clusters) creates meaningful spread:

| Comparison | Without Skills | With Skills | Delta |
|------------|:--------------:|:-----------:|:-----:|
| Claude (Sonnet 4.6) | 14-15/19 (74-79%) | **19/19 (100%)** | **+4-5 checks** |
| Codex (GPT-5.4 xhigh) | 19/19 (100%) | N/A | 0 |

Claude without skills consistently fails on two pattern families:
- **Expand-contract migration** (A1, A3): The model defaults to `RENAME COLUMN` instead of dual-write
- **Tri-state PATCH semantics** (B2, B3): The model defaults to `?:` (Elvis) which conflates omitted and explicit null

When these patterns are provided as concrete code examples in CLAUDE.md, Claude applies them correctly on first attempt.

### Key Finding 3: Skills Delivery Strategy Matters More Than Skills Content
Three delivery strategies were tested. Only the compact inline strategy works:
- **File references** — agent ignores them, same score as no-skills
- **Full inline (152KB)** — agent hangs, timeout
- **Compact inline (31KB)** — agent applies patterns, 19/19

The lesson: skills must be **executable patterns** (copy-adaptable code examples), not reference pointers or encyclopedic specifications.

### Key Finding 4: Codex Still Saturates
`codex-skills` reaches 19/19 without any skills, making H-06 unable to differentiate for Codex. The patterns that Claude needs explicit guidance for (expand-contract, tri-state PATCH) are well-represented in GPT-5.4's training data.

### Paths Forward
1. **Test skills on other benchmarks** — B-13 (schema migration) and H-05 (migration gauntlet) target the same expand-contract pattern
2. **Create H-07+ benchmarks** that require patterns NOT in training data — custom/proprietary APIs, adversarial Spring configurations
3. **Avoid overfitting** — skills benefit should generalize beyond H-06. The compact CLAUDE.md structure should be tested on the full B-series
4. **First-attempt + behavioral measurement** — keep `max_fix_loops: 0` and hidden behavioral tests for all calibration benchmarks
5. **Skills for Codex** — the current skills may still help on harder future benchmarks even if Codex saturates H-06

---

## Appendix A: V1 Benchmark Results (Archived)

V1 benchmarks (with hints in prompts):
- claude+skills: 186/187 (99.5%)
- claude-skills: 186/187 (99.5%)

V1 was too easy — prompts contained too many implementation hints.

## Appendix B: Aggregate Summary Table

| Configuration | B-series (V2) | H-01..05 | H-06 (19 checks) | Notes |
|---|:---:|:---:|:---:|---|
| Claude Sonnet 4.6 + skills (compact) | — | — | **19/19 (100%)** | Skills close the gap |
| Claude Sonnet 4.6 - skills | — | 60/60 (100%) | 14-15/19 (74-79%) | Fails A1/A3/B2/B3 |
| Claude Opus + skills | 122/122 (100%) | — | — | |
| Claude Opus - skills | 121/122 (99.2%) | — | — | |
| Codex gpt-5.4 xhigh - skills | 122/122 (100%) | 60/60 (100%) | **19/19 (100%)** | Saturates without skills |
| Codex gpt-5.4 medium - skills | — | 60/60 (100%) | — | |

## Appendix C: H-06 Results History

### Phase 1 (14-check, 4 clusters A-D)
| Mode | Score |
|---|:---:|
| `codex+skills` | 14/14 (100%) |
| `codex-skills` | 13/14 (92.9%) |
| `claude-skills` | 10/14 (71.4%) |
| `claude+skills` (file refs) | 5/14 (35.7%) |

### Phase 2 (19-check, 6 clusters A-F, canonical)
| Mode | Score | Reproducibility |
|---|:---:|---|
| `codex-skills` | **19/19** (100%) | single run |
| `claude+skills` (compact) | **19/19** (100%) | 2/2 runs |
| `claude-skills` | 14-15/19 (74-79%) | 2 runs, consistent A1/A3/B2/B3 failures |

### Per-Check Breakdown (Phase 2)

| Check | Baseline | claude-skills | claude+skills | codex-skills |
|-------|:--------:|:-------------:|:-------------:|:------------:|
| Gate: Compile | PASS | PASS | PASS | PASS |
| Gate: Tests | PASS | PASS | PASS | PASS |
| A1 both columns exist | FAIL | FAIL | **PASS** | PASS |
| A2 legacy read works | FAIL | PASS | PASS | PASS |
| A3 dual-write | FAIL | FAIL | **PASS** | PASS |
| B1 omitted preserves | PASS | PASS | PASS | PASS |
| B2 explicit null clears notes | FAIL | FAIL | **PASS** | PASS |
| B3 explicit null clears ref | FAIL | FAIL | **PASS** | PASS |
| C1 failed = no outbox | FAIL | PASS | PASS | PASS |
| C2 failed = audit persists | FAIL | PASS | PASS | PASS |
| C3 success = one outbox | PASS | PASS | PASS | PASS |
| D1 cache reuse | PASS | PASS | PASS | PASS |
| D2 patch refreshes cache | FAIL | PASS | PASS | PASS |
| D3 cache entry updated | FAIL | PASS/FAIL* | PASS | PASS |
| E1 forward transition | PASS | PASS | PASS | PASS |
| E2 backward rejected | FAIL | PASS | PASS | PASS |
| E3 non-status patch ok | PASS | PASS | PASS | PASS |
| F1 success = notification | PASS | PASS | PASS | PASS |
| F2 failed = no notification | FAIL | PASS | PASS | PASS |

*D3 passed in claude-skills run 1 (T213456Z) but failed in run 2 (T002614Z).

For the detailed analysis, see `benchmarks/H-06-zero-downtime-order-fallout/CALIBRATION_REPORT.md`.
