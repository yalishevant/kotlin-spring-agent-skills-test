package com.example.inventory.service

import com.example.inventory.domain.StockAdjustment
import com.example.inventory.dto.BatchImportResult
import com.example.inventory.dto.CsvRow
import com.example.inventory.exception.NotFoundException
import com.example.inventory.repository.ProductVariantRepository
import com.example.inventory.repository.StockAdjustmentRepository
import com.example.inventory.repository.StockLevelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class BatchImportService(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val stockLevelRepository: StockLevelRepository,
    private val variantRepository: ProductVariantRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun importRow(row: CsvRow): StockAdjustment {
        val variant = variantRepository.findBySku(row.sku)
            ?: throw NotFoundException("SKU not found: ${row.sku}")
        val stock = stockLevelRepository.findByVariantId(variant.id)
            ?: throw NotFoundException("Stock level not found for variant ${variant.id}")

        stock.availableQuantity += row.quantityChange
        stockLevelRepository.save(stock)

        return stockAdjustmentRepository.save(
            StockAdjustment(
                variantId = variant.id,
                quantityChange = row.quantityChange,
                reason = row.reason,
                adjustedBy = "batch-import"
            )
        )
    }

    fun importBatch(rows: List<CsvRow>): BatchImportResult {
        val results = mutableListOf<StockAdjustment>()
        for (row in rows) {
            val adjustment = importRow(row)
            results.add(adjustment)
        }
        return BatchImportResult(imported = results.size, errors = emptyList())
    }
}
