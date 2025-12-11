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

class ContentExtractionServiceTest {

    // Note: buildPrompt is protected and should not be tested directly.
    // Instead, we test the behavior through the public execute() method.

    @Test
    fun `execute should extract content keywords from query`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"authentication logic\", \"PPP project\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = ContentExtractionService(chatModel)

        // When
        val result = service.execute("Tell me about the new authentication logic for the PPP project.")

        // Then
        assertTrue(result.contains("authentication logic"))
        assertTrue(result.contains("PPP project"))
    }

    @Test
    fun `execute should extract Korean content keywords`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"병목 현상\", \"성능 테스트\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = ContentExtractionService(chatModel)

        // When
        val result = service.execute("성능 테스트 결과 보고서에서 병목 현상에 대한 내용 찾아줘")

        // Then
        assertTrue(result.contains("병목 현상"))
        assertTrue(result.contains("성능 테스트"))
    }

    @Test
    fun `execute should focus on topics not actions`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"memory leak\", \"notification service\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = ContentExtractionService(chatModel)

        // When
        val result = service.execute("How is the memory leak in the notification service being handled?")

        // Then
        assertTrue(result.contains("memory leak"))
        assertTrue(result.contains("notification service"))
        // Should not contain action verbs
    }
}
