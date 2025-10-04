package com.okestro.okchat.chat.pipeline

/**
 * Represents a step in the chat processing pipeline
 * Each step transforms the context and passes it to the next step
 */
interface ChatPipelineStep {

    /**
     * Execute this step and return the transformed context
     */
    suspend fun execute(context: ChatContext): ChatContext

    /**
     * Name of this step for logging purposes
     */
    fun getStepName(): String
}
