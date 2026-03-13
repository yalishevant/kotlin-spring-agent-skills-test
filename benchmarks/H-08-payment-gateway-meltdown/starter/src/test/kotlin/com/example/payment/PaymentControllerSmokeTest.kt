package com.example.payment

import com.example.payment.service.GatewayClient
import com.example.payment.service.GatewayChargeResponse
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerSmokeTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var gatewayClient: GatewayClient

    @Test
    @WithMockUser
    fun `create payment endpoint accepts request`() {
        every { gatewayClient.charge(any(), any()) } returns
            GatewayChargeResponse("txn-123", "COMPLETED")

        mockMvc.post("/api/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"customerName":"Test","amount":100,"idempotencyKey":"key-1"}"""
        }.andExpect {
            status { is2xxSuccessful() }
        }
    }

    @Test
    @WithMockUser
    fun `list payments endpoint works`() {
        mockMvc.get("/api/payments")
            .andExpect {
                status { is2xxSuccessful() }
            }
    }
}
