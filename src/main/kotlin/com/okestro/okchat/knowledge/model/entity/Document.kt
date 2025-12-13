package com.okestro.okchat.knowledge.model.entity

import com.github.f4b6a3.tsid.TsidCreator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "documents",
    indexes = [
        Index(name = "idx_doc_kb_id", columnList = "knowledge_base_id"),
        Index(name = "idx_doc_external_id", columnList = "external_id"),
        Index(name = "idx_doc_path", columnList = "path")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_doc_kb_external", columnNames = ["knowledge_base_id", "external_id"])
    ]
)
data class Document(
    @Id
    @Column(length = 13) // TSID length
    val id: String = TsidCreator.getTsid().toString(),

    @Column(name = "knowledge_base_id", nullable = false)
    val knowledgeBaseId: Long,

    @Column(name = "external_id", nullable = false, length = 255)
    val externalId: String,

    @Column(nullable = false, length = 500)
    val title: String,

    @Column(nullable = false, length = 1000)
    val path: String,

    @Column(name = "web_url", length = 1000)
    val webUrl: String? = null,

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    val metadata: Map<String, Any> = emptyMap(),

    @Column(name = "last_synced_at", nullable = false)
    val lastSyncedAt: Instant = Instant.now()
)
