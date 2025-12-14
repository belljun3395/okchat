package com.okestro.okchat.chat.application

import com.okestro.okchat.chat.application.dto.StreamChatUseCaseIn
import com.okestro.okchat.chat.service.DocumentBaseChatService
import com.okestro.okchat.chat.service.dto.ChatServiceRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class StreamChatUseCaseTest : BehaviorSpec({

    val documentBaseChatService = mockk<DocumentBaseChatService>()
    val useCase = StreamChatUseCase(documentBaseChatService)

    afterEach {
        clearAllMocks()
    }

    given("Chat streaming is requested") {
        val input = StreamChatUseCaseIn(
            message = "What is Spring Boot?",
            sessionId = "session-123",
            keywords = listOf("spring", "framework"),
            isDeepThink = false,
            userEmail = "user@example.com"
        )

        val expectedResponse = Flux.just("Spring", " Boot", " is", " a", " framework")

        `when`("Service returns streaming response") {
            coEvery {
                documentBaseChatService.chat(any())
            } returns expectedResponse

            val result = useCase.execute(input)

            then("Streaming response is returned") {
                StepVerifier.create(result)
                    .expectNext("Spring")
                    .expectNext(" Boot")
                    .expectNext(" is")
                    .expectNext(" a")
                    .expectNext(" framework")
                    .verifyComplete()

                coVerify(exactly = 1) {
                    documentBaseChatService.chat(
                        match<ChatServiceRequest> {
                            it.message == input.message &&
                                it.sessionId == input.sessionId &&
                                it.keywords == input.keywords &&
                                it.isDeepThink == input.isDeepThink &&
                                it.userEmail == input.userEmail
                        }
                    )
                }
            }
        }

        `when`("Service returns error") {
            val exception = RuntimeException("Chat service error")
            coEvery {
                documentBaseChatService.chat(any())
            } returns Flux.error(exception)

            val result = useCase.execute(input)

            then("Error is propagated") {
                StepVerifier.create(result)
                    .expectError(RuntimeException::class.java)
                    .verify()
            }
        }

        `when`("Deep think mode is enabled") {
            val deepThinkInput = input.copy(isDeepThink = true)
            coEvery {
                documentBaseChatService.chat(any())
            } returns expectedResponse

            val result = useCase.execute(deepThinkInput)

            then("Request is passed with deep think enabled") {
                StepVerifier.create(result)
                    .expectNextCount(5)
                    .verifyComplete()

                coVerify(exactly = 1) {
                    documentBaseChatService.chat(
                        match<ChatServiceRequest> {
                            it.isDeepThink
                        }
                    )
                }
            }
        }
    }
})
