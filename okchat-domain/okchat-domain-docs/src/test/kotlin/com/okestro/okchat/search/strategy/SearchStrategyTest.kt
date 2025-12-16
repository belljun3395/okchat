package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.model.SearchCriteria
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchResult
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SearchStrategy Tests")
class SearchStrategyTest {

    @Test
    @DisplayName("should define required interface methods")
    fun `should define required interface methods`() = runTest {
        // given
        val testStrategy = object : SearchStrategy {
            override suspend fun search(criteria: SearchCriteria, topK: Int): List<SearchResult> {
                return emptyList()
            }

            override fun getName(): String {
                return "Test Strategy"
            }
        }

        // when
        val name = testStrategy.getName()
        val keywords = SearchKeywords.fromStrings(listOf("test"))
        val result = testStrategy.search(keywords, 10)

        // then
        name shouldBe "Test Strategy"
        result shouldBe emptyList()
    }
}
