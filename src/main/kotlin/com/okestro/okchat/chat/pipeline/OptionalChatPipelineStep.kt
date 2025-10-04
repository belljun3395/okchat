package com.okestro.okchat.chat.pipeline

/**
 * Marker interface for optional steps in the chat pipeline.
 * The execution order of these steps is determined by the @Order annotation.
 */
interface OptionalChatPipelineStep : ChatPipelineStep
