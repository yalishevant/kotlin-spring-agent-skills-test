package com.example.orderfallout.repo

import com.example.orderfallout.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun countByOrderId(orderId: Long): Long
}
