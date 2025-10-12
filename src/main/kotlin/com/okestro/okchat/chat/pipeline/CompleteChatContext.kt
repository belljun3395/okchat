package com.okestro.okchat.chat.pipeline

class CompleteChatContext(
    input: UserInput,
    conversationHistory: ConversationHistory?,
    analysis: Analysis,
    search: Search?,
    val prompt: Prompt,
    isDeepThink: Boolean = false,
    executedStep: MutableList<String> = mutableListOf()
) : ChatContext(
    input = input,
    conversationHistory = conversationHistory,
    analysis = analysis,
    search = search,
    isDeepThink = isDeepThink,
    executedStep = executedStep
) {
    /**
     * Prompt context (LastChatPipelineStep - always executed at end)
     */
    data class Prompt(
        val text: String
    )
}
