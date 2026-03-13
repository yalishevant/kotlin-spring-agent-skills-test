package com.example.inventory.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class StockAdjustmentRequest(
    @NotBlank val sku: String,
    @Min(1) val quantity: Int,
    @NotBlank val reason: String,
    val type: AdjustmentType = AdjustmentType.ADJUSTMENT
)

enum class AdjustmentType {
    ADJUSTMENT, WRITE_OFF
}

data class StockAdjustmentResponse(
    val id: Long,
    val variantId: Long,
    val quantityChange: Int,
    val reason: String?,
    val adjustedBy: String?,
    val createdAt: LocalDateTime
)

data class WriteOffRequest(
    val variantId: Long,
    @Min(1) val quantity: Int,
    @NotBlank val reason: String
)
