package com.example.orderfallout.repo

import com.example.orderfallout.domain.Order
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderRepository : JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = ["lines"])
    @Query("select o from Order o where o.id = :orderId")
    fun findDetailedById(@Param("orderId") orderId: Long): Order?

    @Query(value = "select shipping_address from orders where id = :orderId", nativeQuery = true)
    fun findLegacyShippingAddress(@Param("orderId") orderId: Long): String?
}
