package com.example.inventory.service

import com.example.inventory.domain.StockAdjustment
import com.example.inventory.dto.StockAdjustmentResponse
import com.example.inventory.exception.InsufficientStockException
import com.example.inventory.exception.NotFoundException
import com.example.inventory.repository.StockAdjustmentRepository
import com.example.inventory.repository.StockLevelRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StockAdjustmentService(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val stockLevelRepository: StockLevelRepository
) {

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun writeOff(variantId: Long, quantity: Int, reason: String, adjustedBy: String = "admin"): StockAdjustmentResponse {
        val stock = stockLevelRepository.findByVariantId(variantId)
            ?: throw NotFoundException("Stock level not found for variant $variantId")

        if (stock.availableQuantity < quantity) {
            throw InsufficientStockException("Cannot write off $quantity units: only ${stock.availableQuantity} available")
        }

        stock.availableQuantity -= quantity
        stockLevelRepository.save(stock)

        val adjustment = stockAdjustmentRepository.save(
            StockAdjustment(
                variantId = variantId,
                quantityChange = -quantity,
                reason = reason,
                adjustedBy = adjustedBy
            )
        )

        return toResponse(adjustment)
    }

    @Transactional
    fun adjust(variantId: Long, quantityChange: Int, reason: String, adjustedBy: String = "system"): StockAdjustmentResponse {
        val stock = stockLevelRepository.findByVariantId(variantId)
            ?: throw NotFoundException("Stock level not found for variant $variantId")

        stock.availableQuantity += quantityChange
        if (stock.availableQuantity < 0) {
            throw InsufficientStockException("Adjustment would result in negative stock")
        }
        stockLevelRepository.save(stock)

        val adjustment = stockAdjustmentRepository.save(
            StockAdjustment(
                variantId = variantId,
                quantityChange = quantityChange,
                reason = reason,
                adjustedBy = adjustedBy
            )
        )

        return toResponse(adjustment)
    }

    @Transactional(readOnly = true)
    fun getAdjustments(variantId: Long): List<StockAdjustmentResponse> {
        return stockAdjustmentRepository.findByVariantId(variantId).map { toResponse(it) }
    }

    private fun toResponse(adjustment: StockAdjustment): StockAdjustmentResponse {
        return StockAdjustmentResponse(
            id = adjustment.id,
            variantId = adjustment.variantId,
            quantityChange = adjustment.quantityChange,
            reason = adjustment.reason,
            adjustedBy = adjustment.adjustedBy,
            createdAt = adjustment.createdAt
        )
    }
}
