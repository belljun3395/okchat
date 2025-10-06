package com.okestro.okchat.ai.interceptor

import com.fasterxml.jackson.databind.ObjectMapper
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
    private val objectMapper = ObjectMapper()

    override fun getToolDefinition(): ToolDefinition = delegate.toolDefinition

    override fun call(toolInput: String): String {
        val toolName = delegate.toolDefinition.name()
        log.info { "[Tool Called] $toolName" }
        log.info { "Input: $toolInput" }

        val startTime = System.currentTimeMillis()
        try {
            val output = delegate.call(toolInput)
            val duration = System.currentTimeMillis() - startTime
            log.info { "[Tool Completed] $toolName (${duration}ms)" }
            try {
                @Suppress("UNCHECKED_CAST")
                val outJson = objectMapper.readValue(output, Map::class.java) as Map<String, Any>
                val thought = outJson["thought"]?.toString() ?: "N/A"
                val answer = outJson["answer"]?.toString() ?: "N/A"
                log.info { "Thought: $thought" }
                log.info { "Answer: ${answer.take(100)}..." }
            } catch (_: Exception) {
                // It's okay if the output is not a JSON map; just log the raw output.
                log.info { "Output: ${output.take(100)}..." }
            }
            return output
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(e) { "[Tool Failed] $toolName (${duration}ms): ${e.message}" }
            throw e
        }
    }
}
