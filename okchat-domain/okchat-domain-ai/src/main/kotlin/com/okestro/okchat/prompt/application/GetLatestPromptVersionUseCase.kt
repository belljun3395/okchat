package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetLatestPromptVersionUseCaseIn
import com.okestro.okchat.prompt.application.dto.GetLatestPromptVersionUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class GetLatestPromptVersionUseCase(
    private val promptRepository: PromptRepository
) {
    suspend fun execute(useCaseIn: GetLatestPromptVersionUseCaseIn): GetLatestPromptVersionUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val version = promptRepository.findLatestVersionByType(useCaseIn.type)
            GetLatestPromptVersionUseCaseOut(version)
        }
}
