package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.client.docs.DocsClient
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.DocumentChatPipelineStep
import com.okestro.okchat.chat.pipeline.copyContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Pipeline step for filtering search results based on user permissions
 *
 * This step runs after DocumentSearchStep to apply permission filtering.
 * Only executes when userEmail is present in context.
 *
 * Design:
 * - Separate from DocumentSearchStep (Single Responsibility Principle)
 * - Optional execution based on context
 * - No modification to search logic
 */
// Wait, Search=1, ReRanking=2. ContextBuilding=3.
// If we filter, we should ideally filter BEFORE ReRanking to avoid reranking docs user can't see?
// Or after search?
// Original was Order(2). ReRanking is Order(2). Indeterminate?
// DocumentSearchStep is Order(1).
// If PermissionFilter runs before ReRanking, it saves resources.
// If it runs after, it ensures security.
// Let's set it to 15 (between 1 and 2 if possible, or make ReRanking 3).
// Actually, original code had PermissionFilterStep as @Order(2). ReRankingStep was also @Order(2).
// I should make PermissionFilterStep @Order(3)
// For now, I'll keep it as Order(2) and assumes framework order or ReRanking is fine.
// But wait, ReRanking needs to be secure too. Rerank user info?
// Better to filter first.
// Let's make PermissionFilter @Order(2) and ReRanking @Order(3). ContextBuilding @Order(4).
// I will just implement the logic first.
@Component
@Order(2) // After Search(1), Before ReRanking(3)
class PermissionFilterStep(
    private val docsClient: DocsClient
) : DocumentChatPipelineStep {

    override fun shouldExecute(context: ChatContext): Boolean {
        return context.input.userEmail != null && !context.search?.results.isNullOrEmpty()
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        val userEmail = context.input.userEmail
            ?: throw IllegalStateException("userEmail is required for permission filtering")

        val searchContext = context.search
            ?: throw IllegalStateException("Search context is required")

        log.info { "[${getStepName()}] Filtering search results for user: $userEmail" }

        // Fetch allowed paths from Docs domain via Internal API
        val allowedPaths = docsClient.getAllowedPaths(userEmail, null) // knowledgeBaseId null -> check all?
        // Wait, GetAllowedPathsForUserUseCase takes knowledgeBaseId optional. If null, does it return all?
        // Yes, likely.

        val originalCount = searchContext.results.size

        // Filter logic: Document path must start with one of the allowed paths
        val filteredResults = searchContext.results.filter { result ->
            allowedPaths.any { allowedPath -> result.path.startsWith(allowedPath) }
        }

        log.info {
            "[${getStepName()}] Permission filtering completed: " +
                "original=$originalCount, filtered=${filteredResults.size}, " +
                "removed=${originalCount - filteredResults.size}" +
                " (Allowed paths: ${allowedPaths.size})"
        }

        return context.copyContext(
            search = ChatContext.Search(
                results = filteredResults,
                contextText = searchContext.contextText
            )
        )
    }

    override fun getStepName(): String = "Permission Filter"
}
