package com.okestro.okchat.user.api.internal.dto

import com.okestro.okchat.user.model.entity.User

data class InternalUserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: String
) {
    companion object {
        fun from(user: User): InternalUserResponse =
            InternalUserResponse(
                id = requireNotNull(user.id) { "User id must not be null" },
                email = user.email,
                name = user.name,
                role = user.role.name
            )
    }
}

