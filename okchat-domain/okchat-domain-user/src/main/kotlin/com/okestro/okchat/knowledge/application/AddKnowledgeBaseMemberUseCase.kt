package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.AddKnowledgeBaseMemberUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUserRole
import com.okestro.okchat.knowledge.repository.KnowledgeBaseUserRepository
import com.okestro.okchat.user.model.entity.UserRole
import com.okestro.okchat.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AddKnowledgeBaseMemberUseCase(
    private val userRepository: UserRepository,
    private val knowledgeBaseUserRepository: KnowledgeBaseUserRepository
) {

    @Transactional("transactionManager")
    fun execute(input: AddKnowledgeBaseMemberUseCaseIn) {
        val caller = userRepository.findByEmail(input.callerEmail)
            ?: throw IllegalArgumentException("Caller not found")

        if (!canManageKb(caller, input.kbId)) {
            throw IllegalAccessException("Insufficient permissions")
        }

        val targetUser = userRepository.findByEmail(input.targetEmail)
            ?: throw NoSuchElementException("Target user not found")

        val targetUserId = requireNotNull(targetUser.id) { "Target user ID must not be null" }

        val existing = knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(targetUserId, input.kbId)
        if (existing != null) {
            throw IllegalArgumentException("User is already a member")
        }

        val newMember = KnowledgeBaseUser(
            userId = targetUserId,
            knowledgeBaseId = input.kbId,
            role = input.role,
            approvedBy = caller.id,
            createdAt = Instant.now()
        )
        knowledgeBaseUserRepository.save(newMember)
    }

    private fun canManageKb(caller: com.okestro.okchat.user.model.entity.User, kbId: Long): Boolean {
        // 1. System Admin
        if (caller.role == UserRole.SYSTEM_ADMIN) return true

        // 2. Space Admin
        val membership = knowledgeBaseUserRepository.findByUserIdAndKnowledgeBaseId(caller.id!!, kbId)
        return membership?.role == KnowledgeBaseUserRole.ADMIN
    }
}
