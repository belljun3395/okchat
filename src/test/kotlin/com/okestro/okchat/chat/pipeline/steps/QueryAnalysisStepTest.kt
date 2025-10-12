package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.service.extraction.ContentExtractionService
import com.okestro.okchat.ai.service.extraction.KeywordExtractionService
import com.okestro.okchat.ai.service.extraction.LocationExtractionService
import com.okestro.okchat.ai.service.extraction.TitleExtractionService
import com.okestro.okchat.ai.support.DateExtractor
import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.chat.pipeline.ChatContext
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("QueryAnalysisStep Tests")
class QueryAnalysisStepTest {

    private val queryClassifier: QueryClassifier = mockk()
    private val titleExtractionService: TitleExtractionService = mockk()
    private val contentExtractionService: ContentExtractionService = mockk()
    private val locationExtractionService: LocationExtractionService = mockk()
    private val keywordExtractionService: KeywordExtractionService = mockk()

    private val step = QueryAnalysisStep(
        queryClassifier,
        keywordExtractionService,
        contentExtractionService,
        locationExtractionService,
        titleExtractionService
    )

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
        name shouldBe "Query Analysis"
    }

    @Test
    @DisplayName("should analyze query and extract information")
    fun `should analyze query and extract information`() = runTest {
        // given
        val context = ChatContext(
            input = ChatContext.UserInput(message = "프로젝트 문서를 찾아줘"),
            isDeepThink = false
        )

        val queryAnalysis = QueryClassifier.QueryAnalysis(
            type = QueryClassifier.QueryType.DOCUMENT_SEARCH,
            confidence = 0.9,
            keywords = listOf("프로젝트", "문서")
        )

        coEvery { queryClassifier.classify("프로젝트 문서를 찾아줘") } returns queryAnalysis
        coEvery { titleExtractionService.execute("프로젝트 문서를 찾아줘") } returns listOf("프로젝트 문서")
        coEvery { contentExtractionService.execute("프로젝트 문서를 찾아줘") } returns listOf("프로젝트")
        coEvery { locationExtractionService.execute("프로젝트 문서를 찾아줘") } returns listOf("문서")
        coEvery { keywordExtractionService.execute("프로젝트 문서를 찾아줘") } returns listOf("프로젝트", "문서")
        mockkObject(DateExtractor)
        every { DateExtractor.extractDateKeywords("프로젝트 문서를 찾아줘", any()) } returns emptyList()

        // when
        val result = step.execute(context)

        // then
        result.analysis.shouldNotBeNull()
        val analysis = result.analysis!!
        analysis.queryAnalysis.type shouldBe QueryClassifier.QueryType.DOCUMENT_SEARCH
        analysis.extractedTitles.size shouldBe 1
        analysis.extractedTitles.first() shouldBe "프로젝트 문서"
        analysis.extractedKeywords.size shouldBe 2

        coVerify(exactly = 1) { queryClassifier.classify("프로젝트 문서를 찾아줘") }
        coVerify(exactly = 1) { titleExtractionService.execute("프로젝트 문서를 찾아줘") }
    }

    @Test
    @DisplayName("should extract date keywords")
    fun `should extract date keywords`() = runTest {
        // given
        val context = ChatContext(
            input = ChatContext.UserInput(message = "2025년 1월 회의록"),
            isDeepThink = false
        )

        val queryAnalysis = QueryClassifier.QueryAnalysis(
            type = QueryClassifier.QueryType.MEETING_RECORDS,
            confidence = 0.95,
            keywords = listOf("회의록")
        )

        coEvery { queryClassifier.classify(any()) } returns queryAnalysis
        coEvery { titleExtractionService.execute(any()) } returns emptyList()
        coEvery { contentExtractionService.execute(any()) } returns emptyList()
        coEvery { locationExtractionService.execute(any()) } returns emptyList()
        coEvery { keywordExtractionService.execute(any()) } returns listOf("회의록")
        mockkObject(DateExtractor)
        every { DateExtractor.extractDateKeywords(any(), any()) } returns listOf("2025", "1월")

        // when
        val result = step.execute(context)

        // then
        result.analysis.shouldNotBeNull()
        val analysis = result.analysis!!
        analysis.dateKeywords.size shouldBe 2
        analysis.dateKeywords.contains("2025") shouldBe true
        analysis.dateKeywords.contains("1월") shouldBe true
    }

    @Test
    @DisplayName("should always execute")
    fun `should always execute`() {
        // given
        val context = ChatContext(
            input = ChatContext.UserInput(message = "test"),
            isDeepThink = false
        )

        // when
        val result = step.shouldExecute(context)

        // then
        result shouldBe true
    }
}
