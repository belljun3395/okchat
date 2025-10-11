package com.okestro.okchat.ai.service.extraction

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

class DocumentKeywordExtractionServiceTest {

    // Note: buildPrompt is protected and should not be tested directly.
    // Instead, we test the behavior through the public execute() method.

    @Test
    fun `execute should extract comprehensive keywords from document`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val keywords = (1..15).joinToString(", ") { "keyword$it" }
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(keywords)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = DocumentKeywordExtractionService(chatModel)

        // When
        val result = service.execute("Long document content...")

        // Then
        assertEquals(15, result.size)
    }

    // Note: getMinKeywordLength and getMaxKeywords are protected methods.
    // Their behavior is tested indirectly through execute() method tests.

    @Test
    fun `execute should limit to 20 keywords`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val keywords = (1..25).map { "keyword$it" }.joinToString(", ")
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(keywords)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = DocumentKeywordExtractionService(chatModel)

        // When
        val result = service.execute("Long document content...")

        // Then
        assertEquals(20, result.size)
    }

    // Note: getOptions is a protected method.
    // Its configuration is tested indirectly through execute() method tests.

    @Test
    fun `execute should filter out single character keywords`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("a, bb, ccc, dddd")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = DocumentKeywordExtractionService(chatModel)

        // When
        val result = service.execute("document content")

        // Then
        assertEquals(listOf("bb", "ccc", "dddd"), result)
    }
}
