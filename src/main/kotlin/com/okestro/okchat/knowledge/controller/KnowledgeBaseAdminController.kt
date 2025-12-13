package com.okestro.okchat.knowledge.controller

import com.okestro.okchat.knowledge.application.AddKnowledgeBaseMemberUseCase
import com.okestro.okchat.knowledge.application.CreateKnowledgeBaseUseCase
import com.okestro.okchat.knowledge.application.GetAllKnowledgeBasesUseCase
import com.okestro.okchat.knowledge.application.GetKnowledgeBaseDetailUseCase
import com.okestro.okchat.knowledge.application.GetKnowledgeBaseMembersUseCase
import com.okestro.okchat.knowledge.application.RemoveKnowledgeBaseMemberUseCase
import com.okestro.okchat.knowledge.application.UpdateKnowledgeBaseUseCase
import com.okestro.okchat.knowledge.application.dto.AddKnowledgeBaseMemberUseCaseIn
import com.okestro.okchat.knowledge.application.dto.CreateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.application.dto.GetAllKnowledgeBasesUseCaseIn
import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseDetailUseCaseIn
import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseMembersUseCaseIn
import com.okestro.okchat.knowledge.application.dto.RemoveKnowledgeBaseMemberUseCaseIn
import com.okestro.okchat.knowledge.application.dto.UpdateKnowledgeBaseUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/admin/knowledge-bases")
@Tag(name = "Knowledge Base Admin API", description = "Knowledge Base 관리 및 멤버십 관리 API")
class KnowledgeBaseAdminController(
    private val getKnowledgeBaseDetailUseCase: GetKnowledgeBaseDetailUseCase,
    private val getAllKnowledgeBasesUseCase: GetAllKnowledgeBasesUseCase,
    private val createKnowledgeBaseUseCase: CreateKnowledgeBaseUseCase,
    private val updateKnowledgeBaseUseCase: UpdateKnowledgeBaseUseCase,
    private val getKnowledgeBaseMembersUseCase: GetKnowledgeBaseMembersUseCase,
    private val addKnowledgeBaseMemberUseCase: AddKnowledgeBaseMemberUseCase,
    private val removeKnowledgeBaseMemberUseCase: RemoveKnowledgeBaseMemberUseCase
) {

    @GetMapping("/{kbId}")
    @Operation(summary = "Knowledge Base 상세 조회")
    fun getKnowledgeBaseDetail(
        @PathVariable kbId: Long,
        @RequestParam callerEmail: String
    ): ResponseEntity<Any> {
        return try {
            val result = getKnowledgeBaseDetailUseCase.execute(
                GetKnowledgeBaseDetailUseCaseIn(kbId, callerEmail)
            )

            // Map DTO to Response
            val response = KnowledgeBaseResponse(
                id = result.id,
                name = result.name,
                description = result.description,
                type = result.type,
                enabled = result.enabled,
                createdBy = result.createdBy,
                createdAt = result.createdAt,
                updatedAt = result.updatedAt,
                config = result.config
            )
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.message)
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    @Operation(summary = "모든 Knowledge Base 목록 조회")
    fun getAllKnowledgeBases(): ResponseEntity<List<KnowledgeBaseResponse>> {
        val kbs = getAllKnowledgeBasesUseCase.execute(GetAllKnowledgeBasesUseCaseIn())
        val response = kbs.map { kb ->
            KnowledgeBaseResponse(
                id = kb.id,
                name = kb.name,
                description = kb.description,
                type = kb.type,
                enabled = kb.enabled,
                createdBy = kb.createdBy,
                createdAt = kb.createdAt,
                updatedAt = kb.updatedAt,
                config = kb.config
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
        return try {
            val savedKb = createKnowledgeBaseUseCase.execute(
                CreateKnowledgeBaseUseCaseIn(
                    callerEmail = callerEmail,
                    name = request.name,
                    description = request.description,
                    type = request.type,
                    config = request.config
                )
            )
            ResponseEntity.ok(savedKb)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.message)
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        }
    }

    @GetMapping("/{kbId}/members")
    @Operation(summary = "KB 멤버 목록 조회")
    fun getMembers(
        @PathVariable kbId: Long,
        @RequestParam callerEmail: String
    ): ResponseEntity<Any> {
        return try {
            val members = getKnowledgeBaseMembersUseCase.execute(
                GetKnowledgeBaseMembersUseCaseIn(kbId, callerEmail)
            )

            val response = members.map { member ->
                KnowledgeBaseMemberResponse(
                    userId = member.userId,
                    email = member.email,
                    name = member.name,
                    role = member.role,
                    createdAt = member.createdAt,
                    approvedBy = member.approvedBy
                )
            }
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.message)
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        }
    }

    @PostMapping("/{kbId}/members")
    @Operation(summary = "KB 멤버 추가")
    fun addMember(
        @PathVariable kbId: Long,
        @RequestParam callerEmail: String,
        @RequestBody request: AddMemberRequest
    ): ResponseEntity<Any> {
        return try {
            addKnowledgeBaseMemberUseCase.execute(
                AddKnowledgeBaseMemberUseCaseIn(
                    kbId = kbId,
                    callerEmail = callerEmail,
                    targetEmail = request.email,
                    role = request.role
                )
            )
            ResponseEntity.ok("Member added")
        } catch (e: IllegalArgumentException) {
            // Distinguish between caller not found vs member already exists vs illegal argument
            if (e.message == "User is already a member") {
                ResponseEntity.badRequest().body(e.message)
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.message)
            }
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }

    @DeleteMapping("/{kbId}/members/{userId}")
    @Operation(summary = "KB 멤버 제거")
    fun removeMember(
        @PathVariable kbId: Long,
        @PathVariable userId: Long,
        @RequestParam callerEmail: String
    ): ResponseEntity<Any> {
        return try {
            removeKnowledgeBaseMemberUseCase.execute(
                RemoveKnowledgeBaseMemberUseCaseIn(
                    kbId = kbId,
                    callerEmail = callerEmail,
                    targetUserId = userId
                )
            )
            ResponseEntity.ok("Member removed")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.message)
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }

    @PutMapping("/{kbId}")
    @Operation(summary = "Knowledge Base 수정")
    fun updateKnowledgeBase(
        @PathVariable kbId: Long,
        @RequestParam callerEmail: String,
        @RequestBody request: UpdateKnowledgeBaseRequest
    ): ResponseEntity<Any> {
        return try {
            val savedKb = updateKnowledgeBaseUseCase.execute(
                UpdateKnowledgeBaseUseCaseIn(
                    kbId = kbId,
                    callerEmail = callerEmail,
                    name = request.name,
                    description = request.description,
                    type = request.type,
                    config = request.config
                )
            )
            ResponseEntity.ok(savedKb)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.message)
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }
}

data class AddMemberRequest(
    val email: String,
    val role: KnowledgeBaseUserRole = KnowledgeBaseUserRole.MEMBER
)

data class CreateKnowledgeBaseRequest(
    val name: String,
    val description: String? = null,
    val type: KnowledgeBaseType,
    val config: Map<String, Any> = emptyMap()
)

data class UpdateKnowledgeBaseRequest(
    val name: String,
    val description: String? = null,
    val type: KnowledgeBaseType,
    val config: Map<String, Any> = emptyMap()
)

data class KnowledgeBaseResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val type: KnowledgeBaseType,
    val enabled: Boolean,
    val createdBy: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val config: Map<String, Any> = emptyMap()
)

data class KnowledgeBaseMemberResponse(
    val userId: Long,
    val email: String,
    val name: String,
    val role: KnowledgeBaseUserRole,
    val createdAt: Instant,
    val approvedBy: String?
)
