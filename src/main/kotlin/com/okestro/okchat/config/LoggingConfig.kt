package com.okestro.okchat.config

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.UUID

/**
 * Logging configuration for multi-server environment
 * Adds request tracking using MDC (Mapped Diagnostic Context)
 */
@Configuration
class LoggingConfig {

    companion object {
        const val REQUEST_ID_KEY = "requestId"
        const val USER_ID_KEY = "userId"
    }

    /**
     * WebFilter to add request ID to MDC for each request
     * This enables request tracking across distributed systems
     */
    @Bean
    fun mdcWebFilter(): WebFilter {
        return WebFilter { exchange: ServerWebExchange, chain: WebFilterChain ->
            val requestId = exchange.request.headers.getFirst("X-Request-ID")
                ?: UUID.randomUUID().toString()

            // Add request ID to response header
            exchange.response.headers.add("X-Request-ID", requestId)

            // Store in reactor context for reactive flows
            chain.filter(exchange)
                .contextWrite { context: Context ->
                    context.put(REQUEST_ID_KEY, requestId)
                }
                .doOnEach { signal ->
                    // Populate MDC for each signal
                    if (signal.isOnNext || signal.isOnError || signal.isOnComplete) {
                        signal.contextView.getOrEmpty<String>(REQUEST_ID_KEY)
                            .ifPresent { id -> MDC.put(REQUEST_ID_KEY, id) }
                    }
                }
                .doFinally {
                    // Clear MDC after request completion
                    MDC.clear()
                }
        }
    }
}

/**
 * Extension function to get request ID from reactor context
 */
fun Context.getRequestId(): String? {
    return getOrEmpty<String>(LoggingConfig.REQUEST_ID_KEY).orElse(null)
}

/**
 * Extension function to add request ID to reactor context
 */
fun Mono<*>.withRequestId(requestId: String): Mono<*> {
    return this.contextWrite { ctx ->
        ctx.put(LoggingConfig.REQUEST_ID_KEY, requestId)
    }
}
