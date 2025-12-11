package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.DocumentChatPipelineStep
import com.okestro.okchat.chat.pipeline.copy
import com.okestro.okchat.config.RagProperties
import com.okestro.okchat.search.application.MultiSearchUseCase
import com.okestro.okchat.search.application.dto.MultiSearchUseCaseIn
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import com.okestro.okchat.search.model.SearchTitles
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
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
    private val multiSearchUseCase: MultiSearchUseCase,
    private val ragProperties: RagProperties,
    private val meterRegistry: MeterRegistry
) : DocumentChatPipelineStep {

    companion object {
        private const val MAX_SEARCH_RESULTS = 50 // Optimized: reduced from 200 to improve performance
    }

    // RRF parameters loaded from externalized configuration
    // These can be tuned in application.yaml without code changes
    private val rrfK = ragProperties.rrf.k
    private val keywordWeight = ragProperties.rrf.keywordWeight
    private val titleWeight = ragProperties.rrf.titleWeight
    private val contentWeight = ragProperties.rrf.contentWeight
    private val pathWeight = ragProperties.rrf.pathWeight
    private val dateBoostFactor = ragProperties.rrf.dateBoostFactor
    private val pathBoostFactor = ragProperties.rrf.pathBoostFactor

    init {
        log.info { "[DocumentSearchStep] RRF configuration loaded: k=$rrfK, keyword=$keywordWeight, title=$titleWeight, content=$contentWeight, dateBoost=$dateBoostFactor, pathBoost=$pathBoostFactor" }
    }

    override fun getStepName(): String = "Document Search"

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
        val searchStartTime = System.currentTimeMillis()
        val sample = Timer.start(meterRegistry)

        val analysis = context.analysis ?: throw IllegalStateException("Analysis not available")
        val searchKeywords = SearchKeywords.fromStrings(analysis.getAllKeywords())
        val searchTitles = SearchTitles.fromStrings(analysis.extractedTitles)
        val searchContents = SearchContents.fromStrings(analysis.extractedContents)
        val searchPaths = SearchPaths.fromStrings(analysis.extractedPaths)

        val searchResult = multiSearchUseCase.execute(
            MultiSearchUseCaseIn(
                titles = searchTitles,
                contents = searchContents,
                paths = searchPaths,
                keywords = searchKeywords,
                topK = MAX_SEARCH_RESULTS
            )
        ).result

        // Sort results by score
        val sortedKeywordResults = searchResult.keywordResults.results.sortedByDescending { it.score }
        val sortedTitleResults = searchResult.titleResults.results.sortedByDescending { it.score }
        val sortedContentResults = searchResult.contentResults.results.sortedByDescending { it.score }
        val sortedPathResults = searchResult.pathResults.results.sortedByDescending { it.score }

        log.info { "[${getStepName()}] Multi-search completed: keyword=${sortedKeywordResults.size}, title=${sortedTitleResults.size}, content=${sortedContentResults.size}" }

        val combinedResults = applyRRF(
            keywordResults = sortedKeywordResults,
            titleResults = sortedTitleResults,
            contentResults = sortedContentResults,
            pathResults = sortedPathResults,
            dateKeywords = analysis.dateKeywords,
            keyWords = analysis.extractedKeywords
        )
            .take(MAX_SEARCH_RESULTS)

        val searchTimeMs = System.currentTimeMillis() - searchStartTime
        
        // Record metrics
        sample.stop(meterRegistry.timer("chat.search.latency"))
        meterRegistry.counter("chat.search.results.count", "zero_results", (combinedResults.isEmpty()).toString()).increment(combinedResults.size.toDouble())

        log.info { "[${getStepName()}] Found ${combinedResults.size} documents via RRF (after deduplication) in ${searchTimeMs}ms" }

        logResults(combinedResults)

        return context.copy(
            search = ChatContext.Search(results = combinedResults)
        )
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
        contentResults: List<SearchResult>,
        pathResults: List<SearchResult>,
        dateKeywords: List<String>,
        keyWords: List<String>
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
        addRRFScore(pathResults, pathWeight)

        fun applyContextualBoosts(
            dateKeywords: List<String>,
            keyWords: List<String>
        ): Map<String, Double> {
            // Apply contextual boosts to RRF scores based on query type and document metadata
            val boostedScores = mutableMapOf<String, Double>()
            val dateBoostCount = mutableMapOf<String, Int>()
            val pathBoostCount = mutableMapOf<String, Int>()

            rrfScores.forEach { (id, score) ->
                val document = documentMap[id]
                val title = document?.title ?: ""
                val path = document?.path ?: ""

                var boostMultiplier = 1.0
                val boostReasons = mutableListOf<String>()

                // 1. Date boost: Check if title contains any date keyword (case-insensitive)
                val matchesDateKeyword = dateKeywords.any { dateKeyword ->
                    title.contains(dateKeyword, ignoreCase = true)
                }

                if (matchesDateKeyword && dateKeywords.isNotEmpty()) {
                    boostMultiplier *= dateBoostFactor
                    dateBoostCount[id] = 1
                    boostReasons.add("date:${dateBoostFactor}x")
                }

                // 2. Path hierarchy boost: Check if path matches query type
                val parentPath = path.substringBeforeLast(">", "").split(">")
                val matchesPathPattern = keyWords.any { keyword ->
                    parentPath.contains(keyword)
                }

                if (matchesPathPattern) {
                    boostMultiplier *= pathBoostFactor
                    pathBoostCount[id] = 1
                    boostReasons.add("path:${pathBoostFactor}x")
                }

                boostedScores[id] = score * boostMultiplier

                if (boostReasons.isNotEmpty()) {
                    log.debug { "[RRF] Boost applied to '$title' (id: $id): $score → ${boostedScores[id]} [${boostReasons.joinToString(", ")}]" }
                }
            }

            // Log boost statistics
            if (dateBoostCount.isNotEmpty() || pathBoostCount.isNotEmpty()) {
                val totalDateBoost = dateBoostCount.size
                val totalPathBoost = pathBoostCount.size
                val bothBoosts = dateBoostCount.keys.intersect(pathBoostCount.keys).size

                log.info { "[RRF] Boost statistics: date=$totalDateBoost docs (${dateBoostFactor}x), path=$totalPathBoost docs (${pathBoostFactor}x), both=$bothBoosts docs (${dateBoostFactor * pathBoostFactor}x)" }
            }
            return boostedScores
        }

        // Apply intelligent boosts based on date and path hierarchy
        val boostedScores = applyContextualBoosts(dateKeywords, keyWords)

        // Sort by boosted RRF scores and return
        return boostedScores.entries
            .sortedByDescending { it.value }
            .mapNotNull { (id, rrfScore) ->
                documentMap[id]?.copy(
                    score = SearchScore.SimilarityScore(rrfScore)
                )
            }
    }

    private fun logResults(combinedResults: List<SearchResult>) {
        if (log.isDebugEnabled()) {
            log.debug { "[${getStepName()}] ━━━ All ${combinedResults.size} RRF results ━━━" }
            combinedResults.forEachIndexed { index, result ->
                log.debug { "  [RRF ${index + 1}] ${result.title} (score: ${"%.4f".format(result.score.value)}, id: ${result.id}, content: ${result.content.length} chars)" }
            }
            log.debug { "[${getStepName()}] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        } else {
            log.info {
                "[${getStepName()}] Top 5: ${
                combinedResults.take(5).joinToString(", ") { "${it.title}(${"%.4f".format(it.score.value)})" }
                }"
            }
        }
    }
}
