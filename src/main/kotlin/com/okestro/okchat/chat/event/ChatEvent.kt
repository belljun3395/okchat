package com.okestro.okchat.chat.event

import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.CompleteChatContext
import java.time.LocalDateTime

/**
 * Base interface for all chat-related events
 * * Events are published to ChatEventBus and processed asynchronously by ChatEventHandler
 */
sealed interface ChatEvent

/**
 * Event published when a chat interaction is completed successfully
 * * Triggers saving of interaction data for analytics purposes
 */
data class ChatInteractionCompletedEvent(
    val requestId: String,
    val sessionId: String,
    val userMessage: String,
    val aiResponse: String,
    val responseTimeMs: Long,
    val processedContext: CompleteChatContext,
    val isDeepThink: Boolean,
    val userEmail: String?,
    val modelUsed: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
) : ChatEvent

/**
 * Event published when conversation history needs to be saved to Redis
 * * Ensures conversation context is preserved for follow-up questions
 */
data class ConversationHistorySaveEvent(
    val sessionId: String,
    val userMessage: String,
    val assistantResponse: String,
    val existingHistory: ChatContext.ConversationHistory?,
    val timestamp: LocalDateTime = LocalDateTime.now()
) : ChatEvent

/**
 * Event published when user submits feedback for a chat interaction
 * * Includes exponential backoff retry logic to handle race conditions
 * where feedback arrives before interaction is saved
 */
data class FeedbackSubmittedEvent(
    val requestId: String,
    val rating: Int?,
    val wasHelpful: Boolean?,
    val feedback: String?,
    val timestamp: LocalDateTime = LocalDateTime.now()
) : ChatEvent
