package com.okestro.okchat.docs.client.user

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Component
class WebClientKnowledgeMemberClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${internal.services.user.url:http://localhost:8080}")
    private val userServiceBaseUrl: String
) : KnowledgeMemberClient {

    private val client: WebClient = webClientBuilder
        .baseUrl(userServiceBaseUrl)
        .build()

    private val membershipListType = object : ParameterizedTypeReference<List<KnowledgeBaseMembershipDto>>() {}

    override suspend fun getMembership(kbId: Long, userId: Long): KnowledgeBaseMembershipDto? {
        return client.get()
            .uri("/internal/api/v1/knowledge-bases/{kbId}/members/{userId}", kbId, userId)
            .exchangeToMono { response ->
                if (response.statusCode().is2xxSuccessful) {
                    response.bodyToMono(KnowledgeBaseMembershipDto::class.java)
                } else {
                    Mono.empty()
                }
            }
            .awaitSingleOrNull()
    }

    override suspend fun getMembershipsByUserId(userId: Long): List<KnowledgeBaseMembershipDto> {
        return client.get()
            .uri("/internal/api/v1/users/{userId}/knowledge-bases", userId)
            .exchangeToMono { response ->
                if (response.statusCode().is2xxSuccessful) {
                    response.bodyToMono(membershipListType)
                } else {
                    Mono.just(emptyList())
                }
            }
            .awaitSingle()
    }

    override suspend fun addMember(kbId: Long, callerEmail: String, targetEmail: String, role: String) {
        val request = AddKnowledgeBaseMemberInternalRequest(
            callerEmail = callerEmail,
            targetEmail = targetEmail,
            role = role
        )

        try {
            client.post()
                .uri("/internal/api/v1/knowledge-bases/{kbId}/members", kbId)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .awaitSingle()
        } catch (e: WebClientResponseException) {
            throw IllegalStateException("Failed to add KB member: kbId=$kbId, status=${e.statusCode}, message=${e.responseBodyAsString}", e)
        }
    }
}

private data class AddKnowledgeBaseMemberInternalRequest(
    val callerEmail: String,
    val targetEmail: String,
    val role: String
)
