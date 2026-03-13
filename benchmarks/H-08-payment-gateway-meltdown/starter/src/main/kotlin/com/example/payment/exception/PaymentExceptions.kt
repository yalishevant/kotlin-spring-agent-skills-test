package com.example.payment.exception

class PaymentNotFoundException(message: String) : RuntimeException(message)

class DuplicatePaymentException(message: String) : RuntimeException(message)

class GatewayTimeoutException(message: String) : RuntimeException(message)

class GatewayException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
