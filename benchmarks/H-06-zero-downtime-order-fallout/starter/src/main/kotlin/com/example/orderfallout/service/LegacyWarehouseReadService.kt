package com.example.orderfallout.service

import com.example.orderfallout.repo.OrderRepository
import com.example.orderfallout.support.OrderNotFoundException
import org.springframework.stereotype.Service

@Service
class LegacyWarehouseReadService(
    private val orderRepository: OrderRepository
) {

    fun shippingAddressFor(orderId: Long): String {
        return orderRepository.findLegacyShippingAddress(orderId) ?: throw OrderNotFoundException(orderId)
    }
}
