package com.example.transactions

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

class PaymentServiceTest {

    private lateinit var paymentRepository: PaymentRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var service: PaymentService

    @BeforeEach
    fun setUp() {
        paymentRepository = mockk(relaxed = true)
        auditLogRepository = mockk(relaxed = true)
        service = PaymentService(paymentRepository, auditLogRepository)
    }

    @Test
    fun `createPayment should save and return entity`() {
        val payment = Payment(id = 1, userId = "user1", amount = BigDecimal("100.00"), status = "CREATED")
        every { paymentRepository.save(any()) } returns payment

        val result = service.createPayment("user1", BigDecimal("100.00"))

        assertEquals("user1", result.userId)
        assertEquals(BigDecimal("100.00"), result.amount)
        verify { paymentRepository.save(any()) }
    }

    @Test
    fun `findPayment should return entity from repository`() {
        val payment = Payment(id = 1, userId = "user1", amount = BigDecimal("50.00"))
        every { paymentRepository.findById(1L) } returns Optional.of(payment)

        val result = service.findPayment(1L)

        assertEquals(1L, result.id)
        assertEquals("user1", result.userId)
    }

    @Test
    fun `findPayment should throw when not found`() {
        every { paymentRepository.findById(99L) } returns Optional.empty()

        assertThrows(NoSuchElementException::class.java) {
            service.findPayment(99L)
        }
    }
}
