package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.OptionalChatPipelineStep
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
        private const val KEYWORD_MATCH_BOOST = 1.5 //  similarity (0~1) + boost
        private const val ADDITIONAL_KEYWORD_BOOST = 0.8 //  추가 매칭마다 누적
        private const val TITLE_MATCH_BOOST = 2.0 //  제목 매칭 중요
        private const val CONTENT_MATCH_BOOST = 1.2 //  내용 매칭 기본
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Starting multi-strategy search" }

        val allKeywords = context.getAllKeywords()

        val allResults = coroutineScope {
            val keywordJob = async { searchByKeywords(allKeywords) }
            val titleJob = async {
                if (!context.dateKeywords.isNullOrEmpty()) {
                    searchByTitle(context.userMessage)
                } else {
                    emptyMap()
                }
            }
            val contentJob = async { searchByContent(context.userMessage) }

            val results = mutableMapOf<String, SearchResult>()
            results.putAll(keywordJob.await())
            results.putAll(titleJob.await())
            results.putAll(contentJob.await())
            results
        }

        // Sort and limit results
        val combinedResults = allResults.values
            .sortedByDescending { it.score }
            .take(MAX_SEARCH_RESULTS)

        log.info { "[${getStepName()}] Found ${combinedResults.size} unique documents" }
        log.info { "[${getStepName()}] Top 5 scores: ${combinedResults.take(5).map { "%.2f".format(it.score) }}" }

        return context.copy(searchResults = combinedResults)
    }

    private suspend fun searchByKeywords(
        keywords: List<String>
    ): Map<String, SearchResult> {
        if (keywords.isEmpty()) return emptyMap()

        log.info { "  [Keyword Search] Searching ${keywords.size} keywords (parallel)" }

        val results = mutableMapOf<String, SearchResult>()
        coroutineScope {
            keywords.chunked(5).map { chunk ->
                async {
                    chunk.forEach { keyword ->
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
                }
            }.awaitAll()
        }

        log.info { "    Found ${results.size} unique documents from keyword search" }
        return results
    }

    private suspend fun searchByTitle(
        query: String
    ): Map<String, SearchResult> {
        log.info { "  [Title Search] Searching titles" }

        val results = mutableMapOf<String, SearchResult>()
        val titleResults = documentSearchService.searchByTitle(query, MAX_SEARCH_RESULTS)

        titleResults.forEach { result ->
            val existing = results[result.id]
            if (existing == null || result.score > existing.score) {
                results[result.id] = result.copy(score = result.score * TITLE_MATCH_BOOST)
            }
        }

        log.info { "    Found ${titleResults.size} results from title search" }
        return results
    }

    private suspend fun searchByContent(
        query: String
    ): Map<String, SearchResult> {
        log.info { "  [Content Search] Semantic search on content" }

        val results = mutableMapOf<String, SearchResult>()
        val contentResults = documentSearchService.searchByContent(query, MAX_SEARCH_RESULTS)

        contentResults.forEach { result ->
            val existing = results[result.id]
            if (existing == null || result.score > existing.score) {
                results[result.id] = result.copy(score = result.score * CONTENT_MATCH_BOOST)
            }
        }

        log.info { "    Found ${contentResults.size} results from content search" }
        return results
    }

    override fun getStepName(): String = "Document Search"
}
