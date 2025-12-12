package com.okestro.okchat.config

import com.okestro.okchat.chat.repository.ChatInteractionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private val log = KotlinLogging.logger {}

@ConfigurationProperties(prefix = "slo")
data class SloProperties(
    val availabilityTarget: Double = 99.9,
    val latencyP95TargetMs: Double = 500.0,
    val qualityHelpfulTarget: Double = 80.0
)

@EnableConfigurationProperties(SloProperties::class)
@Component
class SloMetricsTask(
    private val meterRegistry: MeterRegistry,
    private val chatInteractionRepository: ChatInteractionRepository,
    private val sloProperties: SloProperties
) {

    private val availability = AtomicReference(100.0)
    private val availabilityErrorBudgetRemaining = AtomicReference(100.0)
    private val latencyP95Ms = AtomicReference(0.0)
    private val qualityHelpfulPercentage = AtomicReference(0.0)

    init {
        Gauge.builder("slo_availability_percentage") { availability.get() }
            .description("Availability SLI based on chat.requests.total counters")
            .register(meterRegistry)

        Gauge.builder("slo_availability_error_budget_remaining") { availabilityErrorBudgetRemaining.get() }
            .description("Remaining error budget percentage for availability SLO")
            .register(meterRegistry)

        Gauge.builder("slo_latency_p95_ms") { latencyP95Ms.get() }
            .description("Chat latency P95 in milliseconds")
            .register(meterRegistry)

        Gauge.builder("slo_quality_helpful_percentage") { qualityHelpfulPercentage.get() }
            .description("Helpful percentage SLI (last 24h)")
            .register(meterRegistry)
    }

    @Scheduled(fixedRateString = "\${slo.update-interval-ms:60000}", initialDelayString = "\${slo.initial-delay-ms:10000}")
    fun updateSloMetrics() {
        try {
            updateAvailability()
            updateLatency()
            updateQuality()
        } catch (e: Exception) {
            log.warn(e) { "[SLO] Failed to update SLO metrics: ${e.message}" }
        }
    }

    private fun updateAvailability() {
        val successCount = meterRegistry.find("chat.requests.total")
            .tag("status", "success")
            .counter()
            ?.count() ?: 0.0
        val failureCount = meterRegistry.find("chat.requests.total")
            .tag("status", "failure")
            .counter()
            ?.count() ?: 0.0
        val total = successCount + failureCount
        val availabilityPct = if (total > 0) (successCount / total) * 100.0 else 100.0

        availability.set(availabilityPct)
        availabilityErrorBudgetRemaining.set(calculateErrorBudgetRemaining(availabilityPct, sloProperties.availabilityTarget))

        log.debug { "[SLO] Availability=${"%.3f".format(availabilityPct)}%, budget=${"%.1f".format(availabilityErrorBudgetRemaining.get())}%" }
    }

    private fun updateLatency() {
        val timer = meterRegistry.find("chat.response.time").timer()
        val snapshot = timer?.takeSnapshot()
        val p95 = snapshot?.percentileValues()
            ?.firstOrNull { it.percentile() == 0.95 }
            ?.value(TimeUnit.MILLISECONDS)
            ?: 0.0

        latencyP95Ms.set(p95)
        log.debug { "[SLO] Latency P95=${"%.1f".format(p95)}ms (target=${sloProperties.latencyP95TargetMs}ms)" }
    }

    private fun updateQuality() {
        val now = LocalDateTime.now()
        val oneDayAgo = now.minusDays(1)
        val helpfulCount = chatInteractionRepository.countHelpfulInteractions(oneDayAgo, now)
        val feedbackCount = chatInteractionRepository.countInteractionsWithFeedback(oneDayAgo, now)

        val helpfulPct = if (feedbackCount > 0) {
            (helpfulCount.toDouble() / feedbackCount) * 100.0
        } else {
            0.0
        }
        qualityHelpfulPercentage.set(helpfulPct)
        log.debug { "[SLO] Helpful=${"%.1f".format(helpfulPct)}% (target=${sloProperties.qualityHelpfulTarget}%)" }
    }

    private fun calculateErrorBudgetRemaining(current: Double, target: Double): Double {
        return if (current >= target) {
            ((current - target) / (100.0 - target)) * 100.0
        } else {
            0.0
        }
    }
}
