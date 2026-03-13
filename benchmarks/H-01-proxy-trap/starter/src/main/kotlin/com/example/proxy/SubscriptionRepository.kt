package com.example.proxy

import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findByUserIdAndActiveTrue(userId: String): Subscription?
}
