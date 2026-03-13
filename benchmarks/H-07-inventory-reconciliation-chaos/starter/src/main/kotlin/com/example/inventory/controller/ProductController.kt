package com.example.inventory.controller

import com.example.inventory.dto.ProductResponse
import com.example.inventory.dto.StockSummary
import com.example.inventory.service.InventoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val inventoryService: InventoryService
) {

    @GetMapping
    fun listProducts(): ResponseEntity<List<ProductResponse>> {
        return ResponseEntity.ok(inventoryService.listProducts())
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: Long): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(inventoryService.getProduct(id))
    }

    @GetMapping("/{id}/stock-summary")
    fun getStockSummary(@PathVariable id: Long): ResponseEntity<StockSummary> {
        return ResponseEntity.ok(inventoryService.getProductStockSummary(id))
    }
}
