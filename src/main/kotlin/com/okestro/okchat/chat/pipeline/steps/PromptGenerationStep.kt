package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.support.DynamicPromptBuilder
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.ChatPipelineStep
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Generate prompt for AI
 * Uses dynamic prompt based on query type
 */
@Component
class PromptGenerationStep : ChatPipelineStep {

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Generating prompt" }

        val queryAnalysis = context.queryAnalysis
            ?: throw IllegalStateException("Query analysis not available")

        val contextText = context.contextText
            ?: throw IllegalStateException("Context text not available")

        // Build dynamic prompt based on query type
        val promptTemplate = DynamicPromptBuilder.buildPrompt(queryAnalysis.type)
        log.info { "[${getStepName()}] Using ${queryAnalysis.type} specialized prompt" }

        // Create prompt with context and question
        val template = PromptTemplate(promptTemplate)
        val prompt = template.create(
            mapOf(
                "context" to contextText,
                "question" to context.userMessage
            )
        )

        return context.copy(promptText = prompt.contents)
    }

    override fun getStepName(): String = "Prompt Generation"
}
