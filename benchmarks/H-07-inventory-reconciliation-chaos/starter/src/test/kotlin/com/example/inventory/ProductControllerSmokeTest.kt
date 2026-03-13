package com.example.inventory

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerSmokeTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `list products returns 200`() {
        mockMvc.get("/api/products") {
            with(httpBasic("user", "password"))
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `get product returns 200`() {
        mockMvc.get("/api/products/1") {
            with(httpBasic("user", "password"))
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `get stock summary returns 200`() {
        mockMvc.get("/api/products/1/stock-summary") {
            with(httpBasic("user", "password"))
        }.andExpect {
            status { isOk() }
        }
    }
}
