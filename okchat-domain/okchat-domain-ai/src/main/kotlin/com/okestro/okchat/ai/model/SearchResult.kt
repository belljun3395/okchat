package com.okestro.okchat.ai.model

data class SearchResult(
    val id: String,
    val title: String,
    val content: String,
    val path: String,
    val spaceKey: String,
    val knowledgeBaseId: Long,
    val keywords: String = "",
    val score: Double = 0.0,
    val type: String = "CONTENT", // KEYWORD, TITLE, CONTENT, PATH
    val pageId: String = "",
    val webUrl: String = "",
    val downloadUrl: String = ""
) : Comparable<SearchResult> {
    override fun compareTo(other: SearchResult): Int = other.score.compareTo(this.score)
}
