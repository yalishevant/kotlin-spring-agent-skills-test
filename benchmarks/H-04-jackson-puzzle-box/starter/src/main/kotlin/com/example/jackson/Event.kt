package com.example.jackson

import java.time.Instant

sealed class Event {
    abstract val id: String
    abstract val timestamp: Instant
}

data class UserCreatedEvent(
    override val id: String,
    override val timestamp: Instant,
    val username: String,
    val email: String
) : Event()

data class OrderPlacedEvent(
    override val id: String,
    override val timestamp: Instant,
    val orderId: String,
    val amount: Double
) : Event()

data class SystemAlertEvent(
    override val id: String,
    override val timestamp: Instant,
    val severity: Severity,
    val message: String
) : Event()
