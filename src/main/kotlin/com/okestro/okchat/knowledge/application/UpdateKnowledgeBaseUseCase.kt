package com.okestro.okchat.knowledge.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.knowledge.application.dto.UpdateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.model.config.KnowledgeBaseEmailConfig
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseEmail
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseEmailRepository
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UpdateKnowledgeBaseUseCase(
    private val userRepository: UserRepository,
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository,
    private val knowledgeBaseEmailRepository: KnowledgeBaseEmailRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun execute(input: UpdateKnowledgeBaseUseCaseIn): KnowledgeBase {
        val caller = userRepository.findByEmail(input.callerEmail)
            ?: throw IllegalArgumentException("Caller not found")

        // Permission check
        if (!canManageKb(caller, input.kbId)) {
            throw IllegalAccessException("Insufficient permissions")
        }

        val kb = knowledgeBaseRepository.findById(input.kbId).orElse(null)
            ?: throw NoSuchElementException("Knowledge Base not found")

        // Update fields
        val sanitizedConfig = input.config.toMutableMap()
        sanitizedConfig.remove("emailProviders")

        val updatedKb = kb.copy(
            name = input.name,
            description = input.description,
            type = input.type,
            config = sanitizedConfig,
            updatedAt = Instant.now()
        )

        val savedKb = knowledgeBaseRepository.save(updatedKb)

        // Update Email Config
        try {
            saveEmailConfig(savedKb.id!!, input.config)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return savedKb
    }

    private fun canManageKb(caller: com.okestro.okchat.user.model.entity.User, kbId: Long): Boolean {
        // 1. System Admin
        if (caller.role == UserRole.SYSTEM_ADMIN) return true

        // 2. Space Admin
        val membership = knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId)
        return membership?.role == KnowledgeBaseUserRole.ADMIN
    }

    private fun saveEmailConfig(kbId: Long, configMap: Map<String, Any>) {
        // Delete existing
        knowledgeBaseEmailRepository.deleteByKnowledgeBaseId(kbId)

        if (!configMap.containsKey("emailProviders")) return

        val kbConfig = try {
            objectMapper.convertValue(configMap, KnowledgeBaseEmailConfig::class.java)
        } catch (e: Exception) {
            return
        }

        kbConfig.emailProviders.values.forEach { provider ->
            if (provider.enabled) {
                val entity = KnowledgeBaseEmail(
                    knowledgeBaseId = kbId,
                    providerType = provider.type,
                    emailAddress = provider.username,
                    authType = provider.authType,
                    clientId = provider.oauth2.clientId,
                    clientSecret = provider.oauth2.clientSecret,
                    tenantId = provider.oauth2.tenantId,
                    scopes = provider.oauth2.scopes.joinToString(","),
                    redirectUri = provider.oauth2.redirectUri
                )
                knowledgeBaseEmailRepository.save(entity)
            }
        }
    }
}
