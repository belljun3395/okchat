package com.okestro.okchat.chat.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.okestro.okchat.chat.pipeline.ChatContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Service for managing conversation sessions with Redis-based storage
 * Implements sliding window strategy to maintain recent conversation history
 */
@Service
class SessionManagementService(
    @Qualifier("reactiveStringRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val SESSION_KEY_PREFIX = "chat:session:"
        private const val MAX_MESSAGES = 10 // Keep last 10 messages in sliding window
        private val SESSION_TTL = Duration.ofHours(24) // Session expires after 24 hours
        private const val TOKEN_LIMIT = 3000 // Approximate token limit for conversation history
    }

    /**
     * Generate a new session ID
     */
    fun generateSessionId(): String = UUID.randomUUID().toString()

    /**
     * Load conversation history for a given session
     * Returns empty Mono if session doesn't exist or cannot be loaded
     */
    fun loadConversationHistory(sessionId: String): Mono<ChatContext.ConversationHistory> {
        val key = SESSION_KEY_PREFIX + sessionId
        log.debug { "Loading conversation history for session: $sessionId" }

        return redisTemplate.opsForValue().get(key)
            .flatMap { json ->
                try {
                    val messages = objectMapper.readValue<List<StoredMessage>>(json)
                    val history = ChatContext.ConversationHistory(
                        sessionId = sessionId,
                        messages = messages.map { it.toMessage() },
                        summary = null // Future: implement summarization
                    )
                    Mono.just(history)
                } catch (e: Exception) {
                    log.error(e) { "Failed to deserialize conversation history for session: $sessionId" }
                    Mono.empty()
                }
            }
            .doOnNext { log.info { "Loaded ${it.messages.size} messages for session: $sessionId" } }
            .onErrorResume {
                log.error(it) { "Error loading conversation history for session: $sessionId" }
                Mono.empty()
            }
    }

    /**
     * Save conversation history with sliding window strategy
     */
    fun saveConversationHistory(
        sessionId: String,
        userMessage: String,
        assistantResponse: String,
        existingHistory: ChatContext.ConversationHistory?
    ): Mono<Unit> {
        val key = SESSION_KEY_PREFIX + sessionId
        log.debug { "Saving conversation history for session: $sessionId" }

        val now = Instant.now()
        val existingMessages = existingHistory?.messages?.map { StoredMessage.fromMessage(it) } ?: emptyList()

        // Add new messages
        val newMessages = existingMessages + listOf(
            StoredMessage("user", userMessage, now),
            StoredMessage("assistant", assistantResponse, now)
        )

        // Apply sliding window - keep only recent messages
        val windowedMessages = applySlidingWindow(newMessages)

        val json = try {
            objectMapper.writeValueAsString(windowedMessages)
        } catch (e: Exception) {
            log.error(e) { "Failed to serialize conversation history for session: $sessionId" }
            return Mono.error(e)
        }

        return redisTemplate.opsForValue()
            .set(key, json, SESSION_TTL)
            .doOnSuccess {
                log.info { "Saved ${windowedMessages.size} messages for session: $sessionId (TTL: ${SESSION_TTL.toHours()} hours)" }
            }
            .doOnError { error ->
                log.error(error) { "Failed to save conversation history for session: $sessionId" }
            }
            .then(Mono.just(Unit))
    }

    /**
     * Apply sliding window strategy to limit number of messages
     * Keeps most recent messages within token limit
     */
    private fun applySlidingWindow(messages: List<StoredMessage>): List<StoredMessage> {
        if (messages.size <= MAX_MESSAGES) {
            return messages
        }

        // Take last MAX_MESSAGES
        val windowed = messages.takeLast(MAX_MESSAGES)

        // Check approximate token count (rough estimate: 1 token ≈ 4 characters)
        val estimatedTokens = windowed.sumOf { (it.content.length / 4) }

        return if (estimatedTokens > TOKEN_LIMIT) {
            // If still over limit, further reduce
            val targetMessages = (MAX_MESSAGES * 0.7).toInt()
            windowed.takeLast(targetMessages)
        } else {
            windowed
        }
    }

    /**
     * Clear conversation history for a session
     */
    fun clearSession(sessionId: String): Mono<Boolean> {
        val key = SESSION_KEY_PREFIX + sessionId
        log.info { "Clearing conversation history for session: $sessionId" }

        return redisTemplate.delete(key)
            .map { it > 0 }
            .doOnSuccess { success ->
                if (success) {
                    log.info { "Successfully cleared session: $sessionId" }
                } else {
                    log.warn { "Session not found or already cleared: $sessionId" }
                }
            }
    }

    /**
     * Internal data class for Redis storage
     */
    private data class StoredMessage(
        val role: String,
        val content: String,
        val timestamp: Instant
    ) {
        fun toMessage(): ChatContext.Message {
            return ChatContext.Message(
                role = role,
                content = content,
                timestamp = timestamp
            )
        }

        companion object {
            fun fromMessage(message: ChatContext.Message): StoredMessage {
                return StoredMessage(
                    role = message.role,
                    content = message.content,
                    timestamp = message.timestamp
                )
            }
        }
    }
}
