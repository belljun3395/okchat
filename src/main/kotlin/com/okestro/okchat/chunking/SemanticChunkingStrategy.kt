package com.okestro.okchat.chunking

import com.okestro.okchat.ai.support.MathUtils
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import kotlin.math.sqrt

/**
 * Semantic chunking - splits text based on semantic similarity between sentences
 * Groups semantically similar sentences together
 */
class SemanticChunkingStrategy(
    private val embeddingModel: EmbeddingModel,
    private val similarityThreshold: Double,
    private val maxChunkSize: Int
) : ChunkingStrategy {

    override fun chunk(document: Document): List<Document> {
        val text = document.text ?: return listOf(document)

        // Split into sentences
        val sentences = splitIntoSentences(text)
        if (sentences.size <= 1) return listOf(document)

        // Generate embeddings for each sentence
        val embeddings = sentences.map { sentence ->
            embeddingModel.embed(sentence).toList()
        }

        // Group sentences by semantic similarity
        val chunks = mutableListOf<List<String>>()
        var currentChunk = mutableListOf(sentences[0])

        for (i in 1 until sentences.size) {
            val similarity = MathUtils.cosineSimilarity(embeddings[i - 1], embeddings[i])

            if (similarity >= similarityThreshold && currentChunk.joinToString(" ").length < maxChunkSize) {
                currentChunk.add(sentences[i])
            } else {
                chunks.add(currentChunk.toList())
                currentChunk = mutableListOf(sentences[i])
            }
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk)
        }

        // Convert to documents
        return chunks.mapIndexed { index, chunk ->
            Document(
                "${document.id}_semantic_$index",
                chunk.joinToString(" "),
                document.metadata + mapOf(
                    "chunkIndex" to index,
                    "totalChunks" to chunks.size,
                    "chunkingStrategy" to "semantic"
                )
            )
        }
    }

    private fun splitIntoSentences(text: String): List<String> {
        // Simple sentence splitter
        return text.split(Regex("[.!?]+\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    override fun getName() = "Semantic Chunking"
}
