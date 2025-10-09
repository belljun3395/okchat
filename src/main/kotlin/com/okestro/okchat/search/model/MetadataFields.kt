package com.okestro.okchat.search.model

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

    // Nested field names (without metadata prefix)
    object Nested {
        const val TITLE = "title"
        const val PATH = "path"
        const val SPACE_KEY = "spaceKey"
        const val KEYWORDS = "keywords"
        const val ID = "id"
        const val TYPE = "type"
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
