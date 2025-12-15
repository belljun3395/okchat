package com.okestro.okchat.search.model

data class Document(
    val id: String,
    val title: String? = null,
    var content: String? = null,
    val path: String? = null,
    val spaceKey: String? = null,
    val knowledgeBaseId: Long? = null,
    val keywords: String? = null,
    val score: SearchScore.SimilarityScore? = null,
    val webUrl: String? = null
)
