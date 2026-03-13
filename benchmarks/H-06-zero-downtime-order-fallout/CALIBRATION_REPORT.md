# H-06 Calibration Report

**Benchmark:** `H-06-zero-downtime-order-fallout`
**Date:** 2026-03-13 (updated from 2026-03-12)
**Goal:** measure whether a compound Kotlin/Spring benchmark can differentiate models and demonstrate measurable skill impact.

---

## 1. What H-06 Tests

`H-06` is a compound "starter-fix" benchmark with **six** interacting fault clusters:

- **A. Zero-downtime migration** (3 checks): preserve both `shipping_address` and `delivery_address` during rollout
- **B. PATCH semantics** (3 checks): distinguish omitted fields from explicit `null`
- **C. Transaction boundaries** (3 checks): outbox event only on success, failure audit survives rollback
- **D. Cache/proxy behavior** (3 checks): summary reads cached, successful patches refresh cache
- **E. Status transitions** (3 checks): forward-only status progression enforced
- **F. Notifications** (2 checks): notifications fire only after successful commit

Scoring surface:

- `17` hidden behavioral checks
- `2` gates: `./gradlew compileKotlin`, `./gradlew test`
- total: **19 checks**
- baseline (unmodified starter): **8/19** — passes gates + B1, C3, D1, E1, E3, F1
- net-scoreable checks (fail on baseline): **11**

---

## 2. Evaluation Setup

Canonical runs use:

- hardened `run.sh` with compact skills delivery (`build_skills_context()`)
- isolated temp workspaces outside the repo tree
- masked benchmark-sensitive assets during the agent phase
- hidden verification via `command` checks and `eval-hidden.sh`
- `max_fix_loops: 0`
- `AGENT_TIMEOUT_SECONDS=900`
- `JAVA_HOME` forced to JDK 17 for eval scripts (Kotlin 2.0.21 daemon crashes on JDK 25)

---

## 3. Canonical Results (Phase 2 — 19 checks)

| Mode | Raw Score | Net Score | Elapsed | Status |
|------|:---------:|:---------:|:-------:|--------|
| `codex-skills` | **19/19** (100%) | 11/11 | 635s | completed |
| `claude+skills` (compact) | **19/19** (100%) | 11/11 | 542s | completed |
| `claude-skills` (run 1) | 15/19 (79%) | 7/11 | 581s | completed |
| `claude-skills` (run 2) | 14/19 (74%) | 6/11 | 788s | completed |

Result directories:

- `results/H-06-..._codex-skills_20260312T203654Z` — 19/19
- `results/H-06-..._claude+skills_20260313T004836Z` — 19/19
- `results/H-06-..._claude-skills_20260312T213456Z` — 15/19
- `results/H-06-..._claude-skills_20260313T002614Z` — 14/19

### Phase 1 Results (deprecated — 14 checks, 4 clusters A-D)

| Mode | Score | Notes |
|------|:-----:|-------|
| `codex+skills` | 14/14 (100%) | full pass |
| `codex-skills` | 13/14 (92.9%) | missed D3 |
| `claude-skills` | 10/14 (71.4%) | failed A3, B2, B3, D3 |
| `claude+skills` (file refs) | 5/14 (35.7%) | timed out, no workspace change |

---

## 4. Failure Profiles (Phase 2)

### `codex-skills` — 19/19

All checks pass. GPT-5.4 xhigh solves all six clusters without any skills injection.

### `claude+skills` (compact) — 19/19

All checks pass. The compact CLAUDE.md (~31KB) with extracted concrete patterns enables Claude to:
- Implement expand-contract migration with dual-write `@PrePersist`/`@PreUpdate`
- Build a custom Jackson deserializer tracking `presentFields: Set<String>` for tri-state PATCH
- Fix transaction propagation (OutboxService → `REQUIRED`, AuditService → `REQUIRES_NEW`)
- Replace self-invocation with `@CachePut`
- Add forward-only status transition validation
- Switch `@EventListener` to `@TransactionalEventListener(AFTER_COMMIT)`

