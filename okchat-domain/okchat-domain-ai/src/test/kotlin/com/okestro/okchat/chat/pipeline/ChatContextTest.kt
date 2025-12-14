package com.okestro.okchat.chat.pipeline

import com.okestro.okchat.ai.service.classifier.QueryClassifier
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("ChatContext Tests")
class ChatContextTest {

    @Test
    @DisplayName("should be created with required fields")
    fun `should be created with required fields`() {
        // given & when
        val context = ChatContext(
            input = ChatContext.UserInput(message = "test message"),
            isDeepThink = false
        )

        // then
        context.input.message shouldBe "test message"
        context.isDeepThink shouldBe false
    }

    @Test
    @DisplayName("should handle optional fields in UserInput")
    fun `should handle optional fields in UserInput`() {
        // given & when
        val userInput = ChatContext.UserInput(
            message = "test",
            providedKeywords = listOf("kotlin", "spring"),
            sessionId = "session-123",
            userEmail = "user@example.com"
        )

        // then
        userInput.message shouldBe "test"
        userInput.providedKeywords shouldHaveSize 2
        userInput.sessionId shouldBe "session-123"
        userInput.userEmail shouldBe "user@example.com"
    }

    @Test
    @DisplayName("should combine keywords in Analysis.getAllKeywords")
    fun `should combine keywords in Analysis getAllKeywords`() {
        // given
        val analysis = ChatContext.Analysis(
            queryAnalysis = QueryClassifier.QueryAnalysis(
                type = QueryClassifier.QueryType.DOCUMENT_SEARCH,
                confidence = 0.9,
                keywords = listOf("ai-keyword")
            ),
            extractedTitles = emptyList(),
            extractedContents = emptyList(),
            extractedPaths = emptyList(),
            extractedKeywords = listOf("keyword1", "keyword2"),
            dateKeywords = listOf("2025", "January")
        )

        // when
        val allKeywords = analysis.getAllKeywords()

        // then
        allKeywords shouldHaveSize 4
        allKeywords shouldBe listOf("keyword1", "keyword2", "2025", "January")
    }

    @Test
    @DisplayName("should store message data in Message")
    fun `should store message data in Message`() {
        // given
        val timestamp = Instant.now()

        // when
        val message = ChatContext.Message(
            role = "user",
            content = "Hello",
            timestamp = timestamp
        )

        // then
        message.role shouldBe "user"
        message.content shouldBe "Hello"
        message.timestamp shouldBe timestamp
    }

    @Test
    @DisplayName("should create new context with updated fields using copy extension")
    fun `should create new context with updated fields using copy extension`() {
        // given
        val originalContext = ChatContext(
            input = ChatContext.UserInput(message = "original"),
            isDeepThink = false
        )

        val analysis = ChatContext.Analysis(
            queryAnalysis = QueryClassifier.QueryAnalysis(
                type = QueryClassifier.QueryType.GENERAL,
                confidence = 0.5,
                keywords = emptyList()
            ),
            extractedTitles = emptyList(),
            extractedContents = emptyList(),
            extractedPaths = emptyList(),
            extractedKeywords = emptyList(),
            dateKeywords = emptyList()
        )

        // when
        val copiedContext = originalContext.copyContext(analysis = analysis)

        // then
        copiedContext.input.message shouldBe "original"
        copiedContext.analysis shouldBe analysis
    }
}
