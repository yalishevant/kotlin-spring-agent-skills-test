package com.example.orderfallout.service

import com.example.orderfallout.domain.OrderStatus
import org.springframework.stereotype.Component

@Component
class StatusTransitionValidator {

    fun validate(currentStatus: OrderStatus, newStatus: OrderStatus) {
        OrderStatus.valueOf(newStatus.name)
    }
}
