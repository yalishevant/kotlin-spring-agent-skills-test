package com.example.orderfallout.support

class OrderNotFoundException(orderId: Long) : RuntimeException("Order $orderId was not found")
