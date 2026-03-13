package com.example.orderfallout

import com.example.orderfallout.api.CreateOrderRequest
import com.example.orderfallout.api.OrderLineRequest
import com.example.orderfallout.api.OrderPatchRequest
import com.example.orderfallout.domain.OrderStatus
import com.example.orderfallout.service.OrderService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderServiceSmokeTest {

    @Autowired
    private lateinit var orderService: OrderService

    @Test
    fun `createOrder should persist lines and nullable fields`() {
        val created = orderService.createOrder(
            CreateOrderRequest(
                customerName = "Smoke Buyer",
                deliveryAddress = "Hangar 3",
                customerReference = "SMOKE-REF",
                notes = "stack carefully",
                lines = listOf(
                    OrderLineRequest("SKU-ALPHA", 1),
                    OrderLineRequest("SKU-BETA", 3)
                )
            )
        )

        val reloaded = orderService.getOrder(created.id)

        assertNotNull(created.id)
        assertEquals(2, reloaded.lines.size)
        assertEquals("SMOKE-REF", reloaded.customerReference)
        assertEquals("stack carefully", reloaded.notes)
        assertEquals(OrderStatus.DRAFT, reloaded.status)
    }

    @Test
    fun `patchOrder should update explicit values`() {
        val created = orderService.createOrder(
            CreateOrderRequest(
                customerName = "Patch Service",
                deliveryAddress = "Old Address",
                customerReference = "PATCH-1",
                notes = "before patch",
                lines = listOf(OrderLineRequest("SKU-GAMMA", 2))
            )
        )

        val patched = orderService.patchOrder(
            created.id,
            OrderPatchRequest(
                deliveryAddress = "New Address",
                customerReference = "PATCH-2",
                status = "fulfilling"
            )
        )

        assertEquals("New Address", patched.deliveryAddress)
        assertEquals("PATCH-2", patched.customerReference)
        assertEquals(OrderStatus.FULFILLING, patched.status)
    }
}
