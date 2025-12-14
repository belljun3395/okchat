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
        val keywordsList = (1..15).map { "keyword$it" }
        val jsonResponse = "{\"keywords\": [${keywordsList.joinToString(", ") { "\"$it\"" }}]}"
        val expectedResponse = ChatResponse(
            listOf(Generation(AssistantMessage(jsonResponse)))
        )
        whenever(chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()))
            .thenReturn(expectedResponse)

        val service = DocumentKeywordExtractionService(chatModel)

        // When
        val result = service.execute("Long document content...")

        // Then
        assertEquals(15, result.size)
        assertEquals(keywordsList, result)
    }

    // Note: getOptions is a protected method.
    // Its configuration is tested indirectly through execute() method tests.
}
