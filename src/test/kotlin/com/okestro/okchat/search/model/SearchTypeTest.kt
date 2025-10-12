package com.okestro.okchat.search.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SearchType Unit Tests")
class SearchTypeTest {

    @Test
    @DisplayName("getDisplayName should return correct display names")
    fun `getDisplayName should return correct display names`() {
        // when & then
        SearchType.KEYWORD.getDisplayName() shouldBe "Keyword"
        SearchType.TITLE.getDisplayName() shouldBe "Title"
        SearchType.CONTENT.getDisplayName() shouldBe "Content"
        SearchType.PATH.getDisplayName() shouldBe "Path"
    }

    @Test
    @DisplayName("SearchType enum should have all expected values")
    fun `SearchType enum should have all expected values`() {
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
