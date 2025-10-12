package com.okestro.okchat.permission.model.entity

import com.okestro.okchat.permission.model.PermissionLevel
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

@Entity
@Table(
    name = "document_path_permissions",
    indexes = [
        Index(name = "idx_path_perm_user", columnList = "user_id"),
        Index(name = "idx_path_perm_path", columnList = "document_path"),
        Index(name = "idx_path_perm_user_path", columnList = "user_id,document_path")
    ]
)
data class DocumentPathPermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * User ID from users table
     */
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    /**
     * Document path for hierarchical permission management
     * Example: "팀회의 > 2025 > 1월", "개발팀 > 프로젝트A"
     * Grants access to all documents under this path
     */
    @Column(name = "document_path", nullable = false, length = 500)
    val documentPath: String,

    /**
     * Optional: Space key for easier management
     */
    @Column(name = "space_key", length = 50)
    val spaceKey: String? = null,

    /**
     * Permission level for this path
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
)
