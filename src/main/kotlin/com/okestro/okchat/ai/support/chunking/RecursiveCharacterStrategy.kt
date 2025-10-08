package com.okestro.okchat.ai.support.chunking

import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter

/**
 * Recursive character chunking using TokenTextSplitter
 * Default chunking strategy for compatibility
 */
class RecursiveCharacterStrategy(
    chunkSize: Int,
    chunkOverlap: Int,
    minChunkLengthToEmbed: Int,
    maxNumChunks: Int,
    keepSeparators: Boolean
) : ChunkingStrategy {

    private val splitter = TokenTextSplitter(
        chunkSize,
        chunkOverlap,
        minChunkLengthToEmbed,
        maxNumChunks,
        keepSeparators
    )

    override fun chunk(document: Document): List<Document> {
        return splitter.apply(listOf(document))
    }

    override fun getName() = "Recursive Character Splitter"
}
