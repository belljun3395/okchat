package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.model.Prompt
import com.okestro.okchat.ai.model.StructuredPrompt
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaseExtractionServiceTest {

    private class TestExtractionService(
        chatModel: ChatModel
    ) : BaseExtractionService(chatModel) {

        var customEmptyResult: List<String> = emptyList()
        var customFallbackMessage: String = "Returning empty list."
        var customMinKeywordLength: Int? = null
        var customMaxKeywords: Int? = null

        override fun buildPrompt(message: String): Prompt {
            return StructuredPrompt(
                instruction = "Test instruction",
                examples = emptyList(),
                message = message
            )
        }

        override fun getEmptyResult(): List<String> = customEmptyResult
        override fun getFallbackMessage(): String = customFallbackMessage
        override fun getMinKeywordLength(): Int = customMinKeywordLength ?: super.getMinKeywordLength()
        override fun getMaxKeywords(): Int = customMaxKeywords ?: super.getMaxKeywords()
    }

    @Test
    fun `execute should extract keywords successfully from JSON`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"keyword1\", \"keyword2\", \"keyword3\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `execute should fallback to manual parsing for plain text`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val plainTextResponse = "keyword1, keyword2, keyword3"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(plainTextResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `execute should handle empty response`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute should return empty result on exception`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenThrow(RuntimeException("API error"))

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute should return custom empty result on exception`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenThrow(RuntimeException("API error"))

        val customEmptyResult = listOf("fallback")
        val service = TestExtractionService(chatModel).apply {
            this.customEmptyResult = customEmptyResult
        }

        // When
        val result = service.execute("test message")

        // Then
        assertEquals(customEmptyResult, result)
    }

    @Test
    fun `execute should handle Korean keywords in JSON`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"백엔드\", \"개발\", \"회의록\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        assertEquals(listOf("백엔드", "개발", "회의록"), result)
    }

    @Test
    fun `execute should filter out short keywords`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"a\", \"bb\", \"ccc\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        // "a" should be filtered out (default min length 2)
        assertEquals(listOf("bb", "ccc"), result)
    }

    @Test
    fun `execute should deduplicate keywords case-insensitively`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"Test\", \"test\", \"TEST\", \"other\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        assertEquals(listOf("Test", "other"), result)
    }

    @Test
    fun `execute should limit number of keywords`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        // Generate 25 keywords: keyword1, ..., keyword25
        val keywords = (1..25).map { "keyword$it" }
        val jsonResponse = "{\"keywords\": [${keywords.joinToString(", ") { "\"$it\"" }}]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        // Default max keywords is 20
        assertEquals(20, result.size)
        assertEquals("keyword1", result.first())
        assertEquals("keyword20", result.last())
    }

    @Test
    fun `execute should allow customization of limits`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"a\", \"bb\", \"ccc\", \"dddd\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        // Custom limits: min length 3, max count 1
        val service = TestExtractionService(chatModel).apply {
            this.customMinKeywordLength = 3
            this.customMaxKeywords = 1
        }

        // When
        val result = service.execute("test message")

        // Then
        // "a" (1) filtered, "bb" (2) filtered. "ccc" (3) kept. "dddd" (4) kept but limited by max count.
        assertEquals(listOf("ccc"), result)
    }
}
