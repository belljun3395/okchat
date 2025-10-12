package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.fixture.TestFixtures
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.springframework.ai.chat.model.ChatModel

/**
 * Improved test for TitleExtractionService using Kotest and fixtures.
 * Demonstrates:
 * - Kotest BDD style (DescribeSpec)
 * - Parameterized tests with withData
 * - Fixture pattern for test data
 * - MockK for verification
 * - Better test organization
 */
class TitleExtractionServiceImprovedTest : DescribeSpec({

    describe("TitleExtractionService") {

        context("execute with various queries") {

            data class TitleExtractionTestCase(
                val description: String,
                val query: String,
                val mockResponse: String,
                val expectedContains: String
            )

            withData(
                nameFn = { it.description },
                TitleExtractionTestCase(
                    description = "English query with quotes",
                    query = TestFixtures.ExtractionService.TITLE_ENGLISH_QUERY,
                    mockResponse = "Q3 Performance Review",
                    expectedContains = "Q3 Performance Review"
                ),
                TitleExtractionTestCase(
                    description = "Korean query",
                    query = TestFixtures.ExtractionService.TITLE_KOREAN_QUERY,
                    mockResponse = "2025년 기술 부채 보고서, 기술 부채 보고서",
                    expectedContains = "2025년 기술 부채 보고서"
                ),
                TitleExtractionTestCase(
                    description = "Generic document type",
                    query = "회의록 좀 찾아줄래?",
                    mockResponse = "회의록",
                    expectedContains = "회의록"
                )
            ) { (_, query, mockResponse, expectedContains) ->
                // Given
                val chatModel = mockk<ChatModel>()
                coEvery { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns TestFixtures.createChatResponse(mockResponse)

                val service = TitleExtractionService(chatModel)

                // When
                val result = runBlocking { service.execute(query) }

                // Then
                result.joinToString(" ") shouldContain expectedContains

                // Verify behavior
                coVerify(exactly = 1) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
            }
        }

        context("execute with empty or no title") {

            it("should return empty when no title mentioned") {
                // Given
                val chatModel = mockk<ChatModel>()
                coEvery { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns TestFixtures.ExtractionService.EMPTY_RESPONSE

                val service = TitleExtractionService(chatModel)

                // When
                val result = runBlocking { service.execute("How does the login logic work?") }

                // Then
                result shouldBe emptyList()

                // Verify behavior
                coVerify(exactly = 1) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
            }
        }

        context("error handling") {

            it("should handle ChatModel exceptions gracefully") {
                // Given
                val chatModel = mockk<ChatModel>()
                coEvery { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } throws RuntimeException("API error")

                val service = TitleExtractionService(chatModel)

                // When
                val result = runBlocking { service.execute("test query") }

                // Then
                result shouldBe emptyList()

                // Verify behavior
                coVerify(exactly = 1) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
            }
        }

        context("edge cases") {

            data class EdgeCaseTestCase(
                val description: String,
                val mockResponse: String,
                val expectedResult: String
            )

            withData(
                nameFn = { it.description },
                EdgeCaseTestCase(
                    description = "Whitespace trimming",
                    mockResponse = "  Title with spaces  ",
                    expectedResult = "Title with spaces"
                ),
                EdgeCaseTestCase(
                    description = "Multiple titles in response",
                    mockResponse = "Title1, Title2, Title3",
                    expectedResult = "Title1"
                )
            ) { (_, mockResponse, expectedResult) ->
                // Given
                val chatModel = mockk<ChatModel>()
                coEvery { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } returns TestFixtures.createChatResponse(mockResponse)

                val service = TitleExtractionService(chatModel)

                // When
                val result = runBlocking { service.execute("test query") }

                // Then
                result.joinToString(" ") shouldContain expectedResult

                // Verify
                coVerify(exactly = 1) { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) }
            }
        }
    }
})
