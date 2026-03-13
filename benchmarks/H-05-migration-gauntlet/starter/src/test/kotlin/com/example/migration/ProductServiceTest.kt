package com.example.migration

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

class ProductServiceTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var service: ProductService

    @BeforeEach
    fun setUp() {
        productRepository = mockk(relaxed = true)
        service = ProductService(productRepository)
    }

    @Test
    fun `createProduct should save and return product`() {
        val product = Product(id = 1, displayName = "Widget", price = BigDecimal("9.99"))
        every { productRepository.save(any()) } returns product

        val result = service.createProduct("Widget", null, BigDecimal("9.99"), null)

        assertEquals("Widget", result.displayName)
        verify { productRepository.save(any()) }
    }

    @Test
    fun `findById should return product from repository`() {
        val product = Product(id = 1, displayName = "Widget", price = BigDecimal("9.99"))
        every { productRepository.findById(1L) } returns Optional.of(product)

        val result = service.findById(1L)

        assertEquals(1L, result.id)
        assertEquals("Widget", result.displayName)
    }

    @Test
    fun `findById should throw when not found`() {
        every { productRepository.findById(99L) } returns Optional.empty()

        assertThrows(NoSuchElementException::class.java) {
            service.findById(99L)
        }
    }
}
