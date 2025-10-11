package com.okestro.okchat.ai.service.chunking

import org.springframework.ai.document.Document

/**
 * Strategy interface for different chunking approaches
 * Implementations are automatically selected by Spring based on configuration
 */
interface ChunkingStrategy {
    /**
     * Split a document into chunks
     */
    fun chunk(document: Document): List<Document>

    /**
     * Name of this chunking strategy
     */
    fun getName(): String
}

/**
 * Enum for chunking strategy types
 */
enum class ChunkingStrategyType {
    RECURSIVE_CHARACTER,
    SEMANTIC,
    SENTENCE_WINDOW
}
