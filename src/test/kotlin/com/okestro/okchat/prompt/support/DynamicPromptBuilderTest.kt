package com.okestro.okchat.prompt.support

import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.prompt.service.PromptService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DynamicPromptBuilder 단위 테스트")
class DynamicPromptBuilderTest {

    private lateinit var promptService: PromptService
    private lateinit var builder: DynamicPromptBuilder

    @BeforeEach
    fun setUp() {
        promptService = mockk()
        builder = DynamicPromptBuilder(promptService)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("buildPrompt - 프롬프트 빌드 성공")
    fun `should build prompt successfully`() = runTest {
        // given
        coEvery { promptService.getLatestPrompt(QueryClassifier.QueryType.BASE.name) } returns "Base prompt"
        coEvery { promptService.getLatestPrompt(QueryClassifier.QueryType.COMMON_GUIDELINES.name) } returns "Common guidelines"
        coEvery { promptService.getLatestPrompt(QueryClassifier.QueryType.DOCUMENT_SEARCH.name) } returns "Document search specific"

        // when
        val result = builder.buildPrompt(QueryClassifier.QueryType.DOCUMENT_SEARCH)

        // then
        result shouldContain "Base prompt"
        result shouldContain "Document search specific"
        result shouldContain "Common guidelines"
    }

    @Test
    @DisplayName("buildPrompt - Base 프롬프트 없으면 예외")
    fun `should throw exception when base prompt not found`() = runTest {
        // given
        coEvery { promptService.getLatestPrompt(QueryClassifier.QueryType.BASE.name) } returns null

        // when & then
        shouldThrow<IllegalStateException> {
            builder.buildPrompt(QueryClassifier.QueryType.DOCUMENT_SEARCH)
        }
    }

    @Test
    @DisplayName("buildPrompt - Common Guidelines 없으면 예외")
    fun `should throw exception when common guidelines not found`() = runTest {
        // given
        coEvery { promptService.getLatestPrompt(QueryClassifier.QueryType.BASE.name) } returns "Base"
        coEvery { promptService.getLatestPrompt(QueryClassifier.QueryType.DOCUMENT_SEARCH.name) } returns "Specific"
        coEvery { promptService.getLatestPrompt(QueryClassifier.QueryType.COMMON_GUIDELINES.name) } returns null

        // when & then
        shouldThrow<IllegalStateException> {
            builder.buildPrompt(QueryClassifier.QueryType.DOCUMENT_SEARCH)
        }
    }

    @Test
    @DisplayName("buildPrompt - 특정 타입 프롬프트 없으면 예외")
    fun `should throw exception when specific prompt not found`() = runTest {
        // given
        coEvery { promptService.getLatestPrompt(QueryClassifier.QueryType.BASE.name) } returns "Base"
        coEvery { promptService.getLatestPrompt(QueryClassifier.QueryType.DOCUMENT_SEARCH.name) } returns null

        // when & then
        shouldThrow<IllegalStateException> {
            builder.buildPrompt(QueryClassifier.QueryType.DOCUMENT_SEARCH)
        }
    }
}
