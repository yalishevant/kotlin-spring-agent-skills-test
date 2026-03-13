package com.example.proxy

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubscriptionService(
    private val repository: SubscriptionRepository
) {

    /** Returns a summary of the user's active subscription. */
    fun getSubscriptionSummary(userId: String): SubscriptionSummary {
        val sub = getActiveSubscription(userId)
        return SubscriptionSummary(sub.id, sub.plan, sub.active)
    }

    @Cacheable("subscriptions", key = "#userId")
    fun getActiveSubscription(userId: String): Subscription {
        return repository.findByUserIdAndActiveTrue(userId)
            ?: throw NoSuchElementException("No active subscription for $userId")
    }

    @CacheEvict("subscriptions", key = "#userId")
    fun evictSubscriptionCache(userId: String) {
        // evicts cache entry for the given userId
    }

    /** Batch upgrade: calls upgradeWithRollback for each id. */
    fun batchUpgrade(ids: List<Long>, newPlan: String) {
        for (id in ids) {
            upgradeWithRollback(id, newPlan)
        }
    }

    /**
     * Upgrades the plan inside a transaction. If the new plan is "INVALID",
     * it saves first then throws — the @Transactional should roll back the save.
     */
    @Transactional
    fun upgradeWithRollback(id: Long, newPlan: String): Subscription {
        val sub = repository.findById(id).orElseThrow {
            NoSuchElementException("Subscription not found: $id")
        }
        sub.plan = newPlan
        val saved = repository.save(sub)
        repository.flush()
        if (newPlan == "INVALID") {
            throw IllegalStateException("Plan validation failed after save — should trigger rollback")
        }
        return saved
    }

    @Transactional
    fun upgradePlan(id: Long, newPlan: String): Subscription {
        val sub = repository.findById(id).orElseThrow {
            NoSuchElementException("Subscription not found: $id")
        }
        sub.plan = newPlan
        return repository.save(sub)
    }

    @Transactional
    fun createSubscription(userId: String, plan: String): Subscription {
        return repository.save(Subscription(userId = userId, plan = plan))
    }

    fun cancelSubscription(id: Long) {
        val sub = repository.findById(id).orElseThrow {
            NoSuchElementException("Subscription not found: $id")
        }
        sub.active = false
        repository.save(sub)
    }

    @Async
    fun sendNotificationAsync(userId: String, message: String) {
        Thread.sleep(200)
    }

    /** Upgrades plan and sends async notification. */
    fun upgradeWithNotification(id: Long, newPlan: String) {
        val sub = upgradePlan(id, newPlan)
        sendNotificationAsync(sub.userId, "Plan upgraded to $newPlan")
    }
}
