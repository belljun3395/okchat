package com.okestro.okchat.chat.interceptor

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition

/**
 * Wrapper for ToolCallback that logs all tool invocations
 */
class LoggingToolCallback(
    private val delegate: ToolCallback
) : ToolCallback {

    private val log = KotlinLogging.logger {}

    override fun getToolDefinition(): ToolDefinition = delegate.toolDefinition

    override fun call(toolInput: String): String {
        val toolName = delegate.toolDefinition.name()
        log.info { "🔧 [Tool Called] $toolName" }
        log.info { "   Input: $toolInput" }

        val startTime = System.currentTimeMillis()
        val result = try {
            delegate.call(toolInput).also { output ->
                val duration = System.currentTimeMillis() - startTime
                log.info { "✅ [Tool Completed] $toolName (${duration}ms)" }
                log.info { "   Output: ${output.take(500)}${if (output.length > 500) "..." else ""}" }
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(e) { "❌ [Tool Failed] $toolName (${duration}ms): ${e.message}" }
            throw e
        }

        return result
    }
}
