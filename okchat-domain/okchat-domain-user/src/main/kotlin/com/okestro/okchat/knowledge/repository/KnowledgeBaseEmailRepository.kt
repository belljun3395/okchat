package com.okestro.okchat.knowledge.repository

import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseEmail
import org.springframework.data.jpa.repository.JpaRepository

interface KnowledgeBaseEmailRepository : JpaRepository<KnowledgeBaseEmail, Long> {
    fun findAllByKnowledgeBaseId(knowledgeBaseId: Long): List<KnowledgeBaseEmail>
    fun deleteByKnowledgeBaseId(knowledgeBaseId: Long)
}
