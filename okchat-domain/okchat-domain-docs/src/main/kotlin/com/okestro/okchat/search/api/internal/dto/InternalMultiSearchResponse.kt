package com.okestro.okchat.search.api.internal.dto

import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.SearchType
import com.okestro.okchat.search.model.TypedSearchResults

data class InternalMultiSearchResponse(
    val resultsByType: Map<SearchType, InternalTypedSearchResults>
) {
    companion object {
        fun from(result: MultiSearchResult): InternalMultiSearchResponse {
            // Since MultiSearchResult exposes specific result types via getters but implementation holds map,
            // we can reconstruct or handle each type.
            // MultiSearchResult internal map is private. We should probably use the getters.

            val resultMap = mutableMapOf<SearchType, InternalTypedSearchResults>()

            // We iterate over known types or if there was a way to get all.
            // Using the properties for now.

            val keywordRes = result.keywordResults
            if (keywordRes.isNotEmpty) {
                resultMap[SearchType.KEYWORD] = InternalTypedSearchResults.from(keywordRes)
            }

            val titleRes = result.titleResults
            if (titleRes.isNotEmpty) {
                resultMap[SearchType.TITLE] = InternalTypedSearchResults.from(titleRes)
            }

            val contentRes = result.contentResults
            if (contentRes.isNotEmpty) {
                resultMap[SearchType.CONTENT] = InternalTypedSearchResults.from(contentRes)
            }

            val pathRes = result.pathResults
            if (pathRes.isNotEmpty) {
                resultMap[SearchType.PATH] = InternalTypedSearchResults.from(pathRes)
            }

            return InternalMultiSearchResponse(resultMap)
        }
    }
}

data class InternalTypedSearchResults(
    val type: SearchType,
    val results: List<InternalSearchResultDto>
) {
    companion object {
        fun from(typedResults: TypedSearchResults): InternalTypedSearchResults {
            return InternalTypedSearchResults(
                type = typedResults.type,
                results = typedResults.results.map { InternalSearchResultDto.from(it) }
            )
        }
    }
}

data class InternalSearchResultDto(
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
) {
    companion object {
        fun from(searchResult: com.okestro.okchat.search.model.SearchResult): InternalSearchResultDto {
            return InternalSearchResultDto(
                id = searchResult.id,
                title = searchResult.title,
                content = searchResult.content,
                path = searchResult.path,
                spaceKey = searchResult.spaceKey,
                knowledgeBaseId = searchResult.knowledgeBaseId,
                keywords = searchResult.keywords,
                score = searchResult.score.value,
                type = searchResult.type,
                pageId = searchResult.pageId,
                webUrl = searchResult.webUrl,
                downloadUrl = searchResult.downloadUrl
            )
        }
    }
}
