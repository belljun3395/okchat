package com.okestro.okchat.knowledge.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "knowledge_bases")
data class KnowledgeBase(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(length = 500)
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: KnowledgeBaseType,

    @Column(name = "config", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    val config: Map<String, Any> = emptyMap(),

    @Column(nullable = false)
    val enabled: Boolean = true,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long = 0L, // Default for migration

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class KnowledgeBaseType {
    CONFLUENCE,
    ETC
}
