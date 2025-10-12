package com.okestro.okchat.email.provider

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.support.MailPropertiesBuilder
import com.okestro.okchat.email.support.buildMailProperties
import com.okestro.okchat.email.util.EmailContentCleaner
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

private val logger = KotlinLogging.logger {}

abstract class AbstractEmailProvider(
    protected val config: EmailProperties.EmailProviderConfig,
    protected val emailProperties: EmailProperties
) : EmailProvider {
    protected var store: Store? = null
    protected var inbox: Folder? = null

    // Use LinkedHashSet to maintain insertion order for LRU-like behavior
    private val seenMessageIds = LinkedHashSet<String>()
    private val maxSeenMessageIds = 10000 // Prevent unlimited memory growth

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info { "Attempting to connect to ${config.type} email provider..." }
            logger.info { "Host: ${config.host}, Port: ${config.port}, Username: ${config.username}" }

            val props = createProperties()
            logger.debug { "Mail properties: ${props.entries.joinToString { "${it.key}=${it.value}" }}" }

            val session = Session.getInstance(props)
            session.debug = true

            logger.info { "Getting mail store with protocol: ${getProtocol()}" }
            store = session.getStore(getProtocol())

            logger.info { "Connecting to mail server..." }
            val password = getPassword()
            store?.connect(config.host, config.username, password)
            logger.info { "Mail store connected successfully" }

            logger.info { "Opening INBOX folder..." }
            inbox = store?.getFolder("INBOX")
            inbox?.open(Folder.READ_WRITE)
            logger.info { "INBOX folder opened successfully" }

            logger.info { "Successfully connected to ${config.type} email provider" }
            true
        } catch (e: Exception) {
            logger.error(e) {
                """
                Failed to connect to ${config.type} email provider
                Error Type: ${e.javaClass.simpleName}
                Error Message: ${e.message}
                Host: ${config.host}
                Port: ${config.port}
                Username: ${config.username}
                Stack trace:
                ${e.stackTraceToString()}
                """.trimIndent()
            }
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            inbox?.close(false)
            store?.close()
            logger.info { "Disconnected from ${config.type} email provider" }
        } catch (e: Exception) {
            logger.error(e) { "Error disconnecting from ${config.type} email provider" }
        }
    }

    override suspend fun fetchNewMessages(): List<EmailMessage> = withContext(Dispatchers.IO) {
        if (!isConnectedInternal()) {
            logger.warn { "Not connected to ${config.type} email provider" }
            return@withContext emptyList()
        }

        try {
            val folder = inbox ?: return@withContext emptyList()
            val messages = folder.messages

            messages
                .mapNotNull { message ->
                    try {
                        if (message.flags.contains(Flags.Flag.SEEN)) {
                            return@mapNotNull null
                        }

                        val messageId = message.getHeader("Message-ID")?.firstOrNull() ?: return@mapNotNull null

                        if (seenMessageIds.contains(messageId)) {
                            return@mapNotNull null
                        }

                        // Implement LRU-like eviction
                        if (seenMessageIds.size >= maxSeenMessageIds) {
                            // Remove oldest (first) element
                            val oldest = seenMessageIds.first()
                            seenMessageIds.remove(oldest)
                            logger.debug { "Evicted oldest message ID from cache: $oldest" }
                        }

                        seenMessageIds.add(messageId)

                        EmailMessage(
                            id = messageId,
                            from = (message.from.firstOrNull() as? InternetAddress)?.address ?: "",
                            to = message.allRecipients
                                ?.mapNotNull { (it as? InternetAddress)?.address }
                                ?: emptyList(),
                            subject = message.subject ?: "",
                            content = extractContent(message),
                            receivedDate = message.receivedDate,
                            isRead = message.flags.contains(Flags.Flag.SEEN),
                            rawMessage = message
                        )
                    } catch (e: jakarta.mail.MessageRemovedException) {
                        logger.debug { "Skipped expunged message during fetch: ${e.message}" }
                        null
                    } catch (e: Exception) {
                        logger.error(e) { "Error parsing message" }
                        null
                    }
                }
        } catch (e: Exception) {
            logger.error(e) { "Error fetching messages from ${config.type}" }
            emptyList()
        }
    }

    override suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) {
        isConnectedInternal()
    }

    private fun isConnectedInternal(): Boolean = store?.isConnected == true && inbox?.isOpen == true

    override fun getProviderType(): EmailProperties.EmailProviderType = config.type

    protected open fun createProperties(): Properties =
        buildMailProperties(getProtocol()) {
            // 프로바이더별 설정 또는 전역 설정 사용
            val mailConfig = config.mail.takeIf { it.protocol != "imaps" } ?: emailProperties.mail

            basicConfig(
                host = config.host,
                port = config.port,
                timeout = mailConfig.connectionTimeout
            )

            val trustHost = if (mailConfig.ssl.trust == "*") config.host else mailConfig.ssl.trust
            sslConfig(trustHost)

            when (config.authType) {
                EmailProperties.AuthType.OAUTH2 -> oauth2Config()
            }

            debugConfig(enabled = mailConfig.debug)

            // YAML에서 정의한 커스텀 프로퍼티 적용
            mailConfig.properties.forEach { (key, value) ->
                customProperty(key, value)
            }

            applyProviderSpecificProperties()
        }

    protected open fun getProtocol(): String = "imaps"

    protected open fun MailPropertiesBuilder.applyProviderSpecificProperties() {}

    protected open suspend fun getPassword(): String? = config.password

    /**
     * Extract and clean content from email message
     * Handles both plain text and HTML content with proper preprocessing
     */
    private fun extractContent(message: Message): String {
        val rawContent = when (val content = message.content) {
            is String -> content
            is MimeMultipart -> extractFromMultipart(content)
            else -> ""
        }

        // Clean the extracted content
        return EmailContentCleaner.cleanEmailContent(rawContent)
    }

    /**
     * Recursively extract content from multipart email
     * Prefers plain text, but falls back to HTML converted to text
     */
    private fun extractFromMultipart(multipart: MimeMultipart): String {
        val textBuilder = StringBuilder()
        val htmlBuilder = StringBuilder()

        try {
            for (i in 0 until multipart.count) {
                val bodyPart = multipart.getBodyPart(i)

                when {
                    bodyPart.isMimeType("text/plain") -> {
                        textBuilder.append(bodyPart.content.toString())
                    }
                    bodyPart.isMimeType("text/html") -> {
                        htmlBuilder.append(bodyPart.content.toString())
                    }
                    bodyPart.content is MimeMultipart -> {
                        // Recursive handling for nested multipart
                        val nested = extractFromMultipart(bodyPart.content as MimeMultipart)
                        textBuilder.append(nested)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error extracting content from multipart message" }
        }

        // Prefer plain text, fallback to HTML converted to text
        return when {
            textBuilder.isNotEmpty() -> textBuilder.toString()
            htmlBuilder.isNotEmpty() -> {
                logger.debug { "Converting HTML content to plain text" }
                EmailContentCleaner.convertHtmlToText(htmlBuilder.toString())
            }
            else -> ""
        }
    }
}
