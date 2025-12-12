package com.okestro.okchat.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Security audit logger.
 *
 * Logs immutable, structured security-related events to the "AUDIT" logger,
 * which is routed to a dedicated rolling file appender in logback-spring.xml.
 */
@Component
class SecurityAuditLogger(
    private val objectMapper: ObjectMapper
) {
    private val auditLog = LoggerFactory.getLogger("AUDIT")

    fun logAuthentication(userId: String, success: Boolean, ip: String?, userAgent: String?) {
        emit(
            mapOf(
                "event" to "authentication",
                "userId" to userId,
                "success" to success,
                "ip" to (ip ?: "unknown"),
                "userAgent" to (userAgent ?: "unknown")
            )
        )
    }

    fun logDataAccess(
        userId: String?,
        resource: String,
        action: String,
        details: Map<String, Any?> = emptyMap()
    ) {
        emit(
            mapOf(
                "event" to "data_access",
                "userId" to (userId ?: "anonymous"),
                "resource" to resource,
                "action" to action
            ) + details
        )
    }

    fun logSecurityViolation(userId: String?, violation: String, severity: String, details: Map<String, Any?> = emptyMap()) {
        emit(
            mapOf(
                "event" to "security_violation",
                "userId" to (userId ?: "anonymous"),
                "violation" to violation,
                "severity" to severity
            ) + details
        )
    }

    fun logPermissionChange(
        adminId: String?,
        targetUserId: String,
        action: String,
        details: Map<String, Any?> = emptyMap()
    ) {
        emit(
            mapOf(
                "event" to "permission_change",
                "adminId" to (adminId ?: "system"),
                "targetUserId" to targetUserId,
                "action" to action
            ) + details
        )
    }

    private fun emit(payload: Map<String, Any?>) {
        val body = payload + mapOf("timestamp" to Instant.now().toString())
        auditLog.info(objectMapper.writeValueAsString(body))
    }
}
