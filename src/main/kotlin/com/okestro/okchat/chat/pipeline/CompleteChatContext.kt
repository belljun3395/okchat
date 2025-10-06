package com.okestro.okchat.chat.pipeline

class CompleteChatContext(
    input: UserInput,
    conversationHistory: ConversationHistory?,
    analysis: Analysis,
    search: Search?,
    val prompt: Prompt
) : ChatContext(
    input = input,
    conversationHistory = conversationHistory,
    analysis = analysis,
    search = search
) {
    /**
     * Prompt context (LastChatPipelineStep - always executed at end)
     */
    data class Prompt(
        val text: String
    )
}
