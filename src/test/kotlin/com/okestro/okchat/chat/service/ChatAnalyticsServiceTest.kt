package com.okestro.okchat.chat.service

import com.okestro.okchat.chat.repository.ChatInteractionRepository
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("ChatAnalyticsService 단위 테스트")
class ChatAnalyticsServiceTest {

    private lateinit var chatInteractionRepository: ChatInteractionRepository
    private lateinit var chatAnalyticsService: ChatAnalyticsService

    private val startDate = LocalDateTime.of(2025, 1, 1, 0, 0)
    private val endDate = LocalDateTime.of(2025, 1, 31, 23, 59)

    @BeforeEach
    fun setUp() {
        chatInteractionRepository = mockk()
        chatAnalyticsService = ChatAnalyticsService(chatInteractionRepository)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("getDailyUsageStats - 일일 사용 통계")
    inner class GetDailyUsageStatsTests {

        @Test
        @DisplayName("전체 상호작용 수와 평균 응답 시간 반환")
        fun `should return total interactions and average response time`() = runTest {
            // given
            every { chatInteractionRepository.countByCreatedAtBetween(startDate, endDate) } returns 150L
            every { chatInteractionRepository.getAverageResponseTime(startDate, endDate) } returns 1200.5

            // when
            val result = chatAnalyticsService.getDailyUsageStats(startDate, endDate)

            // then
            result.totalInteractions shouldBe 150L
            result.averageResponseTime shouldBe 1200.5
            verify(exactly = 1) { chatInteractionRepository.countByCreatedAtBetween(startDate, endDate) }
            verify(exactly = 1) { chatInteractionRepository.getAverageResponseTime(startDate, endDate) }
        }

        @Test
        @DisplayName("평균 응답 시간이 null이면 0.0 반환")
        fun `should return 0 for average response time when null`() = runTest {
            // given
            every { chatInteractionRepository.countByCreatedAtBetween(startDate, endDate) } returns 0L
            every { chatInteractionRepository.getAverageResponseTime(startDate, endDate) } returns null

            // when
            val result = chatAnalyticsService.getDailyUsageStats(startDate, endDate)

            // then
            result.totalInteractions shouldBe 0L
            result.averageResponseTime shouldBe 0.0
        }
    }

    @Nested
    @DisplayName("getQualityTrendStats Tests")
    inner class GetQualityTrendStatsTests {

        @Test
        @DisplayName("should calculate average rating and helpful percentage")
        fun `should calculate average rating and helpful percentage`() = runTest {
            // given
            every { chatInteractionRepository.getAverageRating(startDate, endDate) } returns 4.2
            every { chatInteractionRepository.countByCreatedAtBetween(startDate, endDate) } returns 100L
            every { chatInteractionRepository.countHelpfulInteractions(startDate, endDate) } returns 70L
            every { chatInteractionRepository.countInteractionsWithFeedback(startDate, endDate) } returns 80L

            // when
            val result = chatAnalyticsService.getQualityTrendStats(startDate, endDate)

            // then
            result.averageRating shouldBe 4.2
            result.helpfulPercentage shouldBe 87.5 // 70 / 80 * 100
            result.totalInteractions shouldBe 100L
            result.dateRange.startDate shouldBe startDate
            result.dateRange.endDate shouldBe endDate
        }

        @Test
        @DisplayName("should return 0 helpful percentage when no feedback")
        fun `should return 0 helpful percentage when no feedback`() = runTest {
            // given
            every { chatInteractionRepository.getAverageRating(startDate, endDate) } returns 0.0
            every { chatInteractionRepository.countByCreatedAtBetween(startDate, endDate) } returns 50L
            every { chatInteractionRepository.countHelpfulInteractions(startDate, endDate) } returns 0L
            every { chatInteractionRepository.countInteractionsWithFeedback(startDate, endDate) } returns 0L

            // when
            val result = chatAnalyticsService.getQualityTrendStats(startDate, endDate)

            // then
            result.helpfulPercentage shouldBe 0.0
        }

        @Test
        @DisplayName("should return 0 for average rating when null")
        fun `should return 0 for average rating when null`() = runTest {
            // given
            every { chatInteractionRepository.getAverageRating(startDate, endDate) } returns null
            every { chatInteractionRepository.countByCreatedAtBetween(startDate, endDate) } returns 20L
            every { chatInteractionRepository.countHelpfulInteractions(startDate, endDate) } returns 10L
            every { chatInteractionRepository.countInteractionsWithFeedback(startDate, endDate) } returns 15L

            // when
            val result = chatAnalyticsService.getQualityTrendStats(startDate, endDate)

            // then
            result.averageRating shouldBe 0.0
        }
    }

    @Nested
    @DisplayName("getPerformanceMetrics - 성능 메트릭")
    inner class GetPerformanceMetricsTests {

        @Test
        @DisplayName("평균 응답 시간과 에러율 반환")
        fun `should return average response time and error rate`() = runTest {
            // given
            every { chatInteractionRepository.getAverageResponseTime(startDate, endDate) } returns 850.3

            // when
            val result = chatAnalyticsService.getPerformanceMetrics(startDate, endDate)

            // then
            result.averageResponseTimeMs shouldBe 850.3
            result.errorRate shouldBe 0.0 // Error tracking is disabled
            result.dateRange.startDate shouldBe startDate
            result.dateRange.endDate shouldBe endDate
        }

        @Test
        @DisplayName("평균 응답 시간이 null이면 0.0 반환")
        fun `should return 0 for average response time when null`() = runTest {
            // given
            every { chatInteractionRepository.getAverageResponseTime(startDate, endDate) } returns null

            // when
            val result = chatAnalyticsService.getPerformanceMetrics(startDate, endDate)

            // then
            result.averageResponseTimeMs shouldBe 0.0
        }
    }

    @Nested
    @DisplayName("getQueryTypeStats Tests")
    inner class GetQueryTypeStatsTests {

        @Test
        @DisplayName("should return statistics by query type")
        fun `should return statistics by query type`() = runTest {
            // given
            val mockStats = listOf(
                QueryTypeStatProjection(
                    queryType = "DOCUMENT_SEARCH",
                    count = 80L,
                    avgRating = 4.5,
                    avgResponseTime = 1100.0
                ),
                QueryTypeStatProjection(
                    queryType = "KEYWORD",
                    count = 50L,
                    avgRating = 4.2,
                    avgResponseTime = 900.0
                ),
                QueryTypeStatProjection(
                    queryType = "GENERAL",
                    count = 20L,
                    avgRating = null,
                    avgResponseTime = 700.0
                )
            )
            every { chatInteractionRepository.getQueryTypeStats(startDate, endDate) } returns mockStats

            // when
            val result = chatAnalyticsService.getQueryTypeStats(startDate, endDate)

            // then
            result.size shouldBe 3

            result[0].queryType shouldBe "DOCUMENT_SEARCH"
            result[0].count shouldBe 80L
            result[0].averageRating shouldBe 4.5
            result[0].averageResponseTime shouldBe 1100.0

            result[1].queryType shouldBe "KEYWORD"
            result[1].count shouldBe 50L
            result[1].averageRating shouldBe 4.2
            result[1].averageResponseTime shouldBe 900.0

            result[2].queryType shouldBe "GENERAL"
            result[2].count shouldBe 20L
            result[2].averageRating shouldBe 0.0 // null converted to 0.0
            result[2].averageResponseTime shouldBe 700.0
        }

        @Test
        @DisplayName("should return empty list when no stats")
        fun `should return empty list when no stats`() = runTest {
            // given
            every { chatInteractionRepository.getQueryTypeStats(startDate, endDate) } returns emptyList()

            // when
            val result = chatAnalyticsService.getQueryTypeStats(startDate, endDate)

            // then
            result.size shouldBe 0
        }
    }

    // Mock projection implementation for testing
    private data class QueryTypeStatProjection(
        override val queryType: String,
        override val count: Long,
        override val avgRating: Double?,
        override val avgResponseTime: Double
    ) : ChatInteractionRepository.QueryTypeStats
}
