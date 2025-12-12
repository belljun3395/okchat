package com.okestro.okchat.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * WebFlux RED metrics filter.
 *
 * - Rate:     http_requests_total
 * - Error:    http_requests_errors_total
 * - Duration: http_request_duration_seconds
 *
 * Path tag is normalized to avoid high cardinality.
 */
@Configuration
class MetricsWebFilter(
    private val meterRegistry: MeterRegistry
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val startNanos = System.nanoTime()
        val method = exchange.request.method?.name() ?: "UNKNOWN"
        val rawPath = exchange.request.path.pathWithinApplication().value()
        val errorRef = AtomicReference<Throwable?>(null)

        return chain.filter(exchange)
            .doOnError { errorRef.set(it) }
            .doFinally {
                val duration = Duration.ofNanos(System.nanoTime() - startNanos)
                val statusCode = resolveStatus(exchange, errorRef.get())
                val pathTag = resolvePathTag(exchange, rawPath)

                val tags = listOf(
                    Tag.of("method", method),
                    Tag.of("path", pathTag),
                    Tag.of("status", statusCode.toString())
                )

                meterRegistry.counter("http_requests_total", tags).increment()

                if (statusCode >= 500 || errorRef.get() != null) {
                    val errorType = errorRef.get()?.javaClass?.simpleName ?: "HTTP_$statusCode"
                    meterRegistry.counter(
                        "http_requests_errors_total",
                        tags + Tag.of("error_type", errorType)
                    ).increment()
                }

                Timer.builder("http_request_duration_seconds")
                    .description("HTTP request duration (seconds)")
                    .publishPercentileHistogram()
                    .tags(tags)
                    .register(meterRegistry)
                    .record(duration)
            }
    }

    private fun resolveStatus(exchange: ServerWebExchange, error: Throwable?): Int {
        val status = exchange.response.statusCode?.value()
        if (status != null) return status
        return if (error != null) HttpStatus.INTERNAL_SERVER_ERROR.value() else HttpStatus.OK.value()
    }

    private fun resolvePathTag(exchange: ServerWebExchange, rawPath: String): String {
        val pattern = exchange.getAttribute<String>(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
        return pattern ?: normalizePath(rawPath)
    }

    private val uuidRegex =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val numberRegex = Regex("^\\d+$")

    private fun normalizePath(path: String): String {
        val segments = path.split("/").filter { it.isNotBlank() }
        val normalized = segments.map { seg ->
            when {
                uuidRegex.matches(seg) -> "{uuid}"
                numberRegex.matches(seg) -> "{id}"
                else -> seg
            }
        }
        return "/" + normalized.joinToString("/")
    }
}
