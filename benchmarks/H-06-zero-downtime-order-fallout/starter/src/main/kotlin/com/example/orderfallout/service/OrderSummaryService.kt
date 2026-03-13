package com.example.orderfallout.service

import com.example.orderfallout.domain.Order
import com.example.orderfallout.domain.OrderSummary
import com.example.orderfallout.repo.OrderRepository
import com.example.orderfallout.support.OrderNotFoundException
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
open class OrderSummaryService(
    private val orderRepository: OrderRepository
) {

    @Cacheable("orderSummaries")
    open fun getSummary(orderId: Long): OrderSummary {
        val order = orderRepository.findDetailedById(orderId) ?: throw OrderNotFoundException(orderId)
        return order.toSummary()
    }

    open fun refreshSummary(orderId: Long): OrderSummary {
        evictSummary(orderId)
        return getSummary(orderId)
    }

    @CacheEvict(value = ["orderSummaries"], key = "#orderId")
    open fun evictSummary(orderId: Long) {
    }

    private fun Order.toSummary(): OrderSummary {
        return OrderSummary(
            orderId = id,
            customerName = customerName,
            status = status,
            deliveryAddress = deliveryAddress,
            customerReference = customerReference,
            notes = notes,
            lineCount = lines.size
        )
    }
}
