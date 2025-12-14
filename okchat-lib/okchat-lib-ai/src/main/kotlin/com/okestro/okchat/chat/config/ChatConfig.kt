
package com.okestro.okchat.chat.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
class ChatConfig {

    @Bean
    fun chatClient(
        builder: ChatClient.Builder
    ): ChatClient {
        return builder
            .build()
    }
}
