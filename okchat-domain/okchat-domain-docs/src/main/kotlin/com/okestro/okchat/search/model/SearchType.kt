package com.okestro.okchat.search.model

import com.okestro.okchat.search.config.SearchFieldWeightConfig

/**
 * Enum representing different types of search operations.
 * Encapsulates the field configuration for each search type.
 */
enum class SearchType {
    KEYWORD,
    TITLE,
    CONTENT,
    PATH;

    /**
     * Get the field configuration for this search type
     */
    fun getFieldWeights(fieldConfig: SearchFieldWeightConfig): SearchFieldWeightConfig.FieldWeights {
        return when (this) {
            KEYWORD -> fieldConfig.keyword
            TITLE -> fieldConfig.title
            CONTENT -> fieldConfig.content
            PATH -> fieldConfig.path
        }
    }

    /**
     * Get display name for logging
     */
    fun getDisplayName(): String {
        return when (this) {
            KEYWORD -> "Keyword"
            TITLE -> "Title"
            CONTENT -> "Content"
            PATH -> "Path"
        }
    }
}
