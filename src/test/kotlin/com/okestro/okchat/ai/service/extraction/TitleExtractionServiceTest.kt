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
import kotlin.test.assertTrue

class TitleExtractionServiceTest {

    // Note: buildPrompt is protected and should not be tested directly.
    // Instead, we test the behavior through the public execute() method.

    @Test
    fun `execute should extract title from English query with quotes`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("Q3 Performance Review")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TitleExtractionService(chatModel)

        // When
        val result = service.execute("show me the 'Q3 Performance Review' document")

        // Then
        assertTrue(result.contains("Q3 Performance Review"))
    }

    @Test
    fun `execute should extract title from Korean query`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("2025년 기술 부채 보고서, 기술 부채 보고서")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TitleExtractionService(chatModel)

        // When
        val result = service.execute("2025년 기술 부채 보고서 찾아줘")

        // Then
        assertTrue(result.contains("2025년 기술 부채 보고서") || result.contains("기술 부채 보고서"))
    }

    @Test
    fun `execute should extract generic document type when specific title not mentioned`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("회의록")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TitleExtractionService(chatModel)

        // When
        val result = service.execute("회의록 좀 찾아줄래?")

        // Then
        assertTrue(result.contains("회의록"))
    }

    @Test
    fun `execute should return empty when no title mentioned`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = TitleExtractionService(chatModel)

        // When
        val result = service.execute("How does the login logic work?")

        // Then
        assertTrue(result.isEmpty())
    }
}
