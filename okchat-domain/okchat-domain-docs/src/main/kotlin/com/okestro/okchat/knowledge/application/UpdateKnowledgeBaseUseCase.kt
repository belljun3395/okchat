package com.okestro.okchat.knowledge.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.docs.client.user.KnowledgeBaseEmailClient
import com.okestro.okchat.docs.client.user.KnowledgeBaseEmailProviderDto
import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.knowledge.application.dto.UpdateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.model.config.KnowledgeBaseEmailConfig
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class UpdateKnowledgeBaseUseCase(
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val objectMapper: ObjectMapper,
    private val userClient: UserClient,
    private val knowledgeMemberClient: KnowledgeMemberClient,
    private val knowledgeBaseEmailClient: KnowledgeBaseEmailClient
) {

    suspend fun execute(input: UpdateKnowledgeBaseUseCaseIn): KnowledgeBase = withContext(Dispatchers.IO + MDCContext()) {
        val caller = userClient.getByEmail(input.callerEmail)
            ?: throw IllegalArgumentException("Caller not found: ${input.callerEmail}")

        if (!canManageKb(callerId = caller.id, callerRole = caller.role, kbId = input.kbId)) {
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
        val kbId = requireNotNull(savedKb.id) { "Knowledge Base ID must not be null" }

        // Update Email Config (stored in user-domain)
        val emailProviders = extractEmailProviders(input.config)
        try {
            knowledgeBaseEmailClient.replaceEmailProviders(kbId, emailProviders)
        } catch (e: Exception) {
            log.warn(e) { "Failed to replace KB email providers: kbId=$kbId" }
        }

        savedKb
    }

    private suspend fun canManageKb(callerId: Long, callerRole: String, kbId: Long): Boolean {
        if (callerRole == "SYSTEM_ADMIN") return true
        val membership = knowledgeMemberClient.getMembership(kbId = kbId, userId = callerId)
        return membership?.role == "ADMIN"
    }

    private fun extractEmailProviders(configMap: Map<String, Any>): List<KnowledgeBaseEmailProviderDto> {
        if (!configMap.containsKey("emailProviders")) return emptyList()

        val kbConfig = runCatching { objectMapper.convertValue(configMap, KnowledgeBaseEmailConfig::class.java) }
            .getOrNull()
            ?: return emptyList()

        return kbConfig.emailProviders.values
            .filter { it.enabled }
            .map { provider ->
                KnowledgeBaseEmailProviderDto(
                    providerType = provider.type.name,
                    emailAddress = provider.username,
                    authType = provider.authType.name,
                    clientId = provider.oauth2.clientId,
                    clientSecret = provider.oauth2.clientSecret,
                    tenantId = provider.oauth2.tenantId,
                    scopes = provider.oauth2.scopes.joinToString(","),
                    redirectUri = provider.oauth2.redirectUri
                )
            }
    }
}
