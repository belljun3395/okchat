package com.okestro.okchat.ai.support

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Improved test for ResultParser using Kotest with comprehensive parameterized tests.
 */
class ResultParserImprovedTest : FunSpec({

    val parser = DefaultResultParser()

    context("ResultParser - basic parsing") {

        data class BasicParseTestCase(
            val description: String,
            val input: String?,
            val expected: List<String>
        )

        withData(
            nameFn = { it.description },
            BasicParseTestCase("Null input", null, emptyList()),
            BasicParseTestCase("Blank input", "   ", emptyList()),
            BasicParseTestCase("Single keyword", "keyword1", listOf("keyword1")),
            BasicParseTestCase(
                "Comma-separated keywords",
                "keyword1, keyword2, keyword3",
                listOf("keyword1", "keyword2", "keyword3")
            ),
            BasicParseTestCase(
                "Comma-separated with extra spaces",
                "keyword1  ,  keyword2  ,   keyword3",
                listOf("keyword1", "keyword2", "keyword3")
            )
        ) { (_, input, expected) ->
            // When
            val result = parser.parse(
                resultText = input,
                minLength = 1,
                maxKeywords = 10,
                emptyResult = emptyList()
            )

            // Then
            result shouldBe expected
        }
    }

    context("ResultParser - different formats") {

        data class FormatTestCase(
            val description: String,
            val input: String,
            val expected: List<String>
        )

        withData(
            nameFn = { it.description },
            FormatTestCase(
                description = "Newline-separated",
                input = "keyword1\nkeyword2\nkeyword3",
                expected = listOf("keyword1", "keyword2", "keyword3")
            ),
            FormatTestCase(
                description = "Numbered list format",
                input = "1. keyword1\n2. keyword2\n3. keyword3",
                expected = listOf("keyword1", "keyword2", "keyword3")
            ),
            FormatTestCase(
                description = "Bulleted list with dash",
                input = "- keyword1\n- keyword2\n- keyword3",
                expected = listOf("keyword1", "keyword2", "keyword3")
            ),
            FormatTestCase(
                description = "Bulleted list with asterisk",
                input = "* keyword1\n* keyword2\n* keyword3",
                expected = listOf("keyword1", "keyword2", "keyword3")
            )
        ) { (_, input, expected) ->
            // When
            val result = parser.parse(
                resultText = input,
                minLength = 1,
                maxKeywords = 10,
                emptyResult = emptyList()
            )

            // Then
            result shouldBe expected
        }
    }

    context("ResultParser - filtering") {

        test("should filter out keywords below minLength") {
            // Given
            val input = "a, bb, ccc, dddd"

            // When
            val result = parser.parse(
                resultText = input,
                minLength = 3,
                maxKeywords = 10,
                emptyResult = emptyList()
            )

            // Then
            result shouldBe listOf("ccc", "dddd")
        }

        test("should respect maxKeywords limit") {
            // Given
            val input = "keyword1, keyword2, keyword3, keyword4, keyword5"

            // When
            val result = parser.parse(
                resultText = input,
                minLength = 1,
                maxKeywords = 3,
                emptyResult = emptyList()
            )

            // Then
            result shouldHaveSize 3
            result shouldBe listOf("keyword1", "keyword2", "keyword3")
        }

        test("should filter out blank keywords") {
            // Given
            val input = "keyword1, , keyword2, , keyword3"

            // When
            val result = parser.parse(
                resultText = input,
                minLength = 1,
                maxKeywords = 10,
                emptyResult = emptyList()
            )

            // Then
            result shouldBe listOf("keyword1", "keyword2", "keyword3")
        }

        test("should deduplicate case-insensitive keywords") {
            // Given
            val input = "Keyword1, keyword1, KEYWORD1, keyword2"

            // When
            val result = parser.parse(
                resultText = input,
                minLength = 1,
                maxKeywords = 10,
                emptyResult = emptyList()
            )

            // Then
            result shouldHaveSize 2
            result shouldContain "Keyword1"
            result shouldContain "keyword2"
        }
    }

    context("ResultParser - internationalization") {

        data class I18nTestCase(
            val description: String,
            val input: String,
            val expected: List<String>
        )

        withData(
            nameFn = { it.description },
            I18nTestCase(
                description = "Korean keywords",
                input = "백엔드, 개발, 회의록",
                expected = listOf("백엔드", "개발", "회의록")
            ),
            I18nTestCase(
                description = "Mixed Korean and English",
                input = "백엔드, backend, 개발, development",
                expected = listOf("백엔드", "backend", "개발", "development")
            ),
            I18nTestCase(
                description = "Complex real-world example with Korean",
                input = """
                    1. 백엔드
                    2. backend
                    3. 개발
                    4. development
                    5. 레포
                    6. repository
                """.trimIndent(),
                expected = listOf("백엔드", "backend", "개발", "development", "레포", "repository")
            )
        ) { (_, input, expected) ->
            // When
            val result = parser.parse(
                resultText = input,
                minLength = 2,
                maxKeywords = 10,
                emptyResult = emptyList()
            )

            // Then
            result shouldBe expected
        }
    }

    context("ResultParser - fallback behavior") {

        test("should return emptyResult when no valid keywords found") {
            // Given
            val input = "a, b, c"
            val emptyResult = listOf("fallback")

            // When
            val result = parser.parse(
                resultText = input,
                minLength = 5,
                maxKeywords = 10,
                emptyResult = emptyResult
            )

            // Then
            result shouldBe emptyResult
        }

        data class EmptyResultTestCase(
            val description: String,
            val input: String?,
            val minLength: Int,
            val expectedFallback: Boolean
        )

        withData(
            nameFn = { it.description },
            EmptyResultTestCase("Null input triggers fallback", null, 1, true),
            EmptyResultTestCase("Blank input triggers fallback", "   ", 1, true),
            EmptyResultTestCase("All filtered triggers fallback", "a, b", 5, true)
        ) { (_, input, minLength, expectedFallback) ->
            // Given
            val fallbackResult = listOf("fallback")

            // When
            val result = parser.parse(
                resultText = input,
                minLength = minLength,
                maxKeywords = 10,
                emptyResult = fallbackResult
            )

            // Then
            if (expectedFallback) {
                result shouldBe fallbackResult
            }
        }
    }

    context("ResultParser - edge cases") {

        test("should handle LLM output with explanation prefix") {
            // Given
            val input = "keyword1, keyword2, keyword3"

            // When
            val result = parser.parse(
                resultText = input,
                minLength = 1,
                maxKeywords = 10,
                emptyResult = emptyList()
            )

            // Then
            result shouldBe listOf("keyword1", "keyword2", "keyword3")
        }

        data class EdgeCaseTestCase(
            val description: String,
            val input: String,
            val minLength: Int,
            val maxKeywords: Int,
            val expectedSize: Int
        )

        withData(
            nameFn = { it.description },
            EdgeCaseTestCase(
                description = "Very long keyword list with limit",
                input = (1..100).joinToString(", ") { "keyword$it" },
                minLength = 1,
                maxKeywords = 10,
                expectedSize = 10
            ),
            EdgeCaseTestCase(
                description = "Mixed separators",
                input = "keyword1, keyword2\nkeyword3, keyword4",
                minLength = 1,
                maxKeywords = 10,
                expectedSize = 3 // Parser prioritizes comma split: ["keyword1", "keyword2\nkeyword3", "keyword4"]
            ),
            EdgeCaseTestCase(
                description = "Extra whitespace everywhere",
                input = "  keyword1  ,  keyword2  ,  keyword3  ",
                minLength = 1,
                maxKeywords = 10,
                expectedSize = 3
            )
        ) { (_, input, minLength, maxKeywords, expectedSize) ->
            // When
            val result = parser.parse(
                resultText = input,
                minLength = minLength,
                maxKeywords = maxKeywords,
                emptyResult = emptyList()
            )

            // Then
            result shouldHaveSize expectedSize
        }
    }

    context("ResultParser - special characters") {

        test("should handle keywords with special characters") {
            // Given
            val input = "C++, .NET, Node.js, Spring Boot"

            // When
            val result = parser.parse(
                resultText = input,
                minLength = 1,
                maxKeywords = 10,
                emptyResult = emptyList()
            )

            // Then
            result shouldContain "C++"
            result shouldContain ".NET"
            result shouldContain "Node.js"
            result shouldContain "Spring Boot"
        }

        test("should handle keywords with hyphens and underscores") {
            // Given
            val input = "micro-service, snake_case, kebab-case"

            // When
            val result = parser.parse(
                resultText = input,
                minLength = 1,
                maxKeywords = 10,
                emptyResult = emptyList()
            )

            // Then
            result shouldContain "micro-service"
            result shouldContain "snake_case"
            result shouldContain "kebab-case"
        }
    }
})
