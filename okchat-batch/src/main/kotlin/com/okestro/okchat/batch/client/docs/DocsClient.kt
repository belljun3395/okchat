package com.okestro.okchat.batch.client.docs

import com.okestro.okchat.batch.client.docs.dto.InternalConfluenceSyncResponse
import com.okestro.okchat.batch.client.docs.dto.InternalEnabledKnowledgeBaseResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class DocsClient(
    @Qualifier("docsInternalWebClient")
    private val webClient: WebClient
) {

    suspend fun getEnabledKnowledgeBases(): List<InternalEnabledKnowledgeBaseResponse> {
        return webClient.get()
            .uri("/internal/api/v1/knowledge-bases/enabled")
            .retrieve()
            .awaitBody()
    }

    suspend fun syncConfluence(): InternalConfluenceSyncResponse {
        return webClient.post()
            .uri("/internal/api/v1/confluence/sync")
            .retrieve()
            .awaitBody()
    }
}
