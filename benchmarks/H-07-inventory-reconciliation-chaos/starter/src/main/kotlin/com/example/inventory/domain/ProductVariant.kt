package com.example.inventory.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "product_variants")
class ProductVariant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,

    @Column(unique = true, nullable = false, length = 100)
    var sku: String = "",

    @Column(length = 50)
    var color: String? = null,

    @Column(length = 20)
    var size: String? = null,

    @Column(nullable = false, precision = 12, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO
)
