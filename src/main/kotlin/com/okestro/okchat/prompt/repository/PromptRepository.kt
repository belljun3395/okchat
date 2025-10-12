package com.okestro.okchat.prompt.repository

import com.okestro.okchat.prompt.model.entity.Prompt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PromptRepository : JpaRepository<Prompt, Long> {

    /**
     * Find prompt by type, version and active status
     */
    fun findByTypeAndVersionAndActive(
        type: String,
        version: Int,
        active: Boolean = true
    ): Prompt?

    /**
     * Find latest active prompt by type
     */
    @Query(
        """
        SELECT p FROM Prompt p
        WHERE p.type = :type AND p.active = true
        ORDER BY p.version DESC
        LIMIT 1
        """
    )
    fun findLatestByTypeAndActive(@Param("type") type: String): Prompt?

    /**
     * Find all active prompts by type ordered by version descending
     */
    fun findAllByTypeAndActiveOrderByVersionDesc(
        type: String,
        active: Boolean = true
    ): List<Prompt>

    /**
     * Find maximum version number for a prompt type
     */
    @Query(
        """
        SELECT MAX(p.version) FROM Prompt p
        WHERE p.type = :type
        """
    )
    fun findLatestVersionByType(@Param("type") type: String): Int?

    /**
     * Deactivate a prompt by id
     */
    @Modifying
    @Query(
        """
        UPDATE Prompt p
        SET p.active = false, p.updatedAt = CURRENT_TIMESTAMP
        WHERE p.id = :id
        """
    )
    fun deactivatePrompt(@Param("id") id: Long): Int
}
