package com.okestro.okchat.knowledge.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "knowledge_base_users",
    indexes = [
        Index(name = "idx_kb_user_user", columnList = "user_id"),
        Index(name = "idx_kb_user_kb", columnList = "knowledge_base_id"),
        Index(name = "uk_kb_user", columnList = "user_id, knowledge_base_id", unique = true)
    ]
)
data class KnowledgeBaseUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "knowledge_base_id", nullable = false)
    val knowledgeBaseId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: KnowledgeBaseUserRole = KnowledgeBaseUserRole.MEMBER,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class KnowledgeBaseUserRole {
    MEMBER, // Can view general docs
    ADMIN // Can view all docs (ignore DENY) and manage members
}
