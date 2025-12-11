package com.okestro.okchat.ai.support

import com.okestro.okchat.ai.service.classifier.QueryClassifier
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.model.ChatModel

@DisplayName("QueryClassifier Fallback Classification Tests")
class QueryClassifierFallbackTest {

    private lateinit var chatModel: ChatModel
    private lateinit var classifier: QueryClassifier

    @BeforeEach
    fun setUp() {
        chatModel = mockk()
        classifier = QueryClassifier(chatModel)

        // Force fallback by making AI call fail
        every { chatModel.call(any<org.springframework.ai.chat.prompt.Prompt>()) } throws RuntimeException("API error")
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("fallback should classify meeting queries")
    fun `fallback should classify meeting queries`() = runTest {
        // when
        val result1 = classifier.classify("9월 회의록 찾아줘")
        val result2 = classifier.classify("meeting minutes for Q3")
        val result3 = classifier.classify("미팅 내용 정리")

        // then
        result1.type shouldBe QueryClassifier.QueryType.MEETING_RECORDS
        result2.type shouldBe QueryClassifier.QueryType.MEETING_RECORDS
        result3.type shouldBe QueryClassifier.QueryType.MEETING_RECORDS
    }

    @Test
    @DisplayName("fallback should classify project status queries")
    fun `fallback should classify project status queries`() = runTest {
        // when
        val result1 = classifier.classify("프로젝트 현황 알려줘")
        val result2 = classifier.classify("What's the project status?")
        val result3 = classifier.classify("작업 진행 상태는?")

        // then
        result1.type shouldBe QueryClassifier.QueryType.PROJECT_STATUS
        result2.type shouldBe QueryClassifier.QueryType.PROJECT_STATUS
        result3.type shouldBe QueryClassifier.QueryType.PROJECT_STATUS
    }

    @Test
    @DisplayName("fallback should classify how-to queries")
    fun `fallback should classify how-to queries`() = runTest {
        // when
        val result1 = classifier.classify("어떻게 설치하나요?")
        val result2 = classifier.classify("How to deploy?")
        val result3 = classifier.classify("배포 방법 알려줘")

        // then
        result1.type shouldBe QueryClassifier.QueryType.HOW_TO
        result2.type shouldBe QueryClassifier.QueryType.HOW_TO
        result3.type shouldBe QueryClassifier.QueryType.HOW_TO
    }

    @Test
    @DisplayName("fallback should classify information queries")
    fun `fallback should classify information queries`() = runTest {
        // when
        val result1 = classifier.classify("누가 담당자인가요?")
        val result2 = classifier.classify("언제 완료되나요?")
        val result3 = classifier.classify("무엇을 해야 하나요?")

        // then
        result1.type shouldBe QueryClassifier.QueryType.INFORMATION
        result2.type shouldBe QueryClassifier.QueryType.INFORMATION
        result3.type shouldBe QueryClassifier.QueryType.INFORMATION
    }

    @Test
    @DisplayName("fallback should classify document search queries")
    fun `fallback should classify document search queries`() = runTest {
        // when
        val result1 = classifier.classify("API 문서 찾아줘")
        val result2 = classifier.classify("Search for deployment guide")
        val result3 = classifier.classify("자료 검색해줘")

        // then
        result1.type shouldBe QueryClassifier.QueryType.DOCUMENT_SEARCH
        result2.type shouldBe QueryClassifier.QueryType.DOCUMENT_SEARCH
        result3.type shouldBe QueryClassifier.QueryType.DOCUMENT_SEARCH
    }

    @Test
    @DisplayName("fallback should default to GENERAL for unclassified queries")
    fun `fallback should default to GENERAL for unclassified queries`() = runTest {
        // when
        val result1 = classifier.classify("Hello")
        val result2 = classifier.classify("Thanks")
        val result3 = classifier.classify("Random text")

        // then
        result1.type shouldBe QueryClassifier.QueryType.GENERAL
        result2.type shouldBe QueryClassifier.QueryType.GENERAL
        result3.type shouldBe QueryClassifier.QueryType.GENERAL
    }

    @Test
    @DisplayName("fallback should set confidence to reasonable values")
    fun `fallback should set confidence to reasonable values`() = runTest {
        // when
        val meetingResult = classifier.classify("회의록")
        val generalResult = classifier.classify("Hello")

        // then
        meetingResult.confidence shouldBe 0.8
        generalResult.confidence shouldBe 0.5
    }
}
