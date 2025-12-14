package com.okestro.okchat.search.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("SearchCriteria Tests")
class SearchCriteriaTest {

    companion object {
        @JvmStatic
        fun criteriaImplementationTestCases() = listOf(
            Arguments.of(
                SearchKeywords.fromStrings(listOf("kotlin", "spring", "boot")),
                SearchType.KEYWORD,
                3,
                "SearchKeywords"
            ),
            Arguments.of(
                SearchTitles.fromStrings(listOf("User Guide", "API Reference")),
                SearchType.TITLE,
                2,
                "SearchTitles"
            ),
            Arguments.of(
                SearchContents.fromStrings(listOf("tutorial", "documentation")),
                SearchType.CONTENT,
                2,
                "SearchContents"
            ),
            Arguments.of(
                SearchPaths.fromStrings(listOf("Development", "Backend")),
                SearchType.PATH,
                2,
                "SearchPaths"
            )
        )

        @JvmStatic
        fun emptyCriteriaTestCases() = listOf(
            Arguments.of(SearchKeywords.fromStrings(emptyList()), "SearchKeywords"),
            Arguments.of(SearchTitles.fromStrings(emptyList()), "SearchTitles"),
            Arguments.of(SearchContents.fromStrings(emptyList()), "SearchContents"),
            Arguments.of(SearchPaths.fromStrings(emptyList()), "SearchPaths")
        )
    }

    @ParameterizedTest(name = "{3} should implement SearchCriteria correctly")
    @MethodSource("criteriaImplementationTestCases")
    @DisplayName("should implement SearchCriteria correctly")
    fun `should implement SearchCriteria correctly`(
        criteria: SearchCriteria,
        expectedType: SearchType,
        expectedSize: Int,
        description: String
    ) {
        // when & then
        criteria.getSearchType() shouldBe expectedType
        criteria.isEmpty() shouldBe false
        criteria.size() shouldBe expectedSize
    }

    @ParameterizedTest(name = "{1} should report isEmpty as true")
    @MethodSource("emptyCriteriaTestCases")
    @DisplayName("should report isEmpty as true for empty criteria")
    fun `should report isEmpty as true for empty criteria`(criteria: SearchCriteria, description: String) {
        // when & then
        criteria.isEmpty() shouldBe true
        criteria.size() shouldBe 0
    }

    @Test
    @DisplayName("SearchKeywords should generate correct query format")
    fun `should generate correct query format for keywords`() {
        // given
        val keywords = SearchKeywords.fromStrings(listOf("kotlin", "spring", "boot"))

        // when
        val query = keywords.toQuery()

        // then
        query shouldBe "kotlin OR spring OR boot"
    }
}
