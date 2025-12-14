package com.okestro.okchat.knowledge.application

import com.okestro.okchat.knowledge.application.dto.GetAllKnowledgeBasesUseCaseIn
import com.okestro.okchat.knowledge.model.entity.KnowledgeBase
import com.okestro.okchat.knowledge.model.entity.KnowledgeBaseType
import com.okestro.okchat.knowledge.repository.KnowledgeBaseRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

class GetAllKnowledgeBasesUseCaseTest : BehaviorSpec({

    val knowledgeBaseRepository = mockk<KnowledgeBaseRepository>()

    val useCase = GetAllKnowledgeBasesUseCase(knowledgeBaseRepository)

    afterEach {
        clearAllMocks()
    }

    given("All Knowledge Base list retrieval is requested") {
        val input = GetAllKnowledgeBasesUseCaseIn()

        val kb1 = KnowledgeBase(
            name = "KB 1",
            type = KnowledgeBaseType.CONFLUENCE,
            config = mutableMapOf(),
            createdBy = 1L,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ).apply {
            ReflectionTestUtils.setField(this, "id", 1L)
        }

        val kb2 = KnowledgeBase(
            name = "KB 2",
            type = KnowledgeBaseType.ETC,
            config = mutableMapOf(),
            createdBy = 2L,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ).apply {
            ReflectionTestUtils.setField(this, "id", 2L)
        }

        `when`("Retrieval is requested") {
            every { knowledgeBaseRepository.findAll() } returns listOf(kb1, kb2)

            val result = useCase.execute(input)

            then("All KB list is returned") {
                result shouldHaveSize 2
                result[0].id shouldBe 1L
                result[0].name shouldBe "KB 1"
                result[1].id shouldBe 2L
                result[1].name shouldBe "KB 2"
            }
        }

        `when`("No KBs exist") {
            every { knowledgeBaseRepository.findAll() } returns emptyList()

            val result = useCase.execute(input)

            then("Empty list is returned") {
                result shouldHaveSize 0
            }
        }
    }
})
