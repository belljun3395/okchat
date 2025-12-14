package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetAllPromptTypesUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import org.springframework.stereotype.Service

@Service
class GetAllPromptTypesUseCase(
    private val promptRepository: PromptRepository
) {
    fun execute(): GetAllPromptTypesUseCaseOut {
        val types = promptRepository.findDistinctTypes()
        return GetAllPromptTypesUseCaseOut(types)
    }
}
