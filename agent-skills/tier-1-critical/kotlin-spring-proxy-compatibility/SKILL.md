---
name: kotlin-spring-proxy-compatibility
description: Diagnose and prevent Kotlin plus Spring proxy failures around `@Transactional`, `@Cacheable`, `@Async`, method security, retry, configuration proxies, and JPA entity requirements. Use when AOP annotations appear to do nothing, transactional or cache behavior is inconsistent, compiler plugins may be missing, self-invocation is suspected, or Kotlin final-by-default semantics may break Spring behavior.
---

# Kotlin Spring Proxy Compatibility

Source mapping: Tier 1 critical skill derived from `Kotlin_Spring_Developer_Pipeline.md` (`SK-03`).

## Mission

Explain exactly why Spring interception does or does not happen for Kotlin code, then propose the safest fix.
Focus on runtime behavior, not on code that merely looks annotated.

## Read These Inputs

- The class and method carrying `@Transactional`, `@Cacheable`, `@Async`, `@Retryable`, security annotations, or other proxy-triggering annotations.
- The call site. Determine whether the method is called through another bean or through `this`.
- Build files. Verify `kotlin("plugin.spring")`, `kotlin("plugin.jpa")`, or any custom `allOpen` and `noArg` configuration.
- Whether the bean is proxied through an interface or through CGLIB class proxies.
- For persistence problems, read the entity class shape and JPA plugin setup.

## Diagnose In This Order

1. Verify whether a proxy should exist at all.
2. Verify whether the call crosses the proxy boundary.
3. Verify whether the class or method is proxyable.
4. Verify whether the annotation is placed on the method Spring actually intercepts.
5. Verify whether build-time plugins opened the relevant classes.
6. Verify whether the bug is actually transactional semantics, not proxy creation.

## Core Checks

- Check for self-invocation. A method calling another annotated method in the same class bypasses the proxy.
- Check whether the target class or method is effectively final for the chosen proxy strategy.
- Check whether a JDK proxy is used. If so, interception works through interface methods, not arbitrary class methods.
- Check whether the annotated method is private or otherwise non-interceptable.
- Check whether `@Configuration` classes, `@Service` classes, and JPA entities rely on the correct Kotlin compiler plugins.
- Check whether the symptom comes from coroutines or async boundaries rather than proxy absence.

## Preferred Fixes

Prefer fixes in this order:

1. Enable the correct Kotlin compiler plugin in Gradle.
2. Move the intercepted method behind a real Spring bean boundary.
3. Adjust interface or proxy strategy only if the current strategy is incompatible.
4. Refactor responsibilities so that external callers cross the proxy naturally.
5. Only as a last resort, inject the proxied bean into itself or use `AopContext`, and explain why this is a compromise.

## Advanced Interception Traps

- `@PostConstruct`, constructors, and init blocks run on the target object before ordinary proxy interception can help. Advice that depends on transactions, caching, security, or retries will not rescue initialization logic.
- `@Async` changes threads. Transaction, security, MDC, and request context do not automatically flow the same way they do in synchronous code.
- `@Retryable`, `@Transactional`, `@Cacheable`, and method security can stack. Advisor ordering changes behavior, especially when retries and transactions combine.
- `@TransactionalEventListener` depends on transaction phase. If no transaction exists, the listener may never fire or may fire immediately depending on fallback behavior.
- `@Configuration(proxyBeanMethods = false)` disables configuration-class method interception. Treat this as a performance and semantics choice, not a cosmetic flag.
- Interface default methods, non-public methods, and methods reached only from internal helper paths may compile cleanly while still missing interception at runtime.
- In coroutine and reactive flows, transaction and security propagation depend on the underlying stack and context propagation model. A proxy can exist while context still does not flow the way the author expects.

## Expert Heuristics

- If the symptom is "annotation present but behavior absent," first ask whether the relevant call crossed a bean boundary. This catches more real bugs than checking `open` alone.
- If the team wants a local helper method to stay private, do not force proxy semantics onto it. Move the transactional or cache boundary outward instead.
- For caching and security, verify key computation and authorization point as separate concerns from proxy presence.
- For JPA entities and configuration classes, distinguish opening for framework mechanics from opening for general extensibility. The former is usually acceptable; the latter may not be.

## Kotlin-Specific Rules

- Prefer compiler plugins over manually marking entire class hierarchies as `open`.
- Do not generate JPA entities as `data class`.
- Do not assume annotations on private methods will work because they compile.
- Explain the difference between Kotlin source semantics and Spring runtime semantics.
- If migrating Java to Kotlin, re-check every proxy-reliant class after translation.

## Output Contract

Return these sections:

