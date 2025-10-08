package com.okestro.okchat.ai.repository

import com.okestro.okchat.ai.model.Prompt
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PromptRepository : CrudRepository<Prompt, Long> {

    @Query(
        """
        SELECT * FROM prompts
        WHERE type = :type AND version = :version AND is_active = true
        """
    )
    fun findByTypeAndVersionAndIsActive(
        @Param("type") type: String,
        @Param("version") version: Int
    ): Prompt?

    @Query(
        """
        SELECT * FROM prompts
        WHERE type = :type AND is_active = true
        ORDER BY version DESC
        LIMIT 1
        """
    )
    fun findLatestByTypeAndIsActive(@Param("type") type: String): Prompt?

    @Query(
        """
        SELECT * FROM prompts
        WHERE type = :type AND is_active = true
        ORDER BY version DESC
        """
    )
    fun findAllByTypeAndIsActive(@Param("type") type: String): List<Prompt>

    @Query(
        """
        SELECT MAX(version) FROM prompts
        WHERE type = :type
        """
    )
    fun findLatestVersionByType(@Param("type") type: String): Int?

    @Query(
        """
        UPDATE prompts
        SET is_active = false, updated_at = CURRENT_TIMESTAMP
        WHERE id = :id
        """
    )
    fun deactivatePrompt(@Param("id") id: Long): Int
}
