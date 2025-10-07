package com.okestro.okchat.permission.controller

import com.okestro.okchat.permission.entity.DocumentPermission
import com.okestro.okchat.permission.service.PermissionService
import com.okestro.okchat.user.entity.User
import com.okestro.okchat.user.service.UserService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Admin API for managing document permissions
 *
 * Note: This is a basic CRUD API without authentication/authorization
 * In production, you should add proper admin authentication
 */
@RestController
@RequestMapping("/api/admin/permissions")
class PermissionController(
    private val permissionService: PermissionService,
    private val userService: UserService
) {

    /**
     * Grant permission to a user for a document
     */
    @PostMapping
    fun grantPermission(@RequestBody request: GrantPermissionRequest): ResponseEntity<PermissionResponse> {
        log.info { "Grant permission request: user_email=${request.userEmail}, document_id=${request.documentId}" }

        val user = userService.findByEmail(request.userEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        val permission = permissionService.grantPermission(
            userId = user.id!!,
            documentId = request.documentId,
            spaceKey = request.spaceKey,
            grantedBy = null // TODO: Add admin user context
        )

        return ResponseEntity.ok(
            PermissionResponse(
                success = true,
                message = "Permission granted",
                permission = permission
            )
        )
    }

    /**
     * Grant bulk permissions (multiple documents to one user)
     */
    @PostMapping("/bulk")
    fun grantBulkPermissions(@RequestBody request: BulkGrantPermissionRequest): ResponseEntity<BulkPermissionResponse> {
        log.info { "Bulk grant permission request: user_email=${request.userEmail}, documents=${request.documentIds.size}" }

        val user = userService.findByEmail(request.userEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BulkPermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        val grantedCount = permissionService.grantBulkPermissions(
            userId = user.id!!,
            documentIds = request.documentIds,
            spaceKey = request.spaceKey,
            grantedBy = null
        )

        return ResponseEntity.ok(
            BulkPermissionResponse(
                success = true,
                message = "Bulk permissions granted",
                grantedCount = grantedCount,
                totalRequested = request.documentIds.size
            )
        )
    }

    /**
     * Revoke permission
     */
    @DeleteMapping
    fun revokePermission(@RequestBody request: RevokePermissionRequest): ResponseEntity<PermissionResponse> {
        log.info { "Revoke permission request: user_email=${request.userEmail}, document_id=${request.documentId}" }

        val user = userService.findByEmail(request.userEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        permissionService.revokePermission(user.id!!, request.documentId)

        return ResponseEntity.ok(
            PermissionResponse(success = true, message = "Permission revoked")
        )
    }

    /**
     * Revoke path-based permission
     */
    @DeleteMapping("/path")
    fun revokePathPermission(@RequestBody request: RevokePathPermissionRequest): ResponseEntity<PermissionResponse> {
        log.info { "Revoke path permission request: user_email=${request.userEmail}, path=${request.documentPath}" }

        val user = userService.findByEmail(request.userEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        permissionService.revokePathPermission(user.id!!, request.documentPath)

        return ResponseEntity.ok(
            PermissionResponse(success = true, message = "Path permission revoked")
        )
    }

    /**
     * Revoke bulk document permissions
     */
    @DeleteMapping("/bulk")
    fun revokeBulkDocumentPermissions(@RequestBody request: RevokeBulkPermissionRequest): ResponseEntity<PermissionResponse> {
        log.info { "Revoke bulk document permissions request: user_email=${request.userEmail}, count=${request.documentIds.size}" }

        val user = userService.findByEmail(request.userEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        permissionService.revokeBulkDocumentPermissions(user.id!!, request.documentIds)

        return ResponseEntity.ok(
            PermissionResponse(success = true, message = "Bulk document permissions revoked")
        )
    }

    /**
     * Revoke bulk path permissions
     */
    @DeleteMapping("/path/bulk")
    fun revokeBulkPathPermissions(@RequestBody request: RevokeBulkPathPermissionRequest): ResponseEntity<PermissionResponse> {
        log.info { "Revoke bulk path permissions request: user_email=${request.userEmail}, count=${request.documentPaths.size}" }

        val user = userService.findByEmail(request.userEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        permissionService.revokeBulkPathPermissions(user.id!!, request.documentPaths)

        return ResponseEntity.ok(
            PermissionResponse(success = true, message = "Bulk path permissions revoked")
        )
    }

    /**
     * Get all permissions for a user
     */
    @GetMapping("/user/{email}")
    fun getUserPermissions(@PathVariable email: String): ResponseEntity<UserPermissionsResponse> {
        log.info { "Get user permissions request: email=$email" }

        val user = userService.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val permissions = permissionService.getUserPermissions(user.id!!)

        return ResponseEntity.ok(
            UserPermissionsResponse(
                user = user,
                permissions = permissions,
                totalDocuments = permissions.size
            )
        )
    }

    /**
     * Get all permissions for a document
     */
    @GetMapping("/document/{documentId}")
    fun getDocumentPermissions(@PathVariable documentId: String): ResponseEntity<DocumentPermissionsResponse> {
        log.info { "Get document permissions request: document_id=$documentId" }

        val permissions = permissionService.getDocumentPermissions(documentId)

        return ResponseEntity.ok(
            DocumentPermissionsResponse(
                documentId = documentId,
                permissions = permissions,
                totalUsers = permissions.size
            )
        )
    }

    /**
     * Revoke all permissions for a user
     */
    @DeleteMapping("/user/{email}")
    fun revokeAllPermissionsForUser(@PathVariable email: String): ResponseEntity<PermissionResponse> {
        log.info { "Revoke all permissions for user: email=$email" }

        val user = userService.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PermissionResponse(success = false, message = "User not found: $email"))

        permissionService.revokeAllPermissionsForUser(user.id!!)

        return ResponseEntity.ok(
            PermissionResponse(success = true, message = "All permissions revoked for user")
        )
    }

    /**
     * Revoke all permissions for a document
     */
    @DeleteMapping("/document/{documentId}")
    fun revokeAllPermissionsForDocument(@PathVariable documentId: String): ResponseEntity<PermissionResponse> {
        log.info { "Revoke all permissions for document: document_id=$documentId" }

        permissionService.revokeAllPermissionsForDocument(documentId)

        return ResponseEntity.ok(
            PermissionResponse(success = true, message = "All permissions revoked for document")
        )
    }

    /**
     * Grant path-based permission
     */
    @PostMapping("/path")
    fun grantPathPermission(@RequestBody request: GrantPathPermissionRequest): ResponseEntity<PermissionResponse> {
        log.info { "Grant path permission request: user_email=${request.userEmail}, path=${request.documentPath}" }

        val user = userService.findByEmail(request.userEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        val permission = permissionService.grantPathPermission(
            userId = user.id!!,
            documentPath = request.documentPath,
            spaceKey = request.spaceKey,
            grantedBy = null
        )

        return ResponseEntity.ok(
            PermissionResponse(
                success = true,
                message = "Path permission granted",
                permission = permission
            )
        )
    }

    /**
     * Grant bulk path permissions
     */
    @PostMapping("/path/bulk")
    fun grantBulkPathPermissions(@RequestBody request: BulkGrantPathPermissionRequest): ResponseEntity<BulkPermissionResponse> {
        log.info { "Bulk grant path permission request: user_email=${request.userEmail}, paths=${request.documentPaths.size}" }

        val user = userService.findByEmail(request.userEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BulkPermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        val grantedCount = permissionService.grantBulkPathPermissions(
            userId = user.id!!,
            documentPaths = request.documentPaths,
            spaceKey = request.spaceKey,
            grantedBy = null
        )

        return ResponseEntity.ok(
            BulkPermissionResponse(
                success = true,
                message = "Bulk path permissions granted",
                grantedCount = grantedCount,
                totalRequested = request.documentPaths.size
            )
        )
    }

    /**
     * Get all unique document paths from OpenSearch index
     */
    @GetMapping("/paths")
    fun getAllDocumentPaths(): ResponseEntity<PathListResponse> {
        log.info { "Get all document paths request" }
        val paths = permissionService.getAllDocumentPathsFromIndex()
        return ResponseEntity.ok(
            PathListResponse(
                paths = paths,
                totalCount = paths.size
            )
        )
    }

    /**
     * Get all document IDs under a specific path
     * Useful for bulk permission operations
     */
    @GetMapping("/paths/{path}/documents")
    fun getDocumentIdsByPath(@PathVariable path: String): ResponseEntity<DocumentIdsResponse> {
        log.info { "Get document IDs by path: $path" }

        val documents = permissionService.getDocumentsByPath(path)
        val documentIds = documents.map { it.id }

        return ResponseEntity.ok(
            DocumentIdsResponse(
                path = path,
                documentIds = documentIds,
                totalCount = documentIds.size
            )
        )
    }

    /**
     * Get permissions by path
     */
    @GetMapping("/path")
    fun getPathPermissions(@RequestBody request: GetPathPermissionsRequest): ResponseEntity<PathPermissionsResponse> {
        log.info { "Get path permissions request: path=${request.documentPath}" }

        val permissions = permissionService.getPathPermissions(request.documentPath)

        return ResponseEntity.ok(
            PathPermissionsResponse(
                documentPath = request.documentPath,
                permissions = permissions,
                totalUsers = permissions.size
            )
        )
    }

    /**
     * Get document content by ID for preview
     */
    @GetMapping("/document/{documentId}/content")
    fun getDocumentContent(@PathVariable documentId: String): ResponseEntity<DocumentContentResponse> {
        log.info { "Get document content request: document_id=$documentId" }

        val content = permissionService.getDocumentContent(documentId)

        return if (content != null) {
            ResponseEntity.ok(content)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get folder hierarchy with documents for permission management
     */
    @GetMapping("/hierarchy")
    fun getFolderHierarchy(): ResponseEntity<FolderHierarchyResponse> {
        log.info { "Get folder hierarchy request" }

        val hierarchy = permissionService.buildFolderHierarchy()

        return ResponseEntity.ok(
            FolderHierarchyResponse(
                folders = hierarchy
            )
        )
    }
}

// Additional Request/Response DTOs
data class GrantPathPermissionRequest(
    val userEmail: String,
    val documentPath: String,
    val spaceKey: String? = null
)

data class BulkGrantPathPermissionRequest(
    val userEmail: String,
    val documentPaths: List<String>,
    val spaceKey: String? = null
)

data class GetPathPermissionsRequest(
    val documentPath: String
)

data class PathListResponse(
    val paths: List<String>,
    val totalCount: Int
)

data class PathPermissionsResponse(
    val documentPath: String,
    val permissions: List<DocumentPermission>,
    val totalUsers: Int
)

data class DocumentIdsResponse(
    val path: String,
    val documentIds: List<String>,
    val totalCount: Int
)

// Request/Response DTOs
data class GrantPermissionRequest(
    val userEmail: String,
    val documentId: String,
    val spaceKey: String? = null
)

data class BulkGrantPermissionRequest(
    val userEmail: String,
    val documentIds: List<String>,
    val spaceKey: String? = null
)

data class RevokePermissionRequest(
    val userEmail: String,
    val documentId: String
)

data class PermissionResponse(
    val success: Boolean,
    val message: String,
    val permission: DocumentPermission? = null
)

data class BulkPermissionResponse(
    val success: Boolean,
    val message: String,
    val grantedCount: Int? = null,
    val totalRequested: Int? = null
)

data class UserPermissionsResponse(
    val user: User,
    val permissions: List<DocumentPermission>,
    val totalDocuments: Int
)

data class DocumentPermissionsResponse(
    val documentId: String,
    val permissions: List<DocumentPermission>,
    val totalUsers: Int
)

data class DocumentContentResponse(
    val documentId: String,
    val title: String,
    val content: String,
    val path: String,
    val spaceKey: String?
)

data class FolderNode(
    val name: String,
    val path: String,
    val children: List<FolderNode>,
    val documents: List<DocumentNode>
)

data class DocumentNode(
    val id: String,
    val title: String,
    val path: String
)

data class FolderHierarchyResponse(
    val folders: List<FolderNode>
)

data class RevokePathPermissionRequest(
    val userEmail: String,
    val documentPath: String
)

data class RevokeBulkPermissionRequest(
    val userEmail: String,
    val documentIds: List<String>
)

data class RevokeBulkPathPermissionRequest(
    val userEmail: String,
    val documentPaths: List<String>
)
