package com.okestro.okchat.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.nio.NioEventLoopGroup
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

/**
 * WebFlux configuration for performance optimization
 * Optimizes Netty server, codecs, and resource handling
 */
@Configuration
@EnableWebFlux
class WebFluxConfig(
    private val objectMapper: ObjectMapper
) : WebFluxConfigurer {

    /**
     * Configure server codecs with optimized buffer sizes
     */
    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().apply {
            // Increase max in-memory size for large AI responses
            maxInMemorySize(10 * 1024 * 1024) // 10MB
            
            // Use custom Jackson codecs for better performance
            jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON))
            jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
        }
    }

    /**
     * Configure static resource handling with caching
     */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(3600) // 1 hour cache
    }

    /**
     * Configure argument resolvers for better request handling
     */
    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        // Add custom argument resolvers if needed
    }

    /**
     * Customize Netty server configuration for better performance
     */
    @Bean
    fun nettyServerCustomizer(): WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            factory.addServerCustomizers { server ->
                server.wiretap(false) // Disable wiretap for production
                server.compress(true) // Enable compression
                
                // Configure event loop group for better CPU utilization
                val eventLoopGroup = NioEventLoopGroup(
                    Runtime.getRuntime().availableProcessors() * 2,
                    CustomThreadFactory("netty-nio")
                )
                server.runOn(eventLoopGroup)
            }
        }
    }

    /**
     * Custom thread factory for better thread naming
     */
    private class CustomThreadFactory(private val prefix: String) : ThreadFactory {
        private val counter = AtomicInteger(0)
        
        override fun newThread(r: Runnable): Thread {
            return Thread(r, "$prefix-${counter.incrementAndGet()}").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
            }
        }
    }
}