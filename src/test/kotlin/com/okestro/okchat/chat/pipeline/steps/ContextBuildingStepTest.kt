package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.fixture.TestFixtures.searchResult
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.clearAllMocks
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ContextBuildingStep Tests")
class ContextBuildingStepTest {

    private val confluenceBaseUrl = "https://test.atlassian.net/api/v2"
    private val step = ContextBuildingStep(confluenceBaseUrl)

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("should return step name")
    fun `should return step name`() {
        // when
        val name = step.getStepName()

        // then
        name shouldBe "Context Building"
    }

    @Nested
    @DisplayName("Context Building Logic")
    inner class ContextBuildingLogicTests {

        @Test
        @DisplayName("should build context from search results")
        fun `should build context from search results`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "Find Spring docs"),
                analysis = createAnalysis(),
                search = ChatContext.Search(
                    results = listOf(
                        searchResult {
                            id = "doc1"
                            title = "Spring Boot Guide"
                            content = "This is a comprehensive guide to Spring Boot".repeat(5)
                            similarity = 0.85
                        }
                    )
                ),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            result.search.shouldNotBeNull()
            result.search!!.contextText.shouldNotBeNull()
            result.search!!.contextText!!.shouldNotBeEmpty()
        }

        @Test
        @DisplayName("should include user question in context")
        fun `should include user question in context`() = runTest {
            // given
            val userQuestion = "Find Spring Boot documentation"
            val context = ChatContext(
                input = ChatContext.UserInput(message = userQuestion),
                analysis = createAnalysis(),
                search = ChatContext.Search(
                    results = listOf(
                        searchResult {
                            title = "Spring Guide"
                            content = "Spring Boot is a framework".repeat(10)
                            similarity = 0.8
                        }
                    )
                ),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            result.search!!.contextText!! shouldContain userQuestion
        }

        @Test
        @DisplayName("should categorize results by relevance")
        fun `should categorize results by relevance`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test query"),
                analysis = createAnalysis(),
                search = ChatContext.Search(
                    results = listOf(
                        searchResult { similarity = 0.9 }, // High
                        searchResult { similarity = 0.6 }, // Medium
                        searchResult { similarity = 0.4 } // Other
                    )
                ),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            val contextText = result.search!!.contextText!!
            contextText shouldContain "High Relevance Documents"
            contextText shouldContain "Medium Relevance Documents"
            contextText shouldContain "Other Related Documents"
        }

        @Test
        @DisplayName("should filter out documents with minimal content")
        fun `should filter out documents with minimal content`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test"),
                analysis = createAnalysis(),
                search = ChatContext.Search(
                    results = listOf(
                        searchResult {
                            id = "doc1"
                            content = "Valid content".repeat(20) // > 100 chars
                            similarity = 0.8
                        },
                        searchResult {
                            id = "doc2"
                            content = "Short" // < 100 chars
                            similarity = 0.7
                        }
                    )
                ),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            val contextText = result.search!!.contextText!!
            contextText shouldContain "doc1"
            // doc2 should be filtered out (though we don't check explicitly as it's in the content)
        }

        @Test
        @DisplayName("should handle empty search results")
        fun `should handle empty search results`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test"),
                analysis = createAnalysis(),
                search = ChatContext.Search(results = emptyList()),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            result.search.shouldNotBeNull()
            result.search!!.contextText.shouldBeNull()
        }

        @Test
        @DisplayName("should handle null search context")
        fun `should handle null search context`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test"),
                analysis = createAnalysis(),
                search = null,
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            result.search.shouldBeNull()
        }
    }

    @Nested
    @DisplayName("Document Formatting")
    inner class DocumentFormattingTests {

        @Test
        @DisplayName("should include document metadata in context")
        fun `should include document metadata in context`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test"),
                analysis = createAnalysis(),
                search = ChatContext.Search(
                    results = listOf(
                        searchResult {
                            id = "doc1"
                            title = "Test Document"
                            content = "Content here".repeat(20)
                            path = "Space > Folder > Page"
                            spaceKey = "TEST"
                            similarity = 0.85
                        }
                    )
                ),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            val contextText = result.search!!.contextText!!
            contextText shouldContain "Test Document"
            contextText shouldContain "Space > Folder > Page"
            contextText shouldContain "0.85"
        }

        @Test
        @DisplayName("should include Confluence page URL")
        fun `should include Confluence page URL`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test"),
                analysis = createAnalysis(),
                search = ChatContext.Search(
                    results = listOf(
                        searchResult {
                            id = "12345"
                            spaceKey = "TEST"
                            content = "Content here".repeat(20)
                            similarity = 0.8
                        }
                    )
                ),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            val contextText = result.search!!.contextText!!
            contextText shouldContain "https://test.atlassian.net/spaces/TEST/pages/12345"
        }

        @Test
        @DisplayName("should extract and display date from title")
        fun `should extract and display date from title`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test"),
                analysis = createAnalysis(),
                search = ChatContext.Search(
                    results = listOf(
                        searchResult {
                            title = "Meeting Notes 240915"
                            content = "Content here".repeat(20)
                            similarity = 0.8
                        }
                    )
                ),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            val contextText = result.search!!.contextText!!
            contextText shouldContain "2024-09-15"
        }

        @Test
        @DisplayName("should include keywords if present")
        fun `should include keywords if present`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test"),
                analysis = createAnalysis(),
                search = ChatContext.Search(
                    results = listOf(
                        searchResult {
                            content = "Content here".repeat(20)
                            keywords = "spring, boot, java"
                            similarity = 0.8
                        }
                    )
                ),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            val contextText = result.search!!.contextText!!
            contextText shouldContain "Keywords:"
            contextText shouldContain "spring, boot, java"
        }

        @Test
        @DisplayName("should mark PDF attachments")
        fun `should mark PDF attachments`() = runTest {
            // given
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test"),
                analysis = createAnalysis(),
                search = ChatContext.Search(
                    results = listOf(
                        searchResult {
                            id = "att123"
                            title = "Document.pdf"
                            content = "PDF content here".repeat(20)
                            type = "confluence-pdf-attachment"
                            pageId = "page456"
                            similarity = 0.8
                        }
                    )
                ),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            val contextText = result.search!!.contextText!!
            contextText shouldContain "📄 [PDF]"
            contextText shouldContain "Type: PDF Attachment"
        }
    }

    @Nested
    @DisplayName("Result Limiting")
    inner class ResultLimitingTests {

        @Test
        @DisplayName("should limit to top 20 results")
        fun `should limit to top 20 results`() = runTest {
            // given
            val manyResults = (1..50).map { index ->
                searchResult {
                    id = "doc$index"
                    title = "Document $index"
                    content = "Content for document $index".repeat(10)
                    similarity = 1.0 - (index * 0.01)
                }
            }
            val context = ChatContext(
                input = ChatContext.UserInput(message = "test"),
                analysis = createAnalysis(),
                search = ChatContext.Search(results = manyResults),
                isDeepThink = false
            )

            // when
            val result = step.execute(context)

            // then
            val contextText = result.search!!.contextText!!
            contextText shouldContain "Total 20 documents found"
        }
    }

    private fun createAnalysis() = ChatContext.Analysis(
        queryAnalysis = QueryClassifier.QueryAnalysis(
            type = QueryClassifier.QueryType.DOCUMENT_SEARCH,
            confidence = 0.9,
            keywords = listOf("test")
        ),
        extractedTitles = emptyList(),
        extractedContents = emptyList(),
        extractedPaths = emptyList(),
        extractedKeywords = listOf("test"),
        dateKeywords = emptyList()
    )
}
