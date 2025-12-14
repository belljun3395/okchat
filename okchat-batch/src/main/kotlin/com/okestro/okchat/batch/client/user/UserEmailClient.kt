package com.okestro.okchat.batch.client.user

import com.okestro.okchat.batch.client.user.dto.InternalPollEmailResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class UserEmailClient(
    @Qualifier("userInternalWebClient")
    private val webClient: WebClient
) {
    suspend fun pollEmails(knowledgeBaseId: Long): InternalPollEmailResponse {
        return webClient.post()
            .uri("/internal/api/v1/emails/poll/{knowledgeBaseId}", knowledgeBaseId)
            .retrieve()
            .awaitBody()
    }
}
