package com.okestro.okchat.chat.pipeline

import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.search.service.SearchResult

/**
 * Context object that flows through the chat processing pipeline
 * Each step can read from and write to this context
 */
data class ChatContext(
    // Input
    val userMessage: String,
    val providedKeywords: List<String>? = null,

    // Query Analysis
    val queryAnalysis: QueryClassifier.QueryAnalysis? = null,
    val extractedKeywords: List<String>? = null,
    val dateKeywords: List<String>? = null,

    // Search Results
    val searchResults: List<SearchResult>? = null,

    // Context Building
    val contextText: String? = null,

    // Prompt Generation
    val promptText: String? = null
) {
    /**
     * Get all keywords (extracted + date keywords)
     */
    fun getAllKeywords(): List<String> {
        return ((extractedKeywords ?: emptyList()) + (dateKeywords ?: emptyList())).distinct()
    }
}
