package com.okestro.okchat.ai.support

import com.okestro.okchat.fixture.TestFixtures
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Improved test for DateExtractor using Kotest with FunSpec.
 * Demonstrates parameterized testing for date extraction patterns.
 */
class DateExtractorImprovedTest : FunSpec({

    context("extractDateKeywords should handle various date formats") {
        data class DateFormatTestCase(
            val description: String,
            val input: String,
            val expectedKeywords: List<String>
        )

        withData(
            nameFn = { it.description },
            DateFormatTestCase(
                description = "Korean year-month format",
                input = TestFixtures.DateExtractor.KOREAN_YEAR_MONTH,
                expectedKeywords = listOf("2024년 09월", "2024-09", "2024/09", "2409")
            ),
            DateFormatTestCase(
                description = "Short year format",
                input = TestFixtures.DateExtractor.SHORT_YEAR_FORMAT,
                expectedKeywords = listOf("2025년 08월", "2025-08", "2508")
            ),
            DateFormatTestCase(
                description = "Dash format",
                input = TestFixtures.DateExtractor.DASH_FORMAT,
                expectedKeywords = listOf("2024년 09월", "2024-09", "2409")
            ),
            DateFormatTestCase(
                description = "Slash format",
                input = TestFixtures.DateExtractor.SLASH_FORMAT,
                expectedKeywords = listOf("2024년 09월", "2024-09", "2024/09")
            ),
            DateFormatTestCase(
                description = "Short format YYMMDD",
                input = TestFixtures.DateExtractor.SHORT_FORMAT_YYMMDD,
                expectedKeywords = listOf("250908", "2509", "2025년 09월")
            )
        ) { (_, input, expectedKeywords) ->
            // When
            val result = DateExtractor.extractDateKeywords(input)

            // Then
            result shouldContainAll expectedKeywords
        }
    }

    test("extractDateKeywords should add English month names") {
        // Given
        val text = TestFixtures.DateExtractor.KOREAN_YEAR_MONTH

        // When
        val result = DateExtractor.extractDateKeywords(text)

        // Then
        result shouldContain "September"
        result shouldContain "9월"
    }

    test("extractDateKeywords should handle multiple date expressions") {
        // Given
        val text = TestFixtures.DateExtractor.KOREAN_MULTIPLE_DATES

        // When
        val result = DateExtractor.extractDateKeywords(text)

        // Then
        result.any { it.contains("08") || it.contains("8") } shouldBe true
        result.any { it.contains("09") || it.contains("9") } shouldBe true
    }

    test("extractDateKeywords should pad single-digit months") {
        // Given
        val text = "2024년 8월"

        // When
        val result = DateExtractor.extractDateKeywords(text)

        // Then
        result shouldContainAll listOf("2024년 08월", "2024-08", "2408")
    }

    context("containsDateExpression should detect date formats") {
        data class DateDetectionTestCase(
            val description: String,
            val input: String,
            val expectedResult: Boolean
        )

        withData(
            nameFn = { it.description },
            DateDetectionTestCase("Korean date format", "2024년 9월", true),
            DateDetectionTestCase("Short year format", "25년 8월", true),
            DateDetectionTestCase("Dash format", "2024-09", true),
            DateDetectionTestCase("Slash format", "2024/09", true),
            DateDetectionTestCase("Short format", "250908", true),
            DateDetectionTestCase("No date text 1", TestFixtures.DateExtractor.NO_DATE_TEXT, false),
            DateDetectionTestCase("No date text 2", "회의록", false)
        ) { (_, input, expectedResult) ->
            // When
            val result = DateExtractor.containsDateExpression(input)

            // Then
            result shouldBe expectedResult
        }
    }

    context("extractDateKeywords should handle edge cases") {
        data class EdgeCaseTestCase(
            val description: String,
            val input: String,
            val shouldBeEmpty: Boolean
        )

        withData(
            nameFn = { it.description },
            EdgeCaseTestCase("Text without dates", TestFixtures.DateExtractor.NO_DATE_TEXT, true),
            EdgeCaseTestCase("Empty string", "", true)
        ) { (_, input, shouldBeEmpty) ->
            // When
            val result = DateExtractor.extractDateKeywords(input)

            // Then
            if (shouldBeEmpty) {
                result.shouldBeEmpty()
            }
        }
    }
})
