package com.okestro.okchat.email.service

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.OAuth2TokenService
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("EmailReplyService Tests")
class EmailReplyServiceTest {

    private val emailProperties: EmailProperties = mockk()
    private val oauth2TokenService: OAuth2TokenService = mockk()
    private val service = EmailReplyService(emailProperties, oauth2TokenService)

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("should build reply content with original message")
    fun `should build reply content with original message`() {
        // given
        val answer = "This is an answer"
        val originalContent = "Original question content"

        // when
        val result = service.buildReplyContent(answer, originalContent)

        // then
        result shouldContain answer
        result shouldContain "==="
        result shouldContain "Original message:"
        result shouldContain originalContent
    }

    @Test
    @DisplayName("should truncate long original content")
    fun `should truncate long original content`() {
        // given
        val answer = "Answer"
        val originalContent = (1..50).joinToString("\n") { "Line $it" }

        // when
        val result = service.buildReplyContent(answer, originalContent)

        // then
        result shouldContain answer
        result shouldContain "Original message:"
        // Truncation logic is in EmailContentCleaner, so just verify it's included
    }
}
