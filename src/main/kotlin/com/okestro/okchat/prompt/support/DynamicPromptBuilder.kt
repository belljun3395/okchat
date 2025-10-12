package com.okestro.okchat.prompt.support

import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.prompt.application.GetPromptUseCase
import com.okestro.okchat.prompt.application.dto.GetPromptUseCaseIn
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Build dynamic system prompts based on query type
 * Loads prompt templates from database with Redis caching
 */
@Component
class DynamicPromptBuilder(
    private val getPromptUseCase: GetPromptUseCase
) {
    suspend fun buildPrompt(queryType: QueryClassifier.QueryType): String {
        val basePrompt = getPromptUseCase.execute(GetPromptUseCaseIn(QueryClassifier.QueryType.BASE.name)).content
            ?: throw IllegalStateException("Base prompt not found in database")

        val specificGuidance = loadSpecificPrompt(queryType)

        val commonGuidelines = getPromptUseCase.execute(GetPromptUseCaseIn(QueryClassifier.QueryType.COMMON_GUIDELINES.name)).content
            ?: throw IllegalStateException("Common guidelines prompt not found in database")

        return "$basePrompt\n\n$specificGuidance\n$commonGuidelines"
    }

    private suspend fun loadSpecificPrompt(queryType: QueryClassifier.QueryType): String {
        val type = queryType.name
        return getPromptUseCase.execute(GetPromptUseCaseIn(type)).content
            ?: throw IllegalStateException("Prompt not found for query type: $type")
    }
}
