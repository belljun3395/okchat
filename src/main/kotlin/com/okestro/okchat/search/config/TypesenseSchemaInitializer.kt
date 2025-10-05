package com.okestro.okchat.search.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.typesense.api.Client
import org.typesense.api.FieldTypes
import org.typesense.model.CollectionSchema
import org.typesense.model.Field

private val log = KotlinLogging.logger {}

/**
 * Initializes Typesense collection schema with Korean language support.
 * Runs after all beans are fully initialized but BEFORE CommandLineRunner tasks.
 * This ensures the schema exists before any sync tasks attempt to write data.
 */
@Component
class TypesenseSchemaInitializer(
    private val typesenseClient: Client,
    @Value("\${spring.ai.vectorstore.typesense.collection-name}") private val collectionName: String,
    @Value("\${spring.ai.vectorstore.typesense.embedding-dimension}") private val embeddingDimension: Int
) {

    @EventListener(ApplicationStartedEvent::class)
    fun initializeKoreanSupportedSchema() {
        try {
            // Check if collection exists
            try {
                val existingCollection = typesenseClient.collections(collectionName).retrieve()
                log.info {
                    "Collection '$collectionName' already exists with ${existingCollection.numDocuments} documents"
                }

                // Check if the schema needs to be updated for Korean support
                val contentField = existingCollection.fields?.find { it.name == "content" }
                val titleField = existingCollection.fields?.find { it.name == "metadata.title" }
                val keywordsField = existingCollection.fields?.find { it.name == "metadata.keywords" }

                // Check if Korean tokenizer is configured
                val hasKoreanSupport = contentField?.locale == "ko" ||
                    titleField?.locale == "ko" ||
                    keywordsField?.locale == "ko"

                if (!hasKoreanSupport) {
                    log.warn {
                        "Collection exists but doesn't have optimal search configuration. " +
                            "Korean support: $hasKoreanSupport"
                    }
                    log.warn { "To enable improved search (Korean + infix matching):" }
                    log.warn { "  1) Delete collection: curl -X DELETE -H 'X-TYPESENSE-API-KEY: <key>' http://localhost:8108/collections/$collectionName" }
                    log.warn { "  2) Restart the application to recreate with improved schema" }
                    log.warn { "  3) Re-sync Confluence data" }
                } else {
                    log.info { "Collection has Korean support. Infix matching will be enabled on recreation." }
                }

                return
            } catch (e: Exception) {
                log.info { "Collection '$collectionName' does not exist, creating with Korean support..." }
            }

            // Create collection schema with Korean support
            val schema = CollectionSchema()
            schema.name = collectionName

            val fields = mutableListOf<Field>()

            // ID field
            fields.add(
                Field().apply {
                    name = "id"
                    type = FieldTypes.STRING
                }
            )

            // Content field with Korean tokenizer support
            fields.add(
                Field().apply {
                    name = "content"
                    type = FieldTypes.STRING
                    locale = "ko" // Enable Korean tokenization
                }
            )

            // Embedding vector field
            fields.add(
                Field().apply {
                    name = "embedding"
                    type = FieldTypes.FLOAT_ARRAY
                    numDim = embeddingDimension
                }
            )

            // Metadata fields with Korean support and infix matching
            // Using dot notation for nested fields (metadata.*)
            fields.add(
                Field().apply {
                    name = "metadata.title"
                    type = FieldTypes.STRING
                    locale = "ko" // Enable Korean tokenization
                    isInfix = true // Enable infix matching for better partial matches (e.g., "0804" matches "250804")
                }
            )

            fields.add(
                Field().apply {
                    name = "metadata.keywords"
                    type = FieldTypes.STRING
                    locale = "ko" // Enable Korean tokenization
                    isInfix = true // Enable infix matching for better keyword search
                }
            )

            fields.add(
                Field().apply {
                    name = "metadata.type"
                    type = FieldTypes.STRING
                }
            )

            fields.add(
                Field().apply {
                    name = "metadata.spaceKey"
                    type = FieldTypes.STRING
                }
            )

            fields.add(
                Field().apply {
                    name = "metadata.path"
                    type = FieldTypes.STRING
                }
            )

            schema.fields = fields

            typesenseClient.collections().create(schema)
            log.info { "Successfully created collection '$collectionName' with Korean tokenizer support" }
        } catch (e: Exception) {
            log.error(e) { "Failed to initialize Korean-supported schema: ${e.message}" }
            // Don't throw - let Spring AI handle schema initialization as fallback
        }
    }
}
