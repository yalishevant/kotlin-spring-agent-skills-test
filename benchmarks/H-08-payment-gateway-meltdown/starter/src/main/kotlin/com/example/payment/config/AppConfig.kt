package com.example.payment.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

// BUG: missing @EnableCaching — @Cacheable annotations on PaymentService silently do nothing.
// BUG: missing @EnableRetry — spring-retry is on the classpath but @Retryable annotations silently do nothing without it.
@Configuration
@EnableConfigurationProperties(GatewayProperties::class)
class AppConfig
