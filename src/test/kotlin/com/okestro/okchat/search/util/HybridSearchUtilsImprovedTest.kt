package com.okestro.okchat.search.util

import com.okestro.okchat.fixture.TestFixtures
import com.okestro.okchat.search.client.HybridSearchResponse
import com.okestro.okchat.search.client.SearchHit
import com.okestro.okchat.search.model.SearchResult
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Improved test for HybridSearchUtils using Kotest.
 * Demonstrates comprehensive parameterized testing for search utilities.
 */
class HybridSearchUtilsImprovedTest : DescribeSpec({

    describe("HybridSearchUtils - parseSearchResults") {

        context("converting hits to SearchResults") {

            it("should convert hits to SearchResults with default score combiner") {
                // Given
                val response = HybridSearchResponse(
                    hits = listOf(
                        SearchHit(
                            document = TestFixtures.SearchUtils.createSearchHitDocument(
                                id = "doc1",
                                content = "Content 1",
                                title = "Title 1",
                                path = "/path1",
                                spaceKey = "TEST",
                                keywords = "keyword1"
                            ),
                            textScore = 0.8,
                            vectorScore = 0.6
                        ),
                        SearchHit(
                            document = TestFixtures.SearchUtils.createSearchHitDocument(
                                id = "doc2",
                                content = "Content 2",
                                title = "Title 2",
                                path = "/path2",
                                spaceKey = "TEST",
                                keywords = "keyword2"
                            ),
                            textScore = 0.5,
                            vectorScore = 0.9
                        )
                    )
                )

                // When
                val results = HybridSearchUtils.parseSearchResults(response)

                // Then
                results shouldHaveSize 2
                // Should be sorted by combined score (descending)
                (results[0].score.value >= results[1].score.value) shouldBe true
            }

            data class ScoreCombinerTestCase(
                val description: String,
                val textScore: Double,
                val vectorScore: Double,
                val weightText: Double,
                val weightVector: Double,
                val expectedScore: Double
            )

            withData(
                nameFn = { it.description },
                ScoreCombinerTestCase(
                    description = "Equal weights (0.5, 0.5)",
                    textScore = 0.8,
                    vectorScore = 0.6,
                    weightText = 0.5,
                    weightVector = 0.5,
                    expectedScore = 0.7
                ),
                ScoreCombinerTestCase(
                    description = "Text-heavy weights (0.7, 0.3)",
                    textScore = 0.8,
                    vectorScore = 0.6,
                    weightText = 0.7,
                    weightVector = 0.3,
                    expectedScore = 0.74
                ),
                ScoreCombinerTestCase(
                    description = "Vector-heavy weights (0.3, 0.7)",
                    textScore = 0.8,
                    vectorScore = 0.6,
                    weightText = 0.3,
                    weightVector = 0.7,
                    expectedScore = 0.66
                )
            ) { (_, textScore, vectorScore, weightText, weightVector, expectedScore) ->
                // Given
                val response = HybridSearchResponse(
                    hits = listOf(
                        SearchHit(
                            document = TestFixtures.SearchUtils.createSearchHitDocument(
                                id = "doc1",
                                content = "Content 1",
                                title = "Title 1",
                                path = "/path1",
                                spaceKey = "TEST"
                            ),
                            textScore = textScore,
                            vectorScore = vectorScore
                        )
                    )
                )

                // When - use weighted average
                val results = HybridSearchUtils.parseSearchResults(response) { text, vector ->
                    text * weightText + vector * weightVector
                }

                // Then
                kotlin.math.abs(results[0].score.value - expectedScore) shouldBe 0.0.plusOrMinus(0.001)
            }
        }

        context("handling chunked document IDs") {

            data class ChunkedIdTestCase(
                val description: String,
                val inputId: String,
                val expectedId: String
            )

            withData(
                nameFn = { it.description },
                ChunkedIdTestCase(
                    description = "Standard chunk format",
                    inputId = "page123_chunk_0",
                    expectedId = "page123"
                ),
                ChunkedIdTestCase(
                    description = "Multiple chunks",
                    inputId = "page456_chunk_5",
                    expectedId = "page456"
                ),
                ChunkedIdTestCase(
                    description = "No chunk suffix",
                    inputId = "page789",
                    expectedId = "page789"
                )
            ) { (_, inputId, expectedId) ->
                // Given
                val response = HybridSearchResponse(
                    hits = listOf(
                        SearchHit(
                            document = TestFixtures.SearchUtils.createSearchHitDocument(
                                id = inputId,
                                content = "Content",
                                title = "Title",
                                path = "/path",
                                spaceKey = "TEST"
                            ),
                            textScore = 0.8,
                            vectorScore = 0.6
                        )
                    )
                )

                // When
                val results = HybridSearchUtils.parseSearchResults(response)

                // Then
                results[0].id shouldBe expectedId
            }
        }

        context("edge cases") {

            it("should handle empty hits") {
                // Given
                val response = HybridSearchResponse(hits = emptyList())

                // When
                val results = HybridSearchUtils.parseSearchResults(response)

                // Then
                results shouldHaveSize 0
            }

            it("should handle documents with missing optional fields") {
                // Given
                val response = HybridSearchResponse(
                    hits = listOf(
                        SearchHit(
                            document = mapOf(
                                "id" to "doc1",
                                "content" to "Content",
                                "metadata.title" to "Title",
                                "metadata.path" to "/path",
                                "metadata.spaceKey" to "TEST"
                                // keywords missing
                            ),
                            textScore = 0.8,
                            vectorScore = 0.6
                        )
                    )
                )

                // When
                val results = HybridSearchUtils.parseSearchResults(response)

                // Then
                results shouldHaveSize 1
                results[0].id shouldBe "doc1"
            }
        }
    }

    describe("HybridSearchUtils - deduplicateResults") {

        context("merging results with same page ID") {

            it("should merge results with same page ID") {
                // Given
                val results = listOf(
                    SearchResult.withSimilarity(
                        id = "page1",
                        title = "Title",
                        content = "Chunk 1",
                        path = "/path",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.8)
                    ),
                    SearchResult.withSimilarity(
                        id = "page1",
                        title = "Title",
                        content = "Chunk 2",
                        path = "/path",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.6)
                    ),
                    SearchResult.withSimilarity(
                        id = "page2",
                        title = "Another Title",
                        content = "Different content",
                        path = "/path2",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.5)
                    )
                )

                // When
                val deduplicated = HybridSearchUtils.deduplicateResults(results)

                // Then
                deduplicated shouldHaveSize 2 // page1 and page2

                val page1Result = deduplicated.find { it.id == "page1" }!!
                page1Result.content shouldContain "Chunk 1"
                page1Result.content shouldContain "Chunk 2"
                page1Result.score.value shouldBe 0.8 // highest score
            }

            it("should preserve single results without modification") {
                // Given
                val results = listOf(
                    SearchResult.withSimilarity(
                        id = "page1",
                        title = "Title",
                        content = "Content",
                        path = "/path",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.8)
                    )
                )

                // When
                val deduplicated = HybridSearchUtils.deduplicateResults(results)

                // Then
                deduplicated shouldHaveSize 1
                deduplicated[0].content shouldBe "Content"
            }

            data class DeduplicationTestCase(
                val description: String,
                val inputSize: Int,
                val uniqueIds: Int,
                val expectedSize: Int
            )

            withData(
                nameFn = { it.description },
                DeduplicationTestCase(
                    description = "No duplicates",
                    inputSize = 5,
                    uniqueIds = 5,
                    expectedSize = 5
                ),
                DeduplicationTestCase(
                    description = "All duplicates",
                    inputSize = 5,
                    uniqueIds = 1,
                    expectedSize = 1
                ),
                DeduplicationTestCase(
                    description = "Some duplicates",
                    inputSize = 10,
                    uniqueIds = 3,
                    expectedSize = 3
                )
            ) { (_, inputSize, uniqueIds, expectedSize) ->
                // Given
                val results = (0 until inputSize).map { i ->
                    val id = "page${i % uniqueIds}"
                    SearchResult.withSimilarity(
                        id = id,
                        title = "Title $id",
                        content = "Content $i",
                        path = "/path",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.8 - (i * 0.05))
                    )
                }

                // When
                val deduplicated = HybridSearchUtils.deduplicateResults(results)

                // Then
                deduplicated shouldHaveSize expectedSize
            }
        }

        context("sorting by score") {

            it("should sort deduplicated results by score descending") {
                // Given
                val results = listOf(
                    SearchResult.withSimilarity(
                        id = "page1",
                        title = "Title 1",
                        content = "Content 1",
                        path = "/path1",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.5)
                    ),
                    SearchResult.withSimilarity(
                        id = "page2",
                        title = "Title 2",
                        content = "Content 2",
                        path = "/path2",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.9)
                    ),
                    SearchResult.withSimilarity(
                        id = "page3",
                        title = "Title 3",
                        content = "Content 3",
                        path = "/path3",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.7)
                    )
                )

                // When
                val deduplicated = HybridSearchUtils.deduplicateResults(results)

                // Then
                deduplicated shouldHaveSize 3
                deduplicated[0].id shouldBe "page2" // highest score
                deduplicated[1].id shouldBe "page3"
                deduplicated[2].id shouldBe "page1" // lowest score

                // Verify scores are in descending order
                deduplicated.zipWithNext().forEach { (current, next) ->
                    (current.score.value >= next.score.value) shouldBe true
                }
            }
        }

        context("content merging") {

            it("should separate merged chunks with newlines") {
                // Given
                val results = listOf(
                    SearchResult.withSimilarity(
                        id = "page1",
                        title = "Title",
                        content = "First chunk",
                        path = "/path",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.8)
                    ),
                    SearchResult.withSimilarity(
                        id = "page1",
                        title = "Title",
                        content = "Second chunk",
                        path = "/path",
                        spaceKey = "TEST",
                        similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.7)
                    )
                )

                // When
                val deduplicated = HybridSearchUtils.deduplicateResults(results)

                // Then
                deduplicated shouldHaveSize 1
                val merged = deduplicated[0]
                merged.content shouldContain "First chunk"
                merged.content shouldContain "Second chunk"
                // Chunks should be separated
                (merged.content.split("\n").size >= 2) shouldBe true
            }
        }

        context("empty input") {

            it("should handle empty list") {
                // Given
                val results = emptyList<SearchResult>()

                // When
                val deduplicated = HybridSearchUtils.deduplicateResults(results)

                // Then
                deduplicated shouldHaveSize 0
            }
        }
    }
})
