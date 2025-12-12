package com.okestro.okchat.config

import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.spring.jakarta.EnableSentry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.sentry.protocol.Request
import io.sentry.protocol.User

/**
 * Sentry 에러 추적 설정
 *
 * 중앙화된 에러 추적 및 실시간 알림을 위한 Sentry 통합
 *
 * 주요 기능:
 * - 자동 에러 캡처
 * - Breadcrumb 기록 (사용자 행동 추적)
 * - Release 추적
 * - 민감정보 자동 필터링
 * - 환경별 설정
 */
@Configuration
@EnableSentry(
    dsn = "\${sentry.dsn:}",
    sendDefaultPii = false  // 개인정보 자동 전송 금지
)
class SentryConfig(
    @Value("\${spring.application.name}") private val applicationName: String,
    @Value("\${spring.profiles.active:local}") private val activeProfile: String,
    @Value("\${sentry.environment:\${spring.profiles.active:local}}") private val environment: String
) {

    /**
     * Sentry 옵션 커스터마이징
     */
    @Bean
    fun sentryOptionsConfiguration(): SentryOptions.OptionsConfiguration<SentryOptions> {
        return SentryOptions.OptionsConfiguration { options ->
            // 환경 설정
            options.environment = environment
            options.release = getRelease()

            // 트레이싱 설정
            options.tracesSampleRate = when (environment) {
                "production" -> 0.1  // 10% 샘플링
                "staging" -> 0.5     // 50% 샘플링
                else -> 1.0          // 100% 샘플링 (개발)
            }

            // 성능
            options.maxBreadcrumbs = 50
            options.maxCacheItems = 30

            // 에러 필터링 - 무시할 예외들
            options.setIgnoredExceptionsForType(
                setOf(
                    com.okestro.okchat.exception.NotFoundException::class.java,
                    com.okestro.okchat.exception.ValidationException::class.java
                )
            )

            // Before Send Hook - 민감정보 필터링
            options.beforeSend = SentryOptions.BeforeSendCallback { event, hint ->
                filterSensitiveData(event)
            }

            // Before Breadcrumb Hook - Breadcrumb 필터링
            options.beforeBreadcrumb = SentryOptions.BeforeBreadcrumbCallback { breadcrumb, hint ->
                filterBreadcrumb(breadcrumb)
            }

            // 프로파일링 (선택사항)
            options.isEnableTracing = true
            options.isAttachStacktrace = true
            options.isAttachThreads = true

            // 디버그 로깅 (개발 환경만)
            options.isDebug = environment !in listOf("production", "staging")
        }
    }

    /**
     * Release 정보 생성
     *
     * Git commit SHA를 사용하거나, 환경 변수에서 버전 읽기
     */
    private fun getRelease(): String {
        // 환경 변수에서 버전 읽기
        val version = System.getenv("APP_VERSION")
            ?: System.getenv("GIT_COMMIT")?.take(7)
            ?: "unknown"

        return "$applicationName@$version"
    }

    /**
     * 민감정보 필터링
     */
    private fun filterSensitiveData(event: SentryEvent): SentryEvent {
        // Request 데이터 필터링
        event.request?.let { request ->
            request.headers = filterHeaders(request.headers)
            request.cookies = filterCookies(request.cookies)
            request.queryString = filterQueryString(request.queryString)
        }

        // User 데이터 필터링
        event.user?.let { user ->
            // Email 부분 마스킹
            user.email?.let {
                user.email = maskEmail(it)
            }
            // IP 주소 마스킹
            user.ipAddress?.let {
                user.ipAddress = maskIpAddress(it)
            }
        }

        // Extra 데이터 필터링
        event.contexts?.let { contexts ->
            contexts.forEach { (key, value) ->
                if (isSensitiveKey(key)) {
                    contexts[key] = "***FILTERED***"
                }
            }
        }

        return event
    }

    /**
     * HTTP 헤더 필터링
     */
    private fun filterHeaders(headers: Map<String, String>?): Map<String, String>? {
        if (headers == null) return null

        val sensitiveHeaders = setOf(
            "authorization",
            "cookie",
            "x-api-key",
            "x-auth-token",
            "api-key",
            "apikey"
        )

        return headers.mapValues { (key, value) ->
            if (key.lowercase() in sensitiveHeaders) "***FILTERED***" else value
        }
    }

    /**
     * 쿠키 필터링
     */
    private fun filterCookies(cookies: String?): String? {
        if (cookies == null) return null
        return "***FILTERED***"
    }

    /**
     * Query String 필터링
     */
    private fun filterQueryString(queryString: String?): String? {
        if (queryString == null) return null

        val sensitiveParams = setOf("apiKey", "token", "password", "secret")

        return queryString.split("&").joinToString("&") { param ->
            val (key, value) = param.split("=", limit = 2)
            if (key in sensitiveParams) {
                "$key=***FILTERED***"
            } else {
                param
            }
        }
    }

    /**
     * Breadcrumb 필터링
     */
    private fun filterBreadcrumb(breadcrumb: Breadcrumb): Breadcrumb? {
        // SQL 쿼리 필터링
        if (breadcrumb.category == "sql" || breadcrumb.category == "query") {
            breadcrumb.data?.let { data ->
                data.entries.forEach { (key, value) ->
                    if (value is String && isSensitiveData(value)) {
                        data[key] = "***FILTERED***"
                    }
                }
            }
        }

        return breadcrumb
    }

    /**
     * 민감한 키 여부 확인
     */
    private fun isSensitiveKey(key: String): Boolean {
        val sensitiveKeywords = listOf(
            "password",
            "secret",
            "token",
            "apikey",
            "api_key",
            "authorization",
            "credential"
        )

        return sensitiveKeywords.any { keyword ->
            key.lowercase().contains(keyword)
        }
    }

    /**
     * 민감한 데이터 여부 확인
     */
    private fun isSensitiveData(data: String): Boolean {
        // 신용카드 번호 패턴
        val creditCardPattern = Regex("\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}")
        if (creditCardPattern.containsMatchIn(data)) return true

        // JWT 토큰 패턴
        val jwtPattern = Regex("eyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]*")
        if (jwtPattern.containsMatchIn(data)) return true

        // API 키 패턴
        val apiKeyPattern = Regex("[a-zA-Z0-9_-]{32,}")
        if (apiKeyPattern.containsMatchIn(data)) return true

        return false
    }

    /**
     * 이메일 마스킹
     */
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***@***.***"

        val username = parts[0]
        val domain = parts[1]

        val maskedUsername = if (username.length <= 2) {
            "***"
        } else {
            username.take(2) + "***"
        }

        return "$maskedUsername@$domain"
    }

    /**
     * IP 주소 마스킹 (마지막 옥텟 제거)
     */
    private fun maskIpAddress(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}.***"
        } else {
            "***.***.***.***"
        }
    }
}
