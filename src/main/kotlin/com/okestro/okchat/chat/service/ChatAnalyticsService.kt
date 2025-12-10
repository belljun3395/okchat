package com.okestro.okchat.chat.service

import com.okestro.okchat.chat.repository.ChatInteractionRepository
import com.okestro.okchat.chat.service.dto.DailyUsageStats
import com.okestro.okchat.chat.service.dto.DateRange
import com.okestro.okchat.chat.service.dto.InteractionTimeSeries
import com.okestro.okchat.chat.service.dto.PerformanceMetrics
import com.okestro.okchat.chat.service.dto.QualityTrendStats
import com.okestro.okchat.chat.service.dto.QueryTypeStat
import com.okestro.okchat.chat.service.dto.TimeSeriesDataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
     * Get interaction time series data for chart visualization
     *
     * Returns daily interaction counts for the specified period.
     *
     * @param startDate Period start date
     * @param endDate Period end date
     * @return Time series data with daily interaction counts
     */
    suspend fun getInteractionTimeSeries(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): InteractionTimeSeries = withContext(Dispatchers.IO) {
        val dailyCounts = chatInteractionRepository.getDailyInteractionCounts(startDate, endDate)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        val dataPoints = dailyCounts.map { dailyCount ->
            TimeSeriesDataPoint(
                date = dailyCount.date.format(formatter),
                value = dailyCount.count
            )
        }

        InteractionTimeSeries(
            dataPoints = dataPoints,
            dateRange = DateRange(startDate, endDate)
        )
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
