package com.okestro.okchat.ai.config

import com.okestro.okchat.ai.tools.interceptor.LoggingToolCallback
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.tool.ToolCallback
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ToolConfig {
    /**
     * Automatically wrap all ToolCallback beans with LoggingToolCallback
     */
    @Bean
    fun toolCallbackLoggingPostProcessor() = ToolCallbackLoggingPostProcessor()

    class ToolCallbackLoggingPostProcessor : BeanPostProcessor {
        private val log = KotlinLogging.logger {}

        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
            if (bean is ToolCallback && bean !is LoggingToolCallback) {
                log.info { "Wrapping ToolCallback bean: $beanName" }
                return LoggingToolCallback(bean)
            }
            return bean
        }
    }
}
