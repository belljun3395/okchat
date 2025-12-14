package com.okestro.okchat.user.application.dto

import com.okestro.okchat.user.model.entity.User

data class GetUserByIdUseCaseIn(
    val userId: Long
)

data class GetUserByIdUseCaseOut(
    val user: User?
)
