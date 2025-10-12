package com.okestro.okchat.ai.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Type-safe representations of tool inputs and outputs.
 * Replaces untyped Map<String, Any> usage for better type safety.
 */

/**
 * Standard tool output format with thought and answer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ToolOutput(
    @JsonProperty("thought")
    val thought: String = "No thought provided.",

    @JsonProperty("answer")
    val answer: String
)
