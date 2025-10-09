package com.okestro.okchat.search.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Type-safe representation of a search document from OpenSearch.
 * Replaces untyped Map<String, Any> usage for better type safety.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchDocument(
    @JsonProperty("id")
    val id: String = "",

    @JsonProperty("content")
    val content: String = "",

    @JsonProperty("embedding")
    val embedding: List<Float>? = null,

    @JsonProperty("metadata")
    val metadata: DocumentMetadata = DocumentMetadata(),

    // Flattened metadata fields (for backward compatibility with flat structure)
    @JsonProperty(MetadataFields.TITLE)
    val metadataTitle: String? = null,

    @JsonProperty(MetadataFields.PATH)
    val metadataPath: String? = null,

    @JsonProperty(MetadataFields.SPACE_KEY)
    val metadataSpaceKey: String? = null,

    @JsonProperty(MetadataFields.KEYWORDS)
    val metadataKeywords: String? = null,

    @JsonProperty(MetadataFields.ID)
    val metadataId: String? = null,

    @JsonProperty(MetadataFields.TYPE)
    val metadataType: String? = null
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
     * Get title from either nested metadata or flattened field
     */
    fun getTitle(): String {
        return metadataTitle ?: metadata.title ?: "Untitled"
    }

    /**
     * Get path from either nested metadata or flattened field
     */
    fun getPath(): String {
        return metadataPath ?: metadata.path ?: ""
    }

    /**
     * Get space key from either nested metadata or flattened field
     */
    fun getSpaceKey(): String {
        return metadataSpaceKey ?: metadata.spaceKey ?: ""
    }

    /**
     * Get keywords from either nested metadata or flattened field
     */
    fun getKeywords(): String {
        return metadataKeywords ?: metadata.keywords ?: ""
    }

    /**
     * Get metadata ID from either nested metadata or flattened field
     */
    fun resolveMetadataId(): String {
        return metadataId ?: metadata.id ?: id
    }

    /**
     * Get document type from either nested metadata or flattened field
     */
    fun getType(): String {
        return metadataType ?: metadata.type ?: "confluence-page"
    }

    /**
     * Get page ID for building Confluence links
     * For PDF attachments, returns the parent page ID
     * For regular pages, returns the actual page ID
     */
    fun getPageId(): String {
        // For PDF attachments, use the pageId from metadata (the page it's attached to)
        val pageId = metadata.getStringValue("pageId")
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

            // Try to parse as JSON for proper deserialization
            return try {
                val json = objectMapper.writeValueAsString(map)
                objectMapper.readValue(json, SearchDocument::class.java)
            } catch (_: Exception) {
                // Fallback to manual construction
                SearchDocument(
                    id = map["id"]?.toString() ?: "",
                    content = map["content"]?.toString() ?: "",
                    embedding = (map["embedding"] as? List<*>)?.mapNotNull {
                        when (it) {
                            is Float -> it
                            is Double -> it.toFloat()
                            is Number -> it.toFloat()
                            else -> null
                        }
                    },
                    metadata = (map["metadata"] as? Map<*, *>)?.let { metaMap ->
                        @Suppress("UNCHECKED_CAST")
                        DocumentMetadata.fromMap(metaMap as Map<String, Any?>)
                    } ?: DocumentMetadata(),
                    metadataTitle = map[MetadataFields.TITLE]?.toString(),
                    metadataPath = map[MetadataFields.PATH]?.toString(),
                    metadataSpaceKey = map[MetadataFields.SPACE_KEY]?.toString(),
                    metadataKeywords = map[MetadataFields.KEYWORDS]?.toString(),
                    metadataId = map[MetadataFields.ID]?.toString(),
                    metadataType = map[MetadataFields.TYPE]?.toString()
                )
            }
        }
    }
}
