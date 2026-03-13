package com.example.inventory

import com.fasterxml.jackson.databind.ObjectMapper
import com.example.inventory.dto.ReservationRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class ReservationSmokeTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `single reservation succeeds`() {
        val request = ReservationRequest(variantId = 1, orderId = "ORD-SMOKE-001", quantity = 2)

        mockMvc.post("/api/reservations") {
            with(httpBasic("user", "password"))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.orderId") { value("ORD-SMOKE-001") }
            jsonPath("$.quantity") { value(2) }
            jsonPath("$.status") { value("ACTIVE") }
        }
    }
}
