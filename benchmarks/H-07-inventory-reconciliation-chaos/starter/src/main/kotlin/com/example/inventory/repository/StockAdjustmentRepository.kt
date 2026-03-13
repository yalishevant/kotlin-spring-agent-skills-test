package com.example.inventory.repository

import com.example.inventory.domain.StockAdjustment
import org.springframework.data.jpa.repository.JpaRepository

interface StockAdjustmentRepository : JpaRepository<StockAdjustment, Long> {
    fun findByVariantId(variantId: Long): List<StockAdjustment>
}
