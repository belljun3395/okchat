package com.okestro.okchat.permission.application.dto

data class GetAllowedPathsForUserUseCaseIn(
    val email: String
)

data class GetAllowedPathsForUserUseCaseOut(
    val paths: List<String>
)
