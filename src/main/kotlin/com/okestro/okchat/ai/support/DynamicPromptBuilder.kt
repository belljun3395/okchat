package com.okestro.okchat.ai.support

import com.okestro.okchat.ai.service.PromptService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Build dynamic system prompts based on query type
 * Loads prompt templates from database with Redis caching
 */
@Component
class DynamicPromptBuilder(
    private val promptService: PromptService
) {
    suspend fun buildPrompt(queryType: QueryClassifier.QueryType): String {
        val basePrompt = promptService.getLatestPrompt(QueryClassifier.QueryType.BASE.name)
            ?: throw IllegalStateException("Base prompt not found in database")

        val specificGuidance = loadSpecificPrompt(queryType)

        val commonGuidelines = promptService.getLatestPrompt(QueryClassifier.QueryType.COMMON_GUIDELINES.name)
            ?: throw IllegalStateException("Common guidelines prompt not found in database")

        return "$basePrompt\n\n$specificGuidance\n$commonGuidelines"
    }

    private suspend fun loadSpecificPrompt(queryType: QueryClassifier.QueryType): String {
        val type = queryType.name
        return promptService.getLatestPrompt(type)
            ?: throw IllegalStateException("Prompt not found for query type: $type")
    }
}
