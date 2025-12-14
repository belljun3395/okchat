package com.okestro.okchat.search.model

/**
 * Type-safe representation of search relevance scores
 *
 * This sealed class hierarchy prevents confusion between:
 * - Distance (lower is better, 0.0 = perfect match)
 * - Similarity (higher is better, 1.0 = perfect match)
 */
sealed class SearchScore : Comparable<SearchScore> {
    abstract val value: Double
    abstract fun toSimilarity(): SimilarityScore

    /**
     * Distance-based score from vector search
     * Range: 0.0 (perfect match) ~ 1.0 (no match)
     * Lower is better!
     */
    data class Distance(override val value: Double) : SearchScore() {
        init {
            require(value in 0.0..1.0) { "Distance must be between 0.0 and 1.0, got $value" }
        }

        override fun toSimilarity(): SimilarityScore {
            return SimilarityScore(1.0 - value)
        }

        override fun compareTo(other: SearchScore): Int {
            // Distance: lower is better, so compare as similarity (higher is better)
            return toSimilarity().value.compareTo(other.toSimilarity().value)
        }
    }

    /**
     * Similarity-based score
     * Range: 0.0 (no match) ~ unbounded (perfect match + boosts)
     * Higher is better!
     */
    data class SimilarityScore(override val value: Double) : SearchScore() {
        init {
            require(value >= 0.0) { "Similarity must be >= 0.0, got $value" }
        }

        override fun toSimilarity(): SimilarityScore = this

        /**
         * Add boost to the similarity score
         */
        fun boost(amount: Double): SimilarityScore {
            require(amount >= 0.0) { "Boost amount must be >= 0.0, got $amount" }
            return SimilarityScore(value + amount)
        }

        /**
         * Multiply the similarity score
         */
        fun multiply(factor: Double): SimilarityScore {
            require(factor >= 0.0) { "Factor must be >= 0.0, got $factor" }
            return SimilarityScore(value * factor)
        }

        override fun compareTo(other: SearchScore): Int {
            // Similarity: higher is better
            return value.compareTo(other.toSimilarity().value)
        }

        operator fun plus(other: SimilarityScore): SimilarityScore {
            return SimilarityScore(value + other.value)
        }

        operator fun plus(boost: Double): SimilarityScore {
            return boost(boost)
        }

        operator fun times(factor: Double): SimilarityScore {
            return multiply(factor)
        }
    }

    companion object {
        /**
         * Create a Distance score from raw value
         */
        fun fromDistance(distance: Double): Distance {
            return Distance(distance.coerceIn(0.0, 1.0))
        }

        /**
         * Create a Similarity score from raw value
         */
        fun fromSimilarity(similarity: Double): SimilarityScore {
            return SimilarityScore(similarity.coerceAtLeast(0.0))
        }

        /**
         * No score / unknown score
         */
        val NONE: SimilarityScore = SimilarityScore(0.0)

        /**
         * Perfect match
         */
        val PERFECT: SimilarityScore = SimilarityScore(1.0)
    }
}

/**
 * Builder for combining multiple search scores
 */
class SearchScoreBuilder {
    private var baseScore: SearchScore.SimilarityScore = SearchScore.NONE

    fun withBase(score: SearchScore): SearchScoreBuilder {
        this.baseScore = score.toSimilarity()
        return this
    }

    fun addKeywordBoost(matchCount: Int, boostPerMatch: Double = 0.1): SearchScoreBuilder {
        if (matchCount > 0) {
            baseScore = baseScore.boost(matchCount * boostPerMatch)
        }
        return this
    }

    fun addTitleBoost(titleScore: Double): SearchScoreBuilder {
        if (titleScore > 0.0) {
            baseScore = baseScore.boost(titleScore)
        }
        return this
    }

    fun addContentBoost(contentScore: Double): SearchScoreBuilder {
        if (contentScore > 0.0) {
            baseScore = baseScore.boost(contentScore)
        }
        return this
    }

    fun multiplyBy(factor: Double): SearchScoreBuilder {
        baseScore = baseScore.multiply(factor)
        return this
    }

    fun build(): SearchScore.SimilarityScore = baseScore
}

/**
 * Extension function to create score builder
 */
fun SearchScore.builder(): SearchScoreBuilder {
    return SearchScoreBuilder().withBase(this)
}
