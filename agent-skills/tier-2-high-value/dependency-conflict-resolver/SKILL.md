---
name: dependency-conflict-resolver
description: Diagnose and resolve Gradle and Spring classpath conflicts, version drift, and binary incompatibilities in Kotlin applications. Use when `NoSuchMethodError`, `ClassNotFoundException`, linkage errors, duplicate logging bindings, Jackson or Hibernate mismatches, or BOM-versus-explicit-version conflicts appear, and the fix must respect the repository's real version authorities.
---

# Dependency Conflict Resolver

Source mapping: Tier 2 high-value skill derived from `Kotlin_Spring_Developer_Pipeline.md` (`SK-05`).

## Mission

Find which artifact version actually wins, why it wins, and what the narrowest safe correction is.
Treat dependency conflict resolution as a version-authority problem, not a guessing contest.

## Read First

- `./gradlew dependencies` or module-scoped dependencies output.
- `./gradlew dependencyInsight --dependency <artifact>` for the failing library family.
- Root and module build files, version catalogs, imported BOMs, and plugin versions.
- The exact runtime or compile-time error:
  - `NoSuchMethodError`
  - `ClassNotFoundException`
  - `NoClassDefFoundError`
  - `AbstractMethodError`
  - duplicate binding warnings

## Diagnose In This Order

1. Identify the failing class or method owner.
2. Identify which artifact provides that class.
3. Identify all candidate versions on the graph.
4. Identify the winning version and why it won:
   - direct dependency
   - BOM or platform
   - version catalog alias
   - transitive dependency
   - conflict resolution rule
5. Identify the true version authority that should own the family.

## Advanced Classpath Traps

- A compile-time green build and a runtime linkage error usually means binary incompatibility between resolved artifacts, not a missing import.
- Jackson, Netty, SLF4J, Logback, Kotlin stdlib, and Hibernate families should usually be version-aligned as families, not patched one artifact at a time.
- Plugin classpath and application classpath are different worlds. A plugin upgrade may not fix a runtime library conflict and vice versa.
- Test runtime can differ from main runtime. A conflict that appears only in tests may come from test fixtures, mock libraries, or test containers support.
- `enforcedPlatform`, `strictly`, and exclusions can fix a conflict or silently lock the graph into brittle behavior. Use them deliberately.
- Multiple logging bindings often create confusing startup behavior long before they fail hard.
- Shading or relocation is rarely the first answer in an ordinary Spring Boot app. Reach for it only when classpath isolation is truly required.

## Gradle Resolution Nuances

- Variant-aware resolution can choose artifacts by attributes such as usage, JVM version, and capabilities. A "present" dependency may still resolve to the wrong variant.
- BOM import order and multiple platforms can subtly change which family wins even when the declared versions look reasonable.
- `api` versus `implementation` leakage in shared modules can make conflicts appear only in downstream consumers.
- Duplicate classes from repackaged or relocated jars may not surface until runtime classloading or auto-configuration scans.
- Annotation processors, KSP, and plugin classpaths may need separate alignment from the main application runtime.

## Expert Heuristics

- If the symptom is runtime-only, inspect the packaged artifact or runtime classpath, not only IDE dependency trees.
- If a library family is managed by Spring Boot, prefer returning ownership to the BOM over adding more local pins.
- If the same conflict recurs across modules, solve it at the platform or convention level instead of patching one module at a time.
- If a dependency looks unused, verify reflective, auto-configured, or service-loader usage before removing it.

## Preferred Resolution Strategy

- Prefer removing an unnecessary explicit version when the BOM already manages a compatible one.
- Prefer aligning the entire affected dependency family.
- Prefer a direct, documented version override over hidden exclusions if an override is truly needed.
- Use exclusions only when a dependency is genuinely wrong for the module, not merely inconvenient.
- Explain why the chosen layer should own the version so the conflict does not return on the next upgrade.

## Concrete Pattern — Jackson Version Mismatch with Spring Boot BOM

### The Problem
```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")  // pulls Jackson 2.17.x via BOM
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")  // BUG: explicit old version
}
```

At runtime:
```
java.lang.NoSuchMethodError:
  'com.fasterxml.jackson.core.JsonGenerator
   com.fasterxml.jackson.core.JsonGenerator.writeStartObject(Object, int)'
```

The Spring Boot BOM manages Jackson `core`, `databind`, and `annotations` at 2.17.x, but the explicit `jackson-module-kotlin:2.15.3` pulls its own transitive `jackson-core:2.15.3`. Gradle picks the highest version (2.17.x) for core but the module was compiled against 2.15.x — binary mismatch.

### Diagnosis Commands
```bash
# Find all Jackson artifacts and their resolved versions
./gradlew dependencies --configuration runtimeClasspath | grep jackson

# Deep insight into a specific artifact
./gradlew dependencyInsight --dependency jackson-module-kotlin --configuration runtimeClasspath
```

### The Fix — Let the BOM Manage Versions
```kotlin
// build.gradle.kts — CORRECT
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")  // no version! BOM controls it
}
```

The Spring Boot BOM already manages `jackson-module-kotlin` at a compatible version. Removing the explicit version lets the BOM align the entire Jackson family.

### Common mistakes
- Pinning one artifact in a managed family — creates version skew with siblings
- Using `force` to override BOM versions — breaks the entire managed family alignment
- Adding exclusions to suppress the "wrong" version — hides the real conflict, breaks at runtime
- Not checking `runtimeClasspath` specifically — `compileClasspath` may resolve differently

## Output Contract

Return these sections:

- `Conflict summary`: what is colliding and where the symptom appears.
- `Winning path`: how the resolved artifact version is selected.
- `Version authority`: which file or platform should own the fix.
- `Minimal fix`: the smallest safe dependency or platform change.
- `Verification`: compile-time and runtime checks that confirm the graph is healthy.

## Guardrails

- Do not invent version numbers without tracing the existing compatibility model.
- Do not spray exclusions until the graph goes green.
- Do not use `force` as the default answer.
- Do not fix only the visibly failing artifact when the whole library family is misaligned.

## Quality Bar

A good run of this skill explains the conflict path and resolves it at the right authority layer.
A bad run produces a one-off version pin that compiles today and recreates the same conflict in the next branch.
