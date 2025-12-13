package com.okestro.okchat.knowledge.repository

import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KnowledgeBaseRepository : JpaRepository<KnowledgeBase, Long> {
    fun findAllByEnabledTrueAndType(type: KnowledgeBaseType): List<KnowledgeBase>
    fun findAllByEnabledTrue(): List<KnowledgeBase>
}
