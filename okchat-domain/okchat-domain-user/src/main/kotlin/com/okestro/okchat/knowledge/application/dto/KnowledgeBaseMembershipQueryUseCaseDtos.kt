package com.okestro.okchat.knowledge.application.dto

import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser

data class GetKnowledgeBaseMembershipUseCaseIn(
    val kbId: Long,
    val userId: Long
)

data class GetKnowledgeBaseMembershipUseCaseOut(
    val membership: KnowledgeBaseUser?
)

data class GetKnowledgeBaseMembershipsByUserIdUseCaseIn(
    val userId: Long
)

data class GetKnowledgeBaseMembershipsByUserIdUseCaseOut(
    val memberships: List<KnowledgeBaseUser>
)
