package com.okestro.okchat.ai.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ToolModelsTest {

    @Test
    fun `SearchDocumentsInput should validate limit within range`() {
        // Given
        val validInput = SearchDocumentsInput(query = "test", limit = 5)
        val tooSmallInput = SearchDocumentsInput(query = "test", limit = -1)
        val tooLargeInput = SearchDocumentsInput(query = "test", limit = 100)

        // Then
        assertEquals(5, validInput.getValidatedLimit())
        assertEquals(1, tooSmallInput.getValidatedLimit()) // coerced to minimum
        assertEquals(20, tooLargeInput.getValidatedLimit()) // coerced to maximum
    }

    @Test
    fun `SearchDocumentsInput should use default limit`() {
        // Given
        val input = SearchDocumentsInput(query = "test")

        // Then
        assertEquals(5, input.limit)
    }

    @Test
    fun `SearchByQueryInput should validate topK within range`() {
        // Given
        val validInput = SearchByQueryInput(query = "test", topK = 10)
        val tooSmallInput = SearchByQueryInput(query = "test", topK = 0)
        val tooLargeInput = SearchByQueryInput(query = "test", topK = 100)

        // Then
        assertEquals(10, validInput.getValidatedTopK())
        assertEquals(1, tooSmallInput.getValidatedTopK()) // coerced to minimum
        assertEquals(50, tooLargeInput.getValidatedTopK()) // coerced to maximum
    }

    @Test
    fun `SearchByKeywordInput should validate topK within range`() {
        // Given
        val validInput = SearchByKeywordInput(keywords = "test", topK = 10)
        val tooSmallInput = SearchByKeywordInput(keywords = "test", topK = 0)
        val tooLargeInput = SearchByKeywordInput(keywords = "test", topK = 100)

        // Then
        assertEquals(10, validInput.getValidatedTopK())
        assertEquals(1, tooSmallInput.getValidatedTopK()) // coerced to minimum
        assertEquals(50, tooLargeInput.getValidatedTopK()) // coerced to maximum
    }
}
