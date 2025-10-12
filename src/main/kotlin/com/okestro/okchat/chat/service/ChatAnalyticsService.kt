package com.okestro.okchat.chat.service

import com.okestro.okchat.chat.repository.ChatInteractionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Chat analytics service
 * * Provides comprehensive analytics for chatbot interactions including:
 * - Daily usage statistics (interaction count, response time)
 * - Quality trends (user ratings, helpful feedback)
 * - Performance metrics (response time, error rate)
 * - Query type analysis
 */
@Service
class ChatAnalyticsService(
    private val chatInteractionRepository: ChatInteractionRepository
) {

    /**
     * Get daily usage statistics for a given period
     * * @param startDate Period start date
     * @param endDate Period end date
     * @return Daily usage statistics including total interactions and average response time
     */
    suspend fun getDailyUsageStats(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): DailyUsageStats = withContext(Dispatchers.IO) {
        DailyUsageStats(
            totalInteractions = chatInteractionRepository.countByCreatedAtBetween(startDate, endDate),
            averageResponseTime = chatInteractionRepository.getAverageResponseTime(startDate, endDate) ?: 0.0
        )
    }

    /**
     * Get quality trend statistics for a given period
     * * Analyzes user feedback to calculate:
     * - Average rating (1-5 scale)
     * - Helpful feedback percentage
     * * @param startDate Period start date
     * @param endDate Period end date
     * @return Quality trend statistics
     */
    suspend fun getQualityTrendStats(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): QualityTrendStats = withContext(Dispatchers.IO) {
        val avgRating = chatInteractionRepository.getAverageRating(startDate, endDate) ?: 0.0
        val totalInteractions = chatInteractionRepository.countByCreatedAtBetween(startDate, endDate)

        // Calculate helpful percentage at application level to avoid complex nested queries
        val helpfulCount = chatInteractionRepository.countHelpfulInteractions(startDate, endDate)
        val feedbackCount = chatInteractionRepository.countInteractionsWithFeedback(startDate, endDate)
        val helpfulPercentage = if (feedbackCount > 0) {
            (helpfulCount.toDouble() / feedbackCount) * 100.0
        } else {
            0.0
        }

        QualityTrendStats(
            averageRating = avgRating,
            helpfulPercentage = helpfulPercentage,
            totalInteractions = totalInteractions,
            dateRange = DateRange(startDate, endDate)
        )
    }

    /**
     * Get performance metrics for a given period
     * * @param startDate Period start date
     * @param endDate Period end date
     * @return Performance metrics including response time and error rate
     */
    suspend fun getPerformanceMetrics(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): PerformanceMetrics = withContext(Dispatchers.IO) {
        val avgResponseTime = chatInteractionRepository.getAverageResponseTime(startDate, endDate) ?: 0.0
        val errorRate = calculateErrorRate(startDate, endDate)

        PerformanceMetrics(
            averageResponseTimeMs = avgResponseTime,
            errorRate = errorRate,
            dateRange = DateRange(startDate, endDate)
        )
    }

    /**
     * Get statistics grouped by query type
     * * Analyzes interactions by query type (DOCUMENT_SEARCH, KEYWORD, etc.)
     * * @param startDate Period start date
     * @param endDate Period end date
     * @return List of statistics per query type
     */
    suspend fun getQueryTypeStats(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<QueryTypeStat> = withContext(Dispatchers.IO) {
        val stats = chatInteractionRepository.getQueryTypeStats(startDate, endDate)
        stats.map {
            QueryTypeStat(
                queryType = it.queryType,
                count = it.count,
                averageRating = it.avgRating ?: 0.0,
                averageResponseTime = it.avgResponseTime
            )
        }
    }

    /**
     * Calculate error rate (currently disabled as error tracking was removed)
     */
    @Suppress("UNUSED_PARAMETER")
    private fun calculateErrorRate(startDate: LocalDateTime, endDate: LocalDateTime): Double {
        // Error tracking removed - always return 0
        return 0.0
    }
}

// ============================================
// Analytics DTOs
// ============================================

/**
 * Daily usage statistics
 * * @property totalInteractions Total number of chat interactions
 * @property averageResponseTime Average response time in milliseconds
 */
data class DailyUsageStats(
    val totalInteractions: Long,
    val averageResponseTime: Double
)

/**
 * Quality trend statistics
 * * @property averageRating Average user rating (1-5 scale)
 * @property helpfulPercentage Percentage of interactions marked as helpful
 * @property totalInteractions Total interactions in the period
 * @property dateRange Date range for this statistic
 */
data class QualityTrendStats(
    val averageRating: Double,
    val helpfulPercentage: Double,
    val totalInteractions: Long,
    val dateRange: DateRange
)

/**
 * Performance metrics
 * * @property averageResponseTimeMs Average AI response time in milliseconds
 * @property errorRate Error rate percentage (currently always 0 as error tracking is disabled)
 * @property dateRange Date range for this statistic
 */
data class PerformanceMetrics(
    val averageResponseTimeMs: Double,
    val errorRate: Double,
    val dateRange: DateRange
)

/**
 * Statistics for a specific query type
 * * @property queryType Type of query (e.g., DOCUMENT_SEARCH, KEYWORD, GENERAL)
 * @property count Number of interactions for this type
 * @property averageRating Average user rating for this type
 * @property averageResponseTime Average response time for this type in milliseconds
 */
data class QueryTypeStat(
    val queryType: String,
    val count: Long,
    val averageRating: Double,
    val averageResponseTime: Double
)

/**
 * Date range for analytics queries
 */
data class DateRange(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)
