package com.example.inventory.controller

import com.example.inventory.dto.AdjustmentType
import com.example.inventory.dto.StockAdjustmentRequest
import com.example.inventory.dto.StockAdjustmentResponse
import com.example.inventory.exception.NotFoundException
import com.example.inventory.repository.ProductVariantRepository
import com.example.inventory.service.StockAdjustmentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/stock-adjustments")
class StockAdjustmentController(
    private val stockAdjustmentService: StockAdjustmentService,
    private val variantRepository: ProductVariantRepository
) {

    @PostMapping
    fun createAdjustment(
        @Valid @RequestBody request: StockAdjustmentRequest,
        principal: Principal
    ): ResponseEntity<StockAdjustmentResponse> {
        val variant = variantRepository.findBySku(request.sku)
            ?: throw NotFoundException("Variant not found for SKU: ${request.sku}")

        val response = when (request.type) {
            AdjustmentType.WRITE_OFF -> stockAdjustmentService.writeOff(
                variantId = variant.id,
                quantity = request.quantity,
                reason = request.reason,
                adjustedBy = principal.name
            )
            AdjustmentType.ADJUSTMENT -> stockAdjustmentService.adjust(
                variantId = variant.id,
                quantityChange = request.quantity,
                reason = request.reason,
                adjustedBy = principal.name
            )
        }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{variantId}")
    fun getAdjustments(@PathVariable variantId: Long): ResponseEntity<List<StockAdjustmentResponse>> {
        return ResponseEntity.ok(stockAdjustmentService.getAdjustments(variantId))
    }
}
