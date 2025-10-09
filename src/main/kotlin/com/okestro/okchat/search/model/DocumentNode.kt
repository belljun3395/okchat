package com.okestro.okchat.search.model

data class DocumentNode(
    val name: String,
    val path: String,
    val children: List<DocumentNode>,
    val documents: List<Document>
)
