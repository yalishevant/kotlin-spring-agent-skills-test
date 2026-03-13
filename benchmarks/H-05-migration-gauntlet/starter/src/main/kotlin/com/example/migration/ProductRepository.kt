package com.example.migration

import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long> {
    fun findByCategory(category: String): List<Product>
}
