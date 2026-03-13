# Compound Benchmark Design

## Why the current suite saturates

The results in `BENCHMARK_REPORT.md` show that both the `B-*` and `H-*` suites are saturated:

- `B-*`: 99-100% across agents
- `H-*`: 100% across Codex xhigh, Codex medium, and Claude Sonnet 4.6

The main reasons are structural:

1. Each `H-*` benchmark tests one well-known pattern in isolation.
2. Verification tests are visible to the agent, and test names leak the intended fix.
3. The agent gets up to 3 repair loops, so first-pass mistakes are cheap.
4. Evaluation is still partly structural (`grep_present`, `grep_absent`) instead of behavioral.
5. The starter codebases are small enough that brute-force full-code inspection is easy.

If the goal is to tune skills, the benchmark must stop rewarding pattern recall and start rewarding codebase navigation, interaction reasoning, and correct behavioral fixes under ambiguity.

## Target profile for a differentiating benchmark

The benchmark should be designed to produce partial success:

- Target first-attempt score range for frontier agents: `20-80%`
- Target center of mass: `40-50%`
- Lower-tier agents should often solve one cluster and miss the rest
- Strong agents should solve most clusters, but not reliably all

Required properties:

1. Medium codebase, not toy code:
   - 30-50 Kotlin files
   - 4-6 packages
   - multiple services/repositories/config classes
2. Multi-pattern composition:
   - at least 4 bug clusters
   - each cluster maps to a different Kotlin/Spring skill
   - clusters interact, so local fixes can break another area
3. Hidden verification:
   - verification tests must not be copied into `workspace/`
   - the agent should only see smoke tests and incident artifacts
4. First-attempt scoring:
   - official score should be produced with `max_fix_loops: 0`
   - repair-loop score can be tracked separately, but not as the headline metric
5. Behavioral evaluation:
   - runtime behavior, DB state, event side effects, query counts, cache freshness
   - very few structural checks, only for hard guardrails

## Recommended benchmark: `H-06-zero-downtime-order-fallout`

This should be the first "compound incident" benchmark in the repo.

### Scenario

An order service is in the middle of a zero-downtime rollout:

- `shipping_address` is being replaced by `delivery_address`
- a new `PATCH /api/orders/{id}` endpoint was added
- `GET /api/orders/{id}/summary` is cached
- successful updates must record an outbox event for downstream consumers
- failed updates must still write an audit record for incident analysis

After deployment, support tickets report:

- some PATCH requests clear fields that clients did not send
- warehouse integration still reading the legacy column is failing
- order summaries sometimes stay stale after a successful update
- failed updates still appear in the outbound integration feed

The prompt should describe symptoms like the above, but must never mention:

- expand-contract
- tri-state PATCH
- self-invocation
- `REQUIRES_NEW`
- cache eviction annotations

### Skills stressed

- `SK-11` schema migration
- `SK-08` Jackson serialization / PATCH semantics
- `SK-09` transaction design
- `SK-03` proxy compatibility / cache behavior

Optional fifth cluster if more difficulty is needed:

- `SK-10` JPA mapping / N+1 on order lines

## Bug clusters

The benchmark should intentionally plant these clusters at once.

### Cluster A: rollout-safe schema migration

Seeded bugs:

- direct rename from `shipping_address` to `delivery_address`
- no backfill for existing rows
- no dual-write in service layer
- no index on the new column

Hidden verification:

- both columns still exist after migration
- seed data is backfilled
- new writes update both columns
- legacy reads against `shipping_address` still work

Why this matters:
solving only the SQL migration is insufficient; the write path must also be compatible.

### Cluster B: PATCH semantics under omitted vs null values

Seeded bugs:

- request DTO uses plain nullable fields
- mapper treats "missing" and "explicit null" the same

Hidden verification:

- omitted field preserves existing value
- explicit `null` clears the field
- explicit value overwrites the field

Why this matters:
models often know the generic idea, but miss it when it is buried in a larger service.

### Cluster C: transaction / outbox / audit boundaries

Seeded bugs:

- outbox row is written in the same path as the main mutation, but before commit safety is guaranteed
- rollback still leaves an outbound side effect
- audit logging participates in the main transaction and disappears on rollback

Hidden verification:

- failed update does not create an outbound event
- failed update still creates an audit row with failure status
- successful update creates exactly one outbox row

Why this matters:
naive fixes often preserve correctness for the happy path while still failing rollback behavior.

