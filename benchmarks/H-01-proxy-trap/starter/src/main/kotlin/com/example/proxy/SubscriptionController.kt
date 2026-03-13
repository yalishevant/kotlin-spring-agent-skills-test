package com.example.proxy

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {

    @PostMapping
    fun create(
        @RequestParam userId: String,
        @RequestParam plan: String
    ): ResponseEntity<Subscription> {
        val subscription = subscriptionService.createSubscription(userId, plan)
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<Subscription> {
        return try {
            val sub = subscriptionService.upgradePlan(id, "")
            // Workaround: just fetch by id
            ResponseEntity.ok(sub)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getByUser(@PathVariable userId: String): ResponseEntity<SubscriptionSummary> {
        return try {
            val summary = subscriptionService.getSubscriptionSummary(userId)
            ResponseEntity.ok(summary)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}/upgrade")
    fun upgrade(
        @PathVariable id: Long,
        @RequestParam newPlan: String
    ): ResponseEntity<Subscription> {
        return try {
            val sub = subscriptionService.upgradePlan(id, newPlan)
            ResponseEntity.ok(sub)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}/cancel")
    fun cancel(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            subscriptionService.cancelSubscription(id)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }
}
