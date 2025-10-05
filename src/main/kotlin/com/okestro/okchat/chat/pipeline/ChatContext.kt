package com.okestro.okchat.chat.pipeline

import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.search.model.SearchResult

/**
 * Context object that flows through the chat processing pipeline
 * Each step can read from and write to this context
 * * Design principles:
 * - Fields from FirstChatPipelineStep (required): Can be non-null in later stages
 * - Fields from OptionalChatPipelineStep: Must be nullable
 * - Fields from LastChatPipelineStep: Must be nullable (only set at end)
 */
data class ChatContext(
    // ═══ Input (from user) ═══
    val userMessage: String,
    val providedKeywords: List<String>? = null,

    // ═══ Query Analysis (FirstChatPipelineStep - always available after first step) ═══
    val queryAnalysis: QueryClassifier.QueryAnalysis? = null,
    val extractedKeywords: List<String>? = null,
    val dateKeywords: List<String>? = null,

    // ═══ Document Search (OptionalChatPipelineStep - may not be executed) ═══
    val searchResults: List<SearchResult>? = null,

    // ═══ Context Building (OptionalChatPipelineStep - may not be executed) ═══
    val contextText: String? = null,

    // ═══ Prompt Generation (LastChatPipelineStep - always executed) ═══
    val promptText: String? = null
) {
    /**
     * Get all keywords (extracted + date keywords)
     */
    fun getAllKeywords(): List<String> {
        return ((extractedKeywords ?: emptyList()) + (dateKeywords ?: emptyList())).distinct()
    }
}
