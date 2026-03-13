package com.example.inventory.dto

data class CsvRow(
    val sku: String,
    val quantityChange: Int,
    val reason: String? = null
)

data class BatchImportRequest(
    val rows: List<CsvRow>
)

data class BatchImportResult(
    val imported: Int,
    val errors: List<String>
)
