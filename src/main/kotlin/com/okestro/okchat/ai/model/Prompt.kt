package com.okestro.okchat.ai.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("prompts")
data class Prompt(
    @Id
    val id: Long? = null,
    val type: String,
    val version: Int,
    val content: String,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
