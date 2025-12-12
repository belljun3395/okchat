package com.okestro.okchat.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * 표준 에러 응답 DTO
 *
 * 모든 API 에러는 이 형식으로 반환됩니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    /**
     * 에러 코드 (e.g., "E001")
     */
    val code: String,

    /**
     * 에러 메시지 (사용자에게 표시될 메시지)
     */
    val message: String,

    /**
     * 에러 발생 시각 (ISO 8601 형식)
     */
    val timestamp: Instant = Instant.now(),

    /**
     * 요청 경로 (e.g., "/api/chat")
     */
    val path: String,

    /**
     * Trace ID (분산 추적용)
     */
    val traceId: String? = null,

    /**
     * 상세 정보 (개발 환경에서만 포함)
     */
    val details: String? = null,

    /**
     * 추가 메타데이터 (선택사항)
     */
    val metadata: Map<String, Any>? = null
) {
    companion object {
        /**
         * ValidationException용 헬퍼 메서드
         */
        fun validation(
            code: String,
            message: String,
            path: String,
            field: String? = null,
            traceId: String? = null,
            details: String? = null
        ): ErrorResponse {
            val metadata = field?.let { mapOf("field" to it) }
            return ErrorResponse(
                code = code,
                message = message,
                path = path,
                traceId = traceId,
                details = details,
                metadata = metadata
            )
        }

        /**
         * RateLimitException용 헬퍼 메서드
         */
        fun rateLimit(
            code: String,
            message: String,
            path: String,
            retryAfter: Long? = null,
            traceId: String? = null,
            details: String? = null
        ): ErrorResponse {
            val metadata = retryAfter?.let { mapOf("retryAfter" to it) }
            return ErrorResponse(
                code = code,
                message = message,
                path = path,
                traceId = traceId,
                details = details,
                metadata = metadata
            )
        }

        /**
         * ExternalApiException용 헬퍼 메서드
         */
        fun externalApi(
            code: String,
            message: String,
            path: String,
            service: String,
            endpoint: String? = null,
            traceId: String? = null,
            details: String? = null
        ): ErrorResponse {
            val metadata = buildMap {
                put("service", service)
                endpoint?.let { put("endpoint", it) }
            }
            return ErrorResponse(
                code = code,
                message = message,
                path = path,
                traceId = traceId,
                details = details,
                metadata = metadata
            )
        }
    }
}
