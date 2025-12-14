package com.okestro.okchat.user.application.dto

import com.okestro.okchat.user.model.entity.User

data class FindOrCreateUserUseCaseIn(
    val email: String,
    val name: String? = null
)

data class FindOrCreateUserUseCaseOut(
    val user: User,
    val isNewUser: Boolean
)
