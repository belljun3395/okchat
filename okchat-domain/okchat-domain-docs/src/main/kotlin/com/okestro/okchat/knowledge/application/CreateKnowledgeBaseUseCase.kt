package com.okestro.okchat.knowledge.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.docs.client.user.KnowledgeBaseEmailClient
import com.okestro.okchat.docs.client.user.KnowledgeBaseEmailProviderDto
import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.knowledge.application.dto.CreateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.model.config.KnowledgeBaseEmailConfig
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class CreateKnowledgeBaseUseCase(
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val objectMapper: ObjectMapper,
    private val userClient: UserClient,
    private val knowledgeMemberClient: KnowledgeMemberClient,
    private val knowledgeBaseEmailClient: KnowledgeBaseEmailClient
) {

    @Transactional(transactionManager = "transactionManager")
    suspend fun execute(input: CreateKnowledgeBaseUseCaseIn): KnowledgeBase = withContext(Dispatchers.IO + MDCContext()) {
        val caller = userClient.getByEmail(input.callerEmail)
            ?: throw IllegalArgumentException("Caller not found: ${input.callerEmail}")

        if (caller.role != "SYSTEM_ADMIN") {
            throw IllegalAccessException("Only System Admin can create Knowledge Bases")
        }

        val now = Instant.now()

        // Sanitize config - remove emailProviders to be stored separately
        val sanitizedConfig = input.config.toMutableMap()
        sanitizedConfig.remove("emailProviders")

        val newKb = KnowledgeBase(
            name = input.name,
            description = input.description,
            type = input.type,
            config = sanitizedConfig,
            createdBy = caller.id,
            createdAt = now,
            updatedAt = now
        )

        val savedKb = knowledgeBaseRepository.save(newKb)
        val kbId = requireNotNull(savedKb.id) { "Knowledge Base ID must not be null" }

        // Save Email Config if exists (stored in user-domain)
        val emailProviders = extractEmailProviders(input.config)
        knowledgeBaseEmailClient.replaceEmailProviders(kbId, emailProviders)

        // Add creator as ADMIN of the KB (managed by user-domain)
        try {
            knowledgeMemberClient.addMember(
                kbId = kbId,
                callerEmail = input.callerEmail,
                targetEmail = input.callerEmail,
                role = "ADMIN"
            )
        } catch (e: Exception) {
            log.warn(e) { "Failed to add creator as KB ADMIN: kbId=$kbId, caller=${input.callerEmail}" }
        }

        savedKb
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
