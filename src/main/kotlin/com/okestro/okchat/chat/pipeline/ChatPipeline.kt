package com.okestro.okchat.chat.pipeline

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Orchestrates the execution of chat processing pipeline.
 * Assembles a single pipeline from a mandatory first step, a series of optional middle steps, and a mandatory last step,
 * then executes them sequentially.
 * This structure ensures key operations are always performed while allowing for flexible extension.
 */
@Component
class ChatPipeline(
    firstStep: FirstChatPipelineStep,
    lastStep: LastChatPipelineStep,
    optionalSteps: List<OptionalChatPipelineStep> = emptyList()
) {
    private val pipelineSteps: List<ChatPipelineStep> = buildList {
        add(firstStep)
        addAll(optionalSteps)
        add(lastStep)
    }

    init {
        log.info { "[Pipeline] Initialized with ${pipelineSteps.size} total steps." }
        pipelineSteps.forEachIndexed { index, step ->
            log.info { "  - Step ${index + 1}: ${step.getStepName()}" }
        }
    }

    /**
     * Execute the assembled pipeline with the given initial context.
     */
    suspend fun execute(initialContext: ChatContext): ChatContext {
        log.info { "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" }
        log.info { "[Pipeline] Starting execution with ${pipelineSteps.size} steps" }

        var context = initialContext
        for (step in pipelineSteps) {
            log.debug { "[Pipeline] Executing: ${step.getStepName()}" }
            context = step.execute(context)
        }

        log.info { "[Pipeline] All steps completed" }
        return context
    }
}
