package com.okestro.okchat.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    fun corsWebFilter(): org.springframework.web.cors.reactive.CorsWebFilter {
        val config = org.springframework.web.cors.CorsConfiguration()
        config.allowedOrigins = listOf("http://localhost:5173", "http://localhost:3000")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true // Let's try true

        val source = org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)

        return org.springframework.web.cors.reactive.CorsWebFilter(source)
    }
}
