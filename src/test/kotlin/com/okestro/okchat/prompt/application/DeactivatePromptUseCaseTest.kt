package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.DeactivatePromptUseCaseIn
import com.okestro.okchat.prompt.model.Prompt
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class DeactivatePromptUseCaseTest : BehaviorSpec({

    val promptRepository: PromptRepository = mockk()
    val promptCacheService: PromptCacheService = mockk()
    val deactivatePromptUseCase = DeactivatePromptUseCase(promptRepository, promptCacheService)

    given("An existing prompt") {
        val type = "test"
        val version = 1
        val prompt = Prompt(id = 1, type = type, version = version, content = "test content", active = true)
        coEvery { promptRepository.findByTypeAndVersionAndActive(type, version) } returns prompt
        coEvery { promptRepository.deactivatePrompt(prompt.id!!) } returns 1
        coEvery { promptRepository.findLatestByTypeAndActive(type) } returns null
        coEvery { promptCacheService.evictLatestPromptCache(type) } returns Unit

        `when`("the use case is executed") {
            val result = deactivatePromptUseCase.execute(DeactivatePromptUseCaseIn(type, version))

            then("the prompt should be deactivated") {
                result.success shouldBe true
            }
        }
    }

    given("A non-existing prompt") {
        val type = "test"
        val version = 1
        coEvery { promptRepository.findByTypeAndVersionAndActive(type, version) } returns null

        `when`("the use case is executed") {
            val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                kotlinx.coroutines.runBlocking {
                    deactivatePromptUseCase.execute(DeactivatePromptUseCaseIn(type, version))
                }
            }

            then("an exception should be thrown") {
                exception.message shouldBe "Prompt not found: type=$type, version=$version"
            }
        }
    }
})
