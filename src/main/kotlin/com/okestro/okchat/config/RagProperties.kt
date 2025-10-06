package com.okestro.okchat.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for RAG (Retrieval-Augmented Generation) system
 * Externalizes key tuning parameters for chunking, RRF, and other RAG components
 */
@ConfigurationProperties(prefix = "rag")
data class RagProperties(
    val confluence: ConfluenceRagConfig = ConfluenceRagConfig(),
    val rrf: RRFConfig = RRFConfig()
)

/**
 * Confluence-specific RAG configuration
 */
data class ConfluenceRagConfig(
    val chunking: ChunkingConfig = ChunkingConfig()
)

/**
 * Text chunking configuration for document ingestion
 * Controls how large documents are split into smaller, semantically meaningful chunks
 */
data class ChunkingConfig(
    /**
     * Chunking strategy to use: RECURSIVE_CHARACTER, SEMANTIC, SENTENCE_WINDOW
     */
    var strategy: String = "RECURSIVE_CHARACTER",

    /**
     * Maximum number of tokens per chunk
     * Smaller chunks = more precise retrieval but more fragments
     * Larger chunks = more context but less precise matching
     */
    var chunkSize: Int = 512,

    /**
     * Number of tokens to overlap between consecutive chunks
     * Helps preserve context across chunk boundaries
     */
    var chunkOverlap: Int = 50,

    /**
     * Minimum chunk size in characters (before tokenization)
     * Chunks smaller than this will be merged or discarded
     */
    var minChunkSizeChars: Int = 350,

    /**
     * Minimum chunk length to embed
     * Very short chunks (e.g., single words) won't be embedded
     */
    var minChunkLengthToEmbed: Int = 5,

    /**
     * Maximum number of chunks to generate per document
     * Safety limit to prevent memory issues with extremely large documents
     */
    var maxNumChunks: Int = 10000,

    /**
     * Whether to keep separators (e.g., \n\n, \n) in the chunks
     * Useful for preserving formatting in markdown/structured text
     */
    var keepSeparators: Boolean = true,

    /**
     * Semantic chunking: similarity threshold for grouping sentences (0.0-1.0)
     */
    var semanticSimilarityThreshold: Double = 0.5,

    /**
     * Sentence window: number of sentences before/after to include as context
     */
    var sentenceWindowSize: Int = 3
)

/**
 * Reciprocal Rank Fusion (RRF) configuration
 * Controls how multiple search strategies are combined
 */
data class RRFConfig(
    /**
     * RRF constant (k parameter)
     * Higher k = more emphasis on top-ranked results
     * Lower k = more balanced fusion of rankings
     * Standard default: 60
     */
    var k: Double = 60.0,

    /**
     * Weight for keyword-based search in RRF
     * Higher = keyword matches have stronger influence
     */
    var keywordWeight: Double = 1.3,

    /**
     * Weight for title-based search in RRF
     * Higher = title matches have stronger influence
     */
    var titleWeight: Double = 1.5,

    /**
     * Weight for content hybrid search in RRF
     * Higher = semantic/content matches have stronger influence
     */
    var contentWeight: Double = 0.8,

    /**
     * Weight for path-based search in RRF
     * Higher = path matches have stronger influence
     */
    var pathWeight: Double = 3.0,

    /**
     * Date boost multiplier for RRF scores
     * When a document's title matches extracted date keywords (e.g., "250804" matches "2025년 8월"),
     * multiply its RRF score by this factor
     * Higher = stronger prioritization of date-matched results
     * Default: 3.0 (3x boost for date matches)
     */
    var dateBoostFactor: Double = 3.0,

    /**
     * Path hierarchy boost multiplier for RRF scores
     * When a document's path matches query intent (e.g., "팀회의" path for meeting queries),
     * multiply its RRF score by this factor
     * This leverages the fact that users organize documents hierarchically by type
     * Higher = stronger prioritization of path-matched results
     * Default: 2.0 (2x boost for path matches)
     */
    var pathBoostFactor: Double = 2.0
)
