package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.CheckPromptExistsUseCaseIn
import com.okestro.okchat.prompt.model.entity.Prompt
import com.okestro.okchat.prompt.repository.PromptRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class CheckPromptExistsUseCaseTest : BehaviorSpec({

    val promptRepository: PromptRepository = mockk()
    val checkPromptExistsUseCase = CheckPromptExistsUseCase(promptRepository)

    given("An existing prompt") {
        val type = "test"
        val prompt = Prompt(id = 1, type = type, version = 1, content = "test content", active = true)
        coEvery { promptRepository.findLatestByTypeAndActive(type) } returns prompt

        `when`("the use case is executed") {
            val result = checkPromptExistsUseCase.execute(CheckPromptExistsUseCaseIn(type))

            then("true should be returned") {
                result.exists shouldBe true
            }
        }
    }

    given("A non-existing prompt") {
        val type = "test"
        coEvery { promptRepository.findLatestByTypeAndActive(type) } returns null

        `when`("the use case is executed") {
            val result = checkPromptExistsUseCase.execute(CheckPromptExistsUseCaseIn(type))

            then("false should be returned") {
                result.exists shouldBe false
            }
        }
    }
})
