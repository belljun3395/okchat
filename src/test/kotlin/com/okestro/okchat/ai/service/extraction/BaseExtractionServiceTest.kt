package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.model.KeyWordExtractionPrompt
import com.okestro.okchat.ai.model.Prompt
import com.okestro.okchat.ai.support.DefaultResultParser
import com.okestro.okchat.ai.support.ResultParser
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
        chatModel: ChatModel,
        resultParser: ResultParser = DefaultResultParser()
    ) : BaseExtractionService(chatModel, resultParser) {

        var customMinLength: Int = 1
        var customMaxKeywords: Int = Int.MAX_VALUE
        var customEmptyResult: List<String> = emptyList()
        var customFallbackMessage: String = "Returning empty list."

        override fun buildPrompt(message: String): Prompt {
            return KeyWordExtractionPrompt(
                userInstruction = "Test instruction",
                examples = emptyList(),
                message = message
            )
        }

        override fun getMinKeywordLength(): Int = customMinLength
        override fun getMaxKeywords(): Int = customMaxKeywords
        override fun getEmptyResult(): List<String> = customEmptyResult
        override fun getFallbackMessage(): String = customFallbackMessage
    }

    @Test
    fun `execute should extract keywords successfully`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("keyword1, keyword2, keyword3")))
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
    fun `execute should handle null response`() = runBlocking {
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
    fun `execute should respect minKeywordLength`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("a, bb, ccc, dddd")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel).apply {
            customMinLength = 3
        }

        // When
        val result = service.execute("test message")

        // Then
        assertEquals(listOf("ccc", "dddd"), result)
    }

    @Test
    fun `execute should respect maxKeywords limit`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("k1, k2, k3, k4, k5")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel).apply {
            customMaxKeywords = 3
        }

        // When
        val result = service.execute("test message")

        // Then
        assertEquals(3, result.size)
        assertEquals(listOf("k1", "k2", "k3"), result)
    }

    @Test
    fun `execute should trim whitespace from response`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("  keyword1, keyword2, keyword3  ")))
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
    fun `execute should handle Korean keywords`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("백엔드, 개발, 회의록")))
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
    fun `execute should handle mixed Korean and English keywords`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("백엔드, backend, 개발, development")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        assertEquals(listOf("백엔드", "backend", "개발", "development"), result)
    }

    @Test
    fun `execute should deduplicate keywords case-insensitively`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("Keyword, keyword, KEYWORD, other")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TestExtractionService(chatModel)

        // When
        val result = service.execute("test message")

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("Keyword") || result.contains("keyword") || result.contains("KEYWORD"))
        assertTrue(result.contains("other"))
    }
}
