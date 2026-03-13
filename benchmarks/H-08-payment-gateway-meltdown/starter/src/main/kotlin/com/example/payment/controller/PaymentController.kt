package com.example.payment.controller

import com.example.payment.dto.CreatePaymentRequest
import com.example.payment.dto.PaymentResponse
import com.example.payment.service.PaymentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
class PaymentController(private val paymentService: PaymentService) {

    // BUG: returns 200 OK instead of 201 Created for resource creation
    @PostMapping
    fun createPayment(@Valid @RequestBody request: CreatePaymentRequest): ResponseEntity<PaymentResponse> {
        val payment = paymentService.createPayment(request)
        return ResponseEntity.ok(payment)
    }

    @GetMapping("/{id}")
    fun getPayment(@PathVariable id: Long): ResponseEntity<PaymentResponse> {
        return ResponseEntity.ok(paymentService.getPayment(id))
    }

    @PostMapping("/{id}/refresh")
    fun refreshPayment(@PathVariable id: Long): ResponseEntity<PaymentResponse> {
        return ResponseEntity.ok(paymentService.refreshPayment(id))
    }

    @GetMapping
    fun listPayments(): ResponseEntity<List<PaymentResponse>> {
        return ResponseEntity.ok(paymentService.listPayments())
    }
}
