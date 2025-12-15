package com.okestro.okchat.confluence.tools.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Type-safe representations of Confluence tool inputs.
 * Replaces untyped Map<String, Any> usage for better type safety.
 */

/**
 * Input for GetPageByIdConfluenceTool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GetPageByIdInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("pageId")
    val pageId: String
)

/**
 * Input for GetSpaceByKeyConfluenceTool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GetSpaceByKeyInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("spaceKey")
    val spaceKey: String
)

/**
 * Input for GetAllChildPagesConfluenceTool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GetAllChildPagesInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("pageId")
    val pageId: String,

    @JsonProperty("maxDepth")
    val maxDepth: Int = 10
) {
    fun getValidatedMaxDepth(): Int = maxDepth.coerceIn(1, 20)
}

/**
 * Input for GetPagesBySpaceIdConfluenceTool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GetPagesBySpaceIdInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("spaceId")
    val spaceId: String,

    @JsonProperty("limit")
    val limit: Int = 25
) {
    fun getValidatedLimit(): Int = limit.coerceIn(1, 100)
}

/**
 * Input for GetSpaceContentHierarchyConfluenceTool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GetSpaceContentHierarchyInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("spaceId")
    val spaceId: String,

    @JsonProperty("maxDepth")
    val maxDepth: Int = 3
)

/**
 * Input for GetFolderByIdConfluenceTool
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GetFolderByIdInput(
    @JsonProperty("thought")
    val thought: String? = null,

    @JsonProperty("folderId")
    val folderId: String
)
