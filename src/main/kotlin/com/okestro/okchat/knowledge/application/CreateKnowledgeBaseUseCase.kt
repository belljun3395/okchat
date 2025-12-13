package com.okestro.okchat.knowledge.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.knowledge.application.dto.CreateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.model.config.KnowledgeBaseEmailConfig
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseEmail
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser
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
class CreateKnowledgeBaseUseCase(
    private val userRepository: UserRepository,
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository,
    private val knowledgeBaseEmailRepository: KnowledgeBaseEmailRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun execute(input: CreateKnowledgeBaseUseCaseIn): KnowledgeBase {
        val caller = userRepository.findByEmail(input.callerEmail)
            ?: throw IllegalArgumentException("Caller not found")

        if (caller.role != UserRole.SYSTEM_ADMIN) {
            throw IllegalAccessException("Only System Admin can create Knowledge Bases")
        }

        // Sanitize config - remove emailProviders to be stored separately
        val sanitizedConfig = input.config.toMutableMap()
        sanitizedConfig.remove("emailProviders")

        val newKb = KnowledgeBase(
            name = input.name,
            description = input.description,
            type = input.type,
            config = sanitizedConfig,
            createdBy = caller.id!!,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val savedKb = knowledgeBaseRepository.save(newKb)

        // Save Email Config if exists
        try {
            saveEmailConfig(savedKb.id!!, input.config)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Add creator as ADMIN of the KB
        val adminMember = KnowledgeBaseUser(
            userId = requireNotNull(caller.id) { "Caller ID must not be null" },
            knowledgeBaseId = savedKb.id!!,
            role = KnowledgeBaseUserRole.ADMIN,
            approvedBy = caller.id,
            createdAt = Instant.now()
        )
        knowledgeBaseUserRepository.save(adminMember)

        return savedKb
    }

    private fun saveEmailConfig(kbId: Long, configMap: Map<String, Any>) {
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
