package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.OptionalChatPipelineStep
import com.okestro.okchat.search.model.SearchScore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import kotlin.math.sqrt

private val log = KotlinLogging.logger {}

/**
 * Re-ranking step using cross-encoder approach
 * Re-scores top-K documents using query-document similarity
 * More accurate but slower than initial retrieval
 *
 * This is a simplified implementation. Production systems would use:
 * - Dedicated cross-encoder models (e.g., ms-marco-MiniLM-L-12-v2)
 * - Batch processing for efficiency
 * - Caching for frequently-seen query-document pairs
 */
@Component
@Order(2)
class ReRankingStep(
    private val embeddingModel: EmbeddingModel
) : OptionalChatPipelineStep {

    companion object {
        private const val TOP_K_TO_RERANK = 20 // Re-rank top 20 results
    }

    /**
     * Only execute re-ranking if we have search results
     */
    override fun shouldExecute(context: ChatContext): Boolean {
        return !context.searchResults.isNullOrEmpty()
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        val searchResults = context.searchResults ?: return context

        if (searchResults.size <= 1) {
            log.info { "[${getStepName()}] Skipping: only ${searchResults.size} results" }
            return context
        }

        log.info { "[${getStepName()}] Re-ranking top $TOP_K_TO_RERANK of ${searchResults.size} results" }

        // Take top K results for re-ranking
        val topK = searchResults.take(TOP_K_TO_RERANK)

        // Generate query embedding
        val queryEmbedding = embeddingModel.embed(context.userMessage).toList()

        // Re-score each document based on query-document similarity
        val reranked = topK.map { result ->
            val docEmbedding = embeddingModel.embed(result.content).toList()
            val similarity = cosineSimilarity(queryEmbedding, docEmbedding)

            result.copy(
                score = SearchScore.SimilarityScore(similarity)
            )
        }.sortedByDescending { it.score }

        // Combine re-ranked top K with remaining results
        val remaining = searchResults.drop(TOP_K_TO_RERANK)
        val finalResults = reranked + remaining

        log.info { "[${getStepName()}] Re-ranking completed" }
        log.info { "[${getStepName()}] Top 5 re-ranked scores: ${reranked.take(5).map { "%.4f".format(it.score.value) }}" }

        return context.copy(searchResults = finalResults)
    }

    private fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Double {
        require(vec1.size == vec2.size) { "Vectors must have the same dimension" }

        val dotProduct = vec1.zip(vec2) { a, b -> a * b }.sum()
        val magnitude1 = sqrt(vec1.sumOf { (it * it).toDouble() })
        val magnitude2 = sqrt(vec2.sumOf { (it * it).toDouble() })

        return if (magnitude1 > 0 && magnitude2 > 0) {
            dotProduct / (magnitude1 * magnitude2)
        } else {
            0.0
        }
    }

    override fun getStepName(): String = "Re-Ranking"
}
