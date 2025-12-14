package com.okestro.okchat.search.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchScoreTest {

    @Test
    fun `Distance should be within valid range`() {
        // Valid distances
        SearchScore.Distance(0.0)
        SearchScore.Distance(0.5)
        SearchScore.Distance(1.0)

        // Invalid distances
        assertThrows<IllegalArgumentException> {
            SearchScore.Distance(-0.1)
        }
        assertThrows<IllegalArgumentException> {
            SearchScore.Distance(1.1)
        }
    }

    @Test
    fun `Distance should convert to similarity correctly`() {
        // Given
        val distance = SearchScore.Distance(0.2)

        // When
        val similarity = distance.toSimilarity()

        // Then
        assertEquals(0.8, similarity.value, 0.001)
    }

    @Test
    fun `SimilarityScore should allow non-negative values`() {
        // Valid similarities
        SearchScore.SimilarityScore(0.0)
        SearchScore.SimilarityScore(0.5)
        SearchScore.SimilarityScore(1.0)
        SearchScore.SimilarityScore(2.0) // with boosts

        // Invalid similarity
        assertThrows<IllegalArgumentException> {
            SearchScore.SimilarityScore(-0.1)
        }
    }

    @Test
    fun `SimilarityScore boost should increase score`() {
        // Given
        val score = SearchScore.SimilarityScore(0.5)

        // When
        val boosted = score.boost(0.3)

        // Then
        assertEquals(0.8, boosted.value, 0.001)
    }

    @Test
    fun `SimilarityScore multiply should scale score`() {
        // Given
        val score = SearchScore.SimilarityScore(0.5)

        // When
        val scaled = score.multiply(2.0)

        // Then
        assertEquals(1.0, scaled.value, 0.001)
    }

    @Test
    fun `SimilarityScore should support operator overloading`() {
        // Given
        val score1 = SearchScore.SimilarityScore(0.5)
        val score2 = SearchScore.SimilarityScore(0.3)

        // When & Then
        val sum = score1 + score2
        assertEquals(0.8, sum.value, 0.001)

        val boosted = score1 + 0.2
        assertEquals(0.7, boosted.value, 0.001)

        val scaled = score1 * 2.0
        assertEquals(1.0, scaled.value, 0.001)
    }

    @Test
    fun `Distance comparison should be reversed (lower is better)`() {
        // Given
        val distance1 = SearchScore.Distance(0.2) // better
        val distance2 = SearchScore.Distance(0.8) // worse

        // Then
        assertTrue(distance1 > distance2)
    }

    @Test
    fun `SimilarityScore comparison should be normal (higher is better)`() {
        // Given
        val similarity1 = SearchScore.SimilarityScore(0.8) // better
        val similarity2 = SearchScore.SimilarityScore(0.2) // worse

        // Then
        assertTrue(similarity1 > similarity2)
    }

    @Test
    fun `fromDistance should coerce values to valid range`() {
        // Given & When
        val tooLow = SearchScore.fromDistance(-0.5)
        val tooHigh = SearchScore.fromDistance(1.5)
        val valid = SearchScore.fromDistance(0.5)

        // Then
        assertEquals(0.0, tooLow.value)
        assertEquals(1.0, tooHigh.value)
        assertEquals(0.5, valid.value)
    }

    @Test
    fun `fromSimilarity should coerce values to non-negative`() {
        // Given & When
        val negative = SearchScore.fromSimilarity(-0.5)
        val valid = SearchScore.fromSimilarity(0.5)

        // Then
        assertEquals(0.0, negative.value)
        assertEquals(0.5, valid.value)
    }

    @Test
    fun `SearchScoreBuilder should combine scores correctly`() {
        // Given
        val baseScore = SearchScore.SimilarityScore(0.5)

        // When
        val finalScore = baseScore.builder()
            .addKeywordBoost(matchCount = 3, boostPerMatch = 0.1)
            .addTitleBoost(0.2)
            .addContentBoost(0.1)
            .multiplyBy(1.5)
            .build()

        // Then
        // Base: 0.5 + keyword: 0.3 + title: 0.2 + content: 0.1 = 1.1
        // Then multiply by 1.5 = 1.65
        assertEquals(1.65, finalScore.value, 0.001)
    }

    @Test
    fun `SearchScoreBuilder should ignore zero boosts`() {
        // Given
        val baseScore = SearchScore.SimilarityScore(0.5)

        // When
        val finalScore = baseScore.builder()
            .addKeywordBoost(matchCount = 0)
            .addTitleBoost(0.0)
            .addContentBoost(0.0)
            .build()

        // Then
        assertEquals(0.5, finalScore.value, 0.001)
    }
}
