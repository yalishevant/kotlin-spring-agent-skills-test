package com.example.migration

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {

    @PostMapping
    fun create(
        @RequestParam name: String,
        @RequestParam(required = false) description: String?,
        @RequestParam price: BigDecimal,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<Product> {
        val product = productService.createProduct(name, description, price, category)
        return ResponseEntity.status(HttpStatus.CREATED).body(product)
    }

    @GetMapping
    fun listAll(): ResponseEntity<List<Product>> = ResponseEntity.ok(productService.findAll())

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<Product> {
        return try {
            ResponseEntity.ok(productService.findById(id))
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}/name")
    fun updateName(@PathVariable id: Long, @RequestParam name: String): ResponseEntity<Product> {
        return ResponseEntity.ok(productService.updateName(id, name))
    }
}
