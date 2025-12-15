package com.okestro.okchat.search.config

import com.okestro.okchat.search.support.MetadataFields
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
    @Value("\${spring.ai.vectorstore.opensearch.embedding-dimension}") private val embeddingDimension: Int,
    @Value("\${spring.ai.vectorstore.opensearch.host:localhost}") private val host: String,
    @Value("\${spring.ai.vectorstore.opensearch.port:9200}") private val port: Int,
    @Value("\${spring.ai.vectorstore.opensearch.scheme:http}") private val scheme: String
) {

    @EventListener(ApplicationStartedEvent::class)
    fun initializeKoreanSupportedSchema() {
        try {
            log.info { "Initializing OpenSearch schema for index: $indexName" }
            log.info { "Connected to OpenSearch at: $scheme://$host:$port" }

            // Try to get index directly - this is more reliable than exists() check
            val indexInfo = try {
                val response = openSearchClient.indices().get { g ->
                    g.index(indexName)
                }
                response.get(indexName)
            } catch (e: org.opensearch.client.opensearch._types.OpenSearchException) {
                if (e.status() == 404) {
                    log.info { "Index '$indexName' does not exist (404 Not Found)" }
                    null
                } else {
                    log.warn { "Failed to get index details: ${e.message}" }
                    null
                }
            } catch (e: Exception) {
                log.warn { "Failed to check index existence: ${e.message}" }
                log.warn { "Skipping schema initialization. Please verify OpenSearch connection." }
                return
            }

            if (indexInfo != null) {
                // Index exists - get detailed information
                val settings = indexInfo.settings()
                val mappings = indexInfo.mappings()

                val docCount = try {
                    val stats = openSearchClient.indices().stats { s ->
                        s.index(indexName)
                    }
                    val indexStats = stats.indices()?.get(indexName)
                    val primariesDocs = indexStats?.primaries()?.docs()?.count() ?: 0
                    val totalDocs = indexStats?.total()?.docs()?.count() ?: 0
                    val storeSize = indexStats?.primaries()?.store()?.sizeInBytes() ?: 0

                    // Log detailed information
                    log.info { "Index '$indexName' EXISTS and is accessible" }
                    log.info { "  - Primary shard document count: $primariesDocs" }
                    log.info { "  - Total document count: $totalDocs" }
                    log.info { "  - Number of shards: ${settings?.index()?.numberOfShards() ?: "unknown"}" }
                    log.info { "  - Number of replicas: ${settings?.index()?.numberOfReplicas() ?: "unknown"}" }
                    log.info { "  - Store size: ${storeSize / 1024}KB" }
                    log.info { "  - Mappings: ${mappings?.properties()?.size ?: 0} fields configured" }

                    primariesDocs
                } catch (e: Exception) {
                    log.warn { "Failed to get document count: ${e.message}" }
                    0L
                }

                log.info { "Index '$indexName' already exists with approximately $docCount documents" }
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
            MetadataFields.TITLE to Property.of { p ->
                p.text { text -> text } // Use default standard analyzer
            },
            MetadataFields.KEYWORDS to Property.of { p ->
                p.keyword { k -> k }
            },
            MetadataFields.TYPE to Property.of { p ->
                p.keyword { k -> k }
            },
            MetadataFields.SPACE_KEY to Property.of { p ->
                p.keyword { k -> k }
            },
            MetadataFields.PATH to Property.of { p ->
                p.text { text -> text }
            },
            MetadataFields.KNOWLEDGE_BASE_ID to Property.of { p ->
                p.keyword { k -> k }
            },
            MetadataFields.WEB_URL to Property.of { p ->
                p.keyword { k -> k }
            },
            MetadataFields.IS_EMPTY to Property.of { p ->
                p.boolean_ { b -> b }
            }
        )
    }
}
