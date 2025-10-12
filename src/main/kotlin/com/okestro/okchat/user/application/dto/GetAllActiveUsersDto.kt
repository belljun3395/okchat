package com.okestro.okchat.user.application.dto

import com.okestro.okchat.user.model.entity.User

class GetAllActiveUsersUseCaseIn

data class GetAllActiveUsersUseCaseOut(
    val users: List<User>
)
