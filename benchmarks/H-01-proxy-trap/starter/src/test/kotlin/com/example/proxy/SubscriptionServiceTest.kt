package com.example.proxy

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

/**
 * Basic unit tests for SubscriptionService.
 * These tests use MockK and do NOT rely on Spring context, caching, or proxy behavior.
 * They should pass even with the proxy-related bugs present.
 */
class SubscriptionServiceTest {

    private lateinit var repository: SubscriptionRepository
    private lateinit var service: SubscriptionService

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        service = SubscriptionService(repository)
    }

    @Test
    fun `createSubscription should save and return entity`() {
        val subscription = Subscription(id = 1, userId = "user1", plan = "basic")
        every { repository.save(any()) } returns subscription

        val result = service.createSubscription("user1", "basic")

        assertNotNull(result)
        assertEquals("user1", result.userId)
        assertEquals("basic", result.plan)
        verify { repository.save(any()) }
    }

    @Test
    fun `cancelSubscription should set active to false`() {
        val subscription = Subscription(id = 1, userId = "user1", plan = "basic", active = true)
        every { repository.findById(1L) } returns Optional.of(subscription)
        every { repository.save(any()) } returns subscription

        service.cancelSubscription(1L)

        assertFalse(subscription.active)
        verify { repository.save(subscription) }
    }

    @Test
    fun `getActiveSubscription should return subscription from repository`() {
        val subscription = Subscription(id = 1, userId = "user1", plan = "pro")
        every { repository.findByUserIdAndActiveTrue("user1") } returns subscription

        val result = service.getActiveSubscription("user1")

        assertEquals("pro", result.plan)
        assertEquals("user1", result.userId)
    }

    @Test
    fun `upgradePlan should update plan field`() {
        val subscription = Subscription(id = 1, userId = "user1", plan = "basic")
        every { repository.findById(1L) } returns Optional.of(subscription)
        every { repository.save(any()) } returns subscription.copy(plan = "premium")

        val result = service.upgradePlan(1L, "premium")

        assertEquals("premium", result.plan)
    }

    @Test
    fun `getSubscriptionSummary should return summary`() {
        val subscription = Subscription(id = 1, userId = "user1", plan = "pro", active = true)
        every { repository.findByUserIdAndActiveTrue("user1") } returns subscription

        val summary = service.getSubscriptionSummary("user1")

        assertEquals(1L, summary.id)
        assertEquals("pro", summary.plan)
        assertEquals(true, summary.active)
    }
}
