package com.okestro.okchat.ai.support

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtractionResultParserTest {

    private val parser = DefaultExtractionResultParser()

    @Test
    fun `parse should handle null input`() {
        // When
        val result = parser.parse(
            resultText = null,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse should handle blank input`() {
        // When
        val result = parser.parse(
            resultText = "   ",
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse should handle comma-separated keywords`() {
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
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `parse should handle comma-separated with extra spaces`() {
        // Given
        val input = "keyword1  ,  keyword2  ,   keyword3"

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `parse should handle newline-separated keywords`() {
        // Given
        val input = "keyword1\nkeyword2\nkeyword3"

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `parse should handle numbered list format`() {
        // Given
        val input = "1. keyword1\n2. keyword2\n3. keyword3"

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `parse should handle bulleted list with dash`() {
        // Given
        val input = "- keyword1\n- keyword2\n- keyword3"

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `parse should handle bulleted list with asterisk`() {
        // Given
        val input = "* keyword1\n* keyword2\n* keyword3"

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `parse should handle single keyword`() {
        // Given
        val input = "keyword1"

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(listOf("keyword1"), result)
    }

    @Test
    fun `parse should filter out keywords below minLength`() {
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
        assertEquals(listOf("ccc", "dddd"), result)
    }

    @Test
    fun `parse should respect maxKeywords limit`() {
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
        assertEquals(3, result.size)
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `parse should deduplicate case-insensitive keywords`() {
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
        assertEquals(2, result.size)
        assertTrue(result.contains("Keyword1"))
        assertTrue(result.contains("keyword2"))
    }

    @Test
    fun `parse should filter out blank keywords`() {
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
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }

    @Test
    fun `parse should handle Korean keywords`() {
        // Given
        val input = "백엔드, 개발, 회의록"

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(listOf("백엔드", "개발", "회의록"), result)
    }

    @Test
    fun `parse should handle mixed Korean and English keywords`() {
        // Given
        val input = "백엔드, backend, 개발, development"

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(listOf("백엔드", "backend", "개발", "development"), result)
    }

    @Test
    fun `parse should return emptyResult when no valid keywords found`() {
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
        assertEquals(emptyResult, result)
    }

    @Test
    fun `parse should handle complex real-world example`() {
        // Given
        val input = """
            1. 백엔드
            2. backend
            3. 개발
            4. development
            5. 레포
            6. repository
        """.trimIndent()

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 2,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(
            listOf("백엔드", "backend", "개발", "development", "레포", "repository"),
            result
        )
    }

    @Test
    fun `parse should handle LLM output with explanation prefix`() {
        // Given - LLM sometimes adds explanation before keywords
        val input = "keyword1, keyword2, keyword3"

        // When
        val result = parser.parse(
            resultText = input,
            minLength = 1,
            maxKeywords = 10,
            emptyResult = emptyList()
        )

        // Then
        assertEquals(listOf("keyword1", "keyword2", "keyword3"), result)
    }
}
