package com.okestro.okchat.ai.support.chunking

import org.springframework.ai.document.Document

/**
 * Sentence Window Retrieval Strategy
 * Embeds individual sentences but stores surrounding context
 * Provides precise retrieval with rich context
 */
class SentenceWindowStrategy(
    private val windowSize: Int
) : ChunkingStrategy {

    override fun chunk(document: Document): List<Document> {
        val text = document.text ?: return listOf(document)

        // Split into sentences
        val sentences = splitIntoSentences(text)
        if (sentences.isEmpty()) return listOf(document)

        // Create a document for each sentence with window context
        return sentences.mapIndexed { index, sentence ->
            val windowStart = maxOf(0, index - windowSize)
            val windowEnd = minOf(sentences.size, index + windowSize + 1)
            val windowContext = sentences.subList(windowStart, windowEnd).joinToString(" ")

            Document(
                "${document.id}_sentence_$index",
                sentence, // Embed ONLY the single sentence
                document.metadata + mapOf(
                    "sentenceIndex" to index,
                    "totalSentences" to sentences.size,
                    "windowContext" to windowContext, // Store full window for retrieval
                    "chunkingStrategy" to "sentence_window"
                )
            )
        }
    }

    private fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("[.!?]+\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    override fun getName() = "Sentence Window Retrieval"
}
