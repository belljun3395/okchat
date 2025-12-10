package com.okestro.okchat.task

import com.okestro.okchat.chat.repository.ChatInteractionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * Scheduled task to update Prometheus metrics for analytics
 * * This task periodically queries the database and updates Prometheus gauges
 * with the latest statistics in a Spring Cloud Task context.
 * * Features:
 * - Periodic execution via @Scheduled
 * - Single instance guarantee via Spring Cloud Task (enabled in TaskConfig)
 * - Automatic failover when primary instance fails
 * - Can be enabled/disabled via application properties
 * * Metrics exposed:
 * - chat_interactions_hourly: Number of interactions in the last hour
 * - chat_interactions_daily: Number of interactions in the last day
 * - chat_response_time_avg_ms: Average response time in the last hour
 * - chat_quality_helpful_percentage: Percentage of helpful responses
 * - chat_quality_avg_rating: Average user rating
 */
@Component
@ConditionalOnProperty(
    name = ["task.metrics-update.enabled"],
    havingValue = "true",
    matchIfMissing = true // Enabled by default
)
class MetricsUpdateTask(
    private val meterRegistry: MeterRegistry,
    private val chatInteractionRepository: ChatInteractionRepository,
    @Value("\${task.metrics-update.hourly-interval:60000}") private val hourlyInterval: Long,
    @Value("\${task.metrics-update.daily-interval:300000}") private val dailyInterval: Long
) : CommandLineRunner {

    /**
     * CommandLineRunner execution (for task initialization)
     */
    override fun run(vararg args: String?) {
        logger.info { "✓ MetricsUpdateTask initialized" }
        logger.info { "  - Hourly metrics interval: ${hourlyInterval}ms" }
        logger.info { "  - Daily metrics interval: ${dailyInterval}ms" }
    }

    /**
     * Update hourly metrics (default: every minute)
     */
    @Scheduled(fixedRateString = "\${task.metrics-update.hourly-interval:60000}", initialDelayString = "\${task.metrics-update.initial-delay:10000}")
    fun updateHourlyMetrics() {
        runBlocking {
            try {
                val now = LocalDateTime.now()
                val oneHourAgo = now.minusHours(1)

                // Hourly interaction count
                val hourlyInteractions = chatInteractionRepository.countByCreatedAtBetween(oneHourAgo, now)
                meterRegistry.gauge(
                    "chat_interactions_hourly",
                    Tags.of("period", "last_hour"),
                    hourlyInteractions.toDouble()
                )

                // Average response time
                val avgResponseTime = chatInteractionRepository.getAverageResponseTime(oneHourAgo, now) ?: 0.0
                meterRegistry.gauge(
                    "chat_response_time_avg_ms",
                    Tags.of("period", "last_hour"),
                    avgResponseTime
                )

                // Helpful percentage (calculate at application level)
                val helpfulCount = chatInteractionRepository.countHelpfulInteractions(oneHourAgo, now)
                val feedbackCount = chatInteractionRepository.countInteractionsWithFeedback(oneHourAgo, now)
                val helpfulPercentage = if (feedbackCount > 0) {
                    (helpfulCount.toDouble() / feedbackCount) * 100.0
                } else {
                    0.0
                }
                meterRegistry.gauge(
                    "chat_quality_helpful_percentage",
                    Tags.of("period", "last_hour"),
                    helpfulPercentage
                )

                val avgRating = chatInteractionRepository.getAverageRating(oneHourAgo, now) ?: 0.0
                meterRegistry.gauge(
                    "chat_quality_avg_rating",
                    Tags.of("period", "last_hour"),
                    avgRating
                )

                logger.info {
                    "✓ Hourly metrics updated: interactions=$hourlyInteractions, " +
                        "avgResponseTime=${"%.0f".format(avgResponseTime)}ms, " +
                        "helpful=${"%.1f".format(helpfulPercentage)}%, avgRating=${"%.2f".format(avgRating)}"
                }
            } catch (e: Exception) {
                logger.error(e) { "Error updating hourly metrics: ${e.message}" }
            }
        }
    }

    /**
     * Update daily metrics (default: every 5 minutes)
     */
    @Scheduled(fixedRateString = "\${task.metrics-update.daily-interval:300000}", initialDelayString = "\${task.metrics-update.initial-delay:10000}")
    fun updateDailyMetrics() {
        runBlocking {
            try {
                val now = LocalDateTime.now()
                val oneDayAgo = now.minusDays(1)

                // Daily interaction count
                val dailyInteractions = chatInteractionRepository.countByCreatedAtBetween(oneDayAgo, now)
                meterRegistry.gauge(
                    "chat_interactions_daily",
                    Tags.of("period", "last_day"),
                    dailyInteractions.toDouble()
                )

                logger.info {
                    "✓ Daily metrics updated: interactions=$dailyInteractions"
                }
            } catch (e: Exception) {
                logger.error(e) { "Error updating daily metrics: ${e.message}" }
            }
        }
    }
}
