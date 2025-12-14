package com.okestro.okchat.chat.pipeline

import com.okestro.okchat.ai.service.classifier.QueryClassifier
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DocumentChatPipeline Tests")
class DocumentChatPipelineTest {
    private val meterRegistry = SimpleMeterRegistry()

    @Nested
    @DisplayName("Pipeline Initialization")
    inner class PipelineInitializationTests {

        @Test
        @DisplayName("should initialize with first, middle, and last steps")
        fun `should initialize with first, middle, and last steps`() {
            // given
            val firstStep = createMockFirstStep("First")
            val middleStep1 = createMockDocumentStep("Middle1")
            val middleStep2 = createMockDocumentStep("Middle2")
            val lastStep = createMockLastStep("Last")

            // when
            val pipeline = DocumentChatPipeline(
                firstStep = firstStep,
                lastStep = lastStep,
                documentChatPipelineSteps = listOf(middleStep1, middleStep2),
                meterRegistry = meterRegistry
            )

            // then
            // Pipeline should be initialized without errors
        }

        @Test
        @DisplayName("should initialize with only first and last steps")
        fun `should initialize with only first and last steps`() {
            // given
            val firstStep = createMockFirstStep("First")
            val lastStep = createMockLastStep("Last")

            // when
            val pipeline = DocumentChatPipeline(
                firstStep = firstStep,
                lastStep = lastStep,
                documentChatPipelineSteps = emptyList(),
                meterRegistry = meterRegistry
            )

            // then
            // Pipeline should be initialized without errors
        }
    }

    @Nested
    @DisplayName("Pipeline Execution")
    inner class PipelineExecutionTests {

        @Test
        @DisplayName("should execute all steps in order")
        fun `should execute all steps in order`() = runTest {
            // given
            val firstStep = createMockFirstStep("First")
            val middleStep = createMockDocumentStep("Middle")
            val lastStep = createMockLastStep("Last")
            val pipeline = DocumentChatPipeline(firstStep, lastStep, listOf(middleStep), meterRegistry)

            val initialContext = createInitialContext()

            // when
            val result = pipeline.execute(initialContext)

            // then
            coVerify(exactly = 1) { firstStep.execute(any()) }
            coVerify(exactly = 1) { middleStep.execute(any()) }
            coVerify(exactly = 1) { lastStep.execute(any()) }
        }

        @Test
        @DisplayName("should track executed steps")
        fun `should track executed steps`() = runTest {
            // given
            val firstStep = createMockFirstStep("First Step")
            val middleStep = createMockDocumentStep("Middle Step")
            val lastStep = createMockLastStep("Last Step")
            val pipeline = DocumentChatPipeline(firstStep, lastStep, listOf(middleStep), meterRegistry)

            val initialContext = createInitialContext()

            // when
            val result = pipeline.execute(initialContext)

            // then
            result.executedStep shouldHaveSize 3
            result.executedStep shouldContain "First Step"
            result.executedStep shouldContain "Middle Step"
            result.executedStep shouldContain "Last Step"
        }

        @Test
        @DisplayName("should skip steps when shouldExecute returns false")
        fun `should skip steps when shouldExecute returns false`() = runTest {
            // given
            val firstStep = createMockFirstStep("First")
            val skippableStep = createMockDocumentStep("Skippable", shouldExecute = false)
            val executedStep = createMockDocumentStep("Executed", shouldExecute = true)
            val lastStep = createMockLastStep("Last")
            val pipeline = DocumentChatPipeline(
                firstStep,
                lastStep,
                listOf(skippableStep, executedStep),
                meterRegistry
            )

            val initialContext = createInitialContext()

            // when
            val result = pipeline.execute(initialContext)

            // then
            coVerify(exactly = 0) { skippableStep.execute(any()) }
            coVerify(exactly = 1) { executedStep.execute(any()) }
            result.executedStep shouldContain "Executed"
            result.executedStep.contains("Skippable") shouldBe false
        }

        @Test
        @DisplayName("should return CompleteChatContext")
        fun `should return CompleteChatContext`() = runTest {
            // given
            val firstStep = createMockFirstStep("First")
            val lastStep = createMockLastStep("Last")
            val pipeline = DocumentChatPipeline(firstStep, lastStep, emptyList(), meterRegistry)

            val initialContext = createInitialContext()

            // when
            val result = pipeline.execute(initialContext)

            // then
            result::class.simpleName shouldBe "CompleteChatContext"
            result.prompt.text shouldBe "Generated prompt"
        }

        // Note: This test is not feasible with type-safe mocking
        // LastChatPipelineStep.execute() always returns ChatContext by design
        // and the pipeline casts it to CompleteChatContext using 'as?'
        // The type safety is enforced by Kotlin's type system
    }

