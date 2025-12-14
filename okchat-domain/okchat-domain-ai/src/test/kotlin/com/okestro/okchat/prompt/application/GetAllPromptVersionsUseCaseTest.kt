package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetAllPromptVersionsUseCaseIn
import com.okestro.okchat.prompt.model.entity.Prompt
import com.okestro.okchat.prompt.repository.PromptRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class GetAllPromptVersionsUseCaseTest : BehaviorSpec({

    val promptRepository: PromptRepository = mockk()
    val getAllPromptVersionsUseCase = GetAllPromptVersionsUseCase(promptRepository)

    given("A prompt with multiple versions") {
        val type = "test"
        val prompts = listOf(
            Prompt(id = 1, type = type, version = 1, content = "content 1", active = true),
            Prompt(id = 2, type = type, version = 2, content = "content 2", active = true)
        )
        coEvery { promptRepository.findAllByTypeAndActiveOrderByVersionDesc(type) } returns prompts

        `when`("the use case is executed") {
            val result = getAllPromptVersionsUseCase.execute(GetAllPromptVersionsUseCaseIn(type))

            then("all versions of the prompt should be returned") {
                result.prompts shouldBe prompts
            }
        }
    }
})
