package com.okestro.okchat.permission.controller

import com.okestro.okchat.permission.model.DocumentPathPermission
import com.okestro.okchat.permission.service.DocumentPermissionService
import com.okestro.okchat.permission.service.PermissionService
import com.okestro.okchat.user.model.User
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
 * Admin API for managing document permissions (Path-based only)
 *
 * Note: This is a basic CRUD API without authentication/authorization
 * In production, you should add proper admin authentication
 */
@RestController
@RequestMapping("/api/admin/permissions")
class PermissionController(
    private val permissionService: PermissionService,
    private val userService: UserService,
) {

    /**
     * Get all permissions for a user
     * Used by: manage.html, user-detail.html
     */
    @GetMapping("/user/{email}")
    fun getUserPermissions(@PathVariable email: String): ResponseEntity<UserPermissionsResponse> {
        log.info { "Get user permissions request: email=$email" }

        val user = userService.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val pathPermissions = permissionService.getUserPathPermissions(user.id!!)

        return ResponseEntity.ok(
            UserPermissionsResponse(
                user = user,
                pathPermissions = pathPermissions,
                totalDocuments = pathPermissions.size
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
     * Grant bulk path permissions
     * Used by: manage.html, user-detail.html
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
     * Revoke bulk path permissions
     * Used by: manage.html, user-detail.html
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
     * Grant DENY permissions to multiple paths at once
     * Used by: user-detail.html
     * Use this to exclude specific paths from broader READ permissions
     */
    @PostMapping("/path/bulk/deny")
    fun grantBulkDenyPathPermissions(@RequestBody request: BulkGrantPathPermissionRequest): ResponseEntity<BulkPermissionResponse> {
        log.info { "Bulk grant DENY path permission request: user_email=${request.userEmail}, paths=${request.documentPaths.size}" }

        val user = userService.findByEmail(request.userEmail)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BulkPermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        val grantedCount = permissionService.grantBulkDenyPathPermissions(
            userId = user.id!!,
            documentPaths = request.documentPaths,
            spaceKey = request.spaceKey,
            grantedBy = null
        )

        return ResponseEntity.ok(
            BulkPermissionResponse(
                success = true,
                message = "Bulk DENY path permissions granted",
                grantedCount = grantedCount,
                totalRequested = request.documentPaths.size
            )
        )
    }
}

// Request/Response DTOs
data class BulkGrantPathPermissionRequest(
    val userEmail: String,
    val documentPaths: List<String>,
    val spaceKey: String? = null
)

data class RevokeBulkPathPermissionRequest(
    val userEmail: String,
    val documentPaths: List<String>
)

data class PermissionResponse(
    val success: Boolean,
    val message: String
)

data class BulkPermissionResponse(
    val success: Boolean,
    val message: String,
    val grantedCount: Int? = null,
    val totalRequested: Int? = null
)

data class UserPermissionsResponse(
    val user: User,
    val pathPermissions: List<DocumentPathPermission> = emptyList(),
    val totalDocuments: Int
)
