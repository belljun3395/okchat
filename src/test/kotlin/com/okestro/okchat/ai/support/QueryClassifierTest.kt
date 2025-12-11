package com.okestro.okchat.ai.support

import com.okestro.okchat.ai.service.classifier.QueryClassifier
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

@DisplayName("QueryClassifier Tests")
class QueryClassifierTest {

    private val chatModel: ChatModel = mockk()
    private val classifier = QueryClassifier(chatModel)

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("should classify query using AI")
    fun `should classify query using AI`() = runTest {
        // given
        val query = "프로젝트 문서를 찾아줘"
        val aiResponse = """
            TYPE: DOCUMENT_SEARCH
            CONFIDENCE: 0.95
            REASONING: 문서 검색 요청
        """.trimIndent()

        val chatResponse = ChatResponse(
            listOf(Generation(AssistantMessage(aiResponse)))
        )
        every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns chatResponse

        // when
        val result = classifier.classify(query)

        // then
        result.type shouldBe QueryClassifier.QueryType.DOCUMENT_SEARCH
        result.confidence shouldBe 0.95
    }

    @Test
    @DisplayName("should fallback to rule-based when AI fails")
    fun `should fallback to rule-based when AI fails`() = runTest {
        // given
        val query = "안녕하세요"
        every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } throws RuntimeException("API error")

        // when
        val result = classifier.classify(query)

        // then
        result.shouldBeInstanceOf<QueryClassifier.QueryAnalysis>()
        result.type.shouldBeInstanceOf<QueryClassifier.QueryType>()
    }

    @Test
    @DisplayName("should handle invalid AI response format")
    fun `should handle invalid AI response format`() = runTest {
        // given
        val query = "test"
        val invalidResponse = "Invalid response format"

        val chatResponse = ChatResponse(
            listOf(Generation(AssistantMessage(invalidResponse)))
        )
        every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns chatResponse

        // when
        val result = classifier.classify(query)

        // then
        result.type shouldBe QueryClassifier.QueryType.GENERAL
        result.confidence shouldBe 0.5
    }

    @Test
    @DisplayName("should parse confidence as default when invalid")
    fun `should parse confidence as default when invalid`() = runTest {
        // given
        val query = "test"
        val aiResponse = """
            TYPE: GENERAL
            CONFIDENCE: invalid
        """.trimIndent()

        val chatResponse = ChatResponse(
            listOf(Generation(AssistantMessage(aiResponse)))
        )
        every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns chatResponse

        // when
        val result = classifier.classify(query)

        // then
        result.confidence shouldBe 0.5
    }
}
