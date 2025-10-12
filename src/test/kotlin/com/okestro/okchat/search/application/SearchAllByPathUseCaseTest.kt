package com.okestro.okchat.search.application

import com.okestro.okchat.search.application.dto.SearchAllByPathUseCaseIn
import com.okestro.okchat.search.model.Document
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

class SearchAllByPathUseCaseTest : BehaviorSpec({

    val openSearchClient: OpenSearchClient = mockk()
    val indexName = "test-index"
    val searchAllByPathUseCase = SearchAllByPathUseCase(openSearchClient, indexName)

    given("documents exist for a given path") {
        val documentPath = "path/to/document"
        val docId1 = "doc1"
        val docTitle1 = "Document 1"
        val docId2 = "doc2"
        val docTitle2 = "Document 2"

        val hit1 = mockk<Hit<Map<String, Any>>>() {
            coEvery { source() } returns mapOf(
                MetadataFields.ID to docId1,
                MetadataFields.TITLE to docTitle1,
                MetadataFields.PATH to documentPath
            )
        }
        val hit2 = mockk<Hit<Map<String, Any>>>() {
            coEvery { source() } returns mapOf(
                MetadataFields.ID to docId2,
                MetadataFields.TITLE to docTitle2,
                MetadataFields.PATH to documentPath
            )
        }

        val searchResponse1 = mockk<SearchResponse<Map<String, Any>>>() {
            coEvery { hits().hits() } returns listOf(hit1, hit2)
        }
        val searchResponse2 = mockk<SearchResponse<Map<String, Any>>>() {
            coEvery { hits().hits() } returns emptyList()
        }

        coEvery {
            openSearchClient.search(
                any<java.util.function.Function<org.opensearch.client.opensearch.core.SearchRequest.Builder, org.opensearch.client.util.ObjectBuilder<org.opensearch.client.opensearch.core.SearchRequest>>>(),
                any<Class<Map<String, Any>>>()
            )
        } returnsMany listOf(searchResponse1, searchResponse2)

        `when`("searchAllByPath is called") {
            val result = searchAllByPathUseCase.execute(SearchAllByPathUseCaseIn(documentPath))

            then("it should return a list of documents") {
                result.documents.size shouldBe 2
                result.documents shouldContainExactly listOf(
                    Document(id = docId1, title = docTitle1, path = documentPath, content = null, spaceKey = null, keywords = null, score = null),
                    Document(id = docId2, title = docTitle2, path = documentPath, content = null, spaceKey = null, keywords = null, score = null)
                )
            }
        }
    }

    given("no documents exist for a given path") {
        val documentPath = "nonexistent/path"

        val searchResponse = mockk<SearchResponse<Map<String, Any>>>() {
            coEvery { hits().hits() } returns emptyList()
        }

        coEvery {
            openSearchClient.search(
                any<java.util.function.Function<org.opensearch.client.opensearch.core.SearchRequest.Builder, org.opensearch.client.util.ObjectBuilder<org.opensearch.client.opensearch.core.SearchRequest>>>(),
                any<Class<Map<String, Any>>>()
            )
        } returns searchResponse

        `when`("searchAllByPath is called") {
            val result = searchAllByPathUseCase.execute(SearchAllByPathUseCaseIn(documentPath))

            then("it should return an empty list") {
                result.documents.shouldBeEmpty()
            }
        }
    }
})
