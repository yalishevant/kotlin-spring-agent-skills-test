package com.example.orderfallout.api

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateOrderRequest(
    @field:NotBlank
    val customerName: String,

    @field:NotBlank
    val deliveryAddress: String,

    val customerReference: String? = null,
    val notes: String? = null,

    @field:Size(min = 1)
    val lines: List<@Valid OrderLineRequest> = emptyList()
)

data class OrderLineRequest(
    @field:NotBlank
    val sku: String,

    @field:Positive
    val quantity: Int
)

data class OrderPatchRequest(
    val customerName: String? = null,
    val deliveryAddress: String? = null,
    val customerReference: String? = null,
    val notes: String? = null,
    val status: String? = null
)

data class OrderResponse(
    val id: Long,
    val customerName: String,
    val status: String,
    val deliveryAddress: String,
    val customerReference: String?,
    val notes: String?,
    val lines: List<OrderLineResponse>
)

data class OrderLineResponse(
    val sku: String,
    val quantity: Int
)

data class OrderSummaryResponse(
    val orderId: Long,
    val customerName: String,
    val status: String,
    val deliveryAddress: String,
    val customerReference: String?,
    val notes: String?,
    val lineCount: Int
)
