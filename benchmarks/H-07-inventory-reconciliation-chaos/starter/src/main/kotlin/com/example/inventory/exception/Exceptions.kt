package com.example.inventory.exception

class NotFoundException(message: String) : RuntimeException(message)

class InsufficientStockException(message: String) : RuntimeException(message)

class DuplicateReservationException(message: String) : RuntimeException(message)

class BatchImportException(message: String, val errors: List<String> = emptyList()) : RuntimeException(message)
