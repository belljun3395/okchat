package com.okestro.okchat.search.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Type-safe representation of document metadata.
 * Replaces untyped Map<String, Any> usage for better type safety.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DocumentMetadata(
    @JsonProperty("title")
    val title: String? = null,

    @JsonProperty("path")
    val path: String? = null,

    @JsonProperty("spaceKey")
    val spaceKey: String? = null,

    @JsonProperty("keywords")
    val keywords: String? = null,

    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("type")
    val type: String? = null,

    // Additional dynamic properties
    val additionalProperties: Map<String, Any?> = emptyMap()
) {
    /**
     * Convert to nested map (without metadata prefix)
     */
    fun toMap(): Map<String, Any?> {
        return buildMap {
            title?.let { put(MetadataFields.Nested.TITLE, it) }
            path?.let { put(MetadataFields.Nested.PATH, it) }
            spaceKey?.let { put(MetadataFields.Nested.SPACE_KEY, it) }
            keywords?.let { put(MetadataFields.Nested.KEYWORDS, it) }
            id?.let { put(MetadataFields.Nested.ID, it) }
            type?.let { put(MetadataFields.Nested.TYPE, it) }
            putAll(additionalProperties)
        }
    }

    /**
     * Convert to flat map (with metadata. prefix for OpenSearch)
     */
    fun toFlatMap(): Map<String, Any?> {
        return buildMap {
            title?.let { put(MetadataFields.TITLE, it) }
            path?.let { put(MetadataFields.PATH, it) }
            spaceKey?.let { put(MetadataFields.SPACE_KEY, it) }
            keywords?.let { put(MetadataFields.KEYWORDS, it) }
            id?.let { put(MetadataFields.ID, it) }
            type?.let { put(MetadataFields.TYPE, it) }
            additionalProperties.forEach { (key, value) ->
                put("metadata.$key", value)
            }
        }
    }

    companion object {
        /**
         * Create from nested map (without metadata prefix)
         */
        fun fromMap(map: Map<String, Any?>): DocumentMetadata {
            return DocumentMetadata(
                title = map[MetadataFields.Nested.TITLE]?.toString(),
                path = map[MetadataFields.Nested.PATH]?.toString(),
                spaceKey = map[MetadataFields.Nested.SPACE_KEY]?.toString(),
                keywords = map[MetadataFields.Nested.KEYWORDS]?.toString(),
                id = map[MetadataFields.Nested.ID]?.toString(),
                type = map[MetadataFields.Nested.TYPE]?.toString(),
                additionalProperties = map.filterKeys { it !in MetadataFields.ALL_NESTED }
            )
        }

        /**
         * Create from flat map (with metadata. prefix)
         */
        fun fromFlatMap(map: Map<String, Any?>): DocumentMetadata {
            return DocumentMetadata(
                title = map[MetadataFields.TITLE]?.toString(),
                path = map[MetadataFields.PATH]?.toString(),
                spaceKey = map[MetadataFields.SPACE_KEY]?.toString(),
                keywords = map[MetadataFields.KEYWORDS]?.toString(),
                id = map[MetadataFields.ID]?.toString(),
                type = map[MetadataFields.TYPE]?.toString(),
                additionalProperties = map
                    .filterKeys { it.startsWith("metadata.") && it !in MetadataFields.ALL_FLAT }
                    .mapKeys { it.key.removePrefix("metadata.") }
            )
        }
    }
}
