package com.example.orderfallout.service

import com.example.orderfallout.api.CreateOrderRequest
import com.example.orderfallout.api.OrderPatchRequest
import com.example.orderfallout.domain.Order
import com.example.orderfallout.domain.OrderLine
import com.example.orderfallout.domain.OrderPatchedEvent
import com.example.orderfallout.domain.OrderStatus
import com.example.orderfallout.repo.OrderRepository
import com.example.orderfallout.support.OrderNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class OrderService(
    private val orderRepository: OrderRepository,
    private val orderPatchApplier: OrderPatchApplier,
    private val orderValidator: OrderValidator,
    private val outboxService: OutboxService,
    private val auditService: AuditService,
    private val orderSummaryService: OrderSummaryService,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    @Transactional
    open fun createOrder(request: CreateOrderRequest): Order {
        val order = Order(
            customerName = request.customerName,
            status = OrderStatus.DRAFT,
            deliveryAddress = request.deliveryAddress,
            customerReference = request.customerReference,
            notes = request.notes
        )

        request.lines.forEach { line ->
            order.addLine(
                OrderLine(
                    sku = line.sku,
                    quantity = line.quantity
                )
            )
        }

        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    open fun getOrder(orderId: Long): Order {
        return orderRepository.findDetailedById(orderId) ?: throw OrderNotFoundException(orderId)
    }

    @Transactional
    open fun patchOrder(orderId: Long, request: OrderPatchRequest): Order {
        val order = orderRepository.findDetailedById(orderId) ?: throw OrderNotFoundException(orderId)

        try {
            orderPatchApplier.apply(order, request)
            outboxService.publishOrderPatched(order)
            applicationEventPublisher.publishEvent(OrderPatchedEvent(order.id))
            orderValidator.validatePatchedOrder(order)

            val saved = orderRepository.save(order)
            auditService.recordSuccess(saved.id, "Patched order ${saved.id}")
            orderSummaryService.refreshSummary(saved.id)
            return saved
        } catch (ex: RuntimeException) {
            auditService.recordFailure(order.id, ex.message ?: "Patch failed")
            throw ex
        }
    }
}
