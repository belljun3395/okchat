package com.okestro.okchat.search.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.Property
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Initializes OpenSearch index schema with Korean language support.
 * * Responsibilities:
 * - Create index with proper mappings for text, vector, and metadata fields
 * - Configure Korean analyzer (nori) for better Korean text search
 * - Set up k-NN settings for vector similarity search
 * * Runs after all beans are initialized but BEFORE CommandLineRunner tasks.
 * This ensures the index exists before any sync tasks attempt to write data.
 */
@Component
class OpenSearchSchemaInitializer(
    private val openSearchClient: OpenSearchClient,
    @Value("\${spring.ai.vectorstore.opensearch.index-name}") private val indexName: String,
    @Value("\${spring.ai.vectorstore.opensearch.embedding-dimension}") private val embeddingDimension: Int
) {

    @EventListener(ApplicationStartedEvent::class)
    fun initializeKoreanSupportedSchema() {
        try {
            log.info { "Initializing OpenSearch schema for index: $indexName" }

            // Check if index exists
            val existsResponse = try {
                openSearchClient.indices().exists { e ->
                    e.index(indexName)
                }
            } catch (e: Exception) {
                log.warn { "Failed to check if index exists (might be permission issue): ${e.message}" }
                log.warn { "Skipping schema initialization. Please create index manually if needed." }
                return
            }

            if (existsResponse.value()) {
                val docCount = try {
                    openSearchClient.indices().stats { s ->
                        s.index(indexName)
                    }.indices()?.get(indexName)?.primaries()?.docs()?.count() ?: 0
                } catch (_: Exception) {
                    0L
                }

                log.info { "Index '$indexName' already exists with approximately $docCount documents" }

                // Check if Korean analyzer is configured
                log.info { "Index already exists with existing mappings" }
                log.info { "Note: For better Korean language support, consider installing analysis-nori plugin" }

                return
            }

            log.info { "Index '$indexName' does not exist, creating with Korean support..." }

            // Create index with standard analyzer (Nori plugin not installed)
            // TODO: Install analysis-nori plugin for better Korean support
            try {
                openSearchClient.indices().create { create ->
                    create.index(indexName)
                        .settings { settings ->
                            settings
                                .numberOfShards("1")
                                .numberOfReplicas("0")
                        }
                        .mappings { mappings ->
                            mappings.properties(buildIndexMappings())
                        }
                }

                log.info { "Successfully created index '$indexName' with standard analyzer" }
                log.info { "Note: For better Korean language support, install analysis-nori plugin" }
            } catch (e: Exception) {
                log.error(e) { "Failed to create index '$indexName': ${e.message}" }
                log.warn { "If OpenSearch is running in Docker, ensure it has write permissions and index creation is allowed." }
                log.warn { "You may need to restart OpenSearch container with updated settings." }
                throw e
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to initialize Korean-supported schema: ${e.message}" }
            // Don't throw - let the application continue
        }
    }

    /**
     * Build index mappings with proper field types and analyzers.
     */
    private fun buildIndexMappings(): Map<String, Property> {
        return mapOf(
            // ID field
            "id" to Property.of { p ->
                p.keyword { k -> k }
            },

            // Content field with standard analyzer
            "content" to Property.of { p ->
                p.text { text ->
                    text.fielddata(true) // Enable for aggregations if needed
                }
            },

            // Embedding vector field for k-NN search
            "embedding" to Property.of { p ->
                p.knnVector { vector ->
                    vector.dimension(embeddingDimension)
                }
            },

            // Metadata fields (using dot notation for nested fields)
            "metadata.title" to Property.of { p ->
                p.text { text -> text } // Use default standard analyzer
            },
            "metadata.keywords" to Property.of { p ->
                p.text { text -> text } // Use default standard analyzer
            },
            "metadata.type" to Property.of { p ->
                p.keyword { k -> k }
            },
            "metadata.spaceKey" to Property.of { p ->
                p.keyword { k -> k }
            },
            "metadata.path" to Property.of { p ->
                p.text { text -> text.analyzer("keyword") }
            }
        )
    }
}
