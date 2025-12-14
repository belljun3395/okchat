package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.UpdatePromptUseCaseIn
import com.okestro.okchat.prompt.application.dto.UpdatePromptUseCaseOut
import com.okestro.okchat.prompt.model.entity.Prompt
import com.okestro.okchat.prompt.repository.PromptRepository
import com.okestro.okchat.prompt.service.PromptCacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Service
class UpdatePromptUseCase(
    private val promptRepository: PromptRepository,
    private val promptCacheService: PromptCacheService
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: UpdatePromptUseCaseIn): UpdatePromptUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val (type, content) = useCaseIn

            val latestPrompt = promptRepository.findLatestByTypeAndActive(type)
                ?: throw IllegalArgumentException("Prompt type '$type' does not exist. Use createPrompt first.")

            val newVersion = latestPrompt.version + 1
            val prompt = Prompt(
                type = type,
                version = newVersion,
                content = content,
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            latestPrompt.deActive()
            val saved = promptRepository.save(prompt)
            log.info { "Created new version of prompt: type=$type, version=$newVersion" }
            promptCacheService.cacheLatestPrompt(type, content)
            UpdatePromptUseCaseOut(saved)
        }
}
