package com.okestro.okchat.search.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.okestro.okchat.search.index.DocumentIndex

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

    @JsonProperty("knowledgeBaseId")
    val knowledgeBaseId: Long? = null,

    @JsonProperty("webUrl")
    val webUrl: String? = null,

    @JsonProperty("isEmpty")
    val isEmpty: Boolean? = null,

    // PDF specific fields
    @JsonProperty("pageId")
    val pageId: String? = null,

    @JsonProperty("attachmentTitle")
    val attachmentTitle: String? = null,

    @JsonProperty("totalPdfPages")
    val totalPdfPages: Int? = null,

    @JsonProperty("pdfPageNumber")
    val pdfPageNumber: Int? = null,

    @JsonProperty("fileSize")
    val fileSize: Long? = null,

    @JsonProperty("mediaType")
    val mediaType: String? = null,

    @JsonProperty("downloadUrl")
    val downloadUrl: String? = null,

    // Chunk specific fields
    @JsonProperty("chunkIndex")
    val chunkIndex: Int? = null,

    @JsonProperty("totalChunks")
    val totalChunks: Int? = null
) {
    /**
     * Convert to nested map (without metadata prefix)
     */
    fun toMap(): Map<String, Any?> {
        return buildMap {
            title?.let { put(DocumentIndex.DocumentCommonMetadata.TITLE.key, it) }
            path?.let { put(DocumentIndex.DocumentCommonMetadata.PATH.key, it) }
            spaceKey?.let { put(DocumentIndex.DocumentCommonMetadata.SPACE_KEY.key, it) }
            keywords?.let { put(DocumentIndex.DocumentCommonMetadata.KEYWORDS.key, it) }
            id?.let { put(DocumentIndex.DocumentCommonMetadata.ID.key, it) }
            type?.let { put(DocumentIndex.DocumentCommonMetadata.TYPE.key, it) }
            knowledgeBaseId?.let { put(DocumentIndex.DocumentCommonMetadata.KNOWLEDGE_BASE_ID.key, it) }
            webUrl?.let { put(DocumentIndex.DocumentCommonMetadata.WEB_URL.key, it) }
            isEmpty?.let { put(DocumentIndex.DocumentCommonMetadata.IS_EMPTY.key, it) }

            pageId?.let { put(DocumentIndex.AttachmentMetadata.PAGE_ID.key, it) }
            attachmentTitle?.let { put(DocumentIndex.AttachmentMetadata.ATTACHMENT_TITLE.key, it) }
            totalPdfPages?.let { put(DocumentIndex.AttachmentMetadata.TOTAL_PDF_PAGES.key, it) }
            pdfPageNumber?.let { put(DocumentIndex.AttachmentMetadata.PDF_PAGE_NUMBER.key, it) }
            fileSize?.let { put(DocumentIndex.AttachmentMetadata.FILE_SIZE.key, it) }
            mediaType?.let { put(DocumentIndex.AttachmentMetadata.MEDIA_TYPE.key, it) }
            downloadUrl?.let { put(DocumentIndex.AttachmentMetadata.DOWNLOAD_URL.key, it) }
            chunkIndex?.let { put(DocumentIndex.AttachmentMetadata.CHUNK_INDEX.key, it) }
            totalChunks?.let { put(DocumentIndex.AttachmentMetadata.TOTAL_CHUNKS.key, it) }
        }
    }

    /**
     * Convert to flat map (with metadata. prefix for OpenSearch)
     */
    fun toFlatMap(): Map<String, Any?> {
        return buildMap {
            title?.let { put(DocumentIndex.DocumentCommonMetadata.TITLE.fullKey, it) }
            path?.let { put(DocumentIndex.DocumentCommonMetadata.PATH.fullKey, it) }
            spaceKey?.let { put(DocumentIndex.DocumentCommonMetadata.SPACE_KEY.fullKey, it) }
            keywords?.let { put(DocumentIndex.DocumentCommonMetadata.KEYWORDS.fullKey, it) }
            id?.let { put(DocumentIndex.DocumentCommonMetadata.ID.fullKey, it) }
            type?.let { put(DocumentIndex.DocumentCommonMetadata.TYPE.fullKey, it) }
            knowledgeBaseId?.let { put(DocumentIndex.DocumentCommonMetadata.KNOWLEDGE_BASE_ID.fullKey, it) }
            webUrl?.let { put(DocumentIndex.DocumentCommonMetadata.WEB_URL.fullKey, it) }
            isEmpty?.let { put(DocumentIndex.DocumentCommonMetadata.IS_EMPTY.fullKey, it) }

            pageId?.let { put(DocumentIndex.AttachmentMetadata.PAGE_ID.fullKey, it) }
            attachmentTitle?.let { put(DocumentIndex.AttachmentMetadata.ATTACHMENT_TITLE.fullKey, it) }
            totalPdfPages?.let { put(DocumentIndex.AttachmentMetadata.TOTAL_PDF_PAGES.fullKey, it) }
            pdfPageNumber?.let { put(DocumentIndex.AttachmentMetadata.PDF_PAGE_NUMBER.fullKey, it) }
            fileSize?.let { put(DocumentIndex.AttachmentMetadata.FILE_SIZE.fullKey, it) }
            mediaType?.let { put(DocumentIndex.AttachmentMetadata.MEDIA_TYPE.fullKey, it) }
            downloadUrl?.let { put(DocumentIndex.AttachmentMetadata.DOWNLOAD_URL.fullKey, it) }
            chunkIndex?.let { put(DocumentIndex.AttachmentMetadata.CHUNK_INDEX.fullKey, it) }
            totalChunks?.let { put(DocumentIndex.AttachmentMetadata.TOTAL_CHUNKS.fullKey, it) }
        }
    }

    companion object {
        /**
         * Create DocumentMetadata from flat map with metadata. prefix
         */
        fun fromFlatMap(map: Map<String, Any?>): DocumentMetadata {
            return DocumentMetadata(
                title = map[DocumentIndex.DocumentCommonMetadata.TITLE.fullKey] as? String,
                path = map[DocumentIndex.DocumentCommonMetadata.PATH.fullKey] as? String,
                spaceKey = map[DocumentIndex.DocumentCommonMetadata.SPACE_KEY.fullKey] as? String,
                keywords = map[DocumentIndex.DocumentCommonMetadata.KEYWORDS.fullKey] as? String,
                id = map[DocumentIndex.DocumentCommonMetadata.ID.fullKey] as? String,
                type = map[DocumentIndex.DocumentCommonMetadata.TYPE.fullKey] as? String,
                knowledgeBaseId = (map[DocumentIndex.DocumentCommonMetadata.KNOWLEDGE_BASE_ID.fullKey] as? Number)?.toLong(),
                webUrl = map[DocumentIndex.DocumentCommonMetadata.WEB_URL.fullKey] as? String,
                isEmpty = map[DocumentIndex.DocumentCommonMetadata.IS_EMPTY.fullKey] as? Boolean ?: map[DocumentIndex.DocumentCommonMetadata.IS_EMPTY.key] as? Boolean,

                pageId = map[DocumentIndex.AttachmentMetadata.PAGE_ID.fullKey] as? String,
                attachmentTitle = map[DocumentIndex.AttachmentMetadata.ATTACHMENT_TITLE.fullKey] as? String,
                totalPdfPages = (map[DocumentIndex.AttachmentMetadata.TOTAL_PDF_PAGES.fullKey] as? Number)?.toInt(),
                pdfPageNumber = (map[DocumentIndex.AttachmentMetadata.PDF_PAGE_NUMBER.fullKey] as? Number)?.toInt(),
                fileSize = (map[DocumentIndex.AttachmentMetadata.FILE_SIZE.fullKey] as? Number)?.toLong(),
                mediaType = map[DocumentIndex.AttachmentMetadata.MEDIA_TYPE.fullKey] as? String,
                downloadUrl = map[DocumentIndex.AttachmentMetadata.DOWNLOAD_URL.fullKey] as? String,
                chunkIndex = (map[DocumentIndex.AttachmentMetadata.CHUNK_INDEX.fullKey] as? Number)?.toInt(),
                totalChunks = (map[DocumentIndex.AttachmentMetadata.TOTAL_CHUNKS.fullKey] as? Number)?.toInt()
            )
        }
    }
}
