package com.okestro.okchat.search.application

import com.okestro.okchat.search.application.dto.SearchAllByPathUseCaseIn
import com.okestro.okchat.search.index.DocumentIndex
import com.okestro.okchat.search.model.Document
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.SearchResponse
import org.opensearch.client.opensearch.core.search.Hit

class SearchAllByPathUseCaseTest : BehaviorSpec({

    val openSearchClient: OpenSearchClient = mockk()
    val indexName = "test-index"
    val searchAllByPathUseCase = SearchAllByPathUseCase(openSearchClient, indexName, null)

    given("documents exist for a given path") {
        val documentPath = "path/to/document"
        val docId1 = "doc1"
        val docTitle1 = "Document 1"
        val docId2 = "doc2"
        val docTitle2 = "Document 2"

        val hit1 = mockk<Hit<Map<String, Any>>>() {
            coEvery { source() } returns mapOf(
                DocumentIndex.Fields.METADATA_OBJECT to mapOf(
                    DocumentIndex.DocumentCommonMetadata.ID.key to docId1,
                    DocumentIndex.DocumentCommonMetadata.TITLE.key to docTitle1,
                    DocumentIndex.DocumentCommonMetadata.PATH.key to documentPath,
                    DocumentIndex.DocumentCommonMetadata.KNOWLEDGE_BASE_ID.key to 0L
                )
            )
        }
        val hit2 = mockk<Hit<Map<String, Any>>>() {
            coEvery { source() } returns mapOf(
                DocumentIndex.Fields.METADATA_OBJECT to mapOf(
                    DocumentIndex.DocumentCommonMetadata.ID.key to docId2,
                    DocumentIndex.DocumentCommonMetadata.TITLE.key to docTitle2,
                    DocumentIndex.DocumentCommonMetadata.PATH.key to documentPath,
                    DocumentIndex.DocumentCommonMetadata.KNOWLEDGE_BASE_ID.key to 0L
                )
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
                    Document(id = docId1, title = docTitle1, path = documentPath, content = null, spaceKey = null, keywords = null, score = null, knowledgeBaseId = 0L),
                    Document(id = docId2, title = docTitle2, path = documentPath, content = null, spaceKey = null, keywords = null, score = null, knowledgeBaseId = 0L)
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
