package com.okestro.okchat.chat.pipeline

import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.search.model.SearchResult

/**
 * Context object that flows through the chat processing pipeline
 * Each step can read from and write to this context
 *
 * Design principles:
 * - UserInput: Always required (from user)
 * - Analysis: Always required (FirstChatPipelineStep)
 * - Search: Optional (OptionalChatPipelineStep)
 * - Prompt: Required at end (LastChatPipelineStep)
 */
open class ChatContext(
    val input: UserInput,
    val analysis: Analysis? = null,
    val search: Search? = null
) {
    /**
     * User input data
     */
    data class UserInput(
        val message: String,
        val providedKeywords: List<String> = emptyList()
    )

    /**
     * Query analysis result (FirstChatPipelineStep - always executed)
     */
    data class Analysis(
        val queryAnalysis: QueryClassifier.QueryAnalysis,
        val extractedTitles: List<String>,
        val extractedContents: List<String>,
        val extractedPaths: List<String>,
        val extractedKeywords: List<String>,
        val dateKeywords: List<String>
    ) {
        fun getAllKeywords(): List<String> {
            return (extractedKeywords + dateKeywords).distinct()
        }
    }

    /**
     * Search context (OptionalChatPipelineStep - may not be executed)
     */
    data class Search(
        val results: List<SearchResult>,
        val contextText: String? = null
    )
}
