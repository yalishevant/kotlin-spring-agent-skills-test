package com.example.inventory.repository

import com.example.inventory.domain.ProductVariant
import org.springframework.data.jpa.repository.JpaRepository

interface ProductVariantRepository : JpaRepository<ProductVariant, Long> {
    fun findBySku(sku: String): ProductVariant?
    fun findByProductId(productId: Long): List<ProductVariant>
}
