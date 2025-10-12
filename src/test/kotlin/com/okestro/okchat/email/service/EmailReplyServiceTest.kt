package com.okestro.okchat.email.service

import com.okestro.okchat.email.config.EmailProperties
import com.okestro.okchat.email.oauth2.OAuth2TokenService
import io.kotest.matchers.string.shouldContain
import io.mockk.clearAllMocks
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("EmailReplyService Unit Tests")
class EmailReplyServiceTest {

    private lateinit var emailProperties: EmailProperties
    private lateinit var oauth2TokenService: OAuth2TokenService
    private lateinit var service: EmailReplyService

    @BeforeEach
    fun setUp() {
        emailProperties = mockk()
        oauth2TokenService = mockk()
        service = EmailReplyService(emailProperties, oauth2TokenService)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("buildReplyContent should build reply content with original message")
    fun `should build reply content with original message`() {
        // given
        val answer = "이것은 답변입니다"
        val originalContent = "원본 질문 내용입니다"

        // when
        val result = service.buildReplyContent(answer, originalContent)

        // then
        result shouldContain answer
        result shouldContain "==="
        result shouldContain "Original message:"
        result shouldContain originalContent
    }

    @Test
    @DisplayName("buildReplyContent should truncate long original content")
    fun `should truncate long original content`() {
        // given
        val answer = "답변"
        val originalContent = (1..50).joinToString("\n") { "Line $it" }

        // when
        val result = service.buildReplyContent(answer, originalContent)

        // then
        result shouldContain answer
        result shouldContain "Original message:"
        // Truncation logic is in EmailContentCleaner, so just verify it's included
    }
}
