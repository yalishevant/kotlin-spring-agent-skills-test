package com.example.inventory.service

import com.example.inventory.domain.Reservation
import com.example.inventory.domain.ReservationStatus
import com.example.inventory.dto.ReservationResponse
import com.example.inventory.exception.InsufficientStockException
import com.example.inventory.exception.NotFoundException
import com.example.inventory.repository.ReservationRepository
import com.example.inventory.repository.StockLevelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val stockLevelRepository: StockLevelRepository
) {

    @Transactional
    fun reserve(variantId: Long, orderId: String, quantity: Int): ReservationResponse {
        val stockLevel = stockLevelRepository.findByVariantId(variantId)
            ?: throw NotFoundException("Stock level not found for variant $variantId")

        if (stockLevel.availableQuantity < quantity) {
            throw InsufficientStockException("Not enough stock: available=${stockLevel.availableQuantity}, requested=$quantity")
        }

        stockLevel.availableQuantity -= quantity
        stockLevel.reservedQuantity += quantity
        stockLevelRepository.save(stockLevel)

        val reservation = reservationRepository.save(
            Reservation(
                variantId = variantId,
                orderId = orderId,
                quantity = quantity
            )
        )

        return toResponse(reservation)
    }

    @Transactional
    fun cancel(reservationId: Long): ReservationResponse {
        val reservation = reservationRepository.findById(reservationId)
            .orElseThrow { NotFoundException("Reservation not found: $reservationId") }

        if (reservation.status != ReservationStatus.ACTIVE) {
            throw IllegalStateException("Reservation ${reservation.id} is not active")
        }

        val stockLevel = stockLevelRepository.findByVariantId(reservation.variantId)
            ?: throw NotFoundException("Stock level not found for variant ${reservation.variantId}")

        stockLevel.availableQuantity += reservation.quantity
        stockLevel.reservedQuantity -= reservation.quantity
        stockLevelRepository.save(stockLevel)

        reservation.status = ReservationStatus.CANCELLED
        reservationRepository.save(reservation)

        return toResponse(reservation)
    }

    @Transactional(readOnly = true)
    fun getByOrderId(orderId: String): List<ReservationResponse> {
        return reservationRepository.findByOrderId(orderId).map { toResponse(it) }
    }

    private fun toResponse(reservation: Reservation): ReservationResponse {
        return ReservationResponse(
            id = reservation.id,
            variantId = reservation.variantId,
            orderId = reservation.orderId,
            quantity = reservation.quantity,
            status = reservation.status.name,
            createdAt = reservation.createdAt
        )
    }
}
