package com.okestro.okchat.knowledge.api.internal.dto

import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import java.time.Instant

data class InternalKnowledgeBaseMembershipResponse(
    val knowledgeBaseId: Long,
    val userId: Long,
    val role: KnowledgeBaseUserRole,
    val approvedBy: Long?,
    val createdAt: Instant
) {
    companion object {
        fun from(membership: KnowledgeBaseUser): InternalKnowledgeBaseMembershipResponse {
            return InternalKnowledgeBaseMembershipResponse(
                knowledgeBaseId = membership.knowledgeBaseId,
                userId = membership.userId,
                role = membership.role,
                approvedBy = membership.approvedBy,
                createdAt = membership.createdAt
            )
        }
    }
}
