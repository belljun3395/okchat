package com.okestro.okchat.ai.support

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MathUtilsTest {

    @Test
    fun `cosineSimilarity should return 1_0 for identical vectors`() {
        // Given
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(1.0f, 2.0f, 3.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        assertEquals(1.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should return 0_0 for orthogonal vectors`() {
        // Given
        val vec1 = listOf(1.0f, 0.0f)
        val vec2 = listOf(0.0f, 1.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        assertEquals(0.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should return -1_0 for opposite vectors`() {
        // Given
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(-1.0f, -2.0f, -3.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        assertEquals(-1.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should calculate correct similarity for arbitrary vectors`() {
        // Given
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(2.0f, 3.0f, 4.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        // Manual calculation:
        // dot product = 1*2 + 2*3 + 3*4 = 2 + 6 + 12 = 20
        // magnitude1 = sqrt(1^2 + 2^2 + 3^2) = sqrt(14) ≈ 3.742
        // magnitude2 = sqrt(2^2 + 3^2 + 4^2) = sqrt(29) ≈ 5.385
        // similarity = 20 / (3.742 * 5.385) ≈ 0.9925
        val expected = 20.0 / (sqrt(14.0) * sqrt(29.0))
        assertEquals(expected, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should handle zero vectors`() {
        // Given
        val vec1 = listOf(0.0f, 0.0f, 0.0f)
        val vec2 = listOf(1.0f, 2.0f, 3.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        assertEquals(0.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should handle both zero vectors`() {
        // Given
        val vec1 = listOf(0.0f, 0.0f, 0.0f)
        val vec2 = listOf(0.0f, 0.0f, 0.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        assertEquals(0.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should handle single dimension vectors`() {
        // Given
        val vec1 = listOf(5.0f)
        val vec2 = listOf(3.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        // For positive single dimensions, similarity should be 1.0
        assertEquals(1.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should handle high dimensional vectors`() {
        // Given
        val vec1 = (1..100).map { it.toFloat() }
        val vec2 = (1..100).map { it.toFloat() }

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        assertEquals(1.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should handle negative values`() {
        // Given
        val vec1 = listOf(-1.0f, -2.0f, 3.0f)
        val vec2 = listOf(1.0f, 2.0f, -3.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        // dot product = -1*1 + -2*2 + 3*-3 = -1 - 4 - 9 = -14
        // magnitude1 = sqrt(1 + 4 + 9) = sqrt(14)
        // magnitude2 = sqrt(1 + 4 + 9) = sqrt(14)
        // similarity = -14 / 14 = -1.0
        assertEquals(-1.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should throw exception for different dimensions`() {
        // Given
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(1.0f, 2.0f)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            MathUtils.cosineSimilarity(vec1, vec2)
        }
        assertTrue(exception.message!!.contains("same dimension"))
    }

    @Test
    fun `cosineSimilarity should handle very small values`() {
        // Given
        val vec1 = listOf(0.0001f, 0.0002f, 0.0003f)
        val vec2 = listOf(0.0001f, 0.0002f, 0.0003f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        assertEquals(1.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should handle very large values`() {
        // Given
        val vec1 = listOf(10000.0f, 20000.0f, 30000.0f)
        val vec2 = listOf(10000.0f, 20000.0f, 30000.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        assertEquals(1.0, similarity, 0.0001)
    }

    @Test
    fun `cosineSimilarity should be symmetric`() {
        // Given
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(4.0f, 5.0f, 6.0f)

        // When
        val similarity1 = MathUtils.cosineSimilarity(vec1, vec2)
        val similarity2 = MathUtils.cosineSimilarity(vec2, vec1)

        // Then
        assertEquals(similarity1, similarity2, 0.0001)
    }

    @Test
    fun `cosineSimilarity should be between -1 and 1`() {
        // Given
        val vec1 = listOf(1.5f, -2.3f, 4.7f, -0.8f)
        val vec2 = listOf(-3.2f, 5.1f, -1.9f, 2.4f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        assertTrue(similarity >= -1.0 && similarity <= 1.0)
    }

    @Test
    fun `cosineSimilarity should handle mixed positive and negative vectors`() {
        // Given
        val vec1 = listOf(1.0f, -1.0f, 1.0f, -1.0f)
        val vec2 = listOf(1.0f, 1.0f, 1.0f, 1.0f)

        // When
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // Then
        // dot product = 1 - 1 + 1 - 1 = 0
        assertEquals(0.0, similarity, 0.0001)
    }
}
