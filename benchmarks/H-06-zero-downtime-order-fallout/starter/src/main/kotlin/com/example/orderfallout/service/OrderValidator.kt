package com.example.orderfallout.service

import com.example.orderfallout.domain.Order
import org.springframework.stereotype.Component

@Component
class OrderValidator {

    fun validatePatchedOrder(order: Order) {
        require(order.customerName.isNotBlank()) { "customerName must not be blank" }
        require(order.deliveryAddress.isNotBlank()) { "deliveryAddress must not be blank" }
        require(order.lines.isNotEmpty()) { "order must contain at least one line" }
    }
}
