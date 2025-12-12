package com.okestro.okchat.config

import com.okestro.okchat.dto.ErrorResponse
import com.okestro.okchat.exception.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange

class GlobalExceptionHandlerTest : DescribeSpec({

    val meterRegistry = SimpleMeterRegistry()
    val handler = GlobalExceptionHandler(meterRegistry, "local")

    fun createExchange(path: String = "/api/test"): ServerWebExchange {
        val request = MockServerHttpRequest.get(path).build()
        return MockServerWebExchange.from(request)
    }

    describe("GlobalExceptionHandler") {

        context("NotFoundException 처리") {
            it("404 상태 코드와 E001 에러 코드를 반환한다") {
                val exception = NotFoundException("User not found")
                val exchange = createExchange("/api/users/123")

                val result = handler.handleException(exception, exchange).block()

                result shouldNotBe null
                result!!.statusCode shouldBe HttpStatus.NOT_FOUND
                result.body!!.code shouldBe "E001"
                result.body!!.message shouldContain "User not found"
            }
        }

        context("ValidationException 처리") {
            it("400 상태 코드와 E002 에러 코드를 반환한다") {
                val exception = ValidationException("Invalid email", field = "email")
                val exchange = createExchange("/api/users")

                val result = handler.handleException(exception, exchange).block()

                result shouldNotBe null
                result!!.statusCode shouldBe HttpStatus.BAD_REQUEST
                result.body!!.code shouldBe "E002"
                result.body!!.metadata?.get("field") shouldBe "email"
            }
        }

        context("AuthenticationException 처리") {
            it("401 상태 코드와 E003 에러 코드를 반환한다") {
                val exception = AuthenticationException("Invalid token")
                val exchange = createExchange("/api/chat")

                val result = handler.handleException(exception, exchange).block()

                result shouldNotBe null
                result!!.statusCode shouldBe HttpStatus.UNAUTHORIZED
                result.body!!.code shouldBe "E003"
            }
        }

        context("RateLimitException 처리") {
            it("429 상태 코드와 retryAfter 메타데이터를 반환한다") {
                val exception = RateLimitException(retryAfter = 60)
                val exchange = createExchange("/api/chat")

                val result = handler.handleException(exception, exchange).block()

                result shouldNotBe null
                result!!.statusCode shouldBe HttpStatus.TOO_MANY_REQUESTS
                result.body!!.code shouldBe "E005"
                result.body!!.metadata?.get("retryAfter") shouldBe 60L
            }
        }

        context("ExternalApiException 처리") {
            it("503 상태 코드와 서비스 정보를 반환한다") {
                val exception = ExternalApiException(
                    message = "Confluence API timeout",
                    service = "Confluence",
                    endpoint = "/wiki/rest/api/content"
                )
                val exchange = createExchange("/api/sync")

                val result = handler.handleException(exception, exchange).block()

                result shouldNotBe null
                result!!.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
                result.body!!.code shouldBe "E006"
                result.body!!.metadata?.get("service") shouldBe "Confluence"
                result.body!!.metadata?.get("endpoint") shouldBe "/wiki/rest/api/content"
            }
        }

        context("일반 Exception 처리") {
            it("500 상태 코드와 E999 에러 코드를 반환한다") {
                val exception = RuntimeException("Unexpected error")
                val exchange = createExchange("/api/test")

                val result = handler.handleException(exception, exchange).block()

                result shouldNotBe null
                result!!.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                result.body!!.code shouldBe "E999"
            }
        }

        context("에러 메트릭") {
            it("api.errors.total 카운터를 증가시킨다") {
                val exception = NotFoundException("Not found")
                val exchange = createExchange("/api/users/123")

                handler.handleException(exception, exchange).block()

                val counter = meterRegistry.counter(
                    "api.errors.total",
                    "error_code", "E001",
                    "error_type", "NotFoundException",
                    "path", "/api/users/{id}",
                    "method", "GET",
                    "status", "404"
                )

                counter.count() shouldBe 1.0
            }
        }

        context("경로 정규화") {
            it("숫자 ID를 {id}로 변환한다") {
                val exception = NotFoundException("Not found")
                val exchange = createExchange("/api/users/123")

                handler.handleException(exception, exchange).block()

                // 메트릭에서 정규화된 경로 확인
                val counter = meterRegistry.find("api.errors.total")
                    .tag("path", "/api/users/{id}")
                    .counter()

                counter shouldNotBe null
            }

            it("UUID를 {uuid}로 변환한다") {
                val exception = NotFoundException("Not found")
                val exchange = createExchange("/api/sessions/550e8400-e29b-41d4-a716-446655440000")

                handler.handleException(exception, exchange).block()

                val counter = meterRegistry.find("api.errors.total")
                    .tag("path", "/api/sessions/{uuid}")
                    .counter()

                counter shouldNotBe null
            }
        }
    }
})
