package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.CreatePromptUseCaseIn
import com.okestro.okchat.prompt.application.dto.CreatePromptUseCaseOut
import com.okestro.okchat.prompt.model.Prompt
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Service
class CreatePromptUseCase(
    private val promptRepository: PromptRepository,
    private val promptCacheService: PromptCacheService
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: CreatePromptUseCaseIn): CreatePromptUseCaseOut {
        val (type, content) = useCaseIn

        val latestPrompt = promptRepository.findLatestByTypeAndActive(type)
        val version = if (latestPrompt == null) {
            1
        } else {
            latestPrompt.version + 1
        }

        val prompt = Prompt(
            type = type,
            version = version,
            content = content,
            active = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        if (latestPrompt != null) {
            promptRepository.deactivatePrompt(latestPrompt.id!!)
        }
        val saved = promptRepository.save(prompt)
        log.info { "Created new prompt: type=$type, version=$version" }
        promptCacheService.cacheLatestPrompt(type, content)
        return CreatePromptUseCaseOut(saved)
    }
}
