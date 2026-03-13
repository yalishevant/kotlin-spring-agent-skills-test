package com.example.inventory.controller

import com.example.inventory.dto.BatchImportRequest
import com.example.inventory.dto.BatchImportResult
import com.example.inventory.service.BatchImportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/batch-import")
class BatchImportController(
    private val batchImportService: BatchImportService
) {

    @PostMapping
    fun importBatch(@RequestBody request: BatchImportRequest): ResponseEntity<BatchImportResult> {
        val result = batchImportService.importBatch(request.rows)
        return ResponseEntity.ok(result)
    }
}
