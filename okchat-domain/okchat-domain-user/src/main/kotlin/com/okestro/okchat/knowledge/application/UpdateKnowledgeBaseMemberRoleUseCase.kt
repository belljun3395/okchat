package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.UpdateKnowledgeBaseMemberRoleUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.User
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateKnowledgeBaseMemberRoleUseCase(
    private val userRepository: UserRepository,
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository
) {

    @Transactional("transactionManager")
    fun execute(input: UpdateKnowledgeBaseMemberRoleUseCaseIn) {
        val caller = userRepository.findByEmail(input.callerEmail)
            ?: throw IllegalArgumentException("Caller not found")

        if (!canManageKb(caller, input.kbId)) {
            throw IllegalAccessException("Insufficient permissions")
        }

        // Find the existing membership
        val membership = knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(input.targetUserId, input.kbId)
            ?: throw NoSuchElementException("Member not found")

        // Prevent user from changing their own role
        if (caller.id == input.targetUserId) {
            throw IllegalArgumentException("Cannot change your own role")
        }

        // Update role by deleting and re-creating (since entity is immutable)
        knowledgeBaseUserRepository.delete(membership)
        knowledgeBaseUserRepository.save(
            membership.copy(role = input.newRole)
        )
    }

    private fun canManageKb(caller: User, kbId: Long): Boolean {
        // 1. System Admin
        if (caller.role == UserRole.SYSTEM_ADMIN) return true

        // 2. KB Admin
        val membership = knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId)
        return membership?.role == KnowledgeBaseUserRole.ADMIN
    }
}
