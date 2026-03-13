package com.example.inventory.controller

import com.example.inventory.dto.StockAdjustmentResponse
import com.example.inventory.dto.WriteOffRequest
import com.example.inventory.service.StockAdjustmentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val stockAdjustmentService: StockAdjustmentService
) {

    @PostMapping("/write-off")
    fun writeOff(
        @Valid @RequestBody request: WriteOffRequest,
        principal: Principal
    ): ResponseEntity<StockAdjustmentResponse> {
        val response = stockAdjustmentService.writeOff(
            variantId = request.variantId,
            quantity = request.quantity,
            reason = request.reason,
            adjustedBy = principal.name
        )
        return ResponseEntity.ok(response)
    }
}
