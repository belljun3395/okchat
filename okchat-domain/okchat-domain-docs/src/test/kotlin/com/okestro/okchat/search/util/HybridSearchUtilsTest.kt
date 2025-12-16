package com.okestro.okchat.search.util

import com.okestro.okchat.search.client.HybridSearchResponse
import com.okestro.okchat.search.client.SearchHit
import com.okestro.okchat.search.model.SearchResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HybridSearchUtilsTest {

    @Test
    fun `parseSearchResults should convert hits to SearchResults`() {
        // Given
        val response = HybridSearchResponse(
            hits = listOf(
                SearchHit(
                    document = mapOf(
                        "id" to "doc1",
                        "content" to "Content 1",
                        "metadata" to mapOf(
                            "title" to "Title 1",
                            "path" to "/path1",
                            "spaceKey" to "TEST",
                            "keywords" to "keyword1",
                            "knowledgeBaseId" to 1L,
                            "webUrl" to "http://example.com/doc1",
                            "downloadUrl" to "http://example.com/download/doc1"
                        )
                    ),
                    textScore = 0.8,
                    vectorScore = 0.6
                ),
                SearchHit(
                    document = mapOf(
                        "id" to "doc2",
                        "content" to "Content 2",
                        "metadata" to mapOf(
                            "title" to "Title 2",
                            "path" to "/path2",
                            "spaceKey" to "TEST",
                            "keywords" to "keyword2",
                            "knowledgeBaseId" to 2L,
                            "webUrl" to "http://example.com/doc2",
                            "downloadUrl" to "http://example.com/download/doc2"
                        )
                    ),
                    textScore = 0.5,
                    vectorScore = 0.9
                )
            )
        )

        // When
        val results = HybridSearchUtils.parseSearchResults(response)

        // Then
        assertEquals(2, results.size)
        // Should be sorted by combined score (descending)
        assertTrue(results[0].score.value >= results[1].score.value)
    }

    @Test
    fun `parseSearchResults should use custom score combiner`() {
        // Given
        val response = HybridSearchResponse(
            hits = listOf(
                SearchHit(
                    document = mapOf(
                        "id" to "doc1",
                        "content" to "Content 1",
                        "metadata" to mapOf(
                            "title" to "Title 1",
                            "path" to "/path1",
                            "spaceKey" to "TEST",
                            "keywords" to "keyword1",
                            "knowledgeBaseId" to 1L,
                            "webUrl" to "http://example.com/doc1",
                            "downloadUrl" to "http://example.com/download/doc1"
                        )
                    ),
                    textScore = 0.8,
                    vectorScore = 0.6
                )
            )
        )

        // When - use weighted average instead of sum
        val results = HybridSearchUtils.parseSearchResults(response) { text, vector ->
            (text * 0.7 + vector * 0.3)
        }

        // Then
        val expectedScore = 0.8 * 0.7 + 0.6 * 0.3
        assertEquals(expectedScore, results[0].score.value, 0.001)
    }

    @Test
    fun `parseSearchResults should handle chunked document IDs`() {
        // Given
        val response = HybridSearchResponse(
            hits = listOf(
                SearchHit(
                    document = mapOf(
                        "id" to "page123_chunk_0",
                        "content" to "Content",
                        "metadata" to mapOf(
                            "title" to "Title 1",
                            "path" to "/path1",
                            "spaceKey" to "TEST",
                            "keywords" to "keyword1",
                            "knowledgeBaseId" to 1L,
                            "webUrl" to "http://example.com/doc1",
                            "downloadUrl" to "http://example.com/download/doc1"
                        )
                    ),
                    textScore = 0.8,
                    vectorScore = 0.6
                )
            )
        )

        // When
        val results = HybridSearchUtils.parseSearchResults(response)

        // Then
        assertEquals("page123", results[0].id) // should extract page ID
    }

    @Test
    fun `deduplicateResults should merge results with same page ID`() {
        // Given
        val results = listOf(
            SearchResult.withSimilarity(
                id = "page1",
                title = "Title",
                content = "Chunk 1",
                path = "/path",
                spaceKey = "TEST",
                similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.8),
                knowledgeBaseId = 0L
            ),
            SearchResult.withSimilarity(
                id = "page1",
                title = "Title",
                content = "Chunk 2",
                path = "/path",
                spaceKey = "TEST",
                similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.6),
                knowledgeBaseId = 0L
            ),
            SearchResult.withSimilarity(
                id = "page2",
                title = "Another Title",
                content = "Different content",
                path = "/path2",
                spaceKey = "TEST",
                similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.5),
                knowledgeBaseId = 0L
            )
        )

        // When
        val deduplicated = HybridSearchUtils.deduplicateResults(results)

        // Then
        assertEquals(2, deduplicated.size) // page1 and page2
        val page1Result = deduplicated.find { it.id == "page1" }!!
        // Content should be merged
        assertTrue(page1Result.content.contains("Chunk 1"))
        assertTrue(page1Result.content.contains("Chunk 2"))
        // Should keep the highest score
        assertEquals(0.8, page1Result.score.value, 0.001)
    }

    @Test
    fun `deduplicateResults should preserve single results`() {
        // Given
        val results = listOf(
            SearchResult.withSimilarity(
                id = "page1",
                title = "Title",
                content = "Content",
                path = "/path",
                spaceKey = "TEST",
                similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.8),
                knowledgeBaseId = 0L
            )
        )

        // When
        val deduplicated = HybridSearchUtils.deduplicateResults(results)

        // Then
        assertEquals(1, deduplicated.size)
        assertEquals("Content", deduplicated[0].content)
    }

    @Test
    fun `deduplicateResults should sort by score descending`() {
        // Given
        val results = listOf(
            SearchResult.withSimilarity(
                id = "page1",
                title = "Title 1",
                content = "Content 1",
                path = "/path1",
                spaceKey = "TEST",
                similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.5),
                knowledgeBaseId = 0L
            ),
            SearchResult.withSimilarity(
                id = "page2",
                title = "Title 2",
                content = "Content 2",
                path = "/path2",
                spaceKey = "TEST",
                similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.9),
                knowledgeBaseId = 0L
            ),
            SearchResult.withSimilarity(
                id = "page3",
                title = "Title 3",
                content = "Content 3",
                path = "/path3",
                spaceKey = "TEST",
                similarity = com.okestro.okchat.search.model.SearchScore.SimilarityScore(0.7),
                knowledgeBaseId = 0L
            )
        )

        // When
        val deduplicated = HybridSearchUtils.deduplicateResults(results)

        // Then
        assertEquals(3, deduplicated.size)
        assertEquals("page2", deduplicated[0].id) // highest score
        assertEquals("page3", deduplicated[1].id)
        assertEquals("page1", deduplicated[2].id) // lowest score
    }
}
