package com.okestro.okchat.task.application

import com.okestro.okchat.task.application.dto.GetTaskExecutionByIdUseCaseIn
import com.okestro.okchat.task.entity.TaskExecutionEntity
import com.okestro.okchat.task.repository.TaskExecutionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.util.Optional

class GetTaskExecutionByIdUseCaseTest : BehaviorSpec({

    val taskExecutionRepository = mockk<TaskExecutionRepository>()
    val useCase = GetTaskExecutionByIdUseCase(taskExecutionRepository)

    afterEach {
        clearAllMocks()
    }

    given("Requesting task execution details by ID") {
        val taskId = 100L
        val input = GetTaskExecutionByIdUseCaseIn(taskId)

        val task = TaskExecutionEntity(
            taskName = "ConfluenceSync",
            startTime = LocalDateTime.now(),
            exitCode = 0
        ).apply {
            ReflectionTestUtils.setField(this, "taskExecutionId", taskId)
        }

        `when`("Task execution exists") {
            every { taskExecutionRepository.findById(taskId) } returns Optional.of(task)

            val result = useCase.execute(input)

            then("Task details are returned") {
                StepVerifier.create(result)
                    .expectNextMatches { dto ->
                        dto.taskExecutionId == taskId &&
                            dto.taskName == "ConfluenceSync"
                    }
                    .verifyComplete()
            }
        }

        `when`("Task execution does not exist") {
            every { taskExecutionRepository.findById(taskId) } returns Optional.empty()

            val result = useCase.execute(input)

            then("NoSuchElementException is thrown") {
                StepVerifier.create(result)
                    .expectErrorMatches { it is NoSuchElementException }
                    .verify()
            }
        }
    }
})
