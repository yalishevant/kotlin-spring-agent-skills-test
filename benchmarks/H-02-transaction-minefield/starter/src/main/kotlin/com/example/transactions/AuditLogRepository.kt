package com.example.transactions

import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findByEntityId(entityId: Long): List<AuditLog>
}
