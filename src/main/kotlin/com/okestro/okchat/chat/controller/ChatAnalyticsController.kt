package com.okestro.okchat.chat.controller

import com.okestro.okchat.chat.event.ChatEventBus
import com.okestro.okchat.chat.event.FeedbackSubmittedEvent
import com.okestro.okchat.chat.service.ChatAnalyticsService
import com.okestro.okchat.chat.service.DailyUsageStats
import com.okestro.okchat.chat.service.PerformanceMetrics
import com.okestro.okchat.chat.service.QualityTrendStats
import com.okestro.okchat.chat.service.QueryTypeStat
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/admin/chat/analytics")
@Tag(
    name = "Chat Analytics",
    description = """
        챗봇 분석 및 통계 API
        
        채팅 인터랙션 데이터를 기반으로 다양한 분석 지표를 제공합니다:
        - 사용량 통계 (대화 수, 응답 시간)
        - 품질 지표 (사용자 평점, 만족도)
        - 성능 메트릭 (응답 속도, 에러율)
        - 쿼리 타입별 분석
    """
)
class ChatAnalyticsController(
    private val analyticsService: ChatAnalyticsService,
    private val chatEventBus: ChatEventBus
) {

    @GetMapping("/usage/daily")
    @Operation(
        summary = "일별 사용량 통계 조회",
        description = """
            지정된 기간의 일별 사용량 통계를 조회합니다.
            
            반환 데이터:
            - totalInteractions: 총 대화 수
            - averageResponseTime: 평균 응답 시간 (밀리초)
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통계 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = DailyUsageStats::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 날짜 형식"
            )
        ]
    )
    suspend fun getDailyUsage(
        @Parameter(
            description = "통계 시작 날짜 (ISO-8601 형식)",
            example = "2025-01-01T00:00:00",
            required = true
        )
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startDate: LocalDateTime,
        @Parameter(
            description = "통계 종료 날짜 (ISO-8601 형식)",
            example = "2025-01-31T23:59:59",
            required = true
        )
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        endDate: LocalDateTime
    ): DailyUsageStats {
        return analyticsService.getDailyUsageStats(startDate, endDate)
    }

    @GetMapping("/quality/trend")
    @Operation(
        summary = "품질 트렌드 통계 조회",
        description = """
            사용자 평점 및 만족도 트렌드를 조회합니다.
            
            반환 데이터:
            - averageRating: 평균 사용자 평점 (1-5)
            - helpfulPercentage: 도움됨 피드백 비율 (%)
            - totalInteractions: 총 대화 수
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통계 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = QualityTrendStats::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 날짜 형식"
            )
        ]
    )
    suspend fun getQualityTrend(
        @Parameter(
            description = "통계 시작 날짜 (ISO-8601 형식)",
            example = "2025-01-01T00:00:00",
            required = true
        )
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startDate: LocalDateTime,
        @Parameter(
            description = "통계 종료 날짜 (ISO-8601 형식)",
            example = "2025-01-31T23:59:59",
            required = true
        )
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        endDate: LocalDateTime
    ): QualityTrendStats {
        return analyticsService.getQualityTrendStats(startDate, endDate)
    }

    @GetMapping("/performance")
    @Operation(
        summary = "성능 메트릭 조회",
        description = """
            평균 응답 시간 및 에러율 등 성능 지표를 조회합니다.
            
            반환 데이터:
            - averageResponseTimeMs: 평균 응답 시간 (밀리초)
            - errorRate: 에러 발생 비율 (%) - 현재 항상 0 (에러 추적 비활성화)
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통계 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PerformanceMetrics::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 날짜 형식"
            )
        ]
    )
    suspend fun getPerformanceMetrics(
        @Parameter(
            description = "통계 시작 날짜 (ISO-8601 형식)",
            example = "2025-01-01T00:00:00",
            required = true
        )
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startDate: LocalDateTime,
        @Parameter(
            description = "통계 종료 날짜 (ISO-8601 형식)",
            example = "2025-01-31T23:59:59",
            required = true
        )
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        endDate: LocalDateTime
    ): PerformanceMetrics {
        return analyticsService.getPerformanceMetrics(startDate, endDate)
    }

    @GetMapping("/query-types")
    @Operation(
        summary = "쿼리 타입별 통계 조회",
        description = """
            쿼리 타입별 사용량 및 품질 통계를 조회합니다.
            
            각 쿼리 타입별로 다음 정보를 제공:
            - queryType: 쿼리 유형 (DOCUMENT_SEARCH, KEYWORD, GENERAL 등)
            - count: 해당 타입의 대화 수
            - averageRating: 평균 사용자 평점
            - averageResponseTime: 평균 응답 시간 (밀리초)
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통계 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = QueryTypeStat::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 날짜 형식"
            )
        ]
    )
    suspend fun getQueryTypeStats(
        @Parameter(
            description = "통계 시작 날짜 (ISO-8601 형식)",
            example = "2025-01-01T00:00:00",
            required = true
        )
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startDate: LocalDateTime,
        @Parameter(
            description = "통계 종료 날짜 (ISO-8601 형식)",
            example = "2025-01-31T23:59:59",
            required = true
        )
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        endDate: LocalDateTime
    ): List<QueryTypeStat> {
        return analyticsService.getQueryTypeStats(startDate, endDate)
    }

    @PostMapping("/feedback")
    @Operation(
        summary = "채팅 피드백 제출",
        description = """
            사용자가 채팅 응답에 대한 피드백을 제출합니다.
            
            **피드백 항목 (모두 선택사항):**
            - rating: 1-5 별점
            - wasHelpful: 도움됨 여부 (true/false)
            - feedback: 자유 텍스트 코멘트
            
            **처리 방식:**
            - 비동기 이벤트 기반 처리
            - 최대 10회 재시도 (exponential backoff)
            - Race condition 방지 메커니즘 적용
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "피드백 접수 성공 (비동기 처리)"
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 형식",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "Invalid Request ID",
                                value = """{"error": "requestId is required"}"""
                            )
                        ]
                    )
                ]
            )
        ]
    )
    suspend fun submitFeedback(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "피드백 데이터",
            required = true,
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = FeedbackRequest::class),
                    examples = [
                        ExampleObject(
                            name = "Full Feedback",
                            description = "모든 필드가 포함된 피드백",
                            value = """
                            {
                              "requestId": "req-123456",
                              "rating": 5,
                              "wasHelpful": true,
                              "feedback": "답변이 매우 도움되었습니다!"
                            }
                        """
                        ),
                        ExampleObject(
                            name = "Rating Only",
                            description = "별점만 제출",
                            value = """
                            {
                              "requestId": "req-123456",
                              "rating": 4
                            }
                        """
                        ),
                        ExampleObject(
                            name = "Helpful Only",
                            description = "도움됨 여부만 제출",
                            value = """
                            {
                              "requestId": "req-123456",
                              "wasHelpful": true
                            }
                        """
                        )
                    ]
                )
            ]
        )
        @RequestBody
        request: FeedbackRequest
    ) {
        val requestId = request.requestId

        log.info { "[Feedback] Received feedback for requestId=$requestId, rating=${request.rating}, helpful=${request.wasHelpful}" }

        chatEventBus.publish(
            FeedbackSubmittedEvent(
                requestId = requestId,
                rating = request.rating,
                wasHelpful = request.wasHelpful,
                feedback = request.feedback
            )
        )

        log.info { "[Feedback] Event published for requestId=$requestId" }
    }
}

@Schema(description = "피드백 요청 데이터")
data class FeedbackRequest(
    @field:Schema(
        description = "요청 ID",
        example = "req-123",
        required = true
    )
    val requestId: String,

    @field:Schema(
        description = "별점 (1-5)",
        example = "5",
        required = false
    )
    val rating: Int?,

    @field:Schema(
        description = "도움 여부",
        example = "true",
        required = false
    )
    val wasHelpful: Boolean?,

    @field:Schema(
        description = "추가 코멘트",
        example = "답변이 도움되었습니다",
        required = false
    )
    val feedback: String?
)
