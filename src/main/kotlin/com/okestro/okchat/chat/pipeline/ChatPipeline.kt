package com.okestro.okchat.chat.pipeline

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Orchestrates the execution of chat processing pipeline
 * Executes steps in order and passes context between them
 */
@Component
class ChatPipeline(
    @Qualifier("chatPipelineSteps") private val steps: List<ChatPipelineStep>
) {

    /**
     * Execute the pipeline with the given initial context
     */
    suspend fun execute(initialContext: ChatContext): ChatContext {
        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        log.info { "[Pipeline] Starting with ${steps.size} steps" }

        var context = initialContext
        steps.forEach { step ->
            log.debug { "[Pipeline] Executing: ${step.getStepName()}" }
            context = step.execute(context)
        }

        log.info { "[Pipeline] All steps completed" }
        return context
    }
}
