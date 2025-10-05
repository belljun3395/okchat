package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.support.DynamicPromptBuilder
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.LastChatPipelineStep
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Generate prompt for AI
 * Uses dynamic prompt based on query type
 * Prompts are externalized in resources/prompts/ for easy customization
 */
@Component
class PromptGenerationStep(
    private val dynamicPromptBuilder: DynamicPromptBuilder
) : LastChatPipelineStep {

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Generating prompt" }

        val queryAnalysis = context.queryAnalysis
            ?: throw IllegalStateException("Query analysis not available (FirstChatPipelineStep must run)")

        // Context text is optional - may not be available if document search was skipped or found no results
        val contextText = context.contextText ?: run {
            log.warn { "[${getStepName()}] No context text available - generating prompt without RAG context" }
            "검색 결과 없음. 일반적인 지식을 바탕으로 답변해주세요."
        }

        // Build dynamic prompt based on query type (from externalized templates)
        val promptTemplate = dynamicPromptBuilder.buildPrompt(queryAnalysis.type)
        log.info { "[${getStepName()}] Using ${queryAnalysis.type} specialized prompt (with RAG context: ${context.contextText != null})" }

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
