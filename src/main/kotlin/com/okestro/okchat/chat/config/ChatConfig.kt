
package com.okestro.okchat.chat.config

import com.okestro.okchat.chat.interceptor.LoggingToolCallback
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.tool.ToolCallback
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

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

/**
 * Automatically wrap all ToolCallback beans with LoggingToolCallback
 */
@Component
class ToolCallbackLoggingPostProcessor : BeanPostProcessor {

    private val log = KotlinLogging.logger {}

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is ToolCallback && bean !is LoggingToolCallback) {
            log.info { "🔧 Wrapping ToolCallback bean: $beanName" }
            return LoggingToolCallback(bean)
        }
        return bean
    }
}
