package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseDetailUseCaseIn
import com.okestro.okchat.knowledge.application.dto.KnowledgeBaseDetailDto
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseEmailRepository
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetKnowledgeBaseDetailUseCase(
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val knowledgeBaseEmailRepository: KnowledgeBaseEmailRepository,
    private val userRepository: UserRepository,
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository
) {

    @Transactional(readOnly = true) // Use Transactional for read consistency if needed
    fun execute(input: GetKnowledgeBaseDetailUseCaseIn): KnowledgeBaseDetailDto {
        val caller = userRepository.findByEmail(input.callerEmail)
            ?: throw IllegalArgumentException("Caller not found")

        // Check Permission
        if (!canManageKb(caller, input.kbId)) {
            throw IllegalAccessException("Insufficient permissions")
        }

        val kb = knowledgeBaseRepository.findById(input.kbId).orElse(null)
            ?: throw NoSuchElementException("Knowledge Base not found")

        val emailConfigs = knowledgeBaseEmailRepository.findAllByKnowledgeBaseId(input.kbId)

        // Reconstruct Email Config
        val config = kb.config.toMutableMap()
        if (emailConfigs.isNotEmpty()) {
            val emailProviders = emailConfigs.associate { entity ->
                val type = entity.providerType.name.lowercase()
                type to entity.toEmailProviderConfig()
            }
            config["emailProviders"] = emailProviders
        }

        return KnowledgeBaseDetailDto(
            id = kb.id!!,
            name = kb.name,
            description = kb.description,
            type = kb.type,
            enabled = kb.enabled,
            createdBy = kb.createdBy,
            createdAt = kb.createdAt,
            updatedAt = kb.updatedAt,
            config = config
        )
    }

    private fun canManageKb(caller: com.okestro.okchat.user.model.entity.User, kbId: Long): Boolean {
        // 1. System Admin
        if (caller.role == UserRole.SYSTEM_ADMIN) return true

        // 2. Space Admin
        val membership = knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId)
        return membership?.role == KnowledgeBaseUserRole.ADMIN
    }
}
