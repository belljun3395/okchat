package com.okestro.okchat.search.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultsTest {
    private fun createSampleResult(id: String, score: Double): SearchResult {
        return SearchResult.withSimilarity(
            id = id,
            title = "Title $id",
            content = "Content $id",
            path = "/path/$id",
            spaceKey = "TEST",
            similarity = SearchScore.SimilarityScore(score)
        )
    }

    @Test
    fun `KeywordSearchResults should have correct type`() {
        // Given
        val results = listOf(createSampleResult("1", 0.8))
        val searchResults = KeywordSearchResults(results)

        // Then
        assertEquals(SearchType.KEYWORD, searchResults.type)
        assertEquals(1, searchResults.size)
    }

    @Test
    fun `TitleSearchResults should have correct type`() {
        // Given
        val results = listOf(createSampleResult("1", 0.8))
        val searchResults = TitleSearchResults(results)

        // Then
        assertEquals(SearchType.TITLE, searchResults.type)
    }

    @Test
    fun `ContentSearchResults should have correct type`() {
        // Given
        val results = listOf(createSampleResult("1", 0.8))
        val searchResults = ContentSearchResults(results)

        // Then
        assertEquals(SearchType.CONTENT, searchResults.type)
    }

    @Test
    fun `PathSearchResults should have correct type`() {
        // Given
        val results = listOf(createSampleResult("1", 0.8))
        val searchResults = PathSearchResults(results)

        // Then
        assertEquals(SearchType.PATH, searchResults.type)
    }

    @Test
    fun `TypedSearchResults factory should create correct type`() {
        // Given
        val results = listOf(createSampleResult("1", 0.8))

        // When
        val keywordResults = TypedSearchResults.of(SearchType.KEYWORD, results)
        val titleResults = TypedSearchResults.of(SearchType.TITLE, results)
        val contentResults = TypedSearchResults.of(SearchType.CONTENT, results)
        val pathResults = TypedSearchResults.of(SearchType.PATH, results)

        // Then
        assertTrue(keywordResults is KeywordSearchResults)
        assertTrue(titleResults is TitleSearchResults)
        assertTrue(contentResults is ContentSearchResults)
        assertTrue(pathResults is PathSearchResults)
    }

    @Test
    fun `isEmpty should return true for empty results`() {
        // Given
        val emptyResults = KeywordSearchResults(emptyList())

        // Then
        assertTrue(emptyResults.isEmpty)
        assertFalse(emptyResults.isNotEmpty)
        assertEquals(0, emptyResults.size)
    }

    @Test
    fun `isNotEmpty should return true for non-empty results`() {
        // Given
        val results = KeywordSearchResults(listOf(createSampleResult("1", 0.8)))

        // Then
        assertFalse(results.isEmpty)
        assertTrue(results.isNotEmpty)
        assertEquals(1, results.size)
    }

    @Test
    fun `topN should return top N results`() {
        // Given
        val results = KeywordSearchResults(
            listOf(
                createSampleResult("1", 0.9),
                createSampleResult("2", 0.8),
                createSampleResult("3", 0.7),
                createSampleResult("4", 0.6),
                createSampleResult("5", 0.5)
            )
        )

        // When
        val top3 = results.topN(3)

        // Then
        assertEquals(3, top3.size)
        assertEquals("1", top3[0].id)
        assertEquals("2", top3[1].id)
        assertEquals("3", top3[2].id)
    }

    @Test
    fun `topN should return all results if N is larger than size`() {
        // Given
        val results = KeywordSearchResults(
            listOf(
                createSampleResult("1", 0.9),
                createSampleResult("2", 0.8)
            )
        )

        // When
        val top10 = results.topN(10)

        // Then
        assertEquals(2, top10.size)
    }

    @Test
    fun `empty factory methods should create empty results`() {
        // When
        val keywordEmpty = KeywordSearchResults.empty()
        val titleEmpty = TitleSearchResults.empty()
        val contentEmpty = ContentSearchResults.empty()
        val pathEmpty = PathSearchResults.empty()

        // Then
        assertTrue(keywordEmpty.isEmpty)
        assertTrue(titleEmpty.isEmpty)
        assertTrue(contentEmpty.isEmpty)
        assertTrue(pathEmpty.isEmpty)
    }
}
