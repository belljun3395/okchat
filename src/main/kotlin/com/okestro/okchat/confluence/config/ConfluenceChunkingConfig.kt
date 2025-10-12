package com.okestro.okchat.confluence.config

import com.okestro.okchat.ai.service.chunking.ChunkingStrategy
import com.okestro.okchat.ai.service.chunking.ChunkingStrategyType
import com.okestro.okchat.ai.service.chunking.RecursiveCharacterStrategy
import com.okestro.okchat.ai.service.chunking.SemanticChunkingStrategy
import com.okestro.okchat.ai.service.chunking.SentenceWindowStrategy
import com.okestro.okchat.config.RagProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

/**
 * Configuration for chunking strategy selection
 * Similar to SearchStrategyConfig pattern
 */
@Configuration
@EnableConfigurationProperties(RagProperties::class)
class ConfluenceChunkingConfig {

    @Bean
    fun chunkingStrategy(
        embeddingModel: EmbeddingModel,
        ragProperties: RagProperties
    ): ChunkingStrategy {
        val config = ragProperties.confluence.chunking

        // Parse strategy type from config with fallback to default
        val strategyType = try {
            ChunkingStrategyType.valueOf(config.strategy.uppercase())
        } catch (_: IllegalArgumentException) {
            log.warn { "Invalid chunking strategy '${config.strategy}', falling back to RECURSIVE_CHARACTER" }
            ChunkingStrategyType.RECURSIVE_CHARACTER
        }

        log.info { "Creating chunking strategy: $strategyType" }
        log.info {
            "Configuration: chunkSize=${config.chunkSize}, overlap=${config.chunkOverlap}, " +
                "semanticThreshold=${config.semanticSimilarityThreshold}, windowSize=${config.sentenceWindowSize}"
        }

        return when (strategyType) {
            ChunkingStrategyType.SEMANTIC -> {
                log.info { "Using Semantic Chunking Strategy" }
                SemanticChunkingStrategy(
                    embeddingModel = embeddingModel,
                    similarityThreshold = config.semanticSimilarityThreshold,
                    maxChunkSize = config.chunkSize
                )
            }
            ChunkingStrategyType.SENTENCE_WINDOW -> {
                log.info { "Using Sentence Window Strategy" }
                SentenceWindowStrategy(
                    windowSize = config.sentenceWindowSize
                )
            }
            ChunkingStrategyType.RECURSIVE_CHARACTER -> {
                log.info { "Using Recursive Character Strategy (default)" }
                RecursiveCharacterStrategy(
                    chunkSize = config.chunkSize,
                    chunkOverlap = config.chunkOverlap,
                    minChunkLengthToEmbed = config.minChunkLengthToEmbed,
                    maxNumChunks = config.maxNumChunks,
                    keepSeparators = config.keepSeparators
                )
            }
        }
    }
}
