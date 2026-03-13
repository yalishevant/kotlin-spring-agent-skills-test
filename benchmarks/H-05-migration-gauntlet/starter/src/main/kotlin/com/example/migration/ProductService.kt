package com.example.migration

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    @Transactional
    fun createProduct(name: String, description: String?, price: BigDecimal, category: String?): Product {
        val product = Product(
            displayName = name,
            description = description,
            price = price,
            category = category
        )
        return productRepository.save(product)
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Product> = productRepository.findAll()

    @Transactional(readOnly = true)
    fun findById(id: Long): Product = productRepository.findById(id).orElseThrow {
        NoSuchElementException("Product not found: $id")
    }

    @Transactional(readOnly = true)
    fun findByCategory(category: String): List<Product> = productRepository.findByCategory(category)

    @Transactional
    fun updateName(id: Long, newName: String): Product {
        val product = findById(id)
        product.displayName = newName
        return productRepository.save(product)
    }
}
