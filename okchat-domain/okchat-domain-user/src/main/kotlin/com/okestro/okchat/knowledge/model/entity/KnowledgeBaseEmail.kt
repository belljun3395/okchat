package com.okestro.okchat.knowledge.model.entity

import com.okestro.okchat.email.config.EmailProperties
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "knowledge_base_emails")
data class KnowledgeBaseEmail(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "knowledge_base_id", nullable = false)
    val knowledgeBaseId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    val providerType: EmailProperties.EmailProviderType,

    @Column(name = "email_address", nullable = false)
    val emailAddress: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    val authType: EmailProperties.AuthType = EmailProperties.AuthType.OAUTH2,

    @Column(name = "client_id", nullable = false)
    val clientId: String,

    // Ideally this should be encrypted
    @Column(name = "client_secret", nullable = false)
    val clientSecret: String,

    @Column(name = "tenant_id", length = 100)
    val tenantId: String? = "common", // Only for Outlook

    val scopes: String? = null, // Comma separated scopes if needed override

    @Column(name = "redirect_uri")
    val redirectUri: String? = "http://localhost:8080/oauth2/callback",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toEmailProviderConfig(): EmailProperties.EmailProviderConfig {
        return EmailProperties.EmailProviderConfig(
            type = providerType,
            host = if (providerType == EmailProperties.EmailProviderType.GMAIL) "imap.gmail.com" else "outlook.office365.com",
            port = 993,
            username = emailAddress,
            authType = authType,
            enabled = true,
            oauth2 = EmailProperties.OAuth2Config(
                clientId = clientId,
                clientSecret = clientSecret,
                tenantId = tenantId ?: "common",
                scopes = if (providerType == EmailProperties.EmailProviderType.GMAIL) {
                    listOf("https://mail.google.com/")
                } else {
                    listOf("https://outlook.office365.com/IMAP.AccessAsUser.All", "https://outlook.office365.com/SMTP.Send", "offline_access")
                },
                redirectUri = redirectUri ?: "http://localhost:8080/oauth2/callback"
            )
        )
    }
}
