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

class LocationExtractionServiceTest {

    // Note: buildPrompt is protected and should not be tested directly.
    // Instead, we test the behavior through the public execute() method.

    @Test
    fun `execute should extract location from Korean query`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"개발팀 스페이스\", \"개발팀\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = LocationExtractionService(chatModel)

        // When
        val result = service.execute("개발팀 스페이스에 있는 지난 주 회의록 찾아줘")

        // Then
        assertTrue(result.contains("개발팀 스페이스"))
        assertTrue(result.contains("개발팀"))
    }

    @Test
    fun `execute should extract location from English query`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"Mobile App project\", \"Mobile App\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = LocationExtractionService(chatModel)

        // When
        val result = service.execute("Find the design document in the 'Mobile App' project folder.")

        // Then
        assertTrue(result.contains("Mobile App project") || result.contains("Mobile App"))
    }

    @Test
    fun `execute should return empty when no location mentioned`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage("")))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = LocationExtractionService(chatModel)

        // When
        val result = service.execute("Show me the latest architecture diagram.")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute should extract file path locations`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"/docs/infra/networking/\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = LocationExtractionService(chatModel)

        // When
        val result = service.execute("/docs/infra/networking/ 에서 VPN 관련 문서 찾아줘")

        // Then
        assertTrue(result.contains("/docs/infra/networking/"))
    }
}
