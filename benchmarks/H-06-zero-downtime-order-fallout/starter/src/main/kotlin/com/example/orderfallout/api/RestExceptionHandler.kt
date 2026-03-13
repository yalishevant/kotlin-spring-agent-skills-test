package com.example.orderfallout.api

import com.example.orderfallout.support.OrderNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleNotFound(ex: OrderNotFoundException): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError(status = HttpStatus.NOT_FOUND.value(), message = ex.message ?: "Order not found"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> {
        return ResponseEntity.badRequest()
            .body(ApiError(status = HttpStatus.BAD_REQUEST.value(), message = ex.message ?: "Bad request"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Validation failed"
        return ResponseEntity.badRequest()
            .body(ApiError(status = HttpStatus.BAD_REQUEST.value(), message = message))
    }
}
