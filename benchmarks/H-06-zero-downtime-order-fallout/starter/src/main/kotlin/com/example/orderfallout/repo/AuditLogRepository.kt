package com.example.orderfallout.repo

import com.example.orderfallout.domain.AuditLog
import com.example.orderfallout.domain.AuditStatus
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun countByOrderIdAndStatus(orderId: Long, status: AuditStatus): Long
}
