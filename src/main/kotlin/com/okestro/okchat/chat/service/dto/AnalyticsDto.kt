package com.okestro.okchat.chat.service.dto

import java.time.LocalDateTime

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
