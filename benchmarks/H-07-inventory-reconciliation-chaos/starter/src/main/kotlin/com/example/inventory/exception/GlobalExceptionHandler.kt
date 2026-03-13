package com.example.inventory.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(error = "Not Found", message = ex.message ?: "Resource not found"))
    }

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = "Insufficient Stock", message = ex.message ?: "Not enough stock"))
    }

    @ExceptionHandler(DuplicateReservationException::class)
    fun handleDuplicate(ex: DuplicateReservationException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = "Duplicate Reservation", message = ex.message ?: "Duplicate"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = "Validation Failed", message = errors.joinToString("; ")))
    }

    @ExceptionHandler(BatchImportException::class)
    fun handleBatchImport(ex: BatchImportException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(error = "Batch Import Failed", message = ex.message ?: "Import failed", details = ex.errors))
    }

    data class ErrorResponse(
        val error: String,
        val message: String,
        val details: List<String> = emptyList()
    )
}
