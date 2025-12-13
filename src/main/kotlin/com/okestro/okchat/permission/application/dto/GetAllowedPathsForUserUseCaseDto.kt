package com.okestro.okchat.permission.application.dto

data class GetAllowedPathsForUserUseCaseIn(
    val email: String,
    val knowledgeBaseId: Long? = null
)

data class GetAllowedPathsForUserUseCaseOut(
    val paths: List<String>
)
