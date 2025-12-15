package com.okestro.okchat.search.support

import com.okestro.okchat.search.model.DocumentMetadata
import org.springframework.ai.document.Document

/**
 * DSL builder for creating DocumentMetadata instances with a fluent API.
 *
 * Usage example:
 * ```
 * val metadata = metadata {
 *     title = "Example Page"
 *     path = "Space > Folder > Page"
 *     spaceKey = "SPACE"
 *     keywords = listOf("keyword1", "keyword2")
 *     id = "123456"
 *     type = "confluence-page"
 * *     // Additional properties
 *     "customField" to "customValue"
 *     "isEmpty" to false
 * }
 * ```
 */
@DslMarker
annotation class DocumentMetadataDsl

@DocumentMetadataDsl
class DocumentMetadataBuilder {
    var title: String? = null
    var path: String? = null
    var spaceKey: String? = null
    private val keywordsList = mutableListOf<String>()
    var id: String? = null
    var type: String? = null
    var knowledgeBaseId: Long? = null

    private val additionalProperties = mutableMapOf<String, Any?>()

    /**
     * Set keywords from a list
     */
    var keywords: List<String>
        get() = keywordsList
        set(value) {
            keywordsList.clear()
            keywordsList.addAll(value)
        }

    /**
     * Set keywords from a comma-separated string
     */
    fun keywords(value: String) {
        keywordsList.clear()
        keywordsList.addAll(value.split(",").map { it.trim() }.filter { it.isNotBlank() })
    }

    /**
     * Add a single keyword
     */
    fun keyword(value: String) {
        keywordsList.add(value)
    }

    /**
     * Add multiple keywords
     */
    fun keywords(vararg values: String) {
        keywordsList.addAll(values)
    }

    /**
     * Set additional property using infix notation
     */
    infix fun String.to(value: Any?) {
        additionalProperties[this] = value
    }

    /**
     * Set additional property
     */
    fun property(key: String, value: Any?) {
        additionalProperties[key] = value
    }

    fun build(): DocumentMetadata {
        return DocumentMetadata(
            title = title,
            path = path,
            spaceKey = spaceKey,
            keywords = keywordsList.takeIf { it.isNotEmpty() }?.joinToString(", "),
            id = id,
            type = type,
            additionalProperties = additionalProperties
        )
    }
}

/**
 * DSL function to create DocumentMetadata
 */
fun metadata(block: DocumentMetadataBuilder.() -> Unit): DocumentMetadata {
    return DocumentMetadataBuilder().apply(block).build()
}

/**
 * Extension function to create metadata from Spring AI Document
 */
fun Document.buildMetadata(block: DocumentMetadataBuilder.() -> Unit): Document {
    val documentMetadata = metadata(block)
    val metadataMap = documentMetadata.toMap()

    // Merge with existing metadata
    val mergedMetadata = this.metadata.toMutableMap().apply {
        putAll(metadataMap)
    }

    return Document(this.id, this.text ?: "", mergedMetadata)
}
