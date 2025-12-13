package com.okestro.okchat.search.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchResultTest {

    @Test
    fun `fromVectorSearch should convert distance to similarity`() {
        // Given
        val distance = 0.2

        // When
        val result = SearchResult.fromVectorSearch(
            id = "123",
            title = "Test",
            content = "Content",
            path = "/test",
            spaceKey = "TEST",
            distance = distance,
            knowledgeBaseId = 1L
        )

        // Then
        assertEquals(0.8, result.score.value, 0.001)
    }

    @Test
    fun `withSimilarity should use provided similarity score`() {
        // Given
        val similarity = SearchScore.SimilarityScore(0.9)

        // When
        val result = SearchResult.withSimilarity(
            id = "123",
            title = "Test",
            content = "Content",
            path = "/test",
            spaceKey = "TEST",
            similarity = similarity,
            knowledgeBaseId = 1L
        )

        // Then
        assertEquals(0.9, result.score.value, 0.001)
    }

    @Test
    fun `compareTo should compare by score`() {
        // Given
        val result1 = SearchResult.withSimilarity(
            id = "1",
            title = "Test1",
            content = "Content1",
            path = "/test1",
            spaceKey = "TEST",
            similarity = SearchScore.SimilarityScore(0.9),
            knowledgeBaseId = 1L
        )
        val result2 = SearchResult.withSimilarity(
            id = "2",
            title = "Test2",
            content = "Content2",
            path = "/test2",
            spaceKey = "TEST",
            similarity = SearchScore.SimilarityScore(0.5),
            knowledgeBaseId = 1L
        )

        // Then
        assertTrue(result1 > result2)
    }

    @Test
    fun `combineContent should merge content with separator`() {
        // Given
        val result = SearchResult.withSimilarity(
            id = "123",
            title = "Test",
            content = "First content",
            path = "/test",
            spaceKey = "TEST",
            similarity = SearchScore.SimilarityScore(0.5),
            knowledgeBaseId = 1L
        )

        // When
        result.combineContent("Second content")

        // Then
        assertEquals("First content\n\nSecond content", result.content)
    }

    @Test
    fun `scoreValue should return similarity value for backward compatibility`() {
        // Given
        val result = SearchResult.withSimilarity(
            id = "123",
            title = "Test",
            content = "Content",
            path = "/test",
            spaceKey = "TEST",
            similarity = SearchScore.SimilarityScore(0.75),
            knowledgeBaseId = 1L
        )

        // Then
        assertEquals(0.75, result.scoreValue, 0.001)
    }
}
