package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetAllPromptVersionsUseCaseIn
import com.okestro.okchat.prompt.application.dto.GetAllPromptVersionsUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import org.springframework.stereotype.Service

@Service
class GetAllPromptVersionsUseCase(
    private val promptRepository: PromptRepository
) {
    suspend fun execute(useCaseIn: GetAllPromptVersionsUseCaseIn): GetAllPromptVersionsUseCaseOut {
        val prompts = promptRepository.findAllByTypeAndActiveOrderByVersionDesc(useCaseIn.type)
        return GetAllPromptVersionsUseCaseOut(prompts)
    }
}
