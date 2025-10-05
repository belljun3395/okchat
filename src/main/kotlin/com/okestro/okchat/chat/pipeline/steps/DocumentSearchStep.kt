package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.OptionalChatPipelineStep
import com.okestro.okchat.config.RagProperties
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import com.okestro.okchat.search.service.DocumentSearchService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Search for relevant documents using optimized Typesense multi_search
 * Executes 3 search strategies (keyword, title, content) in a SINGLE HTTP request
 * Dramatically reduces network latency: 3 roundtrips → 1 roundtrip
 * RRF weights are externalized in application.yaml for easy tuning
 */
@Component
@Order(1)
class DocumentSearchStep(
    private val documentSearchService: DocumentSearchService,
    private val ragProperties: RagProperties
) : OptionalChatPipelineStep {

    companion object {
        private const val MAX_SEARCH_RESULTS = 50 // Optimized: reduced from 200 to improve performance
    }

    // RRF parameters loaded from externalized configuration
    // These can be tuned in application.yaml without code changes
    private val rrfK = ragProperties.rrf.k
    private val keywordWeight = ragProperties.rrf.keywordWeight
    private val titleWeight = ragProperties.rrf.titleWeight
    private val contentWeight = ragProperties.rrf.contentWeight

    init {
        log.info { "[DocumentSearchStep] RRF configuration loaded: k=$rrfK, keyword=$keywordWeight, title=$titleWeight, content=$contentWeight" }
    }

    /**
     * Determine if document search should be executed
     * Can be extended to skip search for simple queries (e.g., greetings)
     */
    override fun shouldExecute(context: ChatContext): Boolean {
        // Example: Could skip search for certain query types if needed
        // For now, always execute (default behavior)
        // Future enhancement: Skip for simple greetings or queries that don't need RAG
        return true
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Starting optimized multi-search with RRF" }

        val allKeywords = context.getAllKeywords()
        val keywordsString = allKeywords.joinToString(" ")

        // Execute all 3 search strategies in a SINGLE HTTP request using Typesense multi_search
        // This dramatically reduces network latency (3 roundtrips → 1 roundtrip)
        val (keywordResults, titleResults, contentResults) = documentSearchService.multiSearch(
            query = context.userMessage,
            keywords = keywordsString,
            topK = MAX_SEARCH_RESULTS
        )

        // Sort results by score
        val sortedKeywordResults = keywordResults.sortedByDescending { it.score }
        val sortedTitleResults = titleResults.sortedByDescending { it.score }
        val sortedContentResults = contentResults.sortedByDescending { it.score }

        log.info { "[${getStepName()}] Multi-search completed" }
        log.info { "  - Keyword results: ${sortedKeywordResults.size}" }
        log.info { "  - Title results: ${sortedTitleResults.size}" }
        log.info { "  - Content results: ${sortedContentResults.size}" }

        // Apply Reciprocal Rank Fusion to combine rankings
        val combinedResults = applyRRF(
            keywordResults = sortedKeywordResults,
            titleResults = sortedTitleResults,
            contentResults = sortedContentResults
        ).take(MAX_SEARCH_RESULTS)

        log.info { "[${getStepName()}] Found ${combinedResults.size} documents via RRF" }
        log.info { "[${getStepName()}] Top 5 RRF scores: ${combinedResults.take(5).map { "%.4f".format(it.score.value) }}" }

        return context.copy(searchResults = combinedResults)
    }

    /**
     * Apply Reciprocal Rank Fusion (RRF) to combine multiple search result rankings
     *
     * RRF Formula: score(d) = Σ [weight / (rank(d) + k)]
     * where k and weights are configurable via application.yaml
     *
     * Benefits:
     * - No score normalization needed
     * - Handles heterogeneous score distributions
     * - Prevents unbounded score growth
     * - Industry standard (OpenSearch, Elasticsearch, Azure)
     */
    private fun applyRRF(
        keywordResults: List<SearchResult>,
        titleResults: List<SearchResult>,
        contentResults: List<SearchResult>
    ): List<SearchResult> {
        val documentMap = mutableMapOf<String, SearchResult>()
        val rrfScores = mutableMapOf<String, Double>()

        fun addRRFScore(results: List<SearchResult>, weight: Double) {
            results.forEachIndexed { rank, result ->
                // RRF score: weight / (rank + k)
                val score = weight / (rank + rrfK)
                rrfScores[result.id] = rrfScores.getOrDefault(result.id, 0.0) + score
                documentMap.putIfAbsent(result.id, result)
            }
        }

        // Apply RRF with different weights for each strategy (from config)
        addRRFScore(keywordResults, keywordWeight)
        addRRFScore(titleResults, titleWeight)
        addRRFScore(contentResults, contentWeight)

        // Sort by RRF scores and return
        return rrfScores.entries
            .sortedByDescending { it.value }
            .mapNotNull { (id, rrfScore) ->
                documentMap[id]?.copy(
                    score = SearchScore.SimilarityScore(rrfScore)
                )
            }
    }

    override fun getStepName(): String = "Document Search"
}
