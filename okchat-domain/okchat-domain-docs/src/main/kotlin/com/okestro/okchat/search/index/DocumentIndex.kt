package com.okestro.okchat.search.index

/**
 * Defines the structure and constants for the OpenSearch Vector Store index.
 * This class serves as the single source of truth for the index schema.
 */
object DocumentIndex {
    /**
     * The name of the index in OpenSearch.
     */
    const val INDEX_NAME = "vector_store"

    /**
     * Field names used in the index.
     */
    object Fields {
        const val ID = "id"
        const val CONTENT = "content"
        const val EMBEDDING = "embedding"
        const val METADATA_OBJECT = "metadata"
    }

    enum class DocumentCommonMetadata(val key: String) {
        TITLE("title"),
        PATH("path"),
        SPACE_KEY("spaceKey"),
        KEYWORDS("keywords"),
        ID("id"),
        TYPE("type"),
        KNOWLEDGE_BASE_ID("knowledgeBaseId"),
        WEB_URL("webUrl"),
        IS_EMPTY("isEmpty");

        val fullKey: String
            get() = "metadata.$key"
    }

    enum class AttachmentMetadata(val key: String) {
        // PDF Attachment Specific Fields
        PAGE_ID("pageId"),
        ATTACHMENT_TITLE("attachmentTitle"),
        TOTAL_PDF_PAGES("totalPdfPages"),
        FILE_SIZE("fileSize"),
        MEDIA_TYPE("mediaType"),
        DOWNLOAD_URL("downloadUrl"),
        PDF_PAGE_NUMBER("pdfPageNumber"),

        // Chunk Specific Fields
        CHUNK_INDEX("chunkIndex"),
        TOTAL_CHUNKS("totalChunks");

        val fullKey: String
            get() = "metadata.$key"
    }

    /**
     * Returns the complete index mapping (schema) for OpenSearch.
     * This describes the structure of the index including field types.
     */
    fun getMapping(): Map<String, Any> {
        return mapOf(
            "properties" to mapOf(
                Fields.ID to mapOf("type" to "keyword"),
                Fields.CONTENT to mapOf("type" to "text"),
                Fields.EMBEDDING to mapOf(
                    "type" to "knn_vector",
                    "dimension" to 1536
                ),
                "metadata" to mapOf(
                    "properties" to mapOf(
                        DocumentCommonMetadata.TITLE.key to mapOf("type" to "text"),
                        DocumentCommonMetadata.PATH.key to mapOf("type" to "keyword"),
                        DocumentCommonMetadata.SPACE_KEY.key to mapOf("type" to "keyword"),
                        DocumentCommonMetadata.KEYWORDS.key to mapOf("type" to "text"),
                        DocumentCommonMetadata.ID.key to mapOf("type" to "keyword"),
                        DocumentCommonMetadata.TYPE.key to mapOf("type" to "keyword"),
                        DocumentCommonMetadata.KNOWLEDGE_BASE_ID.key to mapOf("type" to "long"),
                        DocumentCommonMetadata.WEB_URL.key to mapOf("type" to "keyword"),
                        DocumentCommonMetadata.IS_EMPTY.key to mapOf("type" to "boolean"),
                        AttachmentMetadata.PAGE_ID.key to mapOf("type" to "keyword"),
                        AttachmentMetadata.ATTACHMENT_TITLE.key to mapOf("type" to "text"),
                        AttachmentMetadata.TOTAL_PDF_PAGES.key to mapOf("type" to "integer"),
                        AttachmentMetadata.FILE_SIZE.key to mapOf("type" to "long"),
                        AttachmentMetadata.MEDIA_TYPE.key to mapOf("type" to "keyword"),
                        AttachmentMetadata.DOWNLOAD_URL.key to mapOf("type" to "keyword"),
                        AttachmentMetadata.PDF_PAGE_NUMBER.key to mapOf("type" to "integer"),
                        AttachmentMetadata.CHUNK_INDEX.key to mapOf("type" to "integer"),
                        AttachmentMetadata.TOTAL_CHUNKS.key to mapOf("type" to "integer")
                    )
                )
            )
        )
    }
}
