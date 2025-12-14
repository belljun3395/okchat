package com.okestro.okchat.docs.client.user

data class UserSummaryDto(
    val id: Long,
    val email: String,
    val name: String,
    val role: String
)

interface UserClient {
    suspend fun getById(userId: Long): UserSummaryDto?
    suspend fun getByEmail(email: String): UserSummaryDto?
}
