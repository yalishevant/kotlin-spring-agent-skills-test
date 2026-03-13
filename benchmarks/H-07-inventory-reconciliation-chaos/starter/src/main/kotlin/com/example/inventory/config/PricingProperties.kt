package com.example.inventory.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.pricing")
class PricingProperties {
    lateinit var baseUrl: String
    var timeout: Long = 0
    var apiKey: String? = null
}
