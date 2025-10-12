package com.okestro.okchat.email.service

import com.okestro.okchat.chat.service.ChatServiceRequest
import com.okestro.okchat.chat.service.DocumentBaseChatService
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@DisplayName("EmailChatService Unit Tests")
class EmailChatServiceTest {

    private lateinit var documentBaseChatService: DocumentBaseChatService
    private lateinit var emailChatService: EmailChatService

    @BeforeEach
    fun setUp() {
        documentBaseChatService = mockk()
        emailChatService = EmailChatService(documentBaseChatService)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("processEmailQuestion should process successfully")
    fun `should process email question successfully`() = runTest {
        // given
        val subject = "질문입니다"
        val content = "안녕하세요. 궁금한 점이 있습니다."
        val requestSlot = slot<ChatServiceRequest>()

        coEvery { documentBaseChatService.chat(capture(requestSlot)) } returns Flux.just("답변", " 내용", " 입니다.")

        // when
        val result = emailChatService.processEmailQuestion(subject, content)

        // then
        StepVerifier.create(result)
            .expectNextMatches { it.contains("Hello. This email is an automatically generated response") }
            .expectNext(" 내용")
            .expectNext(" 입니다.")
            .expectNextMatches { it.contains("The above content is an automatically generated response by AI") }
            .verifyComplete()

        requestSlot.captured.message shouldBe "질문입니다 안녕하세요. 궁금한 점이 있습니다."
        requestSlot.captured.isDeepThink shouldBe true
    }

    @Test
    @DisplayName("processEmailQuestion should skip subject when it is (No subject)")
    fun `should skip subject when it is (No subject)`() = runTest {
        // given
        val subject = "(No subject)"
        val content = "질문 내용"
        val requestSlot = slot<ChatServiceRequest>()

        coEvery { documentBaseChatService.chat(capture(requestSlot)) } returns Flux.just("답변")

        // when
        val result = emailChatService.processEmailQuestion(subject, content)

        // then
        StepVerifier.create(result)
            .expectNextCount(2) // header with first chunk + footer
            .verifyComplete()

        requestSlot.captured.message shouldBe "질문 내용" // subject not included
    }

    @Test
    @DisplayName("processEmailQuestion should skip blank subject")
    fun `should skip blank subject`() = runTest {
        // given
        val subject = "   "
        val content = "질문 내용"
        val requestSlot = slot<ChatServiceRequest>()

        coEvery { documentBaseChatService.chat(capture(requestSlot)) } returns Flux.just("답변")

        // when
        val result = emailChatService.processEmailQuestion(subject, content)

        // then
        StepVerifier.create(result)
            .expectNextCount(2) // header + footer
            .verifyComplete()

        requestSlot.captured.message shouldBe "질문 내용"
    }

    @Test
    @DisplayName("processEmailQuestion should return error when both subject and content are blank")
    fun `should return error message when both subject and content are blank`() = runTest {
        // given
        val subject = ""
        val content = "   "

        // when
        val result = emailChatService.processEmailQuestion(subject, content)

        // then
        StepVerifier.create(result)
            .expectNext("Sorry. Cannot read email content and therefore cannot generate a response.")
            .verifyComplete()
    }

    @Test
    @DisplayName("processEmailQuestion should include subject with Re prefix")
    fun `should include subject with Re prefix`() = runTest {
        // given
        val subject = "Re: 기존 질문"
        val content = "추가 질문"
        val requestSlot = slot<ChatServiceRequest>()

        coEvery { documentBaseChatService.chat(capture(requestSlot)) } returns Flux.just("답변")

        // when
        val result = emailChatService.processEmailQuestion(subject, content)

        // then
        StepVerifier.create(result)
            .expectNextCount(2)
            .verifyComplete()

        requestSlot.captured.message shouldBe "Re: 기존 질문 추가 질문"
    }

    @Test
    @DisplayName("processEmailQuestion should handle long content")
    fun `should handle long content`() = runTest {
        // given
        val subject = "긴 질문"
        val content = "a".repeat(1000)
        val requestSlot = slot<ChatServiceRequest>()

        coEvery { documentBaseChatService.chat(capture(requestSlot)) } returns Flux.just("답변")

        // when
        val result = emailChatService.processEmailQuestion(subject, content)

        // then
        StepVerifier.create(result)
            .expectNextCount(2)
            .verifyComplete()

        requestSlot.captured.message.length shouldBe 1000 + "긴 질문 ".length
    }
}
