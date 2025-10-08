package com.okestro.okchat.ai.service

import com.okestro.okchat.ai.model.Prompt
import com.okestro.okchat.ai.repository.PromptRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PromptService(
    private val promptRepository: PromptRepository,
    private val promptCacheService: PromptCacheService
) {
    private val log = KotlinLogging.logger {}

    /**
     * Get prompt by type and optional version
     * If version is not specified, returns the latest active version
     * Uses Redis cache for performance
     */
    suspend fun getPrompt(type: String, version: Int? = null): String? {
        val prompt = if (version != null) {
            promptRepository.findByTypeAndVersionAndIsActive(type, version)
        } else {
            // Try to get latest from cache first
            promptCacheService.getLatestPrompt(type)?.let {
                return it
            }
            promptRepository.findLatestByTypeAndIsActive(type)
        }

        return prompt?.let {
            if (version == null) {
                promptCacheService.cacheLatestPrompt(type, it.content)
            }
            it.content
        }
    }

    /**
     * Get the latest version of a prompt
     */
    suspend fun getLatestPrompt(type: String): String? {
        return getPrompt(type, null)
    }

    /**
     * Get a specific version of a prompt
     */
    suspend fun getPromptByVersion(type: String, version: Int): String? {
        return getPrompt(type, version)
    }

    /**
     * Create a new prompt
     */
    @Transactional("transactionManager")
    suspend fun createPrompt(type: String, content: String): Prompt {
        val latestPrompt = promptRepository.findLatestByTypeAndIsActive(type)
        val version = if (latestPrompt == null) {
            1
        } else {
            latestPrompt.version + 1
        }

        val prompt = Prompt(
            type = type,
            version = version,
            content = content,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        if (latestPrompt != null) {
            promptRepository.deactivatePrompt(latestPrompt.id!!)
        }
        val saved = promptRepository.save(prompt)
        log.info { "Created new prompt: type=$type, version=$version" }
        promptCacheService.cacheLatestPrompt(type, content)
        return saved
    }

    /**
     * Update a prompt by creating a new version
     */
    @Transactional("transactionManager")
    suspend fun updateLatestPrompt(type: String, content: String): Prompt {
        val latestPrompt = promptRepository.findLatestByTypeAndIsActive(type)
            ?: throw IllegalArgumentException("Prompt type '$type' does not exist. Use createPrompt first.")

        val newVersion = latestPrompt.version + 1
        val prompt = Prompt(
            type = type,
            version = newVersion,
            content = content,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        promptRepository.deactivatePrompt(latestPrompt.id!!)
        val saved = promptRepository.save(prompt)
        log.info { "Created new version of prompt: type=$type, version=$newVersion" }
        promptCacheService.cacheLatestPrompt(type, content)
        return saved
    }

    /**
     * Deactivate a specific version of a prompt
     */
    @Transactional("transactionManager")
    suspend fun deactivatePrompt(type: String, version: Int) {
        val prompt = promptRepository.findByTypeAndVersionAndIsActive(type, version) ?: throw IllegalArgumentException("Prompt not found: type=$type, version=$version")
        promptRepository.deactivatePrompt(prompt.id!!)

        val latestPrompt = promptRepository.findLatestByTypeAndIsActive(type)
        if (latestPrompt != null && latestPrompt.id == prompt.id) {
            promptCacheService.evictLatestPromptCache(type)
        }
    }

    /**
     * Get all versions of a prompt type
     */
    suspend fun getAllVersions(type: String): List<Prompt> {
        return promptRepository.findAllByTypeAndIsActive(type)
    }

    /**
     * Get the latest version number for a prompt type
     */
    suspend fun getLatestVersion(type: String): Int? {
        return promptRepository.findLatestVersionByType(type)
    }

    /**
     * Check if a prompt type exists
     */
    suspend fun exists(type: String): Boolean {
        return promptRepository.findLatestByTypeAndIsActive(type) != null
    }
}
