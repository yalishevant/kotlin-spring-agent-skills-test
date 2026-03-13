package com.example.orderfallout.api

import com.example.orderfallout.domain.Order
import com.example.orderfallout.domain.OrderSummary

fun Order.toResponse(): OrderResponse {
    return OrderResponse(
        id = id,
        customerName = customerName,
        status = status.name,
        deliveryAddress = deliveryAddress,
        customerReference = customerReference,
        notes = notes,
        lines = lines.map { line ->
            OrderLineResponse(
                sku = line.sku,
                quantity = line.quantity
            )
        }
    )
}

fun OrderSummary.toResponse(): OrderSummaryResponse {
    return OrderSummaryResponse(
        orderId = orderId,
        customerName = customerName,
        status = status.name,
        deliveryAddress = deliveryAddress,
        customerReference = customerReference,
        notes = notes,
        lineCount = lineCount
    )
}
