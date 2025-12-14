package com.okestro.okchat.ai.client.docs

interface DocsClient {
    suspend fun multiSearch(request: MultiSearchRequest): MultiSearchResponse
    suspend fun getAllowedPaths(email: String, knowledgeBaseId: Long?): List<String>
}
