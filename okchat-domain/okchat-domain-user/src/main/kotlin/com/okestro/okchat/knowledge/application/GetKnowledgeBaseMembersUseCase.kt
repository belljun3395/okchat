package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.GetKnowledgeBaseMembersUseCaseIn
import com.okestro.okchat.knowledge.application.dto.KnowledgeBaseMemberDto
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetKnowledgeBaseMembersUseCase(
    private val userRepository: UserRepository,
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository
) {

    @Transactional(readOnly = true, transactionManager = "transactionManager")
    fun execute(input: GetKnowledgeBaseMembersUseCaseIn): List<KnowledgeBaseMemberDto> {
        val caller = userRepository.findByEmail(input.callerEmail)
            ?: throw IllegalArgumentException("Caller not found")

        // Check Permission
        if (!canManageKb(caller, input.kbId)) {
            throw IllegalAccessException("Insufficient permissions")
        }

        val members = knowledgeBaseUserRepository.findByKnowledgeBaseId(input.kbId)

        val userIds = members.map { it.userId }.toSet()
        val approverIds = members.mapNotNull { it.approvedBy }.toSet()
        val allUserIds = userIds + approverIds

        val users = userRepository.findAllById(allUserIds).associateBy { it.id }

        return members.map { member ->
            val user = users[member.userId]
            val approver = member.approvedBy?.let { users[it] }

            KnowledgeBaseMemberDto(
                userId = member.userId,
                email = user?.email ?: "Unknown",
                name = user?.name ?: "Unknown",
                role = member.role,
                createdAt = member.createdAt,
                approvedBy = approver?.name
            )
        }
    }

    private fun canManageKb(caller: com.okestro.okchat.user.model.entity.User, kbId: Long): Boolean {
        // 1. System Admin
        if (caller.role == UserRole.SYSTEM_ADMIN) return true

        // 2. Space Admin
        val membership = knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId)
        return membership?.role == KnowledgeBaseUserRole.ADMIN
    }
}
