package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.UpdatePromptUseCaseIn
import com.okestro.okchat.prompt.model.entity.Prompt
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class UpdatePromptUseCaseTest : BehaviorSpec({

    val promptRepository: PromptRepository = mockk()
    val promptCacheService: PromptCacheService = mockk()
    val updatePromptUseCase = UpdatePromptUseCase(promptRepository, promptCacheService)

    given("An existing prompt") {
        val type = "test"
        val content = "new content"
        val oldPrompt = Prompt(id = 1, type = type, version = 1, content = "old content", active = true)
        val newPrompt = Prompt(id = 2, type = type, version = 2, content = content, active = true)
        coEvery { promptRepository.findLatestByTypeAndActive(type) } returns oldPrompt
        coEvery { promptRepository.save(any()) } returns newPrompt
        coEvery { promptCacheService.cacheLatestPrompt(type, content) } returns Unit

        `when`("the use case is executed") {
            val result = updatePromptUseCase.execute(UpdatePromptUseCaseIn(type, content))

            then("a new version of the prompt should be created") {
                result.prompt shouldBe newPrompt
            }
        }
    }

    given("A non-existing prompt") {
        val type = "test"
        val content = "new content"
        coEvery { promptRepository.findLatestByTypeAndActive(type) } returns null

        `when`("the use case is executed") {
            val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                kotlinx.coroutines.runBlocking {
                    updatePromptUseCase.execute(UpdatePromptUseCaseIn(type, content))
                }
            }

            then("an exception should be thrown") {
                exception.message shouldBe "Prompt type '$type' does not exist. Use createPrompt first."
            }
        }
    }
})
