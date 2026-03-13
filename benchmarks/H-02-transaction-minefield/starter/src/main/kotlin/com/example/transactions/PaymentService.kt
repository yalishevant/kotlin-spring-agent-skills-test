package com.example.transactions

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val auditLogRepository: AuditLogRepository
) {

    fun processPayment(userId: String, amount: BigDecimal): Payment {
        val payment = Payment(userId = userId, amount = amount, status = "PENDING")
        return validateAndSave(payment)
    }

    @Transactional
    private fun validateAndSave(payment: Payment): Payment {
        val saved = paymentRepository.save(payment)
        paymentRepository.flush()

        if (saved.amount > BigDecimal("10000")) {
            throw IllegalArgumentException("Amount exceeds limit — should trigger rollback")
        }

        saved.status = "COMPLETED"
        return paymentRepository.save(saved)
    }

    @Transactional
    fun saveAuditLog(action: String, entityId: Long, details: String) {
        auditLogRepository.save(AuditLog(action = action, entityId = entityId, details = details))
    }

    @Transactional
    fun processPaymentWithAudit(userId: String, amount: BigDecimal): Payment {
        val payment = paymentRepository.save(Payment(userId = userId, amount = amount, status = "PROCESSING"))
        paymentRepository.flush()

        saveAuditLog("PAYMENT_ATTEMPT", payment.id, "Processing payment of $amount for $userId")

        if (amount > BigDecimal("10000")) {
            throw IllegalArgumentException("Amount exceeds limit")
        }

        payment.status = "COMPLETED"
        return paymentRepository.save(payment)
    }

    fun transferFunds(fromId: Long, toId: Long, amount: BigDecimal) {
        val from = paymentRepository.findById(fromId).orElseThrow {
            NoSuchElementException("Source payment not found: $fromId")
        }
        from.amount = from.amount.subtract(amount)
        paymentRepository.save(from)
        paymentRepository.flush()

        if (amount > BigDecimal("5000")) {
            throw IllegalStateException("Transfer limit exceeded — partial state!")
        }

        val to = paymentRepository.findById(toId).orElseThrow {
            NoSuchElementException("Target payment not found: $toId")
        }
        to.amount = to.amount.add(amount)
        paymentRepository.save(to)
    }

    fun findPayment(id: Long): Payment {
        return paymentRepository.findById(id).orElseThrow {
            NoSuchElementException("Payment not found: $id")
        }
    }

    fun createPayment(userId: String, amount: BigDecimal): Payment {
        return paymentRepository.save(Payment(userId = userId, amount = amount, status = "CREATED"))
    }
}
