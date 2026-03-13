package com.example.inventory.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PricingProperties::class)
class PricingConfig(private val pricingProperties: PricingProperties)
