package com.okestro.okchat.confluence.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.confluence.client.ConfluenceClient
import feign.Feign
import feign.Logger
import feign.RequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Base64

@Configuration
@EnableConfigurationProperties(ConfluenceProperties::class)
class ConfluenceConfig {
    private val log = KotlinLogging.logger {}
    init {
        log.info { "Confluence client configuration enabled" }
    }

    @Bean
    fun confluenceAuthInterceptor(properties: ConfluenceProperties): RequestInterceptor {
        return RequestInterceptor { template ->
            when (properties.auth.type) {
                ConfluenceProperties.AuthType.BASIC -> {
                    val email = properties.auth.email
                    val apiToken = properties.auth.apiToken
                    if (email != null && apiToken != null) {
                        val credentials = "$email:$apiToken"
                        val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
                        template.header("Authorization", "Basic $encodedCredentials")
                    }
                }
            }
        }
    }

    @Bean
    fun confluenceClient(
        properties: ConfluenceProperties,
        authInterceptor: RequestInterceptor,
        objectMapper: ObjectMapper
    ): ConfluenceClient {
        return Feign.builder()
            .encoder(JacksonEncoder(objectMapper))
            .decoder(JacksonDecoder(objectMapper))
            .logger(Slf4jLogger(ConfluenceClient::class.java))
            .logLevel(Logger.Level.FULL)
            .requestInterceptor(authInterceptor)
            .target(ConfluenceClient::class.java, properties.baseUrl)
    }
}
