package com.okestro.okchat.search.support

/**
 * Constants for metadata field names used in OpenSearch/Typesense.
 * Provides type-safe access to metadata fields in both flat and nested formats.
 */
object MetadataFields {
    // Flat field names (used in OpenSearch queries with dot notation)
    const val TITLE = "metadata.title"
    const val PATH = "metadata.path"
    const val SPACE_KEY = "metadata.spaceKey"
    const val KEYWORDS = "metadata.keywords"
    const val ID = "metadata.id"
    const val TYPE = "metadata.type"
    const val KNOWLEDGE_BASE_ID = "metadata.knowledgeBaseId"

    // Nested field names (without metadata prefix)
    object Nested {
        const val TITLE = "title"
        const val PATH = "path"
        const val SPACE_KEY = "spaceKey"
        const val KEYWORDS = "keywords"
        const val ID = "id"
        const val TYPE = "type"
    }

    // Additional property field names (for dynamic metadata)
    object Additional {
        // Common properties
        const val WEB_URL = "webUrl"
        const val IS_EMPTY = "isEmpty"
        const val KNOWLEDGE_BASE_ID = "knowledgeBaseId"

        // PDF attachment properties
        const val PAGE_ID = "pageId"
        const val ATTACHMENT_TITLE = "attachmentTitle"
        const val TOTAL_PDF_PAGES = "totalPdfPages"
        const val FILE_SIZE = "fileSize"
        const val MEDIA_TYPE = "mediaType"
        const val DOWNLOAD_URL = "downloadUrl"

        // Chunk properties
        const val CHUNK_INDEX = "chunkIndex"
        const val TOTAL_CHUNKS = "totalChunks"
    }

    // All flat field names as a list
    val ALL_FLAT = listOf(TITLE, PATH, SPACE_KEY, KEYWORDS, ID, TYPE)

    // All nested field names as a list
    val ALL_NESTED = listOf(
        Nested.TITLE,
        Nested.PATH,
        Nested.SPACE_KEY,
        Nested.KEYWORDS,
        Nested.ID,
        Nested.TYPE
    )
}
