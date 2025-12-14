package com.okestro.okchat.docs.client.user

data class KnowledgeBaseEmailProviderDto(
    val providerType: String,
    val emailAddress: String,
    val authType: String,
    val clientId: String,
    val clientSecret: String,
    val tenantId: String?,
    val scopes: String?,
    val redirectUri: String?
)

interface KnowledgeBaseEmailClient {
    suspend fun getEmailProviders(kbId: Long): List<KnowledgeBaseEmailProviderDto>
    suspend fun replaceEmailProviders(kbId: Long, providers: List<KnowledgeBaseEmailProviderDto>)
}
