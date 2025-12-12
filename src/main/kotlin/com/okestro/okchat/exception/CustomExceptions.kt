package com.okestro.okchat.exception

/**
 * 커스텀 예외 클래스들
 *
 * 각 예외는 ErrorCode와 매핑되어 일관된 에러 응답을 생성합니다.
 */

/**
 * 리소스를 찾을 수 없을 때 (404)
 */
class NotFoundException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 입력 값 검증 실패 (400)
 */
class ValidationException(
    message: String,
    val field: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 인증 실패 (401)
 */
class AuthenticationException(
    message: String = "인증에 실패했습니다",
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 권한 부족 (403)
 */
class AuthorizationException(
    message: String = "접근 권한이 없습니다",
    val requiredRole: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Rate Limit 초과 (429)
 */
class RateLimitException(
    message: String = "요청 한도를 초과했습니다",
    val retryAfter: Long? = null,  // seconds
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 외부 API 호출 실패 (503)
 */
class ExternalApiException(
    message: String,
    val service: String,  // e.g., "Confluence", "OpenAI"
    val endpoint: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 데이터베이스 오류 (500)
 */
class DatabaseException(
    message: String,
    val operation: String? = null,  // e.g., "SELECT", "INSERT"
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * AI 서비스 오류 (503)
 */
class AiServiceException(
    message: String,
    val model: String? = null,
    val provider: String? = null,  // e.g., "OpenAI"
    cause: Throwable? = null
) : RuntimeException(message, cause)
