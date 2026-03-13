package com.example.orderfallout.service

import com.example.orderfallout.api.OrderPatchRequest
import com.example.orderfallout.domain.Order
import com.example.orderfallout.domain.OrderStatus
import org.springframework.stereotype.Component

@Component
class OrderPatchApplier(
    private val statusTransitionValidator: StatusTransitionValidator
) {

    fun apply(order: Order, request: OrderPatchRequest) {
        order.customerName = request.customerName ?: order.customerName
        order.deliveryAddress = request.deliveryAddress ?: order.deliveryAddress
        order.customerReference = request.customerReference ?: order.customerReference
        order.notes = request.notes ?: order.notes

        if (request.status != null) {
            val newStatus = OrderStatus.valueOf(request.status.uppercase())
            statusTransitionValidator.validate(order.status, newStatus)
            order.status = newStatus
        }
    }
}
