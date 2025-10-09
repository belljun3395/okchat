package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.client.SearchFields
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.config.SearchWeightConfig
import com.okestro.okchat.search.model.MetadataFields
import com.okestro.okchat.search.model.SearchCriteria
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.util.HybridSearchUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel

private val log = KotlinLogging.logger {}

/**
 * Template Method Pattern for hybrid search strategies.
 *
 * Responsibility:
 * - Implement common hybrid search flow (embedding + request + parse)
 * - Allow subclasses to customize field weights and score combination
 * - Accept SearchCriteria for consistency and extensibility
 *
 * Engine-agnostic: works with any SearchClient implementation.
 */
abstract class AbstractHybridSearchStrategy(
    private val searchClient: SearchClient,
    private val embeddingModel: EmbeddingModel
) : SearchStrategy {

    override suspend fun search(criteria: SearchCriteria, topK: Int): List<SearchResult> {
        val query = criteria.toQuery()

        if (query.isBlank()) {
            log.warn { "[${getName()}] Empty query from criteria, returning empty results" }
            return emptyList()
        }

        log.info { "[${getName()}] Searching with ${criteria.getSearchType().name} criteria: '$query' (topK=$topK)" }

        log.debug { "[${getName()}] Generating embedding..." }
        val embedding = embeddingModel.embed(query).toList()

        val request = HybridSearchRequest(
            query,
            embedding,
            SearchFields(
                queryBy = getFieldWeights().queryByList(),
                weights = getFieldWeights().weightsList()
            ),
            emptyMap(), // No type filter - search all document types (pages + PDF attachments)
            topK
        )

        log.debug { "[${getName()}] Executing search..." }
        val response = searchClient.hybridSearch(request)

        log.info { "[${getName()}] Found ${response.hits.size} documents" }

        val results = HybridSearchUtils.parseSearchResults(response, ::combineScores)
        val deduplicated = HybridSearchUtils.deduplicateResults(results)

        log.info { "[${getName()}] Returning ${deduplicated.size} results" }
        return deduplicated
    }

    private fun combineScores(textScore: Double, vectorScore: Double): Double {
        return getWeightSettings().combine(textScore, vectorScore)
    }

    protected abstract fun getFieldWeights(): SearchFieldWeightConfig.FieldWeights
    protected abstract fun getWeightSettings(): SearchWeightConfig.WeightSettings
}
