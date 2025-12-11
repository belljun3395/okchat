package com.okestro.okchat.chat.pipeline

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Orchestrates the execution of chat processing pipeline.
 * Assembles a single pipeline from a mandatory first step, a series of optional middle steps, and a mandatory last step,
 * then executes them sequentially.
 * This structure ensures key operations are always performed while allowing for flexible extension.
 */
@Component
class DocumentChatPipeline(
    firstStep: FirstChatPipelineStep,
    lastStep: LastChatPipelineStep,
    documentChatPipelineSteps: List<DocumentChatPipelineStep> = emptyList(),
    private val meterRegistry: MeterRegistry
) {
    private val pipelineSteps: List<ChatPipelineStep> = buildList {
        add(firstStep)
        addAll(documentChatPipelineSteps)
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
     * Steps are conditionally executed based on shouldExecute() logic.
     */
    suspend fun execute(initialContext: ChatContext): CompleteChatContext {
        log.info { "[Pipeline] Starting execution with ${pipelineSteps.size} steps" }

        var context = initialContext
        var executedSteps = 0
        var skippedSteps = 0

        for (step in pipelineSteps) {
            if (step.shouldExecute(context)) {
                log.debug { "[Pipeline] Executing: ${step.getStepName()}" }
                val sample = Timer.start(meterRegistry)
                try {
                    context = step.execute(context)
                    sample.stop(meterRegistry.timer("chat.pipeline.step.latency", "step", step.getStepName(), "status", "success"))
                } catch (e: Exception) {
                    sample.stop(meterRegistry.timer("chat.pipeline.step.latency", "step", step.getStepName(), "status", "failure"))
                    throw e
                }
                context.executedStep.add(step.getStepName())
                executedSteps++
            } else {
                log.info { "[Pipeline] Skipping: ${step.getStepName()} (shouldExecute returned false)" }
                skippedSteps++
            }
        }

        val completeChatContext = context as? CompleteChatContext
            ?: throw IllegalStateException("Final context is not CompleteChatContext. Last step must produce CompleteChatContext.")

        log.info { "[Pipeline] Completed: $executedSteps executed, $skippedSteps skipped" }
        return completeChatContext
    }
}
