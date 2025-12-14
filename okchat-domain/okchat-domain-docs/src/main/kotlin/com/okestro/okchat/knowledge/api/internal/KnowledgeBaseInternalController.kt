package com.okestro.okchat.knowledge.api.internal

import com.okestro.okchat.knowledge.api.internal.dto.InternalEnabledKnowledgeBaseResponse
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/api/v1/knowledge-bases")
class KnowledgeBaseInternalController(
    private val knowledgeBaseRepository: KnowledgeBaseRepository
) {
    @GetMapping("/enabled")
    fun getEnabledKnowledgeBases(): List<InternalEnabledKnowledgeBaseResponse> {
        return knowledgeBaseRepository.findAllByEnabledTrue()
            .map {
                InternalEnabledKnowledgeBaseResponse(
                    id = requireNotNull(it.id) { "KnowledgeBase.id must not be null" },
                    name = it.name,
                    type = it.type.name
                )
            }
    }
}
