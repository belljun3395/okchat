package com.okestro.okchat.chat.event.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.okestro.okchat.chat.event.ChatEventBus
import com.okestro.okchat.chat.event.ChatInteractionCompletedEvent
import com.okestro.okchat.chat.event.ConversationHistorySaveEvent
import com.okestro.okchat.chat.event.FeedbackSubmittedEvent
import com.okestro.okchat.chat.model.ChatInteraction
import com.okestro.okchat.chat.repository.ChatInteractionRepository
import com.okestro.okchat.chat.service.SessionManagementService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger {}

/**
 * Chat event handler
 * * Subscribes to ChatEventBus and processes events asynchronously:
 * - ChatInteractionCompletedEvent: Saves interaction data for analytics
 * - ConversationHistorySaveEvent: Persists conversation history to Redis
 * - FeedbackSubmittedEvent: Updates interaction with user feedback (with retry logic)
 * * All operations run on IO dispatcher for non-blocking database access
 */
@Component
class ChatEventHandler(
    private val chatEventBus: ChatEventBus,
    private val chatInteractionRepository: ChatInteractionRepository,
    private val sessionManagementService: SessionManagementService,
    private val objectMapper: ObjectMapper
) {

    @PostConstruct
    fun initialize() {
        logger.info { "Initializing ChatEventHandler" }
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        chatEventBus.subscribe()
            .publishOn(Schedulers.boundedElastic())
            .subscribe { event ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        when (event) {
                            is ChatInteractionCompletedEvent -> handleChatInteraction(event)
                            is ConversationHistorySaveEvent -> handleConversationHistory(event)
                            is FeedbackSubmittedEvent -> handleFeedback(event)
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Error processing analytics event: ${event::class.simpleName}" }
                    }
                }
            }
        logger.info { "ChatEventHandler subscribed to event bus" }
    }

    /**
     * Handle chat interaction completed event
     * * Extracts analytics data from the processed context and saves to database
     */
    private suspend fun handleChatInteraction(event: ChatInteractionCompletedEvent) {
        try {
            val analysis = event.processedContext.analysis
                ?: throw IllegalStateException("Analysis is null in CompleteChatContext")
            val search = event.processedContext.search

            val documentsUsed = search?.results?.map {
                mapOf(
                    "id" to it.id,
                    "score" to it.score.value
                )
            }?.let { objectMapper.writeValueAsString(it) }
            val extractedKeywords = analysis.getAllKeywords().joinToString(", ")
            val pipelineSteps = event.processedContext.executedStep.joinToString(" -> ")

            val interaction = ChatInteraction(
                sessionId = event.sessionId,
                requestId = event.requestId,
                userMessage = event.userMessage,
                aiResponse = event.aiResponse,
                queryType = analysis.queryAnalysis.type.name,
                extractedKeywords = extractedKeywords,
                responseTimeMs = event.responseTimeMs,
                searchResultsCount = search?.results?.size ?: 0,
                documentsUsed = documentsUsed,
                pipelineStepsExecuted = pipelineSteps,
                llmModelUsed = event.modelUsed,
                isDeepThink = event.isDeepThink,
                userEmail = event.userEmail,
                createdAt = event.timestamp
            )
            chatInteractionRepository.save(interaction)
            logger.debug { "[Analytics] Saved chat interaction: requestId=${event.requestId}" }
        } catch (e: Exception) {
            logger.error(e) { "[Analytics] Failed to save chat interaction: requestId=${event.requestId}" }
        }
    }

    /**
     * Handle conversation history save event
     * * Persists conversation context to Redis for maintaining chat continuity
     */
    private suspend fun handleConversationHistory(event: ConversationHistorySaveEvent) {
        try {
            sessionManagementService.saveConversationHistory(
                sessionId = event.sessionId,
                userMessage = event.userMessage,
                assistantResponse = event.assistantResponse,
                existingHistory = event.existingHistory
            )
            logger.info { "[Session] Conversation history saved for session: ${event.sessionId}" }
        } catch (e: Exception) {
            logger.error(e) { "[Session] Failed to save conversation history: sessionId=${event.sessionId}" }
        }
    }

    /**
     * Handle feedback submitted event
     * * Implements exponential backoff retry logic to handle race conditions
     * where feedback arrives before the chat interaction is saved.
     * * Retry strategy:
     * - Max retries: 10
     * - Initial delay: 200ms
     * - Max delay: 2000ms (capped)
     * - Backoff: exponential (200ms, 400ms, 800ms, 1600ms, 2000ms...)
     */
    private suspend fun handleFeedback(event: FeedbackSubmittedEvent) {
        var retryCount = 0
        val maxRetries = 10
        val initialRetryDelayMs = 200L
        val maxRetryDelayMs = 2000L

        while (retryCount < maxRetries) {
            try {
                val interaction = chatInteractionRepository.findByRequestId(event.requestId)

                if (interaction != null) {
                    val updated = interaction.copy(
                        userRating = event.rating,
                        wasHelpful = event.wasHelpful,
                        userFeedback = event.feedback,
                        feedbackAt = event.timestamp
                    )
                    chatInteractionRepository.save(updated)
                    logger.info { "[Feedback] ✅ Updated for requestId=${event.requestId} (attempt ${retryCount + 1})" }
                    return // Success
                } else {
                    // Not found yet, retry with exponential backoff
                    retryCount++
                    if (retryCount < maxRetries) {
                        // Exponential backoff: 200ms, 400ms, 800ms, 1600ms, 2000ms, 2000ms...
                        val delayMs = minOf(initialRetryDelayMs * (1 shl retryCount), maxRetryDelayMs)
                        logger.debug { "[Feedback] ⏳ Interaction not found yet, retrying ($retryCount/$maxRetries) after ${delayMs}ms: ${event.requestId}" }
                        delay(delayMs)
                    } else {
                        // Last retry failed - save to pending table
                        logger.warn { "[Feedback] ⚠️ Interaction not found after $maxRetries retries, saving to pending: ${event.requestId}" }
                        savePendingFeedback(event)
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "[Feedback] ❌ Failed to update feedback: requestId=${event.requestId}" }
                return
            }
        }
    }

    /**
     * Save feedback to pending table for later processing
     * This can be processed by a scheduled job that checks for pending feedback
     */
    private suspend fun savePendingFeedback(event: FeedbackSubmittedEvent) {
        try {
            // For now, just log it. You can implement a pending_feedback table later
            logger.info { "[Feedback] 📝 Pending feedback: requestId=${event.requestId}, rating=${event.rating}, helpful=${event.wasHelpful}" }

            // TODO: Implement pending_feedback table
            // pendingFeedbackRepository.save(PendingFeedback(
            //     requestId = event.requestId,
            //     rating = event.rating,
            //     wasHelpful = event.wasHelpful,
            //     feedback = event.feedback,
            //     createdAt = event.timestamp,
            //     retryCount = 0
            // ))
        } catch (e: Exception) {
            logger.error(e) { "[Feedback] Failed to save pending feedback: ${event.requestId}" }
        }
    }
}
