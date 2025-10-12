package com.okestro.okchat.chat.pipeline

import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.fixture.TestFixtures
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DocumentChatPipelineStep Default Behavior Tests")
class OptionalChatPipelineStepImplTest {

    @Test
    @DisplayName("DocumentChatPipelineStep shouldExecute default is true")
    fun `DocumentChatPipelineStep shouldExecute default is true`() {
        // given
        val step = TestStep()
        val context = ChatContext(
            input = ChatContext.UserInput(message = "test"),
            isDeepThink = false
        )

        // when
        val result = step.shouldExecute(context)

        // then
        result shouldBe true
    }

    @Test
    @DisplayName("DocumentChatPipelineStep can override shouldExecute")
    fun `DocumentChatPipelineStep can override shouldExecute`() = runTest {
        // given
        val conditionalStep = ConditionalTestStep()
        val contextWithResults = ChatContext(
            input = ChatContext.UserInput(message = "test"),
            search = ChatContext.Search(results = TestFixtures.searchResults(1)),
            isDeepThink = false
        )
        val contextWithoutResults = ChatContext(
            input = ChatContext.UserInput(message = "test"),
            isDeepThink = false
        )

        // when & then
        conditionalStep.shouldExecute(contextWithResults) shouldBe true
        conditionalStep.shouldExecute(contextWithoutResults) shouldBe false
    }

    // Simple test implementation
    private class TestStep : DocumentChatPipelineStep {
        override fun getStepName() = "Test Step"
        override suspend fun execute(context: ChatContext) = context
    }

    // Test step with custom shouldExecute
    private class ConditionalTestStep : DocumentChatPipelineStep {
        override fun getStepName() = "Conditional Step"
        
        override fun shouldExecute(context: ChatContext): Boolean {
            return context.search?.results?.isNotEmpty() == true
        }
        
        override suspend fun execute(context: ChatContext) = context
    }
}
