# AGENTS.md

## Project Overview

This repository contains **25 AI skills** for Kotlin + Spring Framework development, plus a benchmark suite with **15 standard tasks** and **6 hard/compound calibration benchmarks**.

**Repository:** [github.com/Kotlin/kotlin-backend-agent-skills](https://github.com/Kotlin/kotlin-backend-agent-skills)

## Repository Structure

```
agent-skills/
  tier-1-critical/          # 10 skills — build these first (daily use, high impact)
  tier-2-high-value/        # 10 skills — extended coverage
  tier-3-specialized/       # 5 skills  — rare but important tasks
benchmarks/
  B-01-order-crud/          # 15 standard benchmark directories
  ...
  B-15-production-incident/
  H-01-proxy-trap/          # hard / compound calibration benchmarks
  ...
  H-06-zero-downtime-order-fallout/
  run.sh                    # Benchmark runner
  eval-check.sh             # Automated evaluation
  compare.sh                # Cross-mode comparison
  llm-judge-prompt.md       # LLM-as-Judge scoring template
BENCHMARK_REPORT.md         # Evaluation report + H-06 calibration summary
Kotlin_Spring_Developer_Pipeline.md  # Full specification document
```

## Skills

Each skill is in `agent-skills/tier-N-*/skill-name/` with:
- `SKILL.md` — detailed specification (mission, diagnostic rules, guardrails)
- `agents/openai.yaml` — OpenAI Codex interface config

To use skills with Codex, reference the `agents/openai.yaml` files in each skill directory.

### Tier 1 — Critical (SK-01 through SK-10, SK-21)
SK-15 Stacktrace Triage, SK-02 DI Reasoning, SK-14 Test Builder, SK-03 Proxy Compatibility, SK-06 API Builder, SK-09 Transaction Designer, SK-10 JPA Mapper, SK-01 Context Ingestion, SK-04 Gradle Doctor, SK-21 Code Review

### Tier 2 — High Value
SK-08 Jackson Serialization, SK-07 Error/Validation, SK-16 Config/Profiles, SK-20 Kotlin Refactorer, SK-18 Performance, SK-12 Resilience, SK-13 Security, SK-05 Dependency Resolver, SK-24 Incident Responder, SK-17 Observability

### Tier 3 — Specialized
SK-19 Java→Kotlin Migration, SK-25 Domain/API Design, SK-22 Upgrade Navigator, SK-23 CI/CD, SK-11 Schema Migration

## Benchmarks

15 self-contained tasks (B-01 through B-15). Each has multi-step prompts and automated eval checks.

**Run a benchmark:**
```bash
cd benchmarks
chmod +x run.sh eval-check.sh compare.sh
./run.sh B-01 codex+skills      # with skills
./run.sh B-01 codex-skills      # without skills
./run.sh all codex+skills       # run all
```

**Modes:** `claude+skills`, `claude-skills`, `codex+skills`, `codex-skills`

## Key Kotlin + Spring Rules (from skills)

When working on Kotlin + Spring code, always:

1. **JPA entities must NOT be `data class`** — use regular class with manual equals/hashCode
2. **Use `@field:` targets** for validation annotations on Kotlin data class DTOs
3. **Include `kotlin("plugin.spring")` and `kotlin("plugin.jpa")`** in Gradle
4. **Watch for self-invocation** — `this.method()` bypasses `@Transactional`/`@Cacheable` proxies
5. **Use MockK, not Mockito** for Kotlin tests
6. **Register `jackson-module-kotlin`** for Jackson ObjectMapper
7. **Never use `!!`** — prefer safe calls, `requireNotNull`, or sealed class results

## Conventions

- All specification documents are in **Russian** (source files `Skills_for_kotlin_AI*.md`)
- All skills, benchmarks, and tooling are in **English**
- Filenames with spaces need quoting in shell
