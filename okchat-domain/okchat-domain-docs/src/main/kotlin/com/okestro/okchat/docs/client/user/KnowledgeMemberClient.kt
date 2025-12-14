package com.okestro.okchat.docs.client.user

import java.time.Instant

data class KnowledgeBaseMembershipDto(
    val knowledgeBaseId: Long,
    val userId: Long,
    val role: String,
    val approvedBy: Long?,
    val createdAt: Instant
)

interface KnowledgeMemberClient {
    suspend fun getMembership(kbId: Long, userId: Long): KnowledgeBaseMembershipDto?
    suspend fun getMembershipsByUserId(userId: Long): List<KnowledgeBaseMembershipDto>
    suspend fun addMember(kbId: Long, callerEmail: String, targetEmail: String, role: String)
}
