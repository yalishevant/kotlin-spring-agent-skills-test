package com.example.transactions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentService: PaymentService
) {

    @PostMapping
    fun create(
        @RequestParam userId: String,
        @RequestParam amount: BigDecimal
    ): ResponseEntity<Payment> {
        val payment = paymentService.processPayment(userId, amount)
        return ResponseEntity.status(HttpStatus.CREATED).body(payment)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<Payment> {
        return try {
            ResponseEntity.ok(paymentService.findPayment(id))
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/transfer")
    fun transfer(
        @RequestParam fromId: Long,
        @RequestParam toId: Long,
        @RequestParam amount: BigDecimal
    ): ResponseEntity<Void> {
        paymentService.transferFunds(fromId, toId, amount)
        return ResponseEntity.ok().build()
    }
}
