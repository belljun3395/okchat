package com.okestro.okchat.knowledge.application.dto

import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import java.time.Instant

data class GetKnowledgeBaseDetailUseCaseIn(
    val kbId: Long,
    val callerEmail: String
)

data class KnowledgeBaseDetailDto(
    val id: Long,
    val name: String,
    val description: String?,
    val type: KnowledgeBaseType,
    val enabled: Boolean,
    val createdBy: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val config: Map<String, Any>
)

data class GetAllKnowledgeBasesUseCaseIn(
    val callerEmail: String? = null // Optional if we want to filter by user later
)

data class CreateKnowledgeBaseUseCaseIn(
    val callerEmail: String,
    val name: String,
    val description: String?,
    val type: KnowledgeBaseType,
    val config: Map<String, Any>
)

data class UpdateKnowledgeBaseUseCaseIn(
    val kbId: Long,
    val callerEmail: String,
    val name: String,
    val description: String?,
    val type: KnowledgeBaseType,
    val config: Map<String, Any>
)
