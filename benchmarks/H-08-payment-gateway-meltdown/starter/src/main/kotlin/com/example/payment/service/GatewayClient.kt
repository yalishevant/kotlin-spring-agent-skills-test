package com.example.payment.service

import com.example.payment.config.GatewayProperties
import com.example.payment.exception.GatewayException
import com.example.payment.exception.GatewayTimeoutException
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.math.BigDecimal

data class GatewayChargeResponse(
    val transactionId: String,
    val status: String
)

// BUG: no explicit connect/read timeout configured on RestClient.
// BUG: no retry for transient failures — add @Retryable for GatewayTimeoutException (requires @EnableRetry on config).
@Service
class GatewayClient(private val gatewayProperties: GatewayProperties) {

    private val restClient = RestClient.builder()
        .baseUrl(gatewayProperties.baseUrl)
        .build()

    fun charge(amount: BigDecimal, idempotencyKey: String): GatewayChargeResponse {
        try {
            val response = restClient.post()
                .uri("/api/charges")
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-Api-Key", gatewayProperties.apiKey ?: "")
                .body(mapOf("amount" to amount.toString()))
                .retrieve()
                .body(GatewayChargeResponse::class.java)
            return response ?: throw GatewayException("Empty response from gateway")
        } catch (e: Exception) {
            when {
                e is GatewayException -> throw e
                e.message?.contains("timed out", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                    throw GatewayTimeoutException("Gateway timeout: ${e.message}")
                else -> throw GatewayException("Gateway error: ${e.message}", e)
            }
        }
    }
}
