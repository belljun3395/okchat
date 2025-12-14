package com.okestro.okchat.task.application.dto

data class GetRecentTaskExecutionsUseCaseIn(
    val limit: Int = 50
)

data class GetTaskExecutionByIdUseCaseIn(
    val id: Long
)

data class GetTaskStatsUseCaseIn(
    val dummy: Boolean = true // Empty input usually
)

data class GetTaskParamsUseCaseIn(
    val executionId: Long
)
