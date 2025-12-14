package com.okestro.okchat.knowledge.repository

import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KnowledgeBaseUserRepository : JpaRepository<KnowledgeBaseUser, Long> {
    fun findByUserId(userId: Long): List<KnowledgeBaseUser>
    fun findByKnowledgeBaseId(knowledgeBaseId: Long): List<KnowledgeBaseUser>
    fun findByUserIdAndKnowledgeBaseId(userId: Long, knowledgeBaseId: Long): KnowledgeBaseUser?
    fun deleteByKnowledgeBaseIdAndUserId(knowledgeBaseId: Long, userId: Long)
}