    @Nested
    @DisplayName("Context Flow")
    inner class ContextFlowTests {

        @Test
        @DisplayName("should pass context between steps")
        fun `should pass context between steps`() = runTest {
            // given
            val firstStep = mockk<FirstChatPipelineStep>()
            every { firstStep.getStepName() } returns "First"
            every { firstStep.shouldExecute(any()) } returns true

            val contextAfterFirst = ChatContext(
                input = ChatContext.UserInput("test"),
                analysis = ChatContext.Analysis(
                    queryAnalysis = QueryClassifier.QueryAnalysis(
                        type = QueryClassifier.QueryType.GENERAL,
                        confidence = 0.5,
                        keywords = emptyList()
                    ),
                    extractedTitles = listOf("Title"),
                    extractedContents = emptyList(),
                    extractedPaths = emptyList(),
                    extractedKeywords = emptyList(),
                    dateKeywords = emptyList()
                ),
                isDeepThink = false
            )
            coEvery { firstStep.execute(any()) } returns contextAfterFirst

            val middleStep = mockk<DocumentChatPipelineStep>()
            every { middleStep.getStepName() } returns "Middle"
            every { middleStep.shouldExecute(any()) } returns true
            coEvery { middleStep.execute(contextAfterFirst) } returns contextAfterFirst.copyContext(
                search = ChatContext.Search(results = emptyList())
            )

            val lastStep = createMockLastStep("Last")
            val pipeline = DocumentChatPipeline(firstStep, lastStep, listOf(middleStep), meterRegistry)

            // when
            val result = pipeline.execute(createInitialContext())

            // then
            coVerify { middleStep.execute(match { it.analysis?.extractedTitles?.contains("Title") == true }) }
        }

        @Test
        @DisplayName("should preserve executedStep list across pipeline")
        fun `should preserve executedStep list across pipeline`() = runTest {
            // given
            val firstStep = createMockFirstStep("Step1")
            val step2 = createMockDocumentStep("Step2")
            val step3 = createMockDocumentStep("Step3")
            val lastStep = createMockLastStep("Step4")
            val pipeline = DocumentChatPipeline(firstStep, lastStep, listOf(step2, step3), meterRegistry)

            // when
            val result = pipeline.execute(createInitialContext())

            // then
            result.executedStep shouldHaveSize 4
            result.executedStep[0] shouldBe "Step1"
            result.executedStep[1] shouldBe "Step2"
            result.executedStep[2] shouldBe "Step3"
            result.executedStep[3] shouldBe "Step4"
        }
    }

    @Nested
    @DisplayName("Conditional Execution")
    inner class ConditionalExecutionTests {

        @Test
        @DisplayName("should execute step when shouldExecute returns true")
        fun `should execute step when shouldExecute returns true`() = runTest {
            // given
            val conditionalStep = createMockDocumentStep("Conditional", shouldExecute = true)
            val pipeline = DocumentChatPipeline(
                createMockFirstStep("First"),
                createMockLastStep("Last"),
                listOf(conditionalStep),
                meterRegistry
            )

            // when
            val result = pipeline.execute(createInitialContext())

            // then
            coVerify(exactly = 1) { conditionalStep.execute(any()) }
            result.executedStep shouldContain "Conditional"
        }

        @Test
        @DisplayName("should skip step when shouldExecute returns false")
        fun `should skip step when shouldExecute returns false`() = runTest {
            // given
            val conditionalStep = createMockDocumentStep("Conditional", shouldExecute = false)
            val pipeline = DocumentChatPipeline(
                createMockFirstStep("First"),
                createMockLastStep("Last"),
                listOf(conditionalStep),
                meterRegistry
            )

            // when
            val result = pipeline.execute(createInitialContext())

            // then
            coVerify(exactly = 0) { conditionalStep.execute(any()) }
            result.executedStep.contains("Conditional") shouldBe false
        }

        @Test
        @DisplayName("should evaluate shouldExecute for each step")
        fun `should evaluate shouldExecute for each step`() = runTest {
            // given
            val step1 = createMockDocumentStep("Step1", shouldExecute = true)
            val step2 = createMockDocumentStep("Step2", shouldExecute = false)
            val step3 = createMockDocumentStep("Step3", shouldExecute = true)
            val pipeline = DocumentChatPipeline(
                createMockFirstStep("First"),
                createMockLastStep("Last"),
                listOf(step1, step2, step3),
                meterRegistry
            )

            // when
            val result = pipeline.execute(createInitialContext())

            // then
            coVerify(exactly = 1) { step1.shouldExecute(any()) }
            coVerify(exactly = 1) { step2.shouldExecute(any()) }
            coVerify(exactly = 1) { step3.shouldExecute(any()) }
        }
    }

    // Helper functions
    private fun createInitialContext() = ChatContext(
        input = ChatContext.UserInput(message = "test query"),
        conversationHistory = null,
        isDeepThink = false
    )

    private fun createMockFirstStep(name: String): FirstChatPipelineStep {
        val step = mockk<FirstChatPipelineStep>()
        every { step.getStepName() } returns name
        every { step.shouldExecute(any()) } returns true
        coEvery { step.execute(any()) } answers {
            val ctx = firstArg<ChatContext>()
            ChatContext(
                input = ctx.input,
                conversationHistory = ctx.conversationHistory,
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
                search = ctx.search,
                isDeepThink = ctx.isDeepThink,
                executedStep = ctx.executedStep
            )
        }
        return step
    }

    private fun createMockDocumentStep(name: String, shouldExecute: Boolean = true): DocumentChatPipelineStep {
        val step = mockk<DocumentChatPipelineStep>()
        every { step.getStepName() } returns name
        every { step.shouldExecute(any()) } returns shouldExecute
        coEvery { step.execute(any()) } answers {
            val ctx = firstArg<ChatContext>()
            // Return same context - pipeline will add step name to executedStep
            ctx
        }
        return step
    }

    private fun createMockLastStep(name: String): LastChatPipelineStep {
        val step = mockk<LastChatPipelineStep>()
        every { step.getStepName() } returns name
        every { step.shouldExecute(any()) } returns true
        coEvery { step.execute(any()) } answers {
            val ctx = firstArg<ChatContext>()
            CompleteChatContext(
                input = ctx.input,
                conversationHistory = ctx.conversationHistory,
                analysis = ctx.analysis ?: ChatContext.Analysis(
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
                search = ctx.search,
                prompt = CompleteChatContext.Prompt(text = "Generated prompt"),
                isDeepThink = ctx.isDeepThink,
                executedStep = ctx.executedStep
            )
        }
        return step
    }
}
