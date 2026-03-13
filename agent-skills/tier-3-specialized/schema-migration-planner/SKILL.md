---
name: schema-migration-planner
description: Plan safe database schema evolution and zero-downtime change rollout for Kotlin plus Spring systems using Flyway, Liquibase, or equivalent migration tooling. Use when changing tables, columns, constraints, indexes, or data shape in systems with live traffic, rolling deploys, large datasets, or backward-compatibility requirements between old and new application versions.
---

# Schema Migration Planner

Source mapping: Tier 3 specialized skill derived from `Kotlin_Spring_Developer_Pipeline.md` (`SK-11`).

## Mission

Change schema without breaking live traffic, deployment order, or rollback safety.
Treat schema evolution as a multi-step compatibility exercise, not a single DDL statement.

## Read First

- Current schema and the desired target shape.
- Current code path, target code path, and deployment model.
- Table sizes, traffic pattern, lock sensitivity, and replication or CDC constraints.
- The actual migration tool and dialect in use.
- Backfill capability, rollout windows, and rollback expectations.

## Planning Workflow

1. Identify whether the change is additive, destructive, semantic, or data-moving.
2. Determine whether old and new application versions must coexist.
3. Plan the rollout in phases:
   - expand
   - dual write or compatibility layer
   - backfill
   - switch reads
   - contract
4. Decide whether rollback is realistic or whether roll-forward is the safer operational model.
5. Define smoke checks, validation queries, and observability around the migration.

## Core Migration Rules

- Add before remove.
- Make old code tolerate new schema before making new code require it.
- Backfill separately from latency-sensitive request paths whenever possible.
- Use explicit indexes and constraints as part of the migration design, not as an afterthought.
- Keep migration scripts deterministic and rerunnable according to the tool's expectations.

## Advanced Migration Traps

- Adding a non-null column with a default may rewrite or lock a large table depending on database and version. Small DDL can still be operationally expensive.
- Unique constraints, index builds, and foreign-key validation can be more disruptive than column adds.
- Backfills can saturate replicas, queue workers, caches, and downstream CDC consumers even when primary write latency looks fine.
- Rolling deploys mean old and new code may both write for a while. Dual-write compatibility must be explicit.
- Renames are usually additive-copy-switch-drop plans in disguise. Treat them that way.
- Destructive clean-up should happen only after proving no old readers, writers, or reports still depend on the legacy shape.
- Trigger-based compatibility shims can help but add operational complexity and hidden coupling. Use them knowingly.
- Migration idempotency in tooling does not automatically mean business safety. Backfill scripts and data corrections need their own idempotency story.

## Data Evolution Nuances

- Type changes such as numeric widening, timezone reinterpretation, enum reshaping, or JSON-structure evolution can be more dangerous than simple adds and drops.
- `NOT NULL` and uniqueness enforcement often need a staged approach: detect violations, clean data, validate constraint, then enforce strictly.
- Backfill chunk size, ordering, pause or resume semantics, and checkpointing are part of the migration design for large tables.
- Dual-read or shadow-read phases may be safer than immediate read switching when data transformation logic is non-trivial.
- Online schema change tools or shadow-table strategies may be necessary when ordinary DDL locking is too expensive for the workload.
- Replication lag and CDC downstream consumers can become the real bottleneck during backfill even when primary database metrics look acceptable.

## Dialect And Tool Nuances

- Online index creation, concurrent index build, and lock behavior are vendor-specific. Plan by dialect, not by generic SQL intuition.
- Flyway and Liquibase have different strengths for rollback modeling, checksum handling, and branching workflows. Fit the plan to the tool already in use.
- Some schema tools treat checksum drift and edited history harshly. Never rewrite applied migrations casually.
- Partitioned tables, sharded systems, and CDC pipelines require migration plans that account for topology, not only DDL syntax.

## Column Rename — Expand-Contract Pattern

Column renames are the most common source of rollout failures. Never use `RENAME COLUMN` or `ALTER COLUMN ... RENAME TO` directly — this destroys the old column, breaking backward-compatible reads and native queries that reference it.

### Phase 1 — Expand
Add the new column alongside the old one. Copy existing data:
```sql
-- V2 migration: add new column, preserve old
ALTER TABLE orders ADD COLUMN delivery_address VARCHAR(255);
UPDATE orders SET delivery_address = shipping_address WHERE delivery_address IS NULL;
```

### Phase 2 — Dual-write
Update the JPA entity to map **both** columns. Every write must populate both so that old-version code reading the old column still sees correct data:
```kotlin
@Entity
@Table(name = "orders")
class Order(
    // New canonical column
    @Column(name = "delivery_address")
    var deliveryAddress: String,

    // Legacy column kept in sync for backward compatibility
    @Column(name = "shipping_address")
    var shippingAddress: String = deliveryAddress
) {
    @PrePersist @PreUpdate
    fun syncLegacyColumns() {
        shippingAddress = deliveryAddress
    }
}
```
Native queries referencing the old column name must keep working throughout this phase.

### Phase 3 — Contract (later release)
Drop the old column **only** after proving no readers, writers, reports, or warehouse queries still depend on it.

### Key mistakes to avoid
- Using H2 `MODE=PostgreSQL` with `ALTER COLUMN ... RENAME TO` — H2 may behave differently from real PostgreSQL.
- Forgetting to dual-write: new code updates only the new column, old code reads stale data from the old column.
- Dropping the old column in the same release as the rename.

## Expert Heuristics

- Prefer roll-forward designs over rollback fantasies when data shape already changed in production.
- If the table is large or business-critical, separate compatibility change, data movement, and cleanup into different releases.
- If a migration changes query shape, validate execution plans as part of the migration, not only schema correctness.
- If zero downtime matters, prove compatibility between adjacent deploy versions explicitly.
- Treat cleanup as a separate project step with an explicit proof threshold, not a footnote in the initial rollout plan.
- Validate constraints against real production-shaped data before assuming the schema is enforceable.
- If data correctness matters more than release speed, prefer longer coexistence windows over aggressive cleanup.
- Make the migration observable: counters for rows backfilled, lag, retries, validation failures, and cutover readiness should exist before the dangerous step begins.

## Output Contract

Return these sections:

- `Schema change type`: additive, destructive, semantic, or data-moving.
- `Phased migration plan`: the exact expand/contract sequence.
- `Compatibility story`: how old and new code coexist safely.
- `Operational risks`: locks, backfill load, replication, CDC, or indexing risk.
- `Verification`: SQL checks, smoke checks, and rollout checkpoints.
- `Cleanup phase`: what can be removed later and under what proof.

## Guardrails

- Do not recommend direct destructive DDL on live systems without a phased plan.
- Do not assume rollback is safe once data has been transformed.
- Do not couple request latency to large backfills unless no other option exists.
- Do not edit historical applied migrations casually.

## Quality Bar

A good run of this skill gives the team a deployable, compatibility-safe migration sequence with operational awareness.
A bad run writes correct SQL that is still dangerous to run on a live system.
