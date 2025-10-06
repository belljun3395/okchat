package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.support.DynamicPromptBuilder
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.CompleteChatContext
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

    override suspend fun execute(context: ChatContext): CompleteChatContext {
        log.info { "[${getStepName()}] Generating prompt" }

        val analysis = context.analysis
            ?: throw IllegalStateException("Analysis not available (FirstChatPipelineStep must run)")

        // Context text is optional - may not be available if document search was skipped or found no results
        val contextText = context.search?.contextText ?: run {
            log.warn { "[${getStepName()}] No context text available - generating prompt without RAG context" }
            "No search results found. Please answer based on general knowledge."
        }

        // Build dynamic prompt based on query type (from externalized templates)
        val promptTemplate = dynamicPromptBuilder.buildPrompt(analysis.queryAnalysis.type)
        log.info { "[${getStepName()}] Using ${analysis.queryAnalysis.type} specialized prompt (with RAG context: ${context.search?.contextText != null})" }

        // Create prompt with context and question
        val template = PromptTemplate(promptTemplate)
        val prompt = template.create(
            mapOf(
                "context" to contextText,
                "question" to context.input.message
            )
        )

        return CompleteChatContext(
            input = context.input,
            analysis = context.analysis,
            search = context.search,
            prompt = CompleteChatContext.Prompt(prompt.contents)
        )
    }

    override fun getStepName(): String = "Prompt Generation"
}
