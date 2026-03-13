package com.example.orderfallout.service

import com.example.orderfallout.domain.AuditLog
import com.example.orderfallout.domain.AuditStatus
import com.example.orderfallout.repo.AuditLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class AuditService(
    private val auditLogRepository: AuditLogRepository
) {

    @Transactional
    open fun recordSuccess(orderId: Long, details: String) {
        auditLogRepository.save(
            AuditLog(
                orderId = orderId,
                action = "ORDER_PATCHED",
                status = AuditStatus.SUCCESS,
                details = details
            )
        )
    }

    @Transactional
    open fun recordFailure(orderId: Long, details: String) {
        auditLogRepository.save(
            AuditLog(
                orderId = orderId,
                action = "ORDER_PATCH_FAILED",
                status = AuditStatus.FAILURE,
                details = details
            )
        )
    }
}
