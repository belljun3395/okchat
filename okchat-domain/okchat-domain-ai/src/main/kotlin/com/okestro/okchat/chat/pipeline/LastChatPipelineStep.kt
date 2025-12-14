package com.okestro.okchat.chat.pipeline

/**
 * Marker interface for the last step in the chat pipeline.
 */
interface LastChatPipelineStep : ChatPipelineStep {
    override suspend fun execute(context: ChatContext): CompleteChatContext
}
