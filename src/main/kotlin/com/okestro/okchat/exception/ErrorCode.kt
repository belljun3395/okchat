package com.okestro.okchat.exception

import org.springframework.http.HttpStatus

/**
 * 에러 코드 정의
 *
 * 각 에러 코드는 고유한 코드, HTTP 상태, 메시지 템플릿을 가집니다.
 */
enum class ErrorCode(
    val code: String,
    val httpStatus: HttpStatus,
    val messageTemplate: String
) {
    // 4xx Client Errors
    NOT_FOUND(
        code = "E001",
        httpStatus = HttpStatus.NOT_FOUND,
        messageTemplate = "요청한 리소스를 찾을 수 없습니다"
    ),

    INVALID_INPUT(
        code = "E002",
        httpStatus = HttpStatus.BAD_REQUEST,
        messageTemplate = "입력 값이 올바르지 않습니다"
    ),

    UNAUTHORIZED(
        code = "E003",
        httpStatus = HttpStatus.UNAUTHORIZED,
        messageTemplate = "인증이 필요합니다"
    ),

    FORBIDDEN(
        code = "E004",
        httpStatus = HttpStatus.FORBIDDEN,
        messageTemplate = "접근 권한이 없습니다"
    ),

    RATE_LIMIT_EXCEEDED(
        code = "E005",
        httpStatus = HttpStatus.TOO_MANY_REQUESTS,
        messageTemplate = "요청 한도를 초과했습니다"
    ),

    // 5xx Server Errors
    EXTERNAL_SERVICE_ERROR(
        code = "E006",
        httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
        messageTemplate = "외부 서비스 오류가 발생했습니다"
    ),

    DATABASE_ERROR(
        code = "E007",
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        messageTemplate = "데이터베이스 오류가 발생했습니다"
    ),

    AI_SERVICE_ERROR(
        code = "E008",
        httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
        messageTemplate = "AI 서비스 오류가 발생했습니다"
    ),

    INTERNAL_SERVER_ERROR(
        code = "E999",
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        messageTemplate = "내부 서버 오류가 발생했습니다"
    );

    /**
     * Exception에서 메시지를 추출하거나 기본 템플릿 반환
     */
    fun getMessage(exception: Exception): String {
        return exception.message?.takeIf { it.isNotBlank() } ?: messageTemplate
    }

    companion object {
        /**
         * Exception 타입에 따라 ErrorCode 반환
         */
        fun fromException(exception: Exception): ErrorCode {
            return when (exception) {
                is NotFoundException -> NOT_FOUND
                is ValidationException -> INVALID_INPUT
                is AuthenticationException -> UNAUTHORIZED
                is AuthorizationException -> FORBIDDEN
                is RateLimitException -> RATE_LIMIT_EXCEEDED
                is ExternalApiException -> EXTERNAL_SERVICE_ERROR
                is DatabaseException -> DATABASE_ERROR
                is AiServiceException -> AI_SERVICE_ERROR
                else -> INTERNAL_SERVER_ERROR
            }
        }
    }
}
