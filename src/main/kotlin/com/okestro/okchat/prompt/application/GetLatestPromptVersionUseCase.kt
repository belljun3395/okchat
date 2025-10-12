package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetLatestPromptVersionUseCaseIn
import com.okestro.okchat.prompt.application.dto.GetLatestPromptVersionUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import org.springframework.stereotype.Service

@Service
class GetLatestPromptVersionUseCase(
    private val promptRepository: PromptRepository
) {
    suspend fun execute(useCaseIn: GetLatestPromptVersionUseCaseIn): GetLatestPromptVersionUseCaseOut {
        val version = promptRepository.findLatestVersionByType(useCaseIn.type)
        return GetLatestPromptVersionUseCaseOut(version)
    }
}
