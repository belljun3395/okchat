package com.okestro.okchat.confluence.client

data class SpaceListResponse(
    val results: List<Space>,
    val _links: Links? = null
)

data class Space(
    val id: String,
    val key: String,
    val name: String,
    val type: String? = null,
    val status: String? = null
)

data class PageListResponse(
    val results: List<Page>,
    val _links: Links? = null
)

data class Page(
    val id: String,
    val title: String,
    val parentId: String? = null,
    val parentType: String? = null,
    val position: Int? = null,
    val spaceId: String? = null,
    val status: String? = null,
    val version: Version? = null,
    val type: String? = null
)

data class Version(
    val number: Int,
    val message: String? = null,
    val createdAt: String? = null
)

data class Links(
    val next: String? = null,
    val prev: String? = null,
    val base: String? = null
)

data class FolderResponse(
    val id: String,
    val type: String? = null,
    val status: String? = null,
    val title: String,
    val parentId: String? = null,
    val parentType: String? = null,
    val position: Int? = null,
    val authorId: String? = null,
    val ownerId: String? = null,
    val createdAt: String? = null,
    val version: Version? = null,
    val _links: Links? = null
)
