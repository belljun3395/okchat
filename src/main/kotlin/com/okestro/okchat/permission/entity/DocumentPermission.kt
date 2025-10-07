package com.okestro.okchat.permission.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * Document-level permission entity
 * Maps which users can access which documents
 *
 * Design:
 * - Whitelist approach: Only explicitly granted users can access documents
 * - Document ID refers to Confluence page ID (stored in OpenSearch metadata)
 * - Path-based permissions: Grant access to all documents under a specific path
 * - Permission changes don't require re-indexing documents
 *
 * Permission Types:
 * 1. Document-specific: documentId set, documentPath null
 * 2. Path-based: documentPath set, documentId can be null or wildcard
 */
@Entity
@Table(
    name = "document_permissions",
    indexes = [
        Index(name = "idx_doc_perm_user", columnList = "user_id"),
        Index(name = "idx_doc_perm_document", columnList = "document_id"),
        Index(name = "idx_doc_perm_path", columnList = "document_path"),
        Index(name = "idx_doc_perm_user_doc", columnList = "user_id,document_id"),
        Index(name = "idx_doc_perm_user_path", columnList = "user_id,document_path")
    ]
)
data class DocumentPermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * User ID from users table
     */
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    /**
     * Document ID (Confluence page ID)
     * Example: "12345678" or "12345678_chunk_0" for chunked documents
     * For path-based permissions, this can be null or wildcard "*"
     */
    @Column(name = "document_id", length = 100)
    val documentId: String? = null,

    /**
     * Document path for hierarchical permission management
     * Example: "팀회의 > 2025 > 1월", "개발팀 > 프로젝트A"
     * When set, grants access to all documents under this path
     */
    @Column(name = "document_path", length = 500)
    val documentPath: String? = null,

    /**
     * Optional: Space key for easier management
     */
    @Column(name = "space_key", length = 50)
    val spaceKey: String? = null,

    /**
     * Permission level (for future extensibility)
     * Currently: READ (can view/search)
     * Future: WRITE, ADMIN, etc.
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val permissionLevel: PermissionLevel = PermissionLevel.READ,

    @Column(name = "granted_at", nullable = false)
    val grantedAt: Instant = Instant.now(),

    /**
     * Optional: Who granted this permission (for audit)
     */
    @Column(name = "granted_by")
    val grantedBy: Long? = null
) {
    init {
        require(documentId != null || documentPath != null) {
            "Either documentId or documentPath must be set"
        }
    }

    /**
     * Check if this is a path-based permission
     */
    fun isPathBased(): Boolean = documentPath != null

    /**
     * Check if this is a document-specific permission
     */
    fun isDocumentSpecific(): Boolean = documentId != null && documentPath == null
}

enum class PermissionLevel {
    READ, // Can view and search
    WRITE, // Future: Can edit
    ADMIN // Future: Can manage permissions
}
