package com.okestro.okchat.knowledge.model.dto

import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import java.time.Instant

data class KnowledgeBaseMemberResponse(
    val userId: Long,
    val email: String,
    val name: String,
    val role: KnowledgeBaseUserRole,
    val createdAt: Instant,
    val approvedBy: String? // Name of the approver
)
