package com.okestro.okchat.search.application

import com.okestro.okchat.search.application.dto.MultiSearchUseCaseIn
import com.okestro.okchat.search.model.ContentSearchResults
import com.okestro.okchat.search.model.KeywordSearchResults
import com.okestro.okchat.search.model.MultiSearchResult
import com.okestro.okchat.search.model.PathSearchResults
import com.okestro.okchat.search.model.SearchContents
import com.okestro.okchat.search.model.SearchKeywords
import com.okestro.okchat.search.model.SearchPaths
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import com.okestro.okchat.search.model.SearchType
import com.okestro.okchat.search.model.SearchTitles
import com.okestro.okchat.search.model.TitleSearchResults
import com.okestro.okchat.search.model.TypedSearchResults
import com.okestro.okchat.search.strategy.MultiSearchStrategy
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class MultiSearchUseCaseTest : BehaviorSpec({

    val searchStrategy: MultiSearchStrategy = mockk()
    val multiSearchUseCase = MultiSearchUseCase(searchStrategy)

    given("MultiSearchUseCase is executed with various search criteria") {
        val titles = SearchTitles.fromStrings(listOf("title1"))
        val contents = SearchContents.fromStrings(listOf("content1"))
        val paths = SearchPaths.fromStrings(listOf("path1"))
        val keywords = SearchKeywords.fromStrings(listOf("keyword1"))
        val topK = 50

        val mockResult = MultiSearchResult.fromMap(
            mapOf(
                SearchType.KEYWORD to KeywordSearchResults(listOf(SearchResult(id = "k1", title = "k1", content = "k1", path = "k1", spaceKey = "k1", score = SearchScore.fromSimilarity(0.1)))),
                SearchType.TITLE to TitleSearchResults(listOf(SearchResult(id = "t1", title = "t1", content = "t1", path = "t1", spaceKey = "t1", score = SearchScore.fromSimilarity(0.2)))),
                SearchType.CONTENT to ContentSearchResults(listOf(SearchResult(id = "c1", title = "c1", content = "c1", path = "c1", spaceKey = "c1", score = SearchScore.fromSimilarity(0.3)))),
                SearchType.PATH to PathSearchResults(listOf(SearchResult(id = "p1", title = "p1", content = "p1", path = "p1", spaceKey = "p1", score = SearchScore.fromSimilarity(0.4))))
            )
        )

        coEvery { searchStrategy.getStrategyName() } returns "TestStrategy"
        coEvery { searchStrategy.search(any(), any()) } returns mockResult

        `when`("the execute method is called") {
            val result = multiSearchUseCase.execute(
                MultiSearchUseCaseIn(
                    titles = titles,
                    contents = contents,
                    paths = paths,
                    keywords = keywords,
                    topK = topK
                )
            )

            then("it should return the combined search results") {
                result.result shouldBe mockResult
            }
        }
    }
})