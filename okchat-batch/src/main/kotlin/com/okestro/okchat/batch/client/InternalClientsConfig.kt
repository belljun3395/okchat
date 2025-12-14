package com.okestro.okchat.batch.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(InternalServicesProperties::class)
class InternalClientsConfig {

    @Bean
    @Qualifier("userInternalWebClient")
    fun userInternalWebClient(properties: InternalServicesProperties): WebClient {
        return WebClient.builder()
            .baseUrl(properties.user.url)
            .build()
    }

    @Bean
    @Qualifier("docsInternalWebClient")
    fun docsInternalWebClient(properties: InternalServicesProperties): WebClient {
        return WebClient.builder()
            .baseUrl(properties.docs.url)
            .build()
    }

    @Bean
    @Qualifier("aiInternalWebClient")
    fun aiInternalWebClient(properties: InternalServicesProperties): WebClient {
        return WebClient.builder()
            .baseUrl(properties.ai.url)
            .build()
    }
}
