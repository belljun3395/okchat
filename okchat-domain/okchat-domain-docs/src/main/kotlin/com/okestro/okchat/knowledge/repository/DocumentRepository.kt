package com.okestro.okchat.knowledge.repository

import com.okestro.okchat.knowledge.model.entity.Document
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<Document, String> {
    fun findByKnowledgeBaseIdAndExternalId(knowledgeBaseId: Long, externalId: String): Document?
    fun findAllByKnowledgeBaseId(knowledgeBaseId: Long): List<Document>
    fun deleteByKnowledgeBaseIdAndExternalIdIn(knowledgeBaseId: Long, externalIds: List<String>)
}
