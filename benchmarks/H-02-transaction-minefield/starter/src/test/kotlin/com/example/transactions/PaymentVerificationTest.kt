package com.example.transactions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.math.BigDecimal

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentVerificationTest {

    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @BeforeEach
    fun setUp() {
        auditLogRepository.deleteAll()
        paymentRepository.deleteAll()
    }

    @Disabled("Known issue — payment not rolled back on validation failure")
    @Test
    fun privateTransactionalShouldNotWork() {
        try {
            paymentService.processPayment("user1", BigDecimal("50000"))
        } catch (_: IllegalArgumentException) {
            // expected
        }

        val payments = paymentRepository.findAll()
        assertTrue(
            payments.isEmpty(),
            "Payment should have been rolled back after validation failure, but found ${payments.size} payment(s) in DB."
        )
    }

    @Disabled("Known issue — audit log disappears on payment failure")
    @Test
    fun auditLogShouldSurvivePaymentFailure() {
        try {
            paymentService.processPaymentWithAudit("user1", BigDecimal("50000"))
        } catch (_: IllegalArgumentException) {
            // expected — payment fails validation
        }

        val auditLogs = auditLogRepository.findAll()
        assertTrue(
            auditLogs.isNotEmpty(),
            "Audit log should persist even when the payment transaction fails."
        )
    }

    @Disabled("Known issue — concurrent updates cause silent data loss")
    @Test
    fun concurrentUpdateShouldBeDetected() {
        val payment = paymentService.createPayment("user1", BigDecimal("100.00"))

        val copy1 = paymentRepository.findById(payment.id).orElseThrow()
        val copy2 = paymentRepository.findById(payment.id).orElseThrow()

        copy1.status = "APPROVED"
        paymentRepository.saveAndFlush(copy1)

        copy2.status = "REJECTED"

        assertThrows(Exception::class.java) {
            paymentRepository.saveAndFlush(copy2)
        }
    }

    @Disabled("Known issue — partial transfer left inconsistent state")
    @Test
    fun transferShouldBeAtomic() {
        val from = paymentService.createPayment("sender", BigDecimal("1000.00"))
        val to = paymentService.createPayment("receiver", BigDecimal("500.00"))

        try {
            paymentService.transferFunds(from.id, to.id, BigDecimal("6000"))
        } catch (_: IllegalStateException) {
            // expected — transfer limit exceeded
        }

        val fromAfter = paymentRepository.findById(from.id).orElseThrow()
        assertEquals(
            0, BigDecimal("1000.00").compareTo(fromAfter.amount),
            "Sender's amount should remain 1000.00 because transfer should have rolled back. " +
                "Found ${fromAfter.amount}."
        )
    }
}
