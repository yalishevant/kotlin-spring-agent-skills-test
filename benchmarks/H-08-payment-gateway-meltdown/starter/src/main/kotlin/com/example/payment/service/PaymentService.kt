package com.example.payment.service

import com.example.payment.dto.CreatePaymentRequest
import com.example.payment.dto.PaymentResponse
import com.example.payment.entity.Payment
import com.example.payment.entity.PaymentStatus
import com.example.payment.exception.DuplicatePaymentException
import com.example.payment.exception.PaymentNotFoundException
import com.example.payment.repository.PaymentRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val gatewayClient: GatewayClient
) {

    @Transactional
    fun createPayment(request: CreatePaymentRequest): PaymentResponse {
        // BUG: no duplicate check before save — DataIntegrityViolationException on unique constraint
        // leaks as 500 with SQL details instead of clean 409
        val payment = Payment(
            customerName = request.customerName,
            amount = request.amount,
            idempotencyKey = request.idempotencyKey
        )

        val saved = paymentRepository.save(payment)

        // Call external gateway
        try {
            val gatewayResponse = gatewayClient.charge(request.amount, request.idempotencyKey)
            saved.status = PaymentStatus.COMPLETED
            saved.gatewayTransactionId = gatewayResponse.transactionId
        } catch (e: Exception) {
            saved.status = PaymentStatus.FAILED
            saved.failureReason = e.message
        }

        paymentRepository.save(saved)
        return toResponse(saved)
    }

    @Cacheable("payments")
    fun getPayment(id: Long): PaymentResponse {
        val payment = paymentRepository.findById(id)
            .orElseThrow { PaymentNotFoundException("Payment not found: $id") }
        return toResponse(payment)
    }

    fun refreshPayment(id: Long): PaymentResponse {
        // BUG: self-invocation — evictPaymentCache and getPayment called via this,
        // bypassing Spring proxy. Cache operations silently ignored.
        evictPaymentCache(id)
        return getPayment(id)
    }

    @CacheEvict("payments", key = "#id")
    fun evictPaymentCache(id: Long) {
        // no-op, just evicts cache
    }

    fun listPayments(): List<PaymentResponse> {
        return paymentRepository.findAll().map { toResponse(it) }
    }

    private fun toResponse(payment: Payment): PaymentResponse {
        return PaymentResponse(
            id = payment.id,
            customerName = payment.customerName,
            amount = payment.amount,
            idempotencyKey = payment.idempotencyKey,
            status = payment.status.name,
            gatewayTransactionId = payment.gatewayTransactionId
        )
    }
}
