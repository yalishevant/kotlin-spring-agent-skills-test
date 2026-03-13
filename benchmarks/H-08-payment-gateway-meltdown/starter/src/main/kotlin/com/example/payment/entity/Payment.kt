package com.example.payment.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "payments")
class Payment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var customerName: String,

    @Column(nullable = false)
    var amount: BigDecimal,

    @Column(nullable = false, unique = true)
    var idempotencyKey: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column
    var gatewayTransactionId: String? = null,

    @Column
    var failureReason: String? = null,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Payment) return false
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

enum class PaymentStatus {
    PENDING, COMPLETED, FAILED
}
