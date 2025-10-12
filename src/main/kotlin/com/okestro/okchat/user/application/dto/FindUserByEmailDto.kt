package com.okestro.okchat.user.application.dto

import com.okestro.okchat.user.model.User

data class FindUserByEmailUseCaseIn(
    val email: String
)

data class FindUserByEmailUseCaseOut(
    val user: User?
)
