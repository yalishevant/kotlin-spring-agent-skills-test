package com.example.orderfallout.repo

import com.example.orderfallout.domain.OutboxEvent
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {
    fun countByAggregateIdAndEventType(aggregateId: Long, eventType: String): Long
}
