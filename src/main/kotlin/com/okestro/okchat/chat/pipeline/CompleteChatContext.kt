package com.okestro.okchat.chat.pipeline

class CompleteChatContext(
    input: ChatContext.UserInput,
    analysis: ChatContext.Analysis,
    search: ChatContext.Search?,
    val prompt: Prompt
) : ChatContext(
    input = input,
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
