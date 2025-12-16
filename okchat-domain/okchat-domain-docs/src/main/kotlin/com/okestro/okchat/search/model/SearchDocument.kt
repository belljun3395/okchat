package com.okestro.okchat.search.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.okestro.okchat.search.index.DocumentIndex

/**
 * Type-safe representation of a search document from OpenSearch.
 * Replaces untyped Map<String, Any> usage for better type safety.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchDocument(
    @JsonProperty(DocumentIndex.Fields.ID)
    val id: String = "",

    @JsonProperty(DocumentIndex.Fields.CONTENT)
    val content: String = "",

    @JsonProperty(DocumentIndex.Fields.EMBEDDING)
    val embedding: List<Float>? = null,

    @JsonProperty(DocumentIndex.Fields.METADATA_OBJECT)
    val metadata: DocumentMetadata = DocumentMetadata()
) {
    /**
     * Get the actual page ID, handling chunked document IDs
     */
    fun getActualPageId(): String {
        return if (id.contains("_chunk_")) {
            id.substringBefore("_chunk_")
        } else {
            id
        }
    }

    /**
     * Get title from metadata
     */
    fun getTitle(): String {
        return metadata.title ?: "Untitled"
    }

    /**
     * Get path from metadata
     */
    fun getPath(): String {
        return metadata.path ?: ""
    }

    /**
     * Get space key from metadata
     */
    fun getSpaceKey(): String {
        return metadata.spaceKey ?: ""
    }

    /**
     * Get keywords from metadata
     */
    fun getKeywords(): String {
        return metadata.keywords ?: ""
    }

    /**
     * Get metadata ID
     */
    fun resolveMetadataId(): String {
        return metadata.id ?: id
    }

    /**
     * Get document type from metadata
     */
    fun getType(): String {
        return metadata.type ?: "confluence-page"
    }

    /**
     * Get knowledgeBaseId from metadata
     */
    fun getKnowledgeBaseId(): Long {
        return metadata.knowledgeBaseId ?: -1L
    }

    /**
     * Get page ID for building Confluence links
     * For PDF attachments, returns the parent page ID
     * For regular pages, returns the actual page ID
     */
    fun getPageId(): String {
        // For PDF attachments, use the pageId from metadata (the page it's attached to)
        val pageId = metadata.pageId ?: ""
        return pageId.ifBlank {
            getActualPageId()
        }
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()

        /**
         * Create SearchDocument from untyped source (for migration from legacy code)
         * Accepts both Map and Any from OpenSearch/JSON parsing
         */
        fun fromMap(source: Any): SearchDocument {
            // Convert Any to Map safely
            val map = when (source) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    source as Map<String, Any?>
                }

                else -> {
                    // Try to parse as JSON if not a map
                    try {
                        val json = objectMapper.writeValueAsString(source)
                        @Suppress("UNCHECKED_CAST")
                        objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
                    } catch (_: Exception) {
                        return SearchDocument() // Return empty document on error
                    }
                }
            }

            // Handle flat metadata structure for backward compatibility during deserialization
            val processedMap = if (map.containsKey(DocumentIndex.Fields.METADATA_OBJECT)) {
                map
            } else {
                // If no metadata object, check for flat fields and build metadata map
                val flatMetadata = map.filterKeys { it.startsWith("metadata.") }
                if (flatMetadata.isNotEmpty()) {
                    val metadata = DocumentMetadata.fromFlatMap(map)
                    val newMap = map.toMutableMap()
                    newMap[DocumentIndex.Fields.METADATA_OBJECT] = metadata
                    newMap
                } else {
                    map
                }
            }

            val json = objectMapper.writeValueAsString(processedMap)
            return try {
                objectMapper.readValue(json, SearchDocument::class.java)
            } catch (_: Exception) {
                return SearchDocument() // Return empty document on error
            }
        }
    }
}
