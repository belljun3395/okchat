package com.okestro.okchat.knowledge.application.dto

import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import java.time.Instant

data class GetKnowledgeBaseMembersUseCaseIn(
    val kbId: Long,
    val callerEmail: String
)

data class KnowledgeBaseMemberDto(
    val userId: Long,
    val email: String,
    val name: String,
    val role: KnowledgeBaseUserRole,
    val createdAt: Instant,
    val approvedBy: String?
)

data class AddKnowledgeBaseMemberUseCaseIn(
    val kbId: Long,
    val callerEmail: String,
    val targetEmail: String,
    val role: KnowledgeBaseUserRole
)

data class RemoveKnowledgeBaseMemberUseCaseIn(
    val kbId: Long,
    val callerEmail: String,
    val targetUserId: Long
)

data class UpdateKnowledgeBaseMemberRoleUseCaseIn(
    val kbId: Long,
    val callerEmail: String,
    val targetUserId: Long,
    val newRole: KnowledgeBaseUserRole
)
