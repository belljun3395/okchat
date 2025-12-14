package com.okestro.okchat.knowledge.application

import com.okestro.okchat.docs.client.user.KnowledgeBaseEmailClient
import com.okestro.okchat.docs.client.user.KnowledgeMemberClient
import com.okestro.okchat.docs.client.user.UserClient
import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseDetailUseCaseIn
import com.okestro.okchat.knowledge.application.dto.KnowledgeBaseDetailDto
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class GetKnowledgeBaseDetailUseCase(
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val userClient: UserClient,
    private val knowledgeMemberClient: KnowledgeMemberClient,
    private val knowledgeBaseEmailClient: KnowledgeBaseEmailClient
) {

    suspend fun execute(input: GetKnowledgeBaseDetailUseCaseIn): KnowledgeBaseDetailDto =
        withContext(Dispatchers.IO + MDCContext()) {
            val caller = userClient.getByEmail(input.callerEmail)
                ?: throw IllegalArgumentException("Caller not found: ${input.callerEmail}")

            if (!canManageKb(callerId = caller.id, callerRole = caller.role, kbId = input.kbId)) {
                throw IllegalAccessException("Insufficient permissions")
            }

            val kb = knowledgeBaseRepository.findById(input.kbId).orElse(null)
                ?: throw NoSuchElementException("Knowledge Base not found")

            val emailProviders = knowledgeBaseEmailClient.getEmailProviders(input.kbId)

            // Reconstruct Email Config
            val config = kb.config.toMutableMap()
            if (emailProviders.isNotEmpty()) {
                val providers = emailProviders.associate { provider ->
                    val key = provider.providerType.lowercase()
                    key to provider.toEmailProviderConfig()
                }
                config["emailProviders"] = providers
            }

            KnowledgeBaseDetailDto(
                id = requireNotNull(kb.id) { "Knowledge Base ID must not be null" },
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

    private suspend fun canManageKb(callerId: Long, callerRole: String, kbId: Long): Boolean {
        if (callerRole == "SYSTEM_ADMIN") return true
        val membership = knowledgeMemberClient.getMembership(kbId = kbId, userId = callerId)
        return membership?.role == "ADMIN"
    }

    private fun com.okestro.okchat.docs.client.user.KnowledgeBaseEmailProviderDto.toEmailProviderConfig(): EmailProperties.EmailProviderConfig {
        val providerType = runCatching { EmailProperties.EmailProviderType.valueOf(this.providerType) }.getOrNull()
            ?: throw IllegalArgumentException("Unsupported email provider type: ${this.providerType}")

        val authType = runCatching { EmailProperties.AuthType.valueOf(this.authType) }.getOrNull()
            ?: EmailProperties.AuthType.OAUTH2

        val scopes = when (providerType) {
            EmailProperties.EmailProviderType.GMAIL -> listOf("https://mail.google.com/")
            EmailProperties.EmailProviderType.OUTLOOK -> listOf(
                "https://outlook.office365.com/IMAP.AccessAsUser.All",
                "https://outlook.office365.com/SMTP.Send",
                "offline_access"
            )
        }

        return EmailProperties.EmailProviderConfig(
            type = providerType,
            host = if (providerType == EmailProperties.EmailProviderType.GMAIL) "imap.gmail.com" else "outlook.office365.com",
            port = 993,
            username = this.emailAddress,
            authType = authType,
            enabled = true,
            oauth2 = EmailProperties.OAuth2Config(
                clientId = this.clientId,
                clientSecret = this.clientSecret,
                tenantId = this.tenantId ?: "common",
                scopes = scopes,
                redirectUri = this.redirectUri ?: "http://localhost:8080/oauth2/callback"
            )
        )
    }
}
