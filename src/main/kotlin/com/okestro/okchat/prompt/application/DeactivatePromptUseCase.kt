package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.DeactivatePromptUseCaseIn
import com.okestro.okchat.prompt.application.dto.DeactivatePromptUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeactivatePromptUseCase(
    private val promptRepository: PromptRepository,
    private val promptCacheService: PromptCacheService
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: DeactivatePromptUseCaseIn): DeactivatePromptUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val (type, version) = useCaseIn

            val prompt = promptRepository.findByTypeAndVersionAndActive(type, version)
                ?: throw IllegalArgumentException("Prompt not found: type=$type, version=$version")
            prompt.deActive()
            promptRepository.save(prompt)

            val latestPrompt = promptRepository.findLatestByTypeAndActive(type)
            if (latestPrompt != null && latestPrompt.id == prompt.id) {
                promptCacheService.evictLatestPromptCache(type)
            }
            DeactivatePromptUseCaseOut(true)
        }
}
