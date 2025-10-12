package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetPromptUseCaseIn
import com.okestro.okchat.prompt.application.dto.GetPromptUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import org.springframework.stereotype.Service

@Service
class GetPromptUseCase(
    private val promptRepository: PromptRepository,
    private val promptCacheService: PromptCacheService
) {
    suspend fun execute(useCaseIn: GetPromptUseCaseIn): GetPromptUseCaseOut {
        val (type, version) = useCaseIn

        val prompt = if (version != null) {
            promptRepository.findByTypeAndVersionAndActive(type, version)
        } else {
            // Try to get latest from cache first
            promptCacheService.getLatestPrompt(type)?.let {
                return GetPromptUseCaseOut(it)
            }
            promptRepository.findLatestByTypeAndActive(type)
        }

        val content = prompt?.let {
            if (version == null) {
                promptCacheService.cacheLatestPrompt(type, it.content)
            }
            it.content
        }
        return GetPromptUseCaseOut(content)
    }
}
