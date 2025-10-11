package com.okestro.okchat.prompt.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "prompts",
    indexes = [
        Index(name = "idx_prompt_type_version", columnList = "type,version"),
        Index(name = "idx_prompt_type_active", columnList = "type,is_active")
    ]
)
data class Prompt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    val type: String,

    @Column(nullable = false)
    val version: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "is_active", nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
