package com.okestro.okchat.ai.support

import kotlin.math.sqrt

object  MathUtils {

    fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Double {
        require(vec1.size == vec2.size) { "Vectors must have the same dimension" }

        val dotProduct = vec1.zip(vec2) { a, b -> a * b }.sum()
        val magnitude1 = sqrt(vec1.sumOf { (it * it).toDouble() })
        val magnitude2 = sqrt(vec2.sumOf { (it * it).toDouble() })

        return if (magnitude1 > 0 && magnitude2 > 0) {
            dotProduct / (magnitude1 * magnitude2)
        } else {
            0.0
        }
    }
}