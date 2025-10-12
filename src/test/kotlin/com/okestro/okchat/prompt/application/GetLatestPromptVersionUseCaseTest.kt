package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetLatestPromptVersionUseCaseIn
import com.okestro.okchat.prompt.repository.PromptRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class GetLatestPromptVersionUseCaseTest : BehaviorSpec({

    val promptRepository: PromptRepository = mockk()
    val getLatestPromptVersionUseCase = GetLatestPromptVersionUseCase(promptRepository)

    given("A prompt with a latest version") {
        val type = "test"
        val version = 2
        coEvery { promptRepository.findLatestVersionByType(type) } returns version

        `when`("the use case is executed") {
            val result = getLatestPromptVersionUseCase.execute(GetLatestPromptVersionUseCaseIn(type))

            then("the latest version of the prompt should be returned") {
                result.version shouldBe version
            }
        }
    }

    given("A prompt with no versions") {
        val type = "test"
        coEvery { promptRepository.findLatestVersionByType(type) } returns null

        `when`("the use case is executed") {
            val result = getLatestPromptVersionUseCase.execute(GetLatestPromptVersionUseCaseIn(type))

            then("null should be returned") {
                result.version shouldBe null
            }
        }
    }
})
