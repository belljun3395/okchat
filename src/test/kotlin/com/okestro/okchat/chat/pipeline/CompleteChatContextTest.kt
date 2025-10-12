package com.okestro.okchat.chat.pipeline

import com.okestro.okchat.ai.support.QueryClassifier
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CompleteChatContext Tests")
class CompleteChatContextTest {

    @Test
    @DisplayName("should create CompleteChatContext with prompt")
    fun `should create CompleteChatContext with prompt`() {
        // given
        val userInput = ChatContext.UserInput(message = "test question")
        val analysis = createAnalysis()
        val promptText = "Generated prompt text"

        // when
        val context = CompleteChatContext(
            input = userInput,
            conversationHistory = null,
            analysis = analysis,
            search = null,
            prompt = CompleteChatContext.Prompt(promptText)
        )

        // then
        context.shouldBeInstanceOf<ChatContext>()
        context.prompt.text shouldBe promptText
        context.input.message shouldBe "test question"
        context.isDeepThink shouldBe false
    }

    @Test
    @DisplayName("should inherit from ChatContext")
    fun `should inherit from ChatContext`() {
        // given
        val context = CompleteChatContext(
            input = ChatContext.UserInput(message = "test"),
            conversationHistory = null,
            analysis = createAnalysis(),
            search = null,
            prompt = CompleteChatContext.Prompt("prompt")
        )

        // when & then
        context.shouldBeInstanceOf<ChatContext>()
        context.input shouldBe ChatContext.UserInput(message = "test")
    }

    @Test
    @DisplayName("should create CompleteChatContext with full context")
    fun `should create CompleteChatContext with full context`() {
        // given
        val userInput = ChatContext.UserInput(
            message = "complex question",
            sessionId = "session-123",
            userEmail = "user@example.com"
        )
        val analysis = createAnalysis()
        val search = ChatContext.Search(
            results = emptyList(),
            contextText = "RAG context"
        )
        val conversationHistory = ChatContext.ConversationHistory(
            sessionId = "session-123",
            messages = listOf(
                ChatContext.Message("user", "previous question", java.time.Instant.now()),
                ChatContext.Message("assistant", "previous answer", java.time.Instant.now())
            )
        )
        val prompt = CompleteChatContext.Prompt("Full prompt with {context} and {question}")

        // when
        val context = CompleteChatContext(
            input = userInput,
            conversationHistory = conversationHistory,
            analysis = analysis,
            search = search,
            prompt = prompt
        )

        // then
        context.input.sessionId shouldBe "session-123"
        context.input.userEmail shouldBe "user@example.com"
        context.conversationHistory?.messages?.size shouldBe 2
        context.search?.contextText shouldBe "RAG context"
        context.prompt.text shouldBe "Full prompt with {context} and {question}"
    }

    @Test
    @DisplayName("Prompt data class should hold text")
    fun `Prompt data class should hold text`() {
        // given & when
        val prompt = CompleteChatContext.Prompt("Test prompt text")

        // then
        prompt.text shouldBe "Test prompt text"
    }

    private fun createAnalysis(): ChatContext.Analysis {
        return ChatContext.Analysis(
            queryAnalysis = QueryClassifier.QueryAnalysis(
                type = QueryClassifier.QueryType.DOCUMENT_SEARCH,
                confidence = 0.9,
                keywords = emptyList()
            ),
            extractedTitles = emptyList(),
            extractedContents = emptyList(),
            extractedPaths = emptyList(),
            extractedKeywords = emptyList(),
            dateKeywords = emptyList()
        )
    }
}
