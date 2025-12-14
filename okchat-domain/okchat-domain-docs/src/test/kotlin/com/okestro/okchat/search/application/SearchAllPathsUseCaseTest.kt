package com.okestro.okchat.search.application

import com.okestro.okchat.search.application.dto.SearchAllPathsUseCaseIn
import com.okestro.okchat.search.model.AllowedKnowledgeBases
import com.okestro.okchat.search.support.MetadataFields
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import org.opensearch.client.opensearch.core.search.Hit

class SearchAllPathsUseCaseTest : BehaviorSpec({

    val openSearchClient: OpenSearchClient = mockk()
    val indexName = "test-index"
    val searchAllPathsUseCase = SearchAllPathsUseCase(openSearchClient, indexName)

    given("paths exist in OpenSearch") {
        val path1 = "path/to/document1"
        val path2 = "path/to/document2"

        val hit1 = mockk<Hit<Map<String, Any>>>() {
            coEvery { source() } returns mapOf(MetadataFields.PATH to path1)
        }
        val hit2 = mockk<Hit<Map<String, Any>>>() {
            coEvery { source() } returns mapOf(MetadataFields.PATH to path2)
        }

        val searchResponse1 = mockk<SearchResponse<Map<String, Any>>>() {
            coEvery { hits().hits() } returns listOf(hit1, hit2)
        }
        val searchResponse2 = mockk<SearchResponse<Map<String, Any>>>() {
            coEvery { hits().hits() } returns emptyList()
        }

        coEvery { openSearchClient.search(any<java.util.function.Function<org.opensearch.client.opensearch.core.SearchRequest.Builder, org.opensearch.client.util.ObjectBuilder<org.opensearch.client.opensearch.core.SearchRequest>>>(), any<Class<Map<String, Any>>>()) } returnsMany listOf(searchResponse1, searchResponse2)

        `when`("searchAllPaths is called") {
            val result = searchAllPathsUseCase.execute(SearchAllPathsUseCaseIn(AllowedKnowledgeBases.All))

            then("it should return a list of unique paths") {
                result.paths.size shouldBe 2
                result.paths shouldContainExactly listOf(path1, path2)
            }
        }
    }

    given("no paths exist in OpenSearch") {
        val searchResponse = mockk<SearchResponse<Map<String, Any>>>() {
            coEvery { hits().hits() } returns emptyList()
        }

        coEvery { openSearchClient.search(any<java.util.function.Function<org.opensearch.client.opensearch.core.SearchRequest.Builder, org.opensearch.client.util.ObjectBuilder<org.opensearch.client.opensearch.core.SearchRequest>>>(), any<Class<Map<String, Any>>>()) } returns searchResponse

        `when`("searchAllPaths is called") {
            val result = searchAllPathsUseCase.execute(SearchAllPathsUseCaseIn(AllowedKnowledgeBases.All))

            then("it should return an empty list") {
                result.paths.shouldBeEmpty()
            }
        }
    }
})
