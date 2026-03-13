package com.example.inventory.dto

import java.math.BigDecimal

data class ProductResponse(
    val id: Long,
    val name: String,
    val category: String?,
    val variantCount: Int,
    val variants: List<VariantResponse>
)

data class VariantResponse(
    val id: Long,
    val sku: String,
    val color: String?,
    val size: String?,
    val price: BigDecimal
)

data class StockSummary(
    val productId: Long,
    val totalAvailable: Int,
    val totalReserved: Int
)
