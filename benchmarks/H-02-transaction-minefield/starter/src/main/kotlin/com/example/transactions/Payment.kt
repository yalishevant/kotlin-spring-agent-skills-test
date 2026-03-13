package com.example.transactions

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "payments")
data class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: String = "",

    @Column(nullable = false)
    var amount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var status: String = "PENDING",

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
