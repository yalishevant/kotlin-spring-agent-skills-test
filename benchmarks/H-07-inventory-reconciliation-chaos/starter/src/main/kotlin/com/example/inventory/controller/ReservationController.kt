package com.example.inventory.controller

import com.example.inventory.dto.ReservationRequest
import com.example.inventory.dto.ReservationResponse
import com.example.inventory.service.ReservationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {

    @PostMapping
    fun reserve(@Valid @RequestBody request: ReservationRequest): ResponseEntity<ReservationResponse> {
        val response = reservationService.reserve(request.variantId, request.orderId, request.quantity)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{id}/cancel")
    fun cancel(@PathVariable id: Long): ResponseEntity<ReservationResponse> {
        return ResponseEntity.ok(reservationService.cancel(id))
    }

    @GetMapping
    fun getByOrderId(@RequestParam orderId: String): ResponseEntity<List<ReservationResponse>> {
        return ResponseEntity.ok(reservationService.getByOrderId(orderId))
    }
}
