package com.okestro.okchat.config

import com.okestro.okchat.dto.ErrorResponse
import com.okestro.okchat.exception.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

/**
 * 전역 예외 처리기
 *
 * 모든 컨트롤러에서 발생하는 예외를 일관된 형식으로 처리합니다.
 *
 * 기능:
 * 1. 예외를 ErrorCode로 분류
 * 2. 구조화된 로깅
 * 3. 에러 메트릭 자동 수집
 * 4. 표준 에러 응답 생성
 */
@RestControllerAdvice
class GlobalExceptionHandler(
    private val meterRegistry: MeterRegistry,
    @Value("\${spring.profiles.active:local}") private val activeProfile: String
) {

    /**
     * 커스텀 예외 처리
     */
    @ExceptionHandler(
        NotFoundException::class,
        ValidationException::class,
        AuthenticationException::class,
        AuthorizationException::class,
        RateLimitException::class,
        ExternalApiException::class,
        DatabaseException::class,
        AiServiceException::class
    )
    fun handleCustomException(
        exception: Exception,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        return handleException(exception, exchange)
    }

    /**
     * Spring Validation 예외 처리
     */
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(
        exception: WebExchangeBindException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errorCode = ErrorCode.INVALID_INPUT
        val firstError = exception.bindingResult.allErrors.firstOrNull()
        val message = firstError?.defaultMessage ?: errorCode.messageTemplate
        val field = firstError?.let {
            if (it is org.springframework.validation.FieldError) it.field else null
        }

        logError(exception, exchange, errorCode)
        recordErrorMetric(exception, errorCode, exchange)

        val response = ErrorResponse.validation(
            code = errorCode.code,
            message = message,
            path = exchange.request.path.value(),
            field = field,
            traceId = MDC.get("traceId"),
            details = if (isDevelopment()) exception.stackTraceToString() else null
        )

        return Mono.just(ResponseEntity.status(errorCode.httpStatus).body(response))
    }

    /**
     * 일반 예외 처리 (catch-all)
     */
    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errorCode = ErrorCode.fromException(exception)

        logError(exception, exchange, errorCode)
        recordErrorMetric(exception, errorCode, exchange)

        val response = buildErrorResponse(exception, errorCode, exchange)

        return Mono.just(ResponseEntity.status(errorCode.httpStatus).body(response))
    }

    /**
     * 에러 응답 빌더
     */
    private fun buildErrorResponse(
        exception: Exception,
        errorCode: ErrorCode,
        exchange: ServerWebExchange
    ): ErrorResponse {
        return when (exception) {
            is ValidationException -> ErrorResponse.validation(
                code = errorCode.code,
                message = errorCode.getMessage(exception),
                path = exchange.request.path.value(),
                field = exception.field,
                traceId = MDC.get("traceId"),
                details = if (isDevelopment()) exception.stackTraceToString() else null
            )

            is RateLimitException -> ErrorResponse.rateLimit(
                code = errorCode.code,
                message = errorCode.getMessage(exception),
                path = exchange.request.path.value(),
                retryAfter = exception.retryAfter,
                traceId = MDC.get("traceId"),
                details = if (isDevelopment()) exception.stackTraceToString() else null
            )

            is ExternalApiException -> ErrorResponse.externalApi(
                code = errorCode.code,
                message = errorCode.getMessage(exception),
                path = exchange.request.path.value(),
                service = exception.service,
                endpoint = exception.endpoint,
                traceId = MDC.get("traceId"),
                details = if (isDevelopment()) exception.stackTraceToString() else null
            )

            else -> ErrorResponse(
                code = errorCode.code,
                message = errorCode.getMessage(exception),
                path = exchange.request.path.value(),
                traceId = MDC.get("traceId"),
                details = if (isDevelopment()) exception.stackTraceToString() else null
            )
        }
    }

    /**
     * 구조화된 에러 로깅
     */
    private fun logError(
        exception: Exception,
        exchange: ServerWebExchange,
        errorCode: ErrorCode
    ) {
        val logContext = buildMap {
            put("errorCode", errorCode.code)
            put("errorType", exception::class.simpleName ?: "Unknown")
            put("path", exchange.request.path.value())
            put("method", exchange.request.method.name())
            put("traceId", MDC.get("traceId") ?: "N/A")
            put("userId", MDC.get("userId") ?: "N/A")
            put("httpStatus", errorCode.httpStatus.value())

            // 커스텀 예외별 추가 컨텍스트
            when (exception) {
                is ValidationException -> exception.field?.let { put("field", it) }
                is RateLimitException -> exception.retryAfter?.let { put("retryAfter", it) }
                is ExternalApiException -> {
                    put("service", exception.service)
                    exception.endpoint?.let { put("endpoint", it) }
                }
                is DatabaseException -> exception.operation?.let { put("operation", it) }
                is AiServiceException -> {
                    exception.model?.let { put("model", it) }
                    exception.provider?.let { put("provider", it) }
                }
            }
        }

        // 심각도에 따라 로그 레벨 결정
        when (errorCode.httpStatus.value()) {
            in 400..499 -> log.warn(exception) { "Client error: $logContext" }
            in 500..599 -> log.error(exception) { "Server error: $logContext" }
            else -> log.info(exception) { "Error: $logContext" }
        }
    }

    /**
     * 에러 메트릭 기록
     */
    private fun recordErrorMetric(
        exception: Exception,
        errorCode: ErrorCode,
        exchange: ServerWebExchange
    ) {
        val tags = Tags.of(
            "error_code", errorCode.code,
            "error_type", exception::class.simpleName ?: "Unknown",
            "path", normalizePath(exchange.request.path.value()),
            "method", exchange.request.method.name(),
            "status", errorCode.httpStatus.value().toString()
        )

        meterRegistry.counter("api.errors.total", tags).increment()

        // 에러 유형별 카운터
        meterRegistry.counter(
            "api.errors.by_type",
            Tags.of(
                "error_type", exception::class.simpleName ?: "Unknown",
                "category", if (errorCode.httpStatus.is4xxClientError) "client" else "server"
            )
        ).increment()
    }

    /**
     * 경로 정규화 (동적 ID 제거)
     */
    private fun normalizePath(path: String): String {
        return path
            .replace(Regex("/\\d+"), "/{id}")
            .replace(Regex("/[a-f0-9-]{36}"), "/{uuid}")
            .replace(Regex("/\\d{4}-\\d{2}-\\d{2}"), "/{date}")
    }

    /**
     * 개발 환경 여부 확인
     */
    private fun isDevelopment(): Boolean {
        return activeProfile in listOf("local", "dev", "development")
    }
}
