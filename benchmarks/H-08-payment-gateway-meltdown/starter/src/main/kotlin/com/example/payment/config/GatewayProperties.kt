package com.example.payment.config

import org.springframework.boot.context.properties.ConfigurationProperties

// BUG: mutable lateinit var — loses startup validation, can use wrong defaults.
// BUG: timeout is Long — loses unit semantics, should be Duration.
// BUG: baseUrl has default "" — silently connects to empty URL instead of failing fast.
@ConfigurationProperties(prefix = "app.gateway")
class GatewayProperties {
    lateinit var baseUrl: String
    var timeout: Long = 5000
    var apiKey: String? = null
}
