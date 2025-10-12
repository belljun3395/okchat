package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.DeactivatePromptUseCaseIn
import com.okestro.okchat.prompt.application.dto.DeactivatePromptUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeactivatePromptUseCase(
    private val promptRepository: PromptRepository,
    private val promptCacheService: PromptCacheService
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: DeactivatePromptUseCaseIn): DeactivatePromptUseCaseOut {
        val (type, version) = useCaseIn

        val prompt = promptRepository.findByTypeAndVersionAndActive(type, version) ?: throw IllegalArgumentException("Prompt not found: type=$type, version=$version")
        promptRepository.deactivatePrompt(prompt.id!!)

        val latestPrompt = promptRepository.findLatestByTypeAndActive(type)
        if (latestPrompt != null && latestPrompt.id == prompt.id) {
            promptCacheService.evictLatestPromptCache(type)
        }
        return DeactivatePromptUseCaseOut(true)
    }
}
