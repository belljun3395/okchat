package com.okestro.okchat.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for RAG (Retrieval-Augmented Generation) system
 * Enables externalized configuration from application.yaml
 */
@Configuration
@EnableConfigurationProperties(RagProperties::class)
class RagConfig
