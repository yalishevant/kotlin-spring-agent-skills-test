package com.example.proxy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.annotation.DirtiesContext

/**
 * Verification tests that exercise expected runtime behavior.
 * Currently @Disabled because they fail. Enable them and fix the production code.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SubscriptionVerificationTest {

    @Autowired
    private lateinit var subscriptionService: SubscriptionService

    @Autowired(required = false)
    private var cacheManager: CacheManager? = null

    @Autowired
    private lateinit var repository: SubscriptionRepository

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Disabled("Known issue — cache not returning same instance")
    @Test
    fun cacheShouldReturnSameInstanceOnSecondCall() {
        subscriptionService.createSubscription("cacheUser", "pro")

        val first = subscriptionService.getActiveSubscription("cacheUser")
        val second = subscriptionService.getActiveSubscription("cacheUser")

        assertTrue(
            first === second,
            "Expected same instance from cache, but got different objects."
        )
    }

    @Disabled("Known issue — transaction not rolling back")
    @Test
    fun transactionShouldRollbackOnException() {
        val sub = subscriptionService.createSubscription("txUser", "basic")
        val originalPlan = sub.plan

        try {
            subscriptionService.batchUpgrade(listOf(sub.id), "INVALID")
        } catch (_: IllegalStateException) {
            // expected
        }

        val afterAttempt = repository.findById(sub.id).orElseThrow()
        assertEquals(
            originalPlan, afterAttempt.plan,
            "Plan should remain '$originalPlan' because the transaction should have rolled back. " +
                "Found '${afterAttempt.plan}'."
        )
    }

    @Disabled("Known issue — entity equality check failing")
    @Test
    fun entityEqualityShouldBeById() {
        val sub = subscriptionService.createSubscription("eqUser", "starter")

        val set = HashSet<Subscription>()
        set.add(sub)

        sub.plan = "premium"

        assertTrue(
            set.contains(sub),
            "Entity should be found in HashSet after mutating non-id field."
        )
    }

    @Disabled("Known issue — method not running asynchronously")
    @Test
    fun asyncMethodShouldRunInSeparateThread() {
        val sub = subscriptionService.createSubscription("asyncUser", "basic")

        val start = System.currentTimeMillis()
        subscriptionService.upgradeWithNotification(sub.id, "premium")
        val elapsed = System.currentTimeMillis() - start

        assertTrue(
            elapsed < 150,
            "upgradeWithNotification took ${elapsed}ms — expected < 150ms if async works."
        )
    }

    @Disabled("Known issue — cache eviction has no effect")
    @Test
    fun cacheEvictShouldWork() {
        subscriptionService.createSubscription("evictUser", "basic")

        val first = subscriptionService.getActiveSubscription("evictUser")

        assertTrue(
            first === subscriptionService.getActiveSubscription("evictUser"),
            "Second call should return cached instance (same reference)"
        )

        subscriptionService.evictSubscriptionCache("evictUser")

        val afterEvict = subscriptionService.getActiveSubscription("evictUser")
        assertNotEquals(
            System.identityHashCode(first),
            System.identityHashCode(afterEvict),
            "After cache eviction, should get a fresh instance from DB (different identity)."
        )
    }
}
