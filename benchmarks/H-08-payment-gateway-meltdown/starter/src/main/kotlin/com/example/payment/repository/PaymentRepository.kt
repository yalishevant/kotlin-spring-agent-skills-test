package com.example.payment.repository

import com.example.payment.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByIdempotencyKey(idempotencyKey: String): Payment?
}
