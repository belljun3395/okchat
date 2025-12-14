package com.okestro.okchat.docs.client.user

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Component
class WebClientKnowledgeBaseEmailClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${internal.services.user.url:http://localhost:8080}")
    private val userServiceBaseUrl: String
) : KnowledgeBaseEmailClient {

    private val client: WebClient = webClientBuilder
        .baseUrl(userServiceBaseUrl)
        .build()

    private val providerListType = object : ParameterizedTypeReference<List<KnowledgeBaseEmailProviderDto>>() {}

    override suspend fun getEmailProviders(kbId: Long): List<KnowledgeBaseEmailProviderDto> {
        return client.get()
            .uri("/internal/api/v1/knowledge-bases/{kbId}/email-providers", kbId)
            .exchangeToMono { response ->
                if (response.statusCode().is2xxSuccessful) {
                    response.bodyToMono(providerListType)
                } else {
                    Mono.just(emptyList())
                }
            }
            .awaitSingle()
    }

    override suspend fun replaceEmailProviders(kbId: Long, providers: List<KnowledgeBaseEmailProviderDto>) {
        try {
            client.put()
                .uri("/internal/api/v1/knowledge-bases/{kbId}/email-providers", kbId)
                .bodyValue(ReplaceEmailProvidersInternalRequest(providers = providers))
                .retrieve()
                .toBodilessEntity()
                .awaitSingle()
        } catch (e: WebClientResponseException) {
            throw IllegalStateException(
                "Failed to replace email providers: kbId=$kbId, status=${e.statusCode}, message=${e.responseBodyAsString}",
                e
            )
        }
    }
}

private data class ReplaceEmailProvidersInternalRequest(
    val providers: List<KnowledgeBaseEmailProviderDto>
)
