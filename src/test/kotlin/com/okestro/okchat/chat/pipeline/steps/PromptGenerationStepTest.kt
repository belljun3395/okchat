package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.service.classifier.QueryClassifier
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.CompleteChatContext
import com.okestro.okchat.prompt.support.DynamicPromptBuilder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PromptGenerationStep Unit Tests")
class PromptGenerationStepTest {

    private lateinit var dynamicPromptBuilder: DynamicPromptBuilder
    private lateinit var step: PromptGenerationStep

    @BeforeEach
    fun setUp() {
        dynamicPromptBuilder = mockk()
        step = PromptGenerationStep(dynamicPromptBuilder)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("getStepName should return step name")
    fun `getStepName should return step name`() {
        // when
        val name = step.getStepName()

        // then
        name shouldBe "Prompt Generation"
    }

    @Test
    @DisplayName("execute should generate prompt with RAG context")
    fun `execute should generate prompt with RAG context`() = runTest {
        // given
        val context = createContextWithRAG()
        val promptTemplate = "Context: {context}\nQuestion: {question}"
        coEvery { dynamicPromptBuilder.buildPrompt(QueryClassifier.QueryType.DOCUMENT_SEARCH) } returns promptTemplate

        // when
        val result = step.execute(context)

        // then
        result.shouldBeInstanceOf<CompleteChatContext>()
        result.prompt.text shouldContain "test question"
        result.prompt.text shouldContain "RAG context text"
    }

    @Test
    @DisplayName("execute should handle missing context text")
    fun `execute should handle missing context text`() = runTest {
        // given
        val context = createContextWithoutRAG()
        val promptTemplate = "Context: {context}\nQuestion: {question}"
        coEvery { dynamicPromptBuilder.buildPrompt(QueryClassifier.QueryType.GENERAL) } returns promptTemplate

        // when
        val result = step.execute(context)

        // then
        result.shouldBeInstanceOf<CompleteChatContext>()
        result.prompt.text shouldContain "No search results found"
    }

    @Test
    @DisplayName("execute should use appropriate prompt template for query type")
    fun `execute should use appropriate prompt template for query type`() = runTest {
        // given
        val context = createContextWithQueryType(QueryClassifier.QueryType.MEETING_RECORDS)
        val meetingPromptTemplate = "Meeting Context: {context}\nQuery: {question}"
        coEvery { dynamicPromptBuilder.buildPrompt(QueryClassifier.QueryType.MEETING_RECORDS) } returns meetingPromptTemplate

        // when
        val result = step.execute(context)

        // then
        result.shouldBeInstanceOf<CompleteChatContext>()
        result.prompt.text shouldContain "Meeting Context"
    }

    private fun createContextWithRAG(): ChatContext {
        return ChatContext(
            input = ChatContext.UserInput(message = "test question"),
            analysis = ChatContext.Analysis(
                queryAnalysis = QueryClassifier.QueryAnalysis(
                    type = QueryClassifier.QueryType.DOCUMENT_SEARCH,
                    confidence = 0.9,
                    keywords = emptyList()
                ),
                extractedTitles = emptyList(),
                extractedContents = emptyList(),
                extractedPaths = emptyList(),
                extractedKeywords = emptyList(),
                dateKeywords = emptyList()
            ),
            search = ChatContext.Search(
                results = emptyList(),
                contextText = "RAG context text"
            ),
            isDeepThink = false
        )
    }

    private fun createContextWithoutRAG(): ChatContext {
        return ChatContext(
            input = ChatContext.UserInput(message = "test question"),
            analysis = ChatContext.Analysis(
                queryAnalysis = QueryClassifier.QueryAnalysis(
                    type = QueryClassifier.QueryType.GENERAL,
                    confidence = 0.5,
                    keywords = emptyList()
                ),
                extractedTitles = emptyList(),
                extractedContents = emptyList(),
                extractedPaths = emptyList(),
                extractedKeywords = emptyList(),
                dateKeywords = emptyList()
            ),
            isDeepThink = false
        )
    }

    private fun createContextWithQueryType(queryType: QueryClassifier.QueryType): ChatContext {
        return ChatContext(
            input = ChatContext.UserInput(message = "meeting question"),
            analysis = ChatContext.Analysis(
                queryAnalysis = QueryClassifier.QueryAnalysis(
                    type = queryType,
                    confidence = 0.9,
                    keywords = emptyList()
                ),
                extractedTitles = emptyList(),
                extractedContents = emptyList(),
                extractedPaths = emptyList(),
                extractedKeywords = emptyList(),
                dateKeywords = emptyList()
            ),
            search = ChatContext.Search(
                results = emptyList(),
                contextText = "Meeting records context"
            ),
            isDeepThink = false
        )
    }
}
