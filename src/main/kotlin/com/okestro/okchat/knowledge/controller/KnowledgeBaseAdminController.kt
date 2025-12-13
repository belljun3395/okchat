package com.okestro.okchat.knowledge.controller

import com.okestro.okchat.knowledge.model.dto.KnowledgeBaseMemberResponse
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/admin/knowledge-bases")
@Tag(name = "Knowledge Base Admin API", description = "Knowledge Base 관리 및 멤버십 관리 API")
class KnowledgeBaseAdminController(
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository,
    private val userRepository: UserRepository,
    private val knowledgeBaseRepository: com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
) {

    @GetMapping
    @Operation(summary = "모든 Knowledge Base 목록 조회")
    fun getAllKnowledgeBases(): ResponseEntity<List<KnowledgeBaseResponse>> {
        val kbs = knowledgeBaseRepository.findAll()
        val response = kbs.map { kb ->
            KnowledgeBaseResponse(
                id = kb.id!!,
                name = kb.name,
                description = kb.description,
                type = kb.type,
                enabled = kb.enabled,
                createdBy = kb.createdBy,
                createdAt = kb.createdAt,
                updatedAt = kb.updatedAt
            )
        }
        return ResponseEntity.ok(response)
    }

    @PostMapping
    @Operation(summary = "Knowledge Base 생성")
    fun createKnowledgeBase(
        @RequestParam callerEmail: String,
        @RequestBody request: CreateKnowledgeBaseRequest
    ): ResponseEntity<Any> {
        val caller = userRepository.findByEmail(callerEmail)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Caller not found")

        if (caller.role != UserRole.SYSTEM_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only System Admin can create Knowledge Bases")
        }

        val newKb = com.okestro.okchat.knowledge.model.entity.KnowledgeBase(
            name = request.name,
            description = request.description,
            type = request.type,
            config = request.config,
            createdBy = caller.id!!,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val savedKb = knowledgeBaseRepository.save(newKb)

        // Add creator as ADMIN of the KB
        val adminMember = com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser(
            userId = requireNotNull(caller.id) { "Caller ID must not be null" },
            knowledgeBaseId = savedKb.id!!,
            role = KnowledgeBaseUserRole.ADMIN,
            approvedBy = caller.id,
            createdAt = Instant.now()
        )
        knowledgeBaseUserRepository.save(adminMember)

        return ResponseEntity.ok(savedKb)
    }

    @GetMapping("/{kbId}/members")
    @Operation(summary = "KB 멤버 목록 조회")
    fun getMembers(
        @PathVariable kbId: Long,
        @RequestParam callerEmail: String
    ): ResponseEntity<Any> {
        val caller = userRepository.findByEmail(callerEmail)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Caller not found")

        // Check Permission
        if (!canManageKb(caller, kbId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Insufficient permissions")
        }

        val members = knowledgeBaseUserRepository.findByKnowledgeBaseId(kbId)

        val userIds = members.map { it.userId }.toSet()
        val approverIds = members.mapNotNull { it.approvedBy }.toSet()
        val allUserIds = userIds + approverIds

        val users = userRepository.findAllById(allUserIds).associateBy { it.id }

        val response = members.map { member ->
            val user = users[member.userId]
            val approver = member.approvedBy?.let { users[it] }

            KnowledgeBaseMemberResponse(
                userId = member.userId,
                email = user?.email ?: "Unknown",
                name = user?.name ?: "Unknown",
                role = member.role,
                createdAt = member.createdAt,
                approvedBy = approver?.name
            )
        }

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{kbId}/members")
    @Operation(summary = "KB 멤버 추가")
    fun addMember(
        @PathVariable kbId: Long,
        @RequestParam callerEmail: String,
        @RequestBody request: AddMemberRequest
    ): ResponseEntity<Any> {
        val caller = userRepository.findByEmail(callerEmail)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Caller not found")

        if (!canManageKb(caller, kbId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Insufficient permissions")
        }

        val targetUser = userRepository.findByEmail(request.email)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Target user not found")

        val existing = knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(targetUser.id!!, kbId)
        if (existing != null) {
            return ResponseEntity.badRequest().body("User is already a member")
        }

        val newMember = com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser(
            userId = targetUser.id,
            knowledgeBaseId = kbId,
            role = request.role,
            approvedBy = caller.id,
            createdAt = Instant.now()
        )
        knowledgeBaseUserRepository.save(newMember)

        return ResponseEntity.ok("Member added")
    }

    @DeleteMapping("/{kbId}/members/{userId}")
    @Operation(summary = "KB 멤버 제거")
    fun removeMember(
        @PathVariable kbId: Long,
        @PathVariable userId: Long,
        @RequestParam callerEmail: String
    ): ResponseEntity<Any> {
        val caller = userRepository.findByEmail(callerEmail)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Caller not found")

        if (!canManageKb(caller, kbId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Insufficient permissions")
        }

        val membership =
            knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(userId, kbId) ?: return ResponseEntity.status(
                HttpStatus.NOT_FOUND
            ).body("Member not found")

        knowledgeBaseUserRepository.delete(membership)
        return ResponseEntity.ok("Member removed")
    }

    private fun canManageKb(caller: com.okestro.okchat.user.model.entity.User, kbId: Long): Boolean {
        // 1. System Admin
        if (caller.role == UserRole.SYSTEM_ADMIN) return true

        // 2. Space Admin
        val membership = knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId)
        return membership?.role == KnowledgeBaseUserRole.ADMIN
    }
}

data class AddMemberRequest(
    val email: String,
    val role: KnowledgeBaseUserRole = KnowledgeBaseUserRole.MEMBER
)

data class CreateKnowledgeBaseRequest(
    val name: String,
    val description: String? = null,
    val type: com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType,
    val config: Map<String, Any> = emptyMap()
)

data class KnowledgeBaseResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val type: com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType,
    val enabled: Boolean,
    val createdBy: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)
