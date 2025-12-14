package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetAllPromptVersionsUseCaseIn
import com.okestro.okchat.prompt.application.dto.GetAllPromptVersionsUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class GetAllPromptVersionsUseCase(
    private val promptRepository: PromptRepository
) {
    suspend fun execute(useCaseIn: GetAllPromptVersionsUseCaseIn): GetAllPromptVersionsUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val prompts = promptRepository.findAllByTypeAndActiveOrderByVersionDesc(useCaseIn.type)
            GetAllPromptVersionsUseCaseOut(prompts)
        }
}
