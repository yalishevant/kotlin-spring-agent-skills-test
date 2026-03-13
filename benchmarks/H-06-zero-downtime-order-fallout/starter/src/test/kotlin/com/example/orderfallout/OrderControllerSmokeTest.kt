package com.example.orderfallout

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerSmokeTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `create endpoint should return created order`() {
        mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "customerName": "Controller Buyer",
                      "deliveryAddress": "Pier 7",
                      "customerReference": "CTRL-1",
                      "notes": "keep dry",
                      "lines": [
                        {"sku": "SKU-BOX", "quantity": 2}
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.customerName").value("Controller Buyer"))
            .andExpect(jsonPath("$.deliveryAddress").value("Pier 7"))
            .andExpect(jsonPath("$.lines[0].sku").value("SKU-BOX"))
    }

    @Test
    fun `patch endpoint should update status and keep summary available`() {
        val location = mockMvc.perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "customerName": "Patch Buyer",
                      "deliveryAddress": "West Dock",
                      "lines": [
                        {"sku": "SKU-TAPE", "quantity": 1}
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .getHeader("Location")
            ?: error("Location header missing")

        val orderId = location.substringAfterLast("/").toLong()

        mockMvc.perform(
            patch("/api/orders/$orderId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"confirmed","customerReference":"PATCH-REF"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.customerReference").value("PATCH-REF"))

        mockMvc.perform(get("/api/orders/$orderId/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
    }
}
