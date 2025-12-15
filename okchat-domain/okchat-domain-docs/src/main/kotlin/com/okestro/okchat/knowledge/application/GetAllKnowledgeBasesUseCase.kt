package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.GetAllKnowledgeBasesUseCaseIn
import com.okestro.okchat.knowledge.application.dto.KnowledgeBaseDetailDto
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetAllKnowledgeBasesUseCase(
    private val knowledgeBaseRepository: KnowledgeBaseRepository
) {
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    fun execute(input: GetAllKnowledgeBasesUseCaseIn): List<KnowledgeBaseDetailDto> {
        // Current implementation returns all KBs without permission check for listing?
        // Original controller: getAllKnowledgeBases() just does findAll(). method logic preserved.

        val kbs = knowledgeBaseRepository.findAll()
        return kbs.map { kb ->
            KnowledgeBaseDetailDto(
                id = kb.id!!,
                name = kb.name,
                description = kb.description,
                type = kb.type,
                enabled = kb.enabled,
                createdBy = kb.createdBy,
                createdAt = kb.createdAt,
                updatedAt = kb.updatedAt,
                config = emptyMap() // Minimal info for list view
            )
        }
    }
}
