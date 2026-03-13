package com.example.inventory.repository

import com.example.inventory.domain.StockLevel
import org.springframework.data.jpa.repository.JpaRepository

interface StockLevelRepository : JpaRepository<StockLevel, Long> {
    fun findByVariantId(variantId: Long): StockLevel?
    fun findByVariantIdIn(variantIds: List<Long>): List<StockLevel>
}
