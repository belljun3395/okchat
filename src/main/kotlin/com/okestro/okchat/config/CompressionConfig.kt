package com.okestro.okchat.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory
import org.springframework.boot.web.server.Compression
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType

private val log = KotlinLogging.logger {}

/**
 * Configuration for HTTP response compression
 * Reduces network bandwidth usage and improves load times
 */
@Configuration
class CompressionConfig {

    @Bean
    fun compressionCustomizer(): WebServerFactoryCustomizer<ReactiveWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            val compression = Compression()
            compression.isEnabled = true
            
            // Set minimum response size for compression (in bytes)
            compression.minResponseSize = 1024
            
            // Configure MIME types to compress
            compression.mimeTypes = arrayOf(
                MediaType.TEXT_HTML_VALUE,
                MediaType.TEXT_XML_VALUE,
                MediaType.TEXT_PLAIN_VALUE,
                MediaType.TEXT_CSS_VALUE,
                MediaType.TEXT_JAVASCRIPT_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                "application/javascript",
                "application/x-javascript",
                "text/javascript",
                "text/json",
                "application/vnd.ms-fontobject",
                "application/x-font-ttf",
                "application/x-font-opentype",
                "application/x-font-truetype",
                "image/svg+xml",
                "image/x-icon",
                "image/vnd.microsoft.icon",
                "font/ttf",
                "font/eot",
                "font/otf",
                "font/woff",
                "font/woff2"
            )
            
            // Apply compression configuration
            factory.setCompression(compression)
            
            log.info { "Configured HTTP compression: enabled=true, minSize=1KB" }
        }
    }
}