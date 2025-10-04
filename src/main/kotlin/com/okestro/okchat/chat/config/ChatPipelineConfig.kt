package com.okestro.okchat.chat.config

import com.okestro.okchat.chat.pipeline.ChatPipelineStep
import com.okestro.okchat.chat.pipeline.steps.ContextBuildingStep
import com.okestro.okchat.chat.pipeline.steps.DocumentSearchStep
import com.okestro.okchat.chat.pipeline.steps.PromptGenerationStep
import com.okestro.okchat.chat.pipeline.steps.QueryAnalysisStep
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for chat processing pipeline
 * Defines the order of steps to be executed
 */
@Configuration
class ChatPipelineConfig {

    /**
     * Define the ordered list of pipeline steps
     * Steps are executed in this order
     */
    @Bean
    fun chatPipelineSteps(
        queryAnalysisStep: QueryAnalysisStep,
        documentSearchStep: DocumentSearchStep,
        contextBuildingStep: ContextBuildingStep,
        promptGenerationStep: PromptGenerationStep
    ): List<ChatPipelineStep> {
        return listOf(
            queryAnalysisStep,
            documentSearchStep,
            contextBuildingStep,
            promptGenerationStep
        )
    }
}
