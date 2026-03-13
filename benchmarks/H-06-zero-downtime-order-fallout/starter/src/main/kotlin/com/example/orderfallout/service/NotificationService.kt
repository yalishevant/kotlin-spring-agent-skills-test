package com.example.orderfallout.service

import com.example.orderfallout.domain.Notification
import com.example.orderfallout.domain.OrderPatchedEvent
import com.example.orderfallout.repo.NotificationRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
open class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun onOrderPatched(event: OrderPatchedEvent) {
        notificationRepository.save(
            Notification(orderId = event.orderId, eventType = "ORDER_PATCHED")
        )
    }
}
