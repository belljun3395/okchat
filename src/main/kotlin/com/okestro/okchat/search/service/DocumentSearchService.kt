package com.okestro.okchat.search.service

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchTitles
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
     * Perform multi-search across titles, contents, paths, and keywords
     * Combines results from different search types and deduplicates
     */
    suspend fun multiSearch(
        titles: SearchTitles?,
        contents: SearchContents?,
        paths: SearchPaths?,
        keywords: SearchKeywords?,
        topK: Int = 50
    ): MultiSearchResult {
        log.info {
            "[Multi-Search] Starting multi-search with topK=$topK: " +
                "titles=${titles?.titles?.size ?: 0}, contents=${contents?.contents?.size ?: 0}, " +
                "paths=${paths?.paths?.size ?: 0}, keywords=${keywords?.keywords?.size ?: 0}"
        }

        // Generate embedding once (reused for all searches)
        log.debug { "[Multi-Search] Generating embedding..." }
        val embedding = embeddingModel.embed(
            contents?.contents?.joinToString {
                it.term
            } ?: ""
        ).toList()
        log.debug { "[Multi-Search] Embedding generated with dimension ${embedding.size}" }

        val requests = mutableListOf<HybridSearchRequest>()
        var reqIndex = 0
        var keyWordReq: Int? = null
        var titleReq: Int? = null
        var contentReq: Int? = null
        var pathReq: Int? = null
        keywords?.let {
            requests.add(
                HybridSearchUtils.buildSearchRequest(
                    query = it.toOrQuery(),
                    embedding = embedding,
                    fields = fieldConfig.keyword,
                    topK = topK
                )
            )
            keyWordReq = reqIndex
            reqIndex++
        }

        titles?.let {
            requests.add(
                HybridSearchUtils.buildSearchRequest(
                    query = it.toOrQuery(),
                    embedding = embedding,
                    fields = fieldConfig.title,
                    topK = topK
                )
            )
            titleReq = reqIndex
            reqIndex++
        }

        contents?.let {
            requests.add(
                HybridSearchUtils.buildSearchRequest(
                    query = it.toOrQuery(),
                    embedding = embedding,
                    fields = fieldConfig.content,
                    topK = topK
                )
            )
            contentReq = reqIndex
            reqIndex++
        }

        paths?.let {
            requests.add(
                HybridSearchUtils.buildSearchRequest(
                    query = it.toOrQuery(),
                    embedding = embedding,
                    fields = fieldConfig.path,
                    topK = topK
                )
            )
            pathReq = reqIndex
            reqIndex++
        }

        // Execute all searches in a single HTTP request
        log.debug { "[Multi-Search] Executing batched search..." }
        val responses = searchClient.multiHybridSearch(requests)

        // Parse responses into SearchResults and deduplicate
        var keywordResults: List<SearchResult> = emptyList()
        keyWordReq?.let {
            log.debug { "[Multi-Search] Parsing keyword results..." }
            keywordResults = HybridSearchUtils.deduplicateResults(
                HybridSearchUtils.parseSearchResults(responses[it])
            )
        } ?: run {
            keywordResults = emptyList()
        }

        var titleResults: List<SearchResult> = emptyList()
        titleReq?.let {
            log.debug { "[Multi-Search] Parsing title results..." }
            titleResults = HybridSearchUtils.deduplicateResults(
                HybridSearchUtils.parseSearchResults(responses[it])
            )
        } ?: run {
            titleResults = emptyList()
        }

        var contentResults: List<SearchResult> = emptyList()
        contentReq?.let {
            log.debug { "[Multi-Search] Parsing content results..." }
            contentResults = HybridSearchUtils.deduplicateResults(
                HybridSearchUtils.parseSearchResults(responses[it])
            )
        } ?: run {
            contentResults = emptyList()
        }

        var pathResults: List<SearchResult> = emptyList()
        pathReq?.let {
            log.debug { "[Multi-Search] Parsing path results..." }
            pathResults = HybridSearchUtils.deduplicateResults(
                HybridSearchUtils.parseSearchResults(responses[it])
            )
        } ?: run {
            pathResults = emptyList()
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

            log.debug { "[Multi-Search] ━━━ Path search top 5 ━━━" }
            pathResults.take(5).forEachIndexed { i, r ->
                log.debug { "  [P${i + 1}] ${r.title} (score: ${"%.4f".format(r.score.value)}, content: ${r.content.length} chars)" }
            }
            log.debug { "[Multi-Search] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        }

        return MultiSearchResult(
            keywordResults = keywordResults,
            titleResults = titleResults,
            contentResults = contentResults,
            pathResults = pathResults
        )
    }
}
