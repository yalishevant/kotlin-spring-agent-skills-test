package com.example.orderfallout.service

import com.example.orderfallout.domain.Order
import com.example.orderfallout.domain.OutboxEvent
import com.example.orderfallout.repo.OutboxEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
open class OutboxService(
    private val outboxEventRepository: OutboxEventRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun publishOrderPatched(order: Order) {
        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "Order",
                aggregateId = order.id,
                eventType = "ORDER_PATCHED",
                payload = """{"orderId":${order.id},"customerName":"${order.customerName}","deliveryAddress":"${order.deliveryAddress}"}"""
            )
        )
    }
}
