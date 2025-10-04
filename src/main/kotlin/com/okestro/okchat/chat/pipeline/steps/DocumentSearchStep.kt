package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.OptionalChatPipelineStep
import com.okestro.okchat.search.service.DocumentSearchService
import com.okestro.okchat.search.service.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Search for relevant documents
 * Uses multi-strategy search (keyword, title, content)
 */
@Component
@Order(1)
class DocumentSearchStep(
    private val documentSearchService: DocumentSearchService
) : OptionalChatPipelineStep {

    companion object {
        private const val MAX_SEARCH_RESULTS = 200
        private const val KEYWORD_MATCH_BOOST = 1.2
        private const val ADDITIONAL_KEYWORD_BOOST = 0.5
        private const val TITLE_MATCH_BOOST = 1.3
        private const val CONTENT_MATCH_BOOST = 1.1
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Starting multi-strategy search" }

        val allResults = mutableMapOf<String, SearchResult>()
        val allKeywords = context.getAllKeywords()

        // Strategy 1: Keyword-based search
        searchByKeywords(allKeywords, allResults)

        // Strategy 2: Title-based search (for date queries)
        if (!context.dateKeywords.isNullOrEmpty()) {
            searchByTitle(context.userMessage, allResults)
        }

        // Strategy 3: Content-based semantic search
        searchByContent(context.userMessage, allResults)

        // Sort and limit results
        val combinedResults = allResults.values
            .sortedByDescending { it.score }
            .take(MAX_SEARCH_RESULTS)

        log.info { "[${getStepName()}] Found ${combinedResults.size} unique documents" }
        log.info { "[${getStepName()}] Top 5 scores: ${combinedResults.take(5).map { "%.2f".format(it.score) }}" }

        return context.copy(searchResults = combinedResults)
    }

    private suspend fun searchByKeywords(
        keywords: List<String>,
        results: MutableMap<String, SearchResult>
    ) {
        if (keywords.isEmpty()) return

        log.info { "  [Keyword Search] Searching ${keywords.size} keywords individually" }

        keywords.forEach { keyword ->
            val keywordResults = documentSearchService.searchByKeywords(keyword, MAX_SEARCH_RESULTS)

            keywordResults.forEach { result ->
                val existing = results[result.id]
                if (existing == null) {
                    results[result.id] = result.copy(score = result.score * KEYWORD_MATCH_BOOST)
                } else {
                    val boostedScore = existing.score + (result.score * ADDITIONAL_KEYWORD_BOOST)
                    results[result.id] = existing.copy(score = boostedScore)
                }
            }
        }

        log.info { "    Found ${results.size} unique documents from keyword search" }
    }

    private suspend fun searchByTitle(
        query: String,
        results: MutableMap<String, SearchResult>
    ) {
        log.info { "  [Title Search] Searching titles" }

        val titleResults = documentSearchService.searchByTitle(query, MAX_SEARCH_RESULTS)
        titleResults.forEach { result ->
            val existing = results[result.id]
            if (existing == null || result.score > existing.score) {
                results[result.id] = result.copy(score = result.score * TITLE_MATCH_BOOST)
            }
        }

        log.info { "    Found ${titleResults.size} results from title search" }
    }

    private suspend fun searchByContent(
        query: String,
        results: MutableMap<String, SearchResult>
    ) {
        log.info { "  [Content Search] Semantic search on content" }

        val contentResults = documentSearchService.searchByContent(query, MAX_SEARCH_RESULTS)
        contentResults.forEach { result ->
            val existing = results[result.id]
            if (existing == null || result.score > existing.score) {
                results[result.id] = result.copy(score = result.score * CONTENT_MATCH_BOOST)
            }
        }

        log.info { "    Found ${contentResults.size} results from content search" }
    }

    override fun getStepName(): String = "Document Search"
}
