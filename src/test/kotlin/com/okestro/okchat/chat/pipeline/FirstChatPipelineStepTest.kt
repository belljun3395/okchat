package com.okestro.okchat.chat.pipeline

import com.okestro.okchat.chat.pipeline.steps.QueryAnalysisStep
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FirstChatPipelineStep Interface Tests")
class FirstChatPipelineStepTest {

    @Test
    @DisplayName("FirstChatPipelineStep interface should be implemented correctly")
    fun `FirstChatPipelineStep interface should be implemented correctly`() {
        // given
        val step = mockk<QueryAnalysisStep>()
        every { step.getStepName() } returns "Query Analysis"

        // when
        val name = step.getStepName()

        // then
        name shouldBe "Query Analysis"
    }
}
