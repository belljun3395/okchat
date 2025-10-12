package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.CheckPromptExistsUseCaseIn
import com.okestro.okchat.prompt.application.dto.CheckPromptExistsUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import org.springframework.stereotype.Service

@Service
class CheckPromptExistsUseCase(
    private val promptRepository: PromptRepository
) {
    suspend fun execute(useCaseIn: CheckPromptExistsUseCaseIn): CheckPromptExistsUseCaseOut {
        val exists = promptRepository.findLatestByTypeAndActive(useCaseIn.type) != null
        return CheckPromptExistsUseCaseOut(exists)
    }
}