### Cluster D: cache correctness through proxies

Seeded bugs:

- summary cache uses self-invocation, so proxy advice is bypassed
- cache eviction happens in the wrong place relative to the transaction
- cacheable reads and update flow are split across helpers in a misleading way

Hidden verification:

- repeated read returns cached summary
- read after successful update returns fresh state
- failed update does not poison or incorrectly evict the cache

Why this matters:
this is where a local "annotation patch" often fails unless the agent understands call flow.

## Why this benchmark should land in the 40-50% zone

This benchmark is not hard because any single pattern is novel. It is hard because full success requires the agent to:

1. find all four problem areas in a medium codebase,
2. understand that they are connected by one rollout story,
3. fix them without hidden-test names telling it what to do,
4. get rollback and cache behavior right on the first attempt.

A model that recognizes only migration and PATCH should land around `6/12` checks.
A stronger model that also fixes transaction boundaries should land around `9/12`.
Only models that reason correctly about proxy/caching interactions should approach full score.

## Scoring model

Use 12 hidden checks, grouped as 4 clusters x 3 checks:

- A1-A3: migration compatibility
- B1-B3: PATCH semantics
- C1-C3: transaction/outbox/audit behavior
- D1-D3: cache/proxy behavior

Recommended reporting:

- `first_attempt_score`: official benchmark score
- `repair_score`: optional secondary metric with fix loops enabled
- `cluster_breakdown`: score by cluster, so skill work has directional signal

This gives better tuning signal than one aggregate percentage near 100.

## Repo implementation

Recommended benchmark layout:

```text
benchmarks/H-06-zero-downtime-order-fallout/
  meta.yaml
  step-1.md
  eval.yaml
  eval-hidden.sh
  hidden-tests/
    src/test/kotlin/...
    src/test/resources/...
  starter/
    src/main/kotlin/...
    src/test/kotlin/...   # smoke tests only
    src/main/resources/db/migration/...
```

### `meta.yaml`

Use benchmark-local loop control:

```yaml
id: H-06
name: H-06-zero-downtime-order-fallout
description: "Compound zero-downtime rollout failure across migration, PATCH semantics, transactions, and cache behavior"
difficulty: hard
steps: 1
skills_tested: [SK-11, SK-08, SK-09, SK-03]
format: starter-fix
max_fix_loops: 0
```

### `eval.yaml`

Prefer one behavioral hidden check entry point and only a few structural guardrails:

```yaml
gates:
  - name: Compilation
    command: ./gradlew compileKotlin
  - name: Visible tests
    command: ./gradlew test

automated_checks:
  step_1:
    - name: Hidden verification suite
      type: command
      command: bash "$BENCHMARK_DIR/eval-hidden.sh" "$SOLUTION_DIR"
      fail_message: Hidden verification failed

    - name: No data class entities
      type: grep_absent
      pattern: "data class Order\\b|data class OrderLine\\b"
      glob: "src/main/**/*.kt"

    - name: No double-bang operators
      type: grep_absent
      pattern: "!!"
      glob: "src/main/**/*.kt"
```

### `eval-hidden.sh`

The hidden evaluator should:

1. copy the solution to a temp directory,
2. overlay `hidden-tests/` into that temp directory,
3. run only the hidden verification tests,
4. exit non-zero on any failure.

That keeps the verification suite invisible to the agent while still using your existing Gradle-based workflow.

## Authoring guardrails

Do this:

- keep visible tests as smoke coverage only
- make hidden test names business-oriented, not mechanism-oriented
- include realistic logs, SQL, and support tickets in the prompt
- make every cluster independently scoreable
- reuse patterns from existing `H-*`, but compose them in one app

Do not do this:

- no `@Disabled` verification tests in the workspace
- no test names like `transactionShouldUseRequiresNew`
- no prompt text that names the design pattern to apply
- no scoring that depends mainly on `grep_present`
- no default repair loops on the official score

## Suggested build strategy

The fastest way to author this benchmark is not to invent new Kotlin/Spring tricks. Instead:

1. take the starter shape from `B-15`,
2. merge in bug patterns from `H-01`, `H-04`, and `H-05`,
3. move verification into hidden tests,
4. turn off repair loops,
5. measure cluster-by-cluster results before adding more difficulty.

That should get you to a benchmark that is still skill-relevant, but no longer trivially solved by pattern recall.
