package com.example.inventory.service

import com.example.inventory.dto.ProductResponse
import com.example.inventory.dto.StockSummary
import com.example.inventory.dto.VariantResponse
import com.example.inventory.exception.NotFoundException
import com.example.inventory.repository.ProductRepository
import com.example.inventory.repository.StockLevelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class InventoryService(
    private val productRepository: ProductRepository,
    private val stockLevelRepository: StockLevelRepository
) {

    fun listProducts(): List<ProductResponse> {
        return productRepository.findAll().map { product ->
            ProductResponse(
                id = product.id,
                name = product.name,
                category = product.category,
                variantCount = product.variants.size,
                variants = product.variants.map { variant ->
                    VariantResponse(
                        id = variant.id,
                        sku = variant.sku,
                        color = variant.color,
                        size = variant.size,
                        price = variant.price
                    )
                }
            )
        }
    }

    fun getProduct(productId: Long): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { NotFoundException("Product not found: $productId") }
        return ProductResponse(
            id = product.id,
            name = product.name,
            category = product.category,
            variantCount = product.variants.size,
            variants = product.variants.map { variant ->
                VariantResponse(
                    id = variant.id,
                    sku = variant.sku,
                    color = variant.color,
                    size = variant.size,
                    price = variant.price
                )
            }
        )
    }

    fun getProductStockSummary(productId: Long): StockSummary {
        val product = productRepository.findById(productId)
            .orElseThrow { NotFoundException("Product not found: $productId") }
        var totalAvailable = 0
        var totalReserved = 0
        for (variant in product.variants) {
            val stock = stockLevelRepository.findByVariantId(variant.id)
            if (stock != null) {
                totalAvailable += stock.availableQuantity
                totalReserved += stock.reservedQuantity
            }
        }
        return StockSummary(productId, totalAvailable, totalReserved)
    }
}
