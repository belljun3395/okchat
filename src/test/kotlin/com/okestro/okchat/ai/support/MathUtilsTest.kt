package com.okestro.okchat.ai.support

import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.sqrt

@DisplayName("MathUtils Tests")
class MathUtilsTest {

    companion object {
        @JvmStatic
        fun vectorSimilarityTestCases() = listOf(
            // vec1, vec2, expectedSimilarity, description
            Arguments.of(
                listOf(1.0f, 2.0f, 3.0f),
                listOf(1.0f, 2.0f, 3.0f),
                1.0,
                "identical vectors"
            ),
            Arguments.of(
                listOf(1.0f, 0.0f),
                listOf(0.0f, 1.0f),
                0.0,
                "orthogonal vectors"
            ),
            Arguments.of(
                listOf(1.0f, 2.0f, 3.0f),
                listOf(-1.0f, -2.0f, -3.0f),
                -1.0,
                "opposite vectors"
            ),
            Arguments.of(
                listOf(0.0001f, 0.0002f, 0.0003f),
                listOf(0.0001f, 0.0002f, 0.0003f),
                1.0,
                "very small identical vectors"
            ),
            Arguments.of(
                listOf(10000.0f, 20000.0f, 30000.0f),
                listOf(10000.0f, 20000.0f, 30000.0f),
                1.0,
                "very large identical vectors"
            ),
            Arguments.of(
                listOf(5.0f),
                listOf(3.0f),
                1.0,
                "single dimension positive vectors"
            ),
            Arguments.of(
                listOf(-1.0f, -2.0f, 3.0f),
                listOf(1.0f, 2.0f, -3.0f),
                -1.0,
                "mixed sign vectors"
            )
        )

        @JvmStatic
        fun zeroVectorTestCases() = listOf(
            Arguments.of(listOf(0.0f, 0.0f, 0.0f), listOf(1.0f, 2.0f, 3.0f), "one zero vector"),
            Arguments.of(listOf(0.0f, 0.0f, 0.0f), listOf(0.0f, 0.0f, 0.0f), "both zero vectors")
        )
    }

    @ParameterizedTest(name = "should return {2} for {3}")
    @MethodSource("vectorSimilarityTestCases")
    @DisplayName("cosineSimilarity should calculate correct similarity for various vector pairs")
    fun `should calculate correct cosine similarity`(
        vec1: List<Float>,
        vec2: List<Float>,
        expected: Double,
        description: String
    ) {
        // when
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // then
        similarity shouldBe expected
    }

    @ParameterizedTest(name = "should handle {2}")
    @MethodSource("zeroVectorTestCases")
    @DisplayName("cosineSimilarity should handle zero vectors")
    fun `should handle zero vectors`(vec1: List<Float>, vec2: List<Float>, description: String) {
        // when
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // then
        similarity shouldBe 0.0
    }

    @Test
    @DisplayName("should calculate correct similarity for arbitrary vectors")
    fun `should calculate correct similarity for arbitrary vectors`() {
        // given
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(2.0f, 3.0f, 4.0f)

        // when
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // then
        // Manual calculation:
        // dot product = 1*2 + 2*3 + 3*4 = 2 + 6 + 12 = 20
        // magnitude1 = sqrt(1^2 + 2^2 + 3^2) = sqrt(14) ≈ 3.742
        // magnitude2 = sqrt(2^2 + 3^2 + 4^2) = sqrt(29) ≈ 5.385
        // similarity = 20 / (3.742 * 5.385) ≈ 0.9925
        val expected = 20.0 / (sqrt(14.0) * sqrt(29.0))
        similarity shouldBe expected
    }

    @Test
    @DisplayName("should handle high dimensional vectors")
    fun `should handle high dimensional vectors`() {
        // given
        val vec1 = (1..100).map { it.toFloat() }
        val vec2 = (1..100).map { it.toFloat() }

        // when
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // then
        similarity shouldBe 1.0
    }

    @Test
    @DisplayName("should throw exception for different dimensions")
    fun `should throw exception for different dimensions`() {
        // given
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(1.0f, 2.0f)

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            MathUtils.cosineSimilarity(vec1, vec2)
        }
        exception.message!! shouldContain "same dimension"
    }

    @Test
    @DisplayName("should be symmetric")
    fun `should be symmetric`() {
        // given
        val vec1 = listOf(1.0f, 2.0f, 3.0f)
        val vec2 = listOf(4.0f, 5.0f, 6.0f)

        // when
        val similarity1 = MathUtils.cosineSimilarity(vec1, vec2)
        val similarity2 = MathUtils.cosineSimilarity(vec2, vec1)

        // then
        similarity1 shouldBe similarity2
    }

    @Test
    @DisplayName("should be between -1 and 1")
    fun `should be between -1 and 1`() {
        // given
        val vec1 = listOf(1.5f, -2.3f, 4.7f, -0.8f)
        val vec2 = listOf(-3.2f, 5.1f, -1.9f, 2.4f)

        // when
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // then
        similarity shouldBeGreaterThanOrEqual -1.0
        similarity shouldBeLessThanOrEqual 1.0
    }

    @Test
    @DisplayName("should handle mixed positive and negative vectors")
    fun `should handle mixed positive and negative vectors`() {
        // given
        val vec1 = listOf(1.0f, -1.0f, 1.0f, -1.0f)
        val vec2 = listOf(1.0f, 1.0f, 1.0f, 1.0f)

        // when
        val similarity = MathUtils.cosineSimilarity(vec1, vec2)

        // then
        // dot product = 1 - 1 + 1 - 1 = 0
        similarity shouldBe 0.0
    }
}
