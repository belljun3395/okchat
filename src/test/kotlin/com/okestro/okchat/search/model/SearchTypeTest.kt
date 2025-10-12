package com.okestro.okchat.search.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("SearchType Tests")
class SearchTypeTest {

    @ParameterizedTest(name = "{0} should have display name {1}")
    @EnumSource(SearchType::class)
    @DisplayName("should return correct display names")
    fun `should return correct display name`(searchType: SearchType) {
        // given
        val expectedNames = mapOf(
            SearchType.KEYWORD to "Keyword",
            SearchType.TITLE to "Title",
            SearchType.CONTENT to "Content",
            SearchType.PATH to "Path"
        )

        // when
        val displayName = searchType.getDisplayName()

        // then
        displayName shouldBe expectedNames[searchType]
    }

    @Test
    @DisplayName("should have all expected values")
    fun `should have all expected values`() {
        // when
        val types = SearchType.entries

        // then
        types.size shouldBe 4
        types shouldBe listOf(
            SearchType.KEYWORD,
            SearchType.TITLE,
            SearchType.CONTENT,
            SearchType.PATH
        )
    }
}
