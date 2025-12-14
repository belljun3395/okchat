package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetPromptUseCaseIn
import com.okestro.okchat.prompt.model.entity.Prompt
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class GetPromptUseCaseTest : BehaviorSpec({

    val promptRepository: PromptRepository = mockk()
    val promptCacheService: PromptCacheService = mockk()
    val getPromptUseCase = GetPromptUseCase(promptRepository, promptCacheService)

    given("A prompt with a specific version") {
        val type = "test"
        val version = 1
        val content = "test content"
        val prompt = Prompt(id = 1, type = type, version = version, content = content, active = true)
        coEvery { promptRepository.findByTypeAndVersionAndActive(type, version) } returns prompt

        `when`("the use case is executed with a version") {
            val result = getPromptUseCase.execute(GetPromptUseCaseIn(type, version))

            then("the content of the prompt should be returned") {
                result.content shouldBe content
            }
        }
    }

    given("A prompt without a specific version") {
        val type = "test"
        val content = "latest test content"
        val prompt = Prompt(id = 2, type = type, version = 2, content = content, active = true)
        coEvery { promptCacheService.getLatestPrompt(type) } returns null
        coEvery { promptRepository.findLatestByTypeAndActive(type) } returns prompt
        coEvery { promptCacheService.cacheLatestPrompt(type, content) } returns Unit

        `when`("the use case is executed without a version") {
            val result = getPromptUseCase.execute(GetPromptUseCaseIn(type))

            then("the content of the latest prompt should be returned") {
                result.content shouldBe content
            }
        }
    }

    given("A cached prompt") {
        val type = "test"
        val content = "cached test content"
        coEvery { promptCacheService.getLatestPrompt(type) } returns content

        `when`("the use case is executed without a version") {
            val result = getPromptUseCase.execute(GetPromptUseCaseIn(type))

            then("the cached content should be returned") {
                result.content shouldBe content
            }
        }
    }
})
