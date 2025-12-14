package com.okestro.okchat.docs.client.user

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class WebClientUserClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${internal.services.user.url:http://localhost:8080}")
    private val userServiceBaseUrl: String
) : UserClient {

    private val client: WebClient = webClientBuilder
        .baseUrl(userServiceBaseUrl)
        .build()

    override suspend fun getById(userId: Long): UserSummaryDto? {
        return client.get()
            .uri("/internal/api/v1/users/{id}", userId)
            .exchangeToMono { response ->
                if (response.statusCode().is2xxSuccessful) {
                    response.bodyToMono(UserSummaryDto::class.java)
                } else {
                    Mono.empty()
                }
            }
            .awaitSingleOrNull()
    }

    override suspend fun getByEmail(email: String): UserSummaryDto? {
        return client.get()
            .uri { it.path("/internal/api/v1/users/by-email").queryParam("email", email).build() }
            .exchangeToMono { response ->
                if (response.statusCode().is2xxSuccessful) {
                    response.bodyToMono(UserSummaryDto::class.java)
                } else {
                    Mono.empty()
                }
            }
            .awaitSingleOrNull()
    }
}
