package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.client.HybridSearchRequest
import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.client.SearchFields
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.config.SearchWeightConfig
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.util.HybridSearchUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel

private val log = KotlinLogging.logger {}

/**
 * Template Method Pattern for hybrid search strategies.
 * Engine-agnostic: works with any SearchClient implementation.
 */
abstract class AbstractHybridSearchStrategy(
    private val searchClient: SearchClient,
    private val embeddingModel: EmbeddingModel
) : SearchStrategy {

    override suspend fun search(query: String, topK: Int): List<SearchResult> {
        log.info { "[${getName()}] Searching: '$query' (topK=$topK)" }

        log.debug { "[${getName()}] Generating embedding..." }
        val embedding = embeddingModel.embed(query).toList()

        val request = HybridSearchRequest(
            query,
            embedding,
            SearchFields(
                queryBy = getFieldWeights().queryByList(),
                weights = getFieldWeights().weightsList()
            ),
            mapOf("metadata.type" to "confluence-page"),
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
