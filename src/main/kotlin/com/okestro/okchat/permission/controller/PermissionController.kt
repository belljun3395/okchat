package com.okestro.okchat.permission.controller

import com.okestro.okchat.permission.application.GrantDenyPathPermissionUseCase
import com.okestro.okchat.permission.application.GrantPathPermissionUseCase
import com.okestro.okchat.permission.application.GetUserPermissionsUseCase
import com.okestro.okchat.permission.application.RevokeAllUserPermissionsUseCase
import com.okestro.okchat.permission.application.RevokePathPermissionUseCase
import com.okestro.okchat.permission.application.dto.GrantDenyPathPermissionUseCaseIn
import com.okestro.okchat.permission.application.dto.GrantPathPermissionUseCaseIn
import com.okestro.okchat.permission.application.dto.GetUserPermissionsUseCaseIn
import com.okestro.okchat.permission.application.dto.RevokeAllUserPermissionsUseCaseIn
import com.okestro.okchat.permission.application.dto.RevokePathPermissionUseCaseIn
import com.okestro.okchat.permission.model.DocumentPathPermission
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

@RestController
@RequestMapping("/api/admin/permissions")
@Tag(
    name = "Permission API",
    description = "문서 권한 관리 API. 사용자별로 문서 경로 기반 권한을 부여하거나 취소할 수 있습니다."
)
class PermissionController(
    private val findUserByEmailUseCase: FindUserByEmailUseCase,
    private val getUserPermissionsUseCase: GetUserPermissionsUseCase,
    private val revokeAllUserPermissionsUseCase: RevokeAllUserPermissionsUseCase,
    private val grantPathPermissionUseCase: GrantPathPermissionUseCase,
    private val grantDenyPathPermissionUseCase: GrantDenyPathPermissionUseCase,
    private val revokePathPermissionUseCase: RevokePathPermissionUseCase
) {

    @GetMapping("/user/{email}")
    @Operation(summary = "사용자 권한 조회")
    fun getUserPermissions(@PathVariable email: String): ResponseEntity<UserPermissionsResponse> {
        log.info { "Get user permissions request: email=$email" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(email)).user
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val pathPermissions = getUserPermissionsUseCase.execute(GetUserPermissionsUseCaseIn(user.id!!)).permissions

        return ResponseEntity.ok(
            UserPermissionsResponse(
                user = user,
                pathPermissions = pathPermissions,
                totalDocuments = pathPermissions.size
            )
        )
    }

    @DeleteMapping("/user/{email}")
    @Operation(summary = "사용자 전체 권한 취소")
    fun revokeAllPermissionsForUser(@PathVariable email: String): ResponseEntity<PermissionResponse> {
        log.info { "Revoke all permissions for user: email=$email" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(email)).user
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PermissionResponse(success = false, message = "User not found: $email"))

        revokeAllUserPermissionsUseCase.execute(RevokeAllUserPermissionsUseCaseIn(user.id!!))

        return ResponseEntity.ok(
            PermissionResponse(success = true, message = "All permissions revoked for user")
        )
    }

    @PostMapping("/path/bulk")
    @Operation(summary = "경로 권한 일괄 부여")
    fun grantBulkPathPermissions(@RequestBody request: BulkGrantPathPermissionRequest): ResponseEntity<BulkPermissionResponse> {
        log.info { "Bulk grant path permission request: user_email=${request.userEmail}, paths=${request.documentPaths.size}" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(request.userEmail)).user
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BulkPermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        var grantedCount = 0
        request.documentPaths.forEach {
            try {
                grantPathPermissionUseCase.execute(
                    GrantPathPermissionUseCaseIn(user.id!!, it, request.spaceKey)
                )
                grantedCount++
            } catch (e: Exception) {
                log.warn { "Failed to grant path permission: user_id=${user.id}, path=$it, error=${e.message}" }
            }
        }

        return ResponseEntity.ok(
            BulkPermissionResponse(
                success = true,
                message = "Bulk path permissions granted",
                grantedCount = grantedCount,
                totalRequested = request.documentPaths.size
            )
        )
    }

    @DeleteMapping("/path/bulk")
    @Operation(summary = "경로 권한 일괄 취소")
    fun revokeBulkPathPermissions(@RequestBody request: RevokeBulkPathPermissionRequest): ResponseEntity<PermissionResponse> {
        log.info { "Revoke bulk path permissions request: user_email=${request.userEmail}, count=${request.documentPaths.size}" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(request.userEmail)).user
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        revokePathPermissionUseCase.execute(RevokePathPermissionUseCaseIn(user.id!!, request.documentPaths))

        return ResponseEntity.ok(
            PermissionResponse(success = true, message = "Bulk path permissions revoked")
        )
    }

    @PostMapping("/path/bulk/deny")
    @Operation(summary = "경로 권한 일괄 거부")
    fun grantBulkDenyPathPermissions(@RequestBody request: BulkGrantPathPermissionRequest): ResponseEntity<BulkPermissionResponse> {
        log.info { "Bulk grant DENY path permission request: user_email=${request.userEmail}, paths=${request.documentPaths.size}" }

        val user = findUserByEmailUseCase.execute(FindUserByEmailUseCaseIn(request.userEmail)).user
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BulkPermissionResponse(success = false, message = "User not found: ${request.userEmail}"))

        var grantedCount = 0
        request.documentPaths.forEach {
            try {
                grantDenyPathPermissionUseCase.execute(
                    GrantDenyPathPermissionUseCaseIn(user.id!!, it, request.spaceKey)
                )
                grantedCount++
            } catch (e: Exception) {
                log.warn { "Failed to grant DENY path permission: user_id=${user.id}, path=$it, error=${e.message}" }
            }
        }

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
