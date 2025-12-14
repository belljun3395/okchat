package com.okestro.okchat.knowledge.api.internal

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseEmail
import com.okestro.okchat.knowledge.repository.KnowledgeBaseEmailRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/internal/api/v1/knowledge-bases/{kbId}/email-providers")
class KnowledgeBaseEmailInternalController(
    private val knowledgeBaseEmailRepository: KnowledgeBaseEmailRepository
) {

    @GetMapping
    fun getEmailProviders(
        @PathVariable kbId: Long
    ): List<InternalKnowledgeBaseEmailProviderResponse> {
        val entities = knowledgeBaseEmailRepository.findAllByKnowledgeBaseId(kbId)
        return entities.map(InternalKnowledgeBaseEmailProviderResponse::from)
    }

    @PutMapping
    @Transactional
    fun replaceEmailProviders(
        @PathVariable kbId: Long,
        @RequestBody request: ReplaceKnowledgeBaseEmailProvidersRequest
    ): ResponseEntity<Unit> {
        log.debug { "[Internal] Replace email providers: kbId=$kbId, count=${request.providers.size}" }

        knowledgeBaseEmailRepository.deleteByKnowledgeBaseId(kbId)

        request.providers.forEach { provider ->
            val providerType = runCatching { EmailProperties.EmailProviderType.valueOf(provider.providerType) }.getOrNull()
                ?: return ResponseEntity.badRequest().build()

            val authType = runCatching { EmailProperties.AuthType.valueOf(provider.authType) }.getOrNull()
                ?: EmailProperties.AuthType.OAUTH2

            knowledgeBaseEmailRepository.save(
                KnowledgeBaseEmail(
                    knowledgeBaseId = kbId,
                    providerType = providerType,
                    emailAddress = provider.emailAddress,
                    authType = authType,
                    clientId = provider.clientId,
                    clientSecret = provider.clientSecret,
                    tenantId = provider.tenantId,
                    scopes = provider.scopes,
                    redirectUri = provider.redirectUri
                )
            )
        }

        return ResponseEntity.ok().build()
    }
}

data class ReplaceKnowledgeBaseEmailProvidersRequest(
    val providers: List<InternalKnowledgeBaseEmailProviderDto> = emptyList()
)

data class InternalKnowledgeBaseEmailProviderDto(
    val providerType: String,
    val emailAddress: String,
    val authType: String,
    val clientId: String,
    val clientSecret: String,
    val tenantId: String?,
    val scopes: String?,
    val redirectUri: String?
)

data class InternalKnowledgeBaseEmailProviderResponse(
    val providerType: String,
    val emailAddress: String,
    val authType: String,
    val clientId: String,
    val clientSecret: String,
    val tenantId: String?,
    val scopes: String?,
    val redirectUri: String?
) {
    companion object {
        fun from(entity: KnowledgeBaseEmail): InternalKnowledgeBaseEmailProviderResponse {
            return InternalKnowledgeBaseEmailProviderResponse(
                providerType = entity.providerType.name,
                emailAddress = entity.emailAddress,
                authType = entity.authType.name,
                clientId = entity.clientId,
                clientSecret = entity.clientSecret,
                tenantId = entity.tenantId,
                scopes = entity.scopes,
                redirectUri = entity.redirectUri
            )
        }
    }
}

