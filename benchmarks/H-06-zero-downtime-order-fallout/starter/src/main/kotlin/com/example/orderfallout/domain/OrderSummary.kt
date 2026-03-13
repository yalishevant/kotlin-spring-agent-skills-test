package com.example.orderfallout.domain

data class OrderSummary(
    val orderId: Long,
    val customerName: String,
    val status: OrderStatus,
    val deliveryAddress: String,
    val customerReference: String?,
    val notes: String?,
    val lineCount: Int
)
