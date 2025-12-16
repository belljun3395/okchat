package com.okestro.okchat.search.support

import com.okestro.okchat.search.index.DocumentIndex
import com.okestro.okchat.search.model.DocumentMetadata

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
    var webUrl: String? = null
    var isEmpty: Boolean? = null
    var pageId: String? = null
    var attachmentTitle: String? = null
    var totalPdfPages: Int? = null
    var pdfPageNumber: Int? = null
    var fileSize: Long? = null
    var mediaType: String? = null
    var downloadUrl: String? = null
    var chunkIndex: Int? = null
    var totalChunks: Int? = null

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
     * Set additional property using infix notation (Mapping to strict properties)
     */
    infix fun String.to(value: Any?) {
        property(this, value)
    }

    /**
     * Set property (Mapping to strict properties)
     */
    fun property(key: String, value: Any?) {
        when (key) {
            // Short names (Legacy/Convenience)
            "title" -> title = value?.toString()
            "path" -> path = value?.toString()
            "spaceKey" -> spaceKey = value?.toString()
            "id" -> id = value?.toString()
            "type" -> type = value?.toString()
            "knowledgeBaseId" -> knowledgeBaseId = (value as? Number)?.toLong()
            "webUrl" -> webUrl = value?.toString()
            "isEmpty" -> isEmpty = value as? Boolean
            "pageId" -> pageId = value?.toString()
            "attachmentTitle" -> attachmentTitle = value?.toString()
            "totalPdfPages" -> totalPdfPages = (value as? Number)?.toInt()
            "pdfPageNumber" -> pdfPageNumber = (value as? Number)?.toInt()
            "fileSize" -> fileSize = (value as? Number)?.toLong()
            "mediaType" -> mediaType = value?.toString()
            "downloadUrl" -> downloadUrl = value?.toString()
            "chunkIndex" -> chunkIndex = (value as? Number)?.toInt()
            "totalChunks" -> totalChunks = (value as? Number)?.toInt()

            // DocumentIndex Constants (Single Source of Truth)
            DocumentIndex.DocumentCommonMetadata.TITLE.fullKey -> title = value?.toString()
            DocumentIndex.DocumentCommonMetadata.PATH.fullKey -> path = value?.toString()
            DocumentIndex.DocumentCommonMetadata.SPACE_KEY.fullKey -> spaceKey = value?.toString()
            DocumentIndex.DocumentCommonMetadata.ID.fullKey -> id = value?.toString()
            DocumentIndex.DocumentCommonMetadata.TYPE.fullKey -> type = value?.toString()
            DocumentIndex.DocumentCommonMetadata.KNOWLEDGE_BASE_ID.fullKey -> knowledgeBaseId = (value as? Number)?.toLong()
            DocumentIndex.DocumentCommonMetadata.WEB_URL.fullKey -> webUrl = value?.toString()
            DocumentIndex.DocumentCommonMetadata.IS_EMPTY.fullKey -> isEmpty = value as? Boolean

            // PDF fields
            DocumentIndex.AttachmentMetadata.PAGE_ID.fullKey -> pageId = value?.toString()
            DocumentIndex.AttachmentMetadata.ATTACHMENT_TITLE.fullKey -> attachmentTitle = value?.toString()
            DocumentIndex.AttachmentMetadata.TOTAL_PDF_PAGES.fullKey -> totalPdfPages = (value as? Number)?.toInt()
            DocumentIndex.AttachmentMetadata.PDF_PAGE_NUMBER.fullKey -> pdfPageNumber = (value as? Number)?.toInt()
            DocumentIndex.AttachmentMetadata.FILE_SIZE.fullKey -> fileSize = (value as? Number)?.toLong()
            DocumentIndex.AttachmentMetadata.MEDIA_TYPE.fullKey -> mediaType = value?.toString()
            DocumentIndex.AttachmentMetadata.DOWNLOAD_URL.fullKey -> downloadUrl = value?.toString()

            // Chunk fields
            DocumentIndex.AttachmentMetadata.CHUNK_INDEX.fullKey -> chunkIndex = (value as? Number)?.toInt()
            DocumentIndex.AttachmentMetadata.TOTAL_CHUNKS.fullKey -> totalChunks = (value as? Number)?.toInt()
        }
    }

    fun build(): DocumentMetadata {
        return DocumentMetadata(
            title = title,
            path = path,
            spaceKey = spaceKey,
            keywords = keywordsList.takeIf { it.isNotEmpty() }?.joinToString(", "),
            id = id,
            type = type,
            knowledgeBaseId = knowledgeBaseId,
            webUrl = webUrl,
            isEmpty = isEmpty,
            pageId = pageId,
            attachmentTitle = attachmentTitle,
            totalPdfPages = totalPdfPages,
            pdfPageNumber = pdfPageNumber,
            fileSize = fileSize,
            mediaType = mediaType,
            downloadUrl = downloadUrl,
            chunkIndex = chunkIndex,
            totalChunks = totalChunks
        )
    }
}

/**
 * DSL function to create DocumentMetadata
 */
fun metadata(block: DocumentMetadataBuilder.() -> Unit): DocumentMetadata {
    return DocumentMetadataBuilder().apply(block).build()
}
