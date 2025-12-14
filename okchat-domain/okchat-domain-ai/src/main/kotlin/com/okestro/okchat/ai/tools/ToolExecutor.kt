package com.okestro.okchat.ai.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.ai.model.dto.ToolOutput
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Utility object for executing tool operations with standardized error handling.
 * Eliminates code duplication across tool implementations.
 *
 * Common responsibilities:
 * - Consistent error handling
 * - ToolOutput serialization
 * - Logging integration
 */
object ToolExecutor {
    val log = KotlinLogging.logger {}

    /**
     * Execute a tool operation with standardized error handling.
     *
     * @param toolName Name of the tool (for logging)
     * @param log Logger instance
     * @param objectMapper Jackson ObjectMapper for serialization
     * @param errorThought Default thought message when error occurs
     * @param operation The actual tool logic to execute
     * @return Serialized ToolOutput as JSON string
     */
    inline fun execute(
        toolName: String,
        objectMapper: ObjectMapper,
        errorThought: String,
        operation: () -> ToolOutput
    ): String {
        return try {
            val output = operation()
            objectMapper.writeValueAsString(output)
        } catch (e: Exception) {
            log.error(e) { "[$toolName] Error: ${e.message}" }
            objectMapper.writeValueAsString(
                ToolOutput(
                    thought = errorThought,
                    answer = "Error: ${e.message}"
                )
            )
        }
    }
}
