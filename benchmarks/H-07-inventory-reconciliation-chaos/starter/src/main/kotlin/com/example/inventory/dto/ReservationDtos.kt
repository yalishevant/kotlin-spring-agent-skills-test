package com.example.inventory.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class ReservationRequest(
    val variantId: Long,
    @NotBlank val orderId: String,
    @Positive val quantity: Int
)

data class ReservationResponse(
    val id: Long,
    val variantId: Long,
    val orderId: String,
    val quantity: Int,
    val status: String,
    val createdAt: LocalDateTime
)
