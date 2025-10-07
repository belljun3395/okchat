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
    val type: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): DocumentMetadata {
            return DocumentMetadata(
                title = map["title"]?.toString(),
                path = map["path"]?.toString(),
                spaceKey = map["spaceKey"]?.toString(),
                keywords = map["keywords"]?.toString(),
                id = map["id"]?.toString(),
                type = map["type"]?.toString()
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "path" to path,
            "spaceKey" to spaceKey,
            "keywords" to keywords,
            "id" to id,
            "type" to type
        ).filterValues { it != null }
    }
}
