package com.example.payment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

// BUG: @NotBlank and @Positive target constructor params, not fields.
// Bean Validation silently skips them without @field: prefix.
data class CreatePaymentRequest(
    @NotBlank val customerName: String,
    @Positive val amount: BigDecimal,
    @NotBlank val idempotencyKey: String
)

data class PaymentResponse(
    val id: Long,
    val customerName: String,
    val amount: BigDecimal,
    val idempotencyKey: String,
    val status: String,
    val gatewayTransactionId: String?
)
