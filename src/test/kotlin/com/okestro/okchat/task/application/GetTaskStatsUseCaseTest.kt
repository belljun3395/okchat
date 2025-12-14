package com.okestro.okchat.task.application

import com.okestro.okchat.task.application.dto.GetTaskStatsUseCaseIn
import com.okestro.okchat.task.entity.TaskExecutionEntity
import com.okestro.okchat.task.repository.TaskExecutionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils
import reactor.test.StepVerifier
import java.time.LocalDateTime

class GetTaskStatsUseCaseTest : BehaviorSpec({

    val taskExecutionRepository = mockk<TaskExecutionRepository>()
    val useCase = GetTaskStatsUseCase(taskExecutionRepository)

    afterEach {
        clearAllMocks()
    }

    given("Requesting task execution statistics") {
        val input = GetTaskStatsUseCaseIn()

        val stats = mapOf(
            "total" to 100L,
            "success" to 95L,
            "failure" to 5L
        )

        val lastExecution = TaskExecutionEntity(
            taskName = "ConfluenceSync",
            startTime = LocalDateTime.now(),
            exitCode = 0
        ).apply {
            ReflectionTestUtils.setField(this, "taskExecutionId", 1L)
        }

        `when`("Statistics and recent executions are found") {
            every { taskExecutionRepository.getStatistics() } returns stats
            every { taskExecutionRepository.findRecentExecutions() } returns listOf(lastExecution)

            val result = useCase.execute(input)

            then("Statistics are returned") {
                StepVerifier.create(result)
                    .expectNextMatches { dto ->
                        dto.total == 100L &&
                            dto.success == 95L &&
                            dto.failure == 5L &&
                            dto.lastExecution != null
                    }
                    .verifyComplete()
            }
        }

        `when`("No execution history exists") {
            every { taskExecutionRepository.getStatistics() } returns mapOf(
                "total" to 0L,
                "success" to 0L,
                "failure" to 0L
            )
            every { taskExecutionRepository.findRecentExecutions() } returns emptyList()

            val result = useCase.execute(input)

            then("Default statistics are returned") {
                StepVerifier.create(result)
                    .expectNextMatches { dto ->
                        dto.total == 0L &&
                            dto.success == 0L &&
                            dto.failure == 0L &&
                            dto.lastExecution == null
                    }
                    .verifyComplete()
            }
        }
    }
})