- `Broken mechanism`: what Spring feature is expected to intercept the call.
- `Why it failed`: one concrete runtime reason with code evidence.
- `Minimal fix`: the smallest change that restores interception.
- `Safer design option`: only if the current structure invites future proxy bugs.
- `Verification`: how to prove the annotation now takes effect.

## Concrete Pattern — Self-Invocation Fix

### The Problem
```kotlin
@Service
class OrderSummaryService(private val orderRepository: OrderRepository) {

    @Cacheable("summaries")
    fun getSummary(orderId: Long): OrderSummary { /* ... */ }

    @CacheEvict("summaries", key = "#orderId")
    fun evictSummary(orderId: Long) { /* no-op, just evicts */ }

    fun refreshSummary(orderId: Long): OrderSummary {
        evictSummary(orderId)    // BUG: calls this.evictSummary() — bypasses proxy
        return getSummary(orderId) // BUG: calls this.getSummary() — bypasses proxy
    }
}
```
Spring AOP intercepts calls through the proxy. `this.method()` goes directly to the target object, skipping `@Cacheable`/`@CacheEvict`/`@Transactional`.

### Fix — Option A: Use @CachePut
```kotlin
@CachePut("summaries", key = "#orderId")
fun refreshSummary(orderId: Long): OrderSummary {
    return orderRepository.buildSummary(orderId) // directly compute and update cache
}
```

### Fix — Option B: Inject self
```kotlin
@Service
class OrderSummaryService(
    private val orderRepository: OrderRepository
) {
    @Lazy @Autowired
    private lateinit var self: OrderSummaryService

    fun refreshSummary(orderId: Long): OrderSummary {
        self.evictSummary(orderId)    // calls through proxy
        return self.getSummary(orderId) // calls through proxy
    }
}
```

### Fix — Option C: Extract to separate bean
```kotlin
@Service
class SummaryCacheManager(private val summaryService: OrderSummaryService) {
    fun refreshSummary(orderId: Long): OrderSummary {
        summaryService.evictSummary(orderId)  // crosses bean boundary = proxy works
        return summaryService.getSummary(orderId)
    }
}
```

## Concrete Pattern — Missing @Enable Annotations for AOP Features

### The Problem
```kotlin
@Service
class ProductService(private val productRepository: ProductRepository) {
    @Cacheable("products")
    fun getProduct(id: Long): Product = productRepository.findById(id).orElseThrow()

    @Async
    fun reindexAsync(id: Long): CompletableFuture<Void> { /* ... */ }
}
// BUG: @Cacheable does nothing — no @EnableCaching on any @Configuration class
// BUG: @Async does nothing — no @EnableAsync on any @Configuration class
```
Spring AOP annotations like `@Cacheable`, `@Async`, and `@EnableScheduling` require their corresponding `@Enable*` annotation on a `@Configuration` class. Without it, the annotations compile and are present at runtime, but **Spring never creates the AOP proxy infrastructure** — they silently do nothing.

### The Fix
```kotlin
@Configuration
@EnableCaching       // Required for @Cacheable/@CacheEvict/@CachePut to work
@EnableAsync         // Required for @Async to work
@EnableRetry         // Required for @Retryable to work (from spring-retry)
class AppConfig
```

### Enable annotation checklist
| Annotation Used | Required Enable | Missing Symptom |
|---|---|---|
| `@Cacheable`, `@CacheEvict`, `@CachePut` | `@EnableCaching` | Cache annotations silently ignored |
| `@Async` | `@EnableAsync` | Method runs synchronously in caller thread |
| `@Retryable` | `@EnableRetry` | No retry on failure |
| `@Scheduled` | `@EnableScheduling` | Scheduled method never fires |
| `@PreAuthorize`, `@Secured` | `@EnableMethodSecurity` | Authorization checks silently skipped |

### Common mistakes
- Adding `@Cacheable` without `@EnableCaching` — caching silently disabled
- Adding `@Async` without `@EnableAsync` — method runs synchronously
- Putting `@EnableCaching` on the main application class but not importing it in test slices
- Forgetting that `@EnableRetry` requires `spring-retry` dependency in addition to the annotation

## Guardrails

- Do not suggest making every class `open` manually without first checking compiler plugins.
- Do not confuse proxy absence with wrong propagation, wrong cache key, or other business logic issues.
- Do not recommend `data class` entities or blanket `allOpen` for unrelated code.
- Do not leave self-invocation unexplained. It is one of the most common hidden causes.

## Quality Bar

A good run of this skill makes the proxy model explicit and the fix easy to verify.
A bad run tells the user to add annotations or `open` keywords without proving why interception failed.
