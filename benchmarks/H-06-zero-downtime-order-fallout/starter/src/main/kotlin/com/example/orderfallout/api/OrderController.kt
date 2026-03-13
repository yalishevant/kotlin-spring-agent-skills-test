package com.example.orderfallout.api

import com.example.orderfallout.service.OrderService
import com.example.orderfallout.service.OrderSummaryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.ResponseStatus
import java.net.URI

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService,
    private val orderSummaryService: OrderSummaryService
) {

    @PostMapping
    fun createOrder(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
        val order = orderService.createOrder(request)
        return ResponseEntity
            .created(URI.create("/api/orders/${order.id}"))
            .body(order.toResponse())
    }

    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: Long): OrderResponse {
        return orderService.getOrder(orderId).toResponse()
    }

    @PatchMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    fun patchOrder(
        @PathVariable orderId: Long,
        @RequestBody request: OrderPatchRequest
    ): OrderResponse {
        return orderService.patchOrder(orderId, request).toResponse()
    }

    @GetMapping("/{orderId}/summary")
    fun getSummary(@PathVariable orderId: Long): OrderSummaryResponse {
        return orderSummaryService.getSummary(orderId).toResponse()
    }
}
