package com.okestro.okchat.permission.repository

import com.okestro.okchat.permission.entity.DocumentPermission
import com.okestro.okchat.permission.entity.PermissionLevel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DocumentPermissionRepository : JpaRepository<DocumentPermission, Long> {

    /**
     * Find all document IDs accessible by a user
     */
    @Query("SELECT dp.documentId FROM DocumentPermission dp WHERE dp.userId = :userId AND dp.permissionLevel = :level")
    fun findDocumentIdsByUserId(
        @Param("userId") userId: Long,
        @Param("level") level: PermissionLevel = PermissionLevel.READ
    ): List<String>

    /**
     * Find all permissions for a user
     */
    fun findByUserId(userId: Long): List<DocumentPermission>

    /**
     * Find all permissions for a document
     */
    fun findByDocumentId(documentId: String): List<DocumentPermission>

    /**
     * Check if user has permission for a document
     */
    fun existsByUserIdAndDocumentIdAndPermissionLevel(
        userId: Long,
        documentId: String,
        permissionLevel: PermissionLevel = PermissionLevel.READ
    ): Boolean

    /**
     * Find specific permission
     */
    fun findByUserIdAndDocumentId(userId: Long, documentId: String): DocumentPermission?

    /**
     * Delete permission
     */
    fun deleteByUserIdAndDocumentId(userId: Long, documentId: String)

    /**
     * Delete all permissions for a document
     */
    fun deleteByDocumentId(documentId: String)

    /**
     * Delete all permissions for a user
     */
    fun deleteByUserId(userId: Long)

    /**
     * Find all documents in a space accessible by user
     */
    fun findByUserIdAndSpaceKey(userId: Long, spaceKey: String): List<DocumentPermission>

    /**
     * Find all path-based permissions for a user
     */
    @Query("SELECT dp FROM DocumentPermission dp WHERE dp.userId = :userId AND dp.documentPath IS NOT NULL")
    fun findPathBasedPermissionsByUserId(@Param("userId") userId: Long): List<DocumentPermission>

    /**
     * Find all unique document paths (for path listing)
     */
    @Query("SELECT DISTINCT dp.documentPath FROM DocumentPermission dp WHERE dp.documentPath IS NOT NULL ORDER BY dp.documentPath")
    fun findAllDistinctDocumentPaths(): List<String>

    /**
     * Find permissions by path
     */
    fun findByDocumentPath(documentPath: String): List<DocumentPermission>

    /**
     * Check if user has path-based permission
     */
    fun existsByUserIdAndDocumentPath(userId: Long, documentPath: String): Boolean

    /**
     * Delete path-based permission
     */
    fun deleteByUserIdAndDocumentPath(userId: Long, documentPath: String)

    /**
     * Delete multiple document-based permissions
     */
    fun deleteByUserIdAndDocumentIdIn(userId: Long, documentIds: List<String>)

    /**
     * Delete multiple path-based permissions
     */
    fun deleteByUserIdAndDocumentPathIn(userId: Long, documentPaths: List<String>)
}
