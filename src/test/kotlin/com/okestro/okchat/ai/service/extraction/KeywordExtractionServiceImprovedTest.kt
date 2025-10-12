package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.fixture.TestFixtures
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.springframework.ai.chat.model.ChatModel

/**
 * Improved test for KeywordExtractionService using Kotest.
 * Demonstrates:
 * - FunSpec style (simpler for function testing)
 * - Parameterized tests for similar test cases
 * - Fixture pattern
 * - Behavior verification with coVerify
 */
class KeywordExtractionServiceImprovedTest : FunSpec({

    context("KeywordExtractionService - execute") {

        data class KeywordExtractionTestCase(
            val description: String,
            val query: String,
            val mockResponse: String,
            val expectedKeywords: List<String>
        )

        withData(
            nameFn = { it.description },
            KeywordExtractionTestCase(
                description = "Korean query",
                query = TestFixtures.ExtractionService.KOREAN_QUERY,
                mockResponse = "백엔드, backend, 개발, development, 레포, repository",
                expectedKeywords = listOf("백엔드", "backend", "개발", "development", "레포", "repository")
            ),
            KeywordExtractionTestCase(
                description = "English query",
                query = TestFixtures.ExtractionService.ENGLISH_QUERY,
                mockResponse = "design document, authentication logic, Mobile App",
                expectedKeywords = listOf("design document", "authentication logic", "Mobile App")
            ),
            KeywordExtractionTestCase(
                description = "Mixed Korean and English",
                query = "백엔드 backend 개발",
                mockResponse = "백엔드, backend, 개발, development",
                expectedKeywords = listOf("백엔드", "backend", "개발", "development")
            )
        ) { (_, query, mockResponse, expectedKeywords) ->
            // Given
            val chatModel = mockk<ChatModel>()
            coEvery { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns TestFixtures.createChatResponse(mockResponse)

            val service = KeywordExtractionService(chatModel)

            // When
            val result = runBlocking { service.execute(query) }

            // Then
            result.size shouldBe expectedKeywords.size
            result shouldContainAll expectedKeywords

            // Verify behavior
            coVerify(exactly = 1) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
        }
    }

    context("KeywordExtractionService - filtering") {

        test("should filter out single character keywords") {
            // Given
            val chatModel = mockk<ChatModel>()
            coEvery { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns TestFixtures.ExtractionService.SINGLE_CHAR_KEYWORDS_RESPONSE

            val service = KeywordExtractionService(chatModel)

            // When
            val result = runBlocking { service.execute("test message") }

            // Then
            result shouldBe listOf("bb", "ccc", "dddd")
            result shouldContain "bb"
            result.forEach { it.length shouldBe it.length.coerceAtLeast(2) }

            // Verify
            coVerify(exactly = 1) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
        }

        test("should limit to 12 keywords") {
            // Given
            val chatModel = mockk<ChatModel>()
            coEvery { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns TestFixtures.ExtractionService.MANY_KEYWORDS_RESPONSE

            val service = KeywordExtractionService(chatModel)

            // When
            val result = runBlocking { service.execute("test message") }

            // Then
            result.size shouldBe 12

            // Verify
            coVerify(exactly = 1) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
        }

        data class FilterTestCase(
            val description: String,
            val mockResponse: String,
            val expectedSize: Int,
            val expectedFirst: String
        )

        withData(
            nameFn = { it.description },
            FilterTestCase(
                description = "Whitespace trimming",
                mockResponse = "  keyword1  ,  keyword2  ,  keyword3  ",
                expectedSize = 3,
                expectedFirst = "keyword1"
            ),
            FilterTestCase(
                description = "Empty string filtering",
                mockResponse = "keyword1, , keyword2, , keyword3",
                expectedSize = 3,
                expectedFirst = "keyword1"
            ),
            FilterTestCase(
                description = "Deduplicate case-insensitive",
                mockResponse = "Keyword, keyword, KEYWORD, other",
                expectedSize = 2,
                expectedFirst = "Keyword"
            )
        ) { (_, mockResponse, expectedSize, expectedFirst) ->
            // Given
            val chatModel = mockk<ChatModel>()
            coEvery { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns TestFixtures.createChatResponse(mockResponse)

            val service = KeywordExtractionService(chatModel)

            // When
            val result = runBlocking { service.execute("test") }

            // Then
            result.size shouldBe expectedSize
            result.first() shouldBe expectedFirst

            // Verify
            coVerify(exactly = 1) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
        }
    }

    context("KeywordExtractionService - error handling") {

        test("should return empty list on exception") {
            // Given
            val chatModel = mockk<ChatModel>()
            coEvery { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } throws RuntimeException("API error")

            val service = KeywordExtractionService(chatModel)

            // When
            val result = runBlocking { service.execute("test message") }

            // Then
            result.size shouldBe 0

            // Verify that call was attempted
            coVerify(exactly = 1) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
        }
    }
})
