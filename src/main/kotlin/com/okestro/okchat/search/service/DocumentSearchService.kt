package com.okestro.okchat.search.service

import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.util.HybridSearchUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Document search service optimized for multi-search performance
 * Uses HybridSearchUtils for common search logic
 */
@Service
class DocumentSearchService(
    private val searchClient: SearchClient,
    private val embeddingModel: EmbeddingModel,
    private val fieldConfig: SearchFieldWeightConfig
) {

    /**
     * Execute multiple searches in a single request (optimized with Typesense multi_search)
     * Returns results for keyword, title, and content searches
     *
     * This dramatically reduces network latency by batching all searches into one HTTP request
     */
    suspend fun multiSearch(
        query: String,
        keywords: String,
        topK: Int = 50
    ): MultiSearchResult {
        log.info { "[Multi-Search] Executing optimized multi-search: query='$query', keywords='$keywords', topK=$topK" }

        // Generate embedding once (reused for all searches)
        log.debug { "[Multi-Search] Generating embedding..." }
        val embedding = embeddingModel.embed(query).toList()

        // Build 3 search requests with different field configurations
        val requests = listOf(
            // 1. Keyword search
            HybridSearchUtils.buildSearchRequest(
                query = keywords,
                embedding = embedding,
                fields = fieldConfig.keyword,
                topK = topK
            ),
            // 2. Title search
            HybridSearchUtils.buildSearchRequest(
                query = query,
                embedding = embedding,
                fields = fieldConfig.title,
                topK = topK
            ),
            // 3. Content search
            HybridSearchUtils.buildSearchRequest(
                query = keywords.ifEmpty { query },
                embedding = embedding,
                fields = fieldConfig.content,
                topK = topK
            )
        )

        // Execute all searches in a single HTTP request
        log.debug { "[Multi-Search] Executing batched search..." }
        val responses = searchClient.multiHybridSearch(requests)

        // Parse responses into SearchResults and deduplicate
        log.debug { "[Multi-Search] Parsing keyword results..." }
        val keywordResults = HybridSearchUtils.deduplicateResults(
            HybridSearchUtils.parseSearchResults(responses[0])
        )

        log.debug { "[Multi-Search] Parsing title results..." }
        val titleResults = HybridSearchUtils.deduplicateResults(
            HybridSearchUtils.parseSearchResults(responses[1])
        )

        log.debug { "[Multi-Search] Parsing content results..." }
        val contentResults = HybridSearchUtils.deduplicateResults(
            HybridSearchUtils.parseSearchResults(responses[2])
        )

        log.info {
            "[Multi-Search] Completed: keyword=${keywordResults.size}, " +
                "title=${titleResults.size}, content=${contentResults.size}"
        }

        // Log top results from each search type (DEBUG only)
        if (log.isDebugEnabled()) {
            log.debug { "[Multi-Search] ━━━ Keyword search top 5 ━━━" }
            keywordResults.take(5).forEachIndexed { i, r ->
                log.debug { "  [K${i + 1}] ${r.title} (score: ${"%.4f".format(r.score.value)}, content: ${r.content.length} chars)" }
            }

            log.debug { "[Multi-Search] ━━━ Title search top 5 ━━━" }
            titleResults.take(5).forEachIndexed { i, r ->
                log.debug { "  [T${i + 1}] ${r.title} (score: ${"%.4f".format(r.score.value)}, content: ${r.content.length} chars)" }
            }

            log.debug { "[Multi-Search] ━━━ Content search top 5 ━━━" }
            contentResults.take(5).forEachIndexed { i, r ->
                log.debug { "  [C${i + 1}] ${r.title} (score: ${"%.4f".format(r.score.value)}, content: ${r.content.length} chars)" }
            }
        }

        return MultiSearchResult(
            keywordResults = keywordResults,
            titleResults = titleResults,
            contentResults = contentResults
        )
    }
}
