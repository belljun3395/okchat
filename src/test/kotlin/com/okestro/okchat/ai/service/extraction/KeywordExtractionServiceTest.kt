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
import kotlin.test.assertTrue

class KeywordExtractionServiceTest {

    // Note: buildPrompt is protected and should not be tested directly.
    // Instead, we test the behavior through the public execute() method.

    @Test
    fun `execute should extract keywords from Korean query`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"백엔드\", \"backend\", \"개발\", \"development\", \"레포\", \"repository\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = KeywordExtractionService(chatModel)

        // When
        val result = service.execute("백엔드 개발 레포 정보")

        // Then
        assertEquals(6, result.size)
        assertTrue(result.contains("백엔드"))
        assertTrue(result.contains("backend"))
        assertTrue(result.contains("개발"))
        assertTrue(result.contains("development"))
    }

    @Test
    fun `execute should extract keywords from English query`() = runBlocking {
        // Given
        val chatModel = mock<ChatModel>()
        val jsonResponse = "{\"keywords\": [\"design document\", \"authentication logic\", \"Mobile App\"]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = KeywordExtractionService(chatModel)

        // When
        val result = service.execute("Show me the design document for authentication logic in Mobile App")

        // Then
        assertTrue(result.contains("design document"))
        assertTrue(result.contains("authentication logic"))
        assertTrue(result.contains("Mobile App"))
    }

    // Note: getOptions is a protected method.
    // Its configuration is tested indirectly through execute() method tests.
}
