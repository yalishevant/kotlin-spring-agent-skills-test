package com.example.inventory.repository

import com.example.inventory.domain.Reservation
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<Reservation, Long> {
    fun findByOrderId(orderId: String): List<Reservation>
    fun findByVariantId(variantId: Long): List<Reservation>
    fun findByVariantIdAndOrderId(variantId: Long, orderId: String): List<Reservation>
}
