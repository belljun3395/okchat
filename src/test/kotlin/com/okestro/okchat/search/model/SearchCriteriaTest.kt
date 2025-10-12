package com.okestro.okchat.search.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SearchCriteria Implementation Tests")
class SearchCriteriaTest {

    @Test
    @DisplayName("SearchKeywords should implement SearchCriteria correctly")
    fun `SearchKeywords should implement SearchCriteria correctly`() {
        // given
        val keywords = SearchKeywords.fromStrings(listOf("kotlin", "spring", "boot"))

        // when & then
        keywords.getSearchType() shouldBe SearchType.KEYWORD
        keywords.isEmpty() shouldBe false
        keywords.size() shouldBe 3
        keywords.toQuery() shouldBe "kotlin OR spring OR boot"
    }

    @Test
    @DisplayName("SearchTitles should implement SearchCriteria correctly")
    fun `SearchTitles should implement SearchCriteria correctly`() {
        // given
        val titles = SearchTitles.fromStrings(listOf("User Guide", "API Reference"))

        // when & then
        titles.getSearchType() shouldBe SearchType.TITLE
        titles.isEmpty() shouldBe false
        titles.size() shouldBe 2
    }

    @Test
    @DisplayName("SearchContents should implement SearchCriteria correctly")
    fun `SearchContents should implement SearchCriteria correctly`() {
        // given
        val contents = SearchContents.fromStrings(listOf("tutorial", "documentation"))

        // when & then
        contents.getSearchType() shouldBe SearchType.CONTENT
        contents.isEmpty() shouldBe false
        contents.size() shouldBe 2
    }

    @Test
    @DisplayName("SearchPaths should implement SearchCriteria correctly")
    fun `SearchPaths should implement SearchCriteria correctly`() {
        // given
        val paths = SearchPaths.fromStrings(listOf("Development", "Backend"))

        // when & then
        paths.getSearchType() shouldBe SearchType.PATH
        paths.isEmpty() shouldBe false
        paths.size() shouldBe 2
    }

    @Test
    @DisplayName("empty criteria should report isEmpty as true")
    fun `empty criteria should report isEmpty as true`() {
        // given
        val emptyKeywords = SearchKeywords.fromStrings(emptyList())

        // when & then
        emptyKeywords.isEmpty() shouldBe true
        emptyKeywords.size() shouldBe 0
    }
}
