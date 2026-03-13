package com.example.inventory.domain

import jakarta.persistence.*

@Entity
@Table(name = "stock_levels")
class StockLevel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "variant_id", nullable = false)
    var variantId: Long = 0,

    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Int = 0,

    @Column(name = "reserved_quantity", nullable = false)
    var reservedQuantity: Int = 0,

    @Column(nullable = false)
    var version: Long = 0
)
