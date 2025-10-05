package com.okestro.okchat.chat.pipeline

/**
 * Represents a step in the chat processing pipeline
 * Each step transforms the context and passes it to the next step
 *
 * Steps can be conditionally executed based on the current context state,
 * enabling dynamic routing and optimization of the pipeline flow
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

    /**
     * Determine if this step should be executed based on the current context
     * Default implementation always returns true (unconditional execution)
     *
     * Override this method to implement conditional execution logic, such as:
     * - Skipping document search for simple greetings
     * - Bypassing RAG pipeline for general queries without search results
     * - Optimizing expensive operations based on query classification
     *
     * @param context Current pipeline context
     * @return true if step should execute, false to skip
     */
    fun shouldExecute(context: ChatContext): Boolean = true
}
