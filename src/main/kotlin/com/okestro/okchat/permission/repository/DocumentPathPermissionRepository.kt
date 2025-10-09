package com.okestro.okchat.permission.repository

import com.okestro.okchat.permission.model.DocumentPathPermission
import com.okestro.okchat.permission.model.PermissionLevel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

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
    fun deleteByUserId(userId: Long)

    /**
     * Delete multiple path permissions
     */
    fun deleteByUserIdAndDocumentPathIn(userId: Long, documentPaths: List<String>)
}
