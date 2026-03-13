package com.example.orderfallout.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "outbox_events")
class OutboxEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String = "",

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: Long = 0,

    @Column(name = "event_type", nullable = false)
    val eventType: String = "",

    @Column(nullable = false, columnDefinition = "CLOB")
    val payload: String = "",

    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: LocalDateTime? = null
)
