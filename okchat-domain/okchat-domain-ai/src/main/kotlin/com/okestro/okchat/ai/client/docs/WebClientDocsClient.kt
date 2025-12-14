package com.okestro.okchat.ai.client.docs

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class WebClientDocsClient(
    private val webClientBuilder: WebClient.Builder,
    @Value("\${internal.services.docs.url:http://localhost:8080}") private val docsServiceUrl: String
) : DocsClient {

    private val client by lazy {
        webClientBuilder.baseUrl(docsServiceUrl).build()
    }

    override suspend fun multiSearch(request: MultiSearchRequest): MultiSearchResponse {
        return client.post()
            .uri("/internal/api/v1/search/multi")
            .bodyValue(request)
            .retrieve()
            .awaitBody()
    }

    override suspend fun getAllowedPaths(email: String, knowledgeBaseId: Long?): List<String> {
        return client.get()
            .uri { builder ->
                builder.path("/internal/api/v1/permissions/allowed-paths")
                    .queryParam("email", email)
                    .apply {
                        if (knowledgeBaseId != null) {
                            queryParam("knowledgeBaseId", knowledgeBaseId)
                        }
                    }
                    .build()
            }
            .retrieve()
            .bodyToMono(InternalGetAllowedPathsResponse::class.java)
            .map { it.paths }
            .awaitSingle()
    }

    // Response wrapper for permissions endpoint
    private data class InternalGetAllowedPathsResponse(
        val paths: List<String>
    )
}
