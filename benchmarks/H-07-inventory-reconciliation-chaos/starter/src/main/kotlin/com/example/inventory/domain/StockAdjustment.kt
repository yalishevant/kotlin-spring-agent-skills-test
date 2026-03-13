package com.example.inventory.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "stock_adjustments")
class StockAdjustment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "variant_id", nullable = false)
    var variantId: Long = 0,

    @Column(name = "quantity_change", nullable = false)
    var quantityChange: Int = 0,

    @Column(length = 255)
    var reason: String? = null,

    @Column(name = "adjusted_by", length = 100)
    var adjustedBy: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
