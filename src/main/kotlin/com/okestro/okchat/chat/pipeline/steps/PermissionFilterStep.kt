package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.DocumentChatPipelineStep
import com.okestro.okchat.chat.pipeline.copy
import com.okestro.okchat.permission.service.DocumentPermissionService
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
@Component
@Order(2)
class PermissionFilterStep(
    private val permissionFilterService: DocumentPermissionService
) : DocumentChatPipelineStep {

    /**
     * Only execute if user email is present (email-based requests)
     */
    override fun shouldExecute(context: ChatContext): Boolean {
        return context.input.userEmail != null && context.search != null
    }

    override suspend fun execute(context: ChatContext): ChatContext {
        val userEmail = context.input.userEmail
            ?: throw IllegalStateException("userEmail is required for permission filtering")

        val searchContext = context.search
            ?: throw IllegalStateException("Search context is required")

        log.info { "[${getStepName()}] Filtering search results for user: $userEmail" }

        val originalCount = searchContext.results.size
        val filteredResults = permissionFilterService.filterByUserEmail(
            searchContext.results,
            userEmail
        )

        log.info {
            "[${getStepName()}] Permission filtering completed: " +
                "original=$originalCount, filtered=${filteredResults.size}, " +
                "removed=${originalCount - filteredResults.size}"
        }

        return context.copy(
            search = ChatContext.Search(
                results = filteredResults,
                contextText = searchContext.contextText
            )
        )
    }

    override fun getStepName(): String = "Permission Filter"
}
