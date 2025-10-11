package com.okestro.okchat.ai.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Input for SearchDocumentsTool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchDocumentsInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("query")
    val query: String,

    @JsonProperty("limit")
    val limit: Int = 5,

    @JsonProperty("filterBySpace")
    val filterBySpace: String? = null
) {
    fun getValidatedLimit(): Int = limit.coerceIn(1, 20)
}

/**
 * Input for GetDocumentSchemaInfoTool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GetDocumentSchemaInput(
    @JsonProperty("thought")
    val thought: String? = null
)

/**
 * Generic tool input with just thought field (for simple tools)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SimpleToolInput(
    @JsonProperty("thought")
    val thought: String? = null
)

/**
 * Input for search tools (title, content)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchByQueryInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("query")
    val query: String,

    @JsonProperty("topK")
    val topK: Int = 10
) {
    fun getValidatedTopK(): Int = topK.coerceIn(1, 50)
}

/**
 * Input for keyword search tool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchByKeywordInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("keywords")
    val keywords: String,

    @JsonProperty("topK")
    val topK: Int = 10
) {
    fun getValidatedTopK(): Int = topK.coerceIn(1, 50)
}

/**
 * Input for SearchPagePdfAttachmentsTool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchPagePdfAttachmentsInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("pageId")
    val pageId: String
)
