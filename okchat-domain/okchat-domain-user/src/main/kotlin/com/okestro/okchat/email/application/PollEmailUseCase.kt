package com.okestro.okchat.email.application

import com.okestro.okchat.email.event.EmailEventBus
import com.okestro.okchat.email.event.EmailReceivedEvent
import com.okestro.okchat.email.provider.EmailProviderFactory
import com.okestro.okchat.knowledge.repository.KnowledgeBaseEmailRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PollEmailUseCase(
    private val knowledgeBaseEmailRepository: KnowledgeBaseEmailRepository,
    private val emailProviderFactory: EmailProviderFactory,
    private val emailEventBus: EmailEventBus
) {
    private val log = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    suspend fun execute(knowledgeBaseId: Long): PollEmailResult {
        log.info { "Polling emails for Knowledge Base ID: $knowledgeBaseId" }

        val emailConfigs = knowledgeBaseEmailRepository.findAllByKnowledgeBaseId(knowledgeBaseId)
        if (emailConfigs.isEmpty()) {
            log.info { "No email configurations found for KB ID: $knowledgeBaseId" }
            return PollEmailResult(0, 0)
        }

        var totalMessages = 0
        var totalEvents = 0

        emailConfigs.forEach { emailConfig ->
            try {
                // Determine Provider Config
                val pollingConfig = emailConfig.toEmailProviderConfig()

                val provider = emailProviderFactory.createProvider(pollingConfig) ?: return@forEach

                if (!provider.isConnected()) {
                    if (!provider.connect()) {
                        log.error { "Failed to connect to provider for KB ID: $knowledgeBaseId" }
                        return@forEach
                    }
                }

                try {
                    val messages = provider.fetchNewMessages()
                    if (messages.isNotEmpty()) {
                        val events = messages.map { message ->
                            EmailReceivedEvent(
                                message = message,
                                providerType = provider.getProviderType(),
                                knowledgeBaseId = knowledgeBaseId
                            )
                        }
                        emailEventBus.publishAll(events)
                        totalMessages += messages.size
                        totalEvents += events.size
                    }
                } finally {
                    try {
                        provider.disconnect()
                    } catch (e: Exception) {
                        log.warn { "Error disconnecting provider: ${e.message}" }
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Error polling email config ${emailConfig.id} for KB ID: $knowledgeBaseId" }
            }
        }

        return PollEmailResult(totalMessages, totalEvents)
    }

    data class PollEmailResult(
        val messagesCount: Int,
        val eventsCount: Int
    )
}
