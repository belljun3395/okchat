package com.okestro.okchat.permission.service.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "문서 검색 결과")
data class DocumentSearchResult(
    @field:Schema(description = "문서 ID")
    val id: String,

    @field:Schema(description = "문서 제목")
    val title: String? = null,

    @field:Schema(description = "문서 경로/URL")
    val url: String? = null,

    @field:Schema(description = "스페이스 키")
    val spaceKey: String? = null
)
