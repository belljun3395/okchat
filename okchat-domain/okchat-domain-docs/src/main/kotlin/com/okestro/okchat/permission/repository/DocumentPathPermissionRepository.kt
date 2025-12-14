package com.okestro.okchat.permission.repository

import com.okestro.okchat.permission.model.entity.DocumentPathPermission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface DocumentPathPermissionRepository : JpaRepository<DocumentPathPermission, Long> {

    /**
     * Find all path-based permissions for a user
     */
    fun findByUserId(userId: Long): List<DocumentPathPermission>

    /**
     * Find permissions for a specific path
     */
    fun findByDocumentPath(documentPath: String): List<DocumentPathPermission>

    /**
     * Find specific permission for user and path
     */
    fun findByUserIdAndDocumentPath(userId: Long, documentPath: String): DocumentPathPermission?

    /**
     * Delete all permissions for a user
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentPathPermission d WHERE d.userId = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)

    /**
     * Delete multiple path permissions
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentPathPermission d WHERE d.userId = :userId AND d.documentPath IN :documentPaths")
    fun deleteByUserIdAndDocumentPathIn(
        @Param("userId") userId: Long,
        @Param("documentPaths") documentPaths: List<String>
    )
}
