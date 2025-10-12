package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.CreatePromptUseCaseIn
import com.okestro.okchat.prompt.model.Prompt
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class CreatePromptUseCaseTest : BehaviorSpec({

    val promptRepository: PromptRepository = mockk()
    val promptCacheService: PromptCacheService = mockk()
    val createPromptUseCase = CreatePromptUseCase(promptRepository, promptCacheService)

    given("A new prompt") {
        val type = "test"
        val content = "test content"
        val prompt = Prompt(id = 1, type = type, version = 1, content = content, active = true)
        coEvery { promptRepository.findLatestByTypeAndActive(type) } returns null
        coEvery { promptRepository.save(any()) } returns prompt
        coEvery { promptCacheService.cacheLatestPrompt(type, content) } returns Unit

        `when`("the use case is executed") {
            val result = createPromptUseCase.execute(CreatePromptUseCaseIn(type, content))

            then("a new prompt should be created") {
                result.prompt shouldBe prompt
            }
        }
    }

    given("An existing prompt") {
        val type = "test"
        val content = "new content"
        val oldPrompt = Prompt(id = 1, type = type, version = 1, content = "old content", active = true)
        val newPrompt = Prompt(id = 2, type = type, version = 2, content = content, active = true)
        coEvery { promptRepository.findLatestByTypeAndActive(type) } returns oldPrompt
        coEvery { promptRepository.deactivatePrompt(oldPrompt.id!!) } returns 1
        coEvery { promptRepository.save(any()) } returns newPrompt
        coEvery { promptCacheService.cacheLatestPrompt(type, content) } returns Unit

        `when`("the use case is executed") {
            val result = createPromptUseCase.execute(CreatePromptUseCaseIn(type, content))

            then("a new version of the prompt should be created") {
                result.prompt shouldBe newPrompt
            }
        }
    }
})