Reproducible across 2 independent runs (T235513Z, T004836Z).

### `claude-skills` — 15/19 and 14/19

Consistent failures across both runs:

| Check | Run 1 (T213456Z) | Run 2 (T002614Z) |
|-------|:-----------------:|:-----------------:|
| A1 both columns exist | FAIL | FAIL |
| A3 dual-write | FAIL | FAIL |
| B2 explicit null clears notes | FAIL | FAIL |
| B3 explicit null clears ref | FAIL | FAIL |
| D3 cache entry updated | PASS | FAIL |

**Root cause analysis:**

- **A1/A3:** Without the expand-contract pattern, Claude uses `ALTER TABLE ... RENAME COLUMN` or drops the old column entirely. The hidden tests verify that BOTH columns exist and are synchronized.
- **B2/B3:** Without the tri-state pattern, Claude uses `?:` (Elvis operator) which treats omitted and explicit null identically. The hidden tests send `{"notes": null}` and verify the value is cleared.
- **D3:** Flaky — depends on exact cache eviction vs update strategy. Passed in run 1, failed in run 2.

---

## 5. Skills Delivery Optimization

Three strategies were tested for injecting skills into Claude's CLAUDE.md:

### Strategy 1: File References Only (~3KB)
CLAUDE.md lists skill names with file paths. Agent expected to read `.benchmark-skills/*/SKILL.md`.

**Result:** Agent never reads the files. Score = same as `claude-skills` (14-15/19).

### Strategy 2: Full Inline (~152KB, ~38K tokens)
All 25 SKILL.md files concatenated into CLAUDE.md.

**Result:** Agent hangs analyzing context. Times out with no workspace changes.

### Strategy 3: Compact Inline (~31KB, ~8K tokens) — Winner
CLAUDE.md contains:
1. One-line skill index (25 lines)
2. Extracted concrete patterns via awk (code examples for expand-contract, tri-state PATCH, etc.)
3. Categorized rule bullets (~30 rules)

**Result:** 19/19 on 2/2 runs. Agent correctly applies all patterns on first attempt.

### Why This Works

The compact strategy delivers **executable patterns** — code examples the agent can directly adapt. The model doesn't need to understand the full specification; it needs to see the solution shape. The awk extraction rule:

```
/^## .*[Pp]attern|^## .*[Cc]oncrete|^## .*[Ee]xpand.[Cc]ontract|^## .*[Tt]ri.[Ss]tate/
```

captures exactly the sections that contain working code examples for the two pattern families that Claude lacks from training data.

---

## 6. What H-06 Proves

1. **Skills have measurable, reproducible impact** on Claude: +4-5 checks (74-79% → 100%) on a first-attempt compound benchmark
2. **Delivery strategy matters more than content volume**: compact inline >> full inline >> file references
3. **The skill benefit targets specific knowledge gaps**: expand-contract migration and tri-state PATCH semantics
4. **Codex saturates without skills**: GPT-5.4 xhigh reaches 19/19 with no external patterns

---

## 7. What H-06 Does Not Prove

- Skills impact on tasks beyond H-06 (generalization needs testing on B-13, H-05, etc.)
- Skills benefit for Codex (codex-skills already saturates)
- Skills effectiveness on truly novel patterns not in any training data
- Whether the compact strategy generalizes to other skill areas beyond migration and PATCH

---

## 8. Recommended Next Steps

1. **Test generalization**: Run `claude+skills` on B-13 (schema migration) and H-05 (migration gauntlet) to verify expand-contract pattern transfers
2. **Test full B-series with skills**: Verify no regression from compact CLAUDE.md
3. **Create H-07+**: Benchmarks requiring patterns not in training data — custom APIs, adversarial Spring configs
4. **Codex+skills on hardened H-06**: Verify codex+skills also hits 19/19 (expected)
5. **Optimize compact strategy further**: Test which specific pattern sections are necessary vs redundant
