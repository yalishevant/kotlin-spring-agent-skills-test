package com.example.orderfallout

import com.example.orderfallout.api.CreateOrderRequest
import com.example.orderfallout.api.OrderLineRequest
import com.example.orderfallout.service.OrderService
import com.example.orderfallout.service.OrderSummaryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderSummarySmokeTest {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var orderSummaryService: OrderSummaryService

    @Test
    fun `summary service should cache repeated reads`() {
        val created = orderService.createOrder(
            CreateOrderRequest(
                customerName = "Cache Buyer",
                deliveryAddress = "Depot Road",
                notes = "cached",
                lines = listOf(OrderLineRequest("SKU-CACHE", 1))
            )
        )

        val first = orderSummaryService.getSummary(created.id)
        val second = orderSummaryService.getSummary(created.id)

        assertSame(first, second)
        assertEquals("Depot Road", second.deliveryAddress)
    }
}
