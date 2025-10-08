package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.DocumentChatPipelineStep
import com.okestro.okchat.chat.pipeline.copy
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
) : DocumentChatPipelineStep {

    companion object {
        private const val TOP_K_TO_RERANK = 20 // Re-rank top 20 results
        private const val RRF_WEIGHT = 0.4 // Weight for original RRF score (40%)
        private const val SEMANTIC_WEIGHT = 0.6 // Weight for semantic similarity (60%)
    }

    /**
     * Only execute re-ranking if we have search results
     */
    override fun shouldExecute(context: ChatContext): Boolean {
        return !context.search?.results.isNullOrEmpty() && context.isDeepThink
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        val searchResults = context.search?.results ?: return context

        if (searchResults.size <= 1) {
            log.info { "[${getStepName()}] Skipping: only ${searchResults.size} results" }
            return context
        }

        log.info { "[${getStepName()}] Re-ranking top $TOP_K_TO_RERANK of ${searchResults.size} results" }

        // Take top K results for re-ranking
        val topK = searchResults.take(TOP_K_TO_RERANK)

        // Generate query embedding
        val queryEmbedding = embeddingModel.embed(context.input.message).toList()

        // Re-score each document using hybrid approach:
        // Combine original RRF score (which includes date/path boosts) with semantic similarity
        // This preserves intelligent boosts while improving ranking quality
        val reranked = topK.mapNotNull { result ->
            // Skip documents with empty content (metadata-only chunks)
            if (result.content.isBlank()) {
                log.warn { "[${getStepName()}] Skipping document with empty content: ${result.title} (id: ${result.id})" }
                return@mapNotNull null
            }

            try {
                val docEmbedding = embeddingModel.embed(result.content).toList()
                val semanticSimilarity = cosineSimilarity(queryEmbedding, docEmbedding)

                // Preserve original RRF score (includes date/path boosts)
                val originalScore = result.score.value

                // Normalize original score to 0-1 range for fair combination
                // RRF scores are typically in 0.01-0.20 range, so we scale them
                val normalizedOriginal = minOf(originalScore * 5.0, 1.0)

                // Hybrid score: combine RRF (with boosts) and semantic similarity
                val hybridScore = (normalizedOriginal * RRF_WEIGHT) + (semanticSimilarity * SEMANTIC_WEIGHT)

                log.debug { "[${getStepName()}] ${result.title}: RRF=$originalScore (norm=${"%.4f".format(normalizedOriginal)}), Semantic=${"%.4f".format(semanticSimilarity)}, Hybrid=${"%.4f".format(hybridScore)}" }

                result.copy(
                    score = SearchScore.SimilarityScore(hybridScore)
                )
            } catch (e: Exception) {
                log.error(e) { "[${getStepName()}] Failed to re-rank document ${result.id}: ${e.message}" }
                // Keep original score on error
                result
            }
        }.sortedByDescending { it.score }

        // Combine re-ranked top K with remaining results
        val remaining = searchResults.drop(TOP_K_TO_RERANK)
        val finalResults = reranked + remaining

        log.info { "[${getStepName()}] Re-ranking completed: ${topK.size} input → ${reranked.size} output" }

        // Log top 5 for quick reference, full list in DEBUG
        if (log.isDebugEnabled()) {
            log.debug { "[${getStepName()}] ━━━ All ${reranked.size} re-ranked results ━━━" }
            reranked.forEachIndexed { index, result ->
                log.debug { "  [${index + 1}] ${result.title} (score: ${"%.4f".format(result.score.value)}, id: ${result.id}, content: ${result.content.length} chars)" }
            }
            log.debug { "[${getStepName()}] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        } else {
            log.info {
                "[${getStepName()}] Top 5: ${
                reranked.take(5).joinToString(", ") { "${it.title}(${"%.4f".format(it.score.value)})" }
                }"
            }
        }

        return context.copy(
            search = context.search.copy(results = finalResults)
        )
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
