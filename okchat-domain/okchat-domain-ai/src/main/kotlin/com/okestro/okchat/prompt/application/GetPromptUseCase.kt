package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.GetPromptUseCaseIn
import com.okestro.okchat.prompt.application.dto.GetPromptUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class GetPromptUseCase(
    private val promptRepository: PromptRepository,
    private val promptCacheService: PromptCacheService
) {
    suspend fun execute(useCaseIn: GetPromptUseCaseIn): GetPromptUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val (type, version) = useCaseIn

            val prompt = if (version != null) {
                promptRepository.findByTypeAndVersionAndActive(type, version)
            } else {
                // Try to get latest from cache first
                promptCacheService.getLatestPrompt(type)?.let {
                    return@withContext GetPromptUseCaseOut(it)
                }
                promptRepository.findLatestByTypeAndActive(type)
            }

            val content = prompt?.let {
                if (version == null) {
                    promptCacheService.cacheLatestPrompt(type, it.content)
                }
                it.content
            }
            GetPromptUseCaseOut(content)
        }
}
