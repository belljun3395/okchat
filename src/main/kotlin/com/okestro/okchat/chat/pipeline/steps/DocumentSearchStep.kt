package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.OptionalChatPipelineStep
import com.okestro.okchat.search.model.SearchScore
import com.okestro.okchat.search.service.DocumentSearchService
import com.okestro.okchat.search.service.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Search for relevant documents
 * Uses multi-strategy search (keyword, title, content) with PARALLEL execution
 */
@Component
@Order(1)
class DocumentSearchStep(
    private val documentSearchService: DocumentSearchService
) : OptionalChatPipelineStep {

    companion object {
        private const val MAX_SEARCH_RESULTS = 200
        private const val RRF_K = 60.0 // Standard RRF constant
        private const val KEYWORD_WEIGHT = 1.0 // RRF weight for keyword search
        private const val TITLE_WEIGHT = 1.2 // RRF weight for title search (slightly prefer)
        private const val CONTENT_WEIGHT = 0.8 // RRF weight for content search
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Starting multi-strategy search with RRF" }

        val allKeywords = context.getAllKeywords()

        // Execute all search strategies in parallel and get ranked results
        val (keywordResults, titleResults, contentResults) = coroutineScope {
            val keywordJob = async {
                searchByKeywords(allKeywords).sortedByDescending { it.score }
            }
            val titleJob = async {
                searchTitlesByQuery(context.userMessage).sortedByDescending { it.score }
            }
            val contentJob = async {
                searchContentByQuery(context.userMessage).sortedByDescending { it.score }
            }

            Triple(
                keywordJob.await(),
                titleJob.await(),
                contentJob.await()
            )
        }

        // Apply Reciprocal Rank Fusion to combine rankings
        val combinedResults = applyRRF(
            keywordResults = keywordResults,
            titleResults = titleResults,
            contentResults = contentResults
        ).take(MAX_SEARCH_RESULTS)

        log.info { "[${getStepName()}] Found ${combinedResults.size} documents via RRF" }
        log.info { "[${getStepName()}] Top 5 RRF scores: ${combinedResults.take(5).map { "%.4f".format(it.score.value) }}" }

        return context.copy(searchResults = combinedResults)
    }

    /**
     * Apply Reciprocal Rank Fusion (RRF) to combine multiple search result rankings
     *
     * RRF Formula: score(d) = Σ [weight / (rank(d) + k)]
     * where k = 60 (standard constant), rank starts at 0
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
                val score = weight / (rank + RRF_K)
                rrfScores[result.id] = rrfScores.getOrDefault(result.id, 0.0) + score
                documentMap.putIfAbsent(result.id, result)
            }
        }

        // Apply RRF with different weights for each strategy
        addRRFScore(keywordResults, KEYWORD_WEIGHT)
        addRRFScore(titleResults, TITLE_WEIGHT)
        addRRFScore(contentResults, CONTENT_WEIGHT)

        // Sort by RRF scores and return
        return rrfScores.entries
            .sortedByDescending { it.value }
            .mapNotNull { (id, rrfScore) ->
                documentMap[id]?.copy(
                    score = SearchScore.SimilarityScore(rrfScore)
                )
            }
    }

    private suspend fun searchByKeywords(
        keywords: List<String>
    ): List<SearchResult> {
        if (keywords.isEmpty()) return emptyList()

        log.info { "  [Keyword Search] Searching ${keywords.size} keywords (parallel)" }

        val results = mutableMapOf<String, SearchResult>()
        coroutineScope {
            keywords.chunked(5).map { chunk ->
                async {
                    chunk.forEach { keyword ->
                        val keywordResults = documentSearchService.searchByKeywords(keyword, MAX_SEARCH_RESULTS)
                        keywordResults.forEach { result ->
                            val existing = results[result.id]
                            if (existing == null || result.score > existing.score) {
                                results[result.id] = result
                            }
                        }
                    }
                }
            }.awaitAll()
        }

        log.info { "    Found ${results.size} unique documents from keyword search" }
        return results.values.toList()
    }

    /**
     * Search titles in parallel
     */
    private suspend fun searchTitlesByQuery(
        query: String
    ): List<SearchResult> {
        log.info { "  [Title Search] Searching titles" }

        val titleResults = documentSearchService.searchByTitle(query, MAX_SEARCH_RESULTS)

        log.info { "    Found ${titleResults.size} results from title search" }
        return titleResults
    }

    /**
     * Search content semantically
     */
    private suspend fun searchContentByQuery(
        query: String
    ): List<SearchResult> {
        log.info { "  [Content Search] Semantic search on content" }

        val contentResults = documentSearchService.searchByContent(query, MAX_SEARCH_RESULTS)

        log.info { "    Found ${contentResults.size} results from content search" }
        return contentResults
    }

    override fun getStepName(): String = "Document Search"
}
