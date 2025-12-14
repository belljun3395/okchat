package com.okestro.okchat.ai.client.docs

// Copied from docs domain to avoid compile dependency, as per strategy.

data class MultiSearchRequest(
    val titles: List<String>?,
    val contents: List<String>?,
    val paths: List<String>?,
    val keywords: List<String>?,
    val topK: Int = 50
)

data class MultiSearchResponse(
    val resultsByType: Map<SearchType, TypedSearchResults>
) {
    // Helper to get results easily
    val keywordResults: List<SearchResultDto> get() = resultsByType[SearchType.KEYWORD]?.results ?: emptyList()
    val titleResults: List<SearchResultDto> get() = resultsByType[SearchType.TITLE]?.results ?: emptyList()
    val contentResults: List<SearchResultDto> get() = resultsByType[SearchType.CONTENT]?.results ?: emptyList()
    val pathResults: List<SearchResultDto> get() = resultsByType[SearchType.PATH]?.results ?: emptyList()
}

enum class SearchType {
    KEYWORD, TITLE, CONTENT, PATH
}

data class TypedSearchResults(
    val type: SearchType,
    val results: List<SearchResultDto>
)

data class SearchResultDto(
    val id: String,
    val title: String,
    val content: String,
    val path: String,
    val spaceKey: String,
    val knowledgeBaseId: Long,
    val keywords: String,
    val score: Double,
    val type: String,
    val pageId: String,
    val webUrl: String,
    val downloadUrl: String
)
