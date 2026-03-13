package com.example.orderfallout

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class OrderFalloutApplication

fun main(args: Array<String>) {
    runApplication<OrderFalloutApplication>(*args)
}
