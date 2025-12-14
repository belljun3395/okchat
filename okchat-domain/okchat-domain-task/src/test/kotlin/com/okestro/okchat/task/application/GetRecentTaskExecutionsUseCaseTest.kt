package com.okestro.okchat.task.application

import com.okestro.okchat.task.application.dto.GetRecentTaskExecutionsUseCaseIn
import com.okestro.okchat.task.entity.TaskExecutionEntity
import com.okestro.okchat.task.repository.TaskExecutionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils
import reactor.test.StepVerifier
import java.time.LocalDateTime

class GetRecentTaskExecutionsUseCaseTest : BehaviorSpec({

    val taskExecutionRepository = mockk<TaskExecutionRepository>()
    val useCase = GetRecentTaskExecutionsUseCase(taskExecutionRepository)

    afterEach {
        clearAllMocks()
    }

    given("Requesting recent task executions") {
        val input = GetRecentTaskExecutionsUseCaseIn()

        val task1 = TaskExecutionEntity(
            taskName = "ConfluenceSync",
            startTime = LocalDateTime.now().minusHours(1),
            exitCode = 0
        ).apply {
            ReflectionTestUtils.setField(this, "taskExecutionId", 1L)
        }

        val task2 = TaskExecutionEntity(
            taskName = "EmailSync",
            startTime = LocalDateTime.now().minusHours(2),
            exitCode = 0
        ).apply {
            ReflectionTestUtils.setField(this, "taskExecutionId", 2L)
        }

        `when`("Executions are found") {
            every { taskExecutionRepository.findRecentExecutions() } returns listOf(task1, task2)

            val result = useCase.execute(input)

            then("Execution list is returned") {
                StepVerifier.create(result.collectList())
                    .expectNextMatches { list ->
                        list.size == 2 &&
                            list[0].taskName == "ConfluenceSync" &&
                            list[1].taskName == "EmailSync"
                    }
                    .verifyComplete()
            }
        }

        `when`("No execution history found") {
            every { taskExecutionRepository.findRecentExecutions() } returns emptyList()

            val result = useCase.execute(input)

            then("Empty list is returned") {
                StepVerifier.create(result.collectList())
                    .expectNextMatches { it.isEmpty() }
                    .verifyComplete()
            }
        }
    }
})
