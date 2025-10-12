package com.okestro.okchat.permission.controller

import com.okestro.okchat.permission.model.DocumentPathPermission
import com.okestro.okchat.permission.service.PermissionService
import com.okestro.okchat.user.application.FindUserByEmailUseCase
import com.okestro.okchat.user.application.dto.FindUserByEmailUseCaseIn
import com.okestro.okchat.user.model.User
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(
    name = "Permission API",
    description = "문서 권한 관리 API. 사용자별로 문서 경로 기반 권한을 부여하거나 취소할 수 있습니다."
)
class PermissionController(
    private val permissionService: PermissionService,
    private val findUserByEmailUseCase: FindUserByEmailUseCase
) {

    /**
     * Get all permissions for a user
     * Used by: manage.html, user-detail.html
     */
    @GetMapping("/user/{email}")
    @Operation(
        summary = "사용자 권한 조회",
        description = "특정 사용자의 모든 문서 권한을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        ]
    )
    fun getUserPermissions(
        @Parameter(description = "사용자 이메일", example = "user@example.com", required = true)
        @PathVariable
        email: String
    ): ResponseEntity<UserPermissionsResponse> {
        log.info { "Get user permissions request: email=$email" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(email)).user
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
    @Operation(
        summary = "사용자 전체 권한 취소",
        description = "특정 사용자의 모든 권한을 취소합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "취소 성공"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        ]
    )
    fun revokeAllPermissionsForUser(
        @Parameter(description = "사용자 이메일", example = "user@example.com", required = true)
        @PathVariable
        email: String
    ): ResponseEntity<PermissionResponse> {
        log.info { "Revoke all permissions for user: email=$email" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(email)).user
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
    @Operation(
        summary = "경로 권한 일괄 부여",
        description = "사용자에게 여러 문서 경로에 대한 읽기 권한을 일괄 부여합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "부여 성공"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        ]
    )
    fun grantBulkPathPermissions(
        @Parameter(description = "권한 부여 요청", required = true)
        @RequestBody
        request: BulkGrantPathPermissionRequest
    ): ResponseEntity<BulkPermissionResponse> {
        log.info { "Bulk grant path permission request: user_email=${request.userEmail}, paths=${request.documentPaths.size}" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(request.userEmail)).user
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
    @Operation(
        summary = "경로 권한 일괄 취소",
        description = "사용자의 여러 문서 경로 권한을 일괄 취소합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "취소 성공"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        ]
    )
    fun revokeBulkPathPermissions(
        @Parameter(description = "권한 취소 요청", required = true)
        @RequestBody
        request: RevokeBulkPathPermissionRequest
    ): ResponseEntity<PermissionResponse> {
        log.info { "Revoke bulk path permissions request: user_email=${request.userEmail}, count=${request.documentPaths.size}" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(request.userEmail)).user
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
    @Operation(
        summary = "경로 권한 일괄 거부",
        description = "사용자에게 여러 문서 경로에 대한 거부 권한을 일괄 부여합니다. 넓은 범위의 READ 권한에서 특정 경로를 제외할 때 사용합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "부여 성공"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        ]
    )
    fun grantBulkDenyPathPermissions(
        @Parameter(description = "거부 권한 부여 요청", required = true)
        @RequestBody
        request: BulkGrantPathPermissionRequest
    ): ResponseEntity<BulkPermissionResponse> {
        log.info { "Bulk grant DENY path permission request: user_email=${request.userEmail}, paths=${request.documentPaths.size}" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(request.userEmail)).user
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
@Schema(description = "권한 일괄 부여 요청")
data class BulkGrantPathPermissionRequest(
    @field:Schema(description = "사용자 이메일", example = "user@example.com", required = true)
    val userEmail: String,

    @field:Schema(description = "문서 경로 목록", example = "[\"/space/page1\", \"/space/page2\"]", required = true)
    val documentPaths: List<String>,

    @field:Schema(description = "스페이스 키 (선택사항)", example = "PROJ", required = false)
    val spaceKey: String? = null
)

@Schema(description = "권한 일괄 취소 요청")
data class RevokeBulkPathPermissionRequest(
    @field:Schema(description = "사용자 이메일", example = "user@example.com", required = true)
    val userEmail: String,

    @field:Schema(description = "문서 경로 목록", example = "[\"/space/page1\", \"/space/page2\"]", required = true)
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
