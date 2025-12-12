package com.okestro.okchat.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import io.opentelemetry.sdk.trace.samplers.SamplingResult
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

@ConfigurationProperties(prefix = "management.tracing.adaptive")
data class AdaptiveSamplingProperties(
    val baseRate: Double = 0.1,
    val errorRate: Double = 1.0,
    val slowRate: Double = 0.5,
    val slowThresholdMs: Long = 2000,
    val chatRate: Double = 0.3,
    val adminRate: Double = 1.0
)

@Configuration
@EnableConfigurationProperties(AdaptiveSamplingProperties::class)
class AdaptiveSamplingConfig(
    private val properties: AdaptiveSamplingProperties
) {
    /**
     * Provide a custom OpenTelemetry Sampler bean.
     * Spring Boot auto-configuration will pick this up and skip the default ratio sampler.
     */
    @Bean
    fun otelSampler(): Sampler {
        return AdaptiveSampler(properties)
    }
}
class AdaptiveSampler(
    private val props: AdaptiveSamplingProperties
) : Sampler {

    private val errorKey = AttributeKey.booleanKey("error")
    private val durationKey = AttributeKey.longKey("duration_ms")
    private val httpTargetKey = AttributeKey.stringKey("http.target")
    private val httpRouteKey = AttributeKey.stringKey("http.route")

    override fun shouldSample(
        parentContext: Context,
        traceId: String,
        name: String,
        spanKind: SpanKind,
        attributes: Attributes,
        parentLinks: List<LinkData>
    ): SamplingResult {
        // 1) Explicit error flag (rare at span start, but safe)
        val hasError = attributes.get(errorKey) == true
        if (hasError) {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE)
        }

        // 2) Slow request hint (if duration is pre-populated)
        val duration = attributes.get(durationKey)
        if (duration != null && duration > props.slowThresholdMs) {
            if (Random.nextDouble() < props.slowRate) {
                return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE)
            }
        }

        // 3) Path-based override
        val path = attributes.get(httpRouteKey) ?: attributes.get(httpTargetKey)
        val customRate = getCustomRateForPath(path)
        if (customRate != null) {
            return if (Random.nextDouble() < customRate) {
                SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE)
            } else {
                SamplingResult.create(SamplingDecision.DROP)
            }
        }

        // 4) Default sampling
        return if (Random.nextDouble() < props.baseRate) {
            SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE)
        } else {
            SamplingResult.create(SamplingDecision.DROP)
        }
    }

    private fun getCustomRateForPath(path: String?): Double? {
        return when {
            path?.startsWith("/api/chat") == true -> props.chatRate
            path?.startsWith("/api/admin") == true -> props.adminRate
            else -> null
        }
    }

    override fun getDescription(): String {
        return "AdaptiveSampler(base=${props.baseRate}, slow=${props.slowRate}, chat=${props.chatRate})"
    }
}
