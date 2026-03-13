package com.example.inventory.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "reservations")
class Reservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "variant_id", nullable = false)
    var variantId: Long = 0,

    @Column(name = "order_id", nullable = false, length = 100)
    var orderId: String = "",

    @Column(nullable = false)
    var quantity: Int = 0,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: ReservationStatus = ReservationStatus.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)

enum class ReservationStatus {
    ACTIVE, CONFIRMED, CANCELLED
}
