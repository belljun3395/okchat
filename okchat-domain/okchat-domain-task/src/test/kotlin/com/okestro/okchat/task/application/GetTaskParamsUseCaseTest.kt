package com.okestro.okchat.task.application

import com.okestro.okchat.task.application.dto.GetTaskParamsUseCaseIn
import com.okestro.okchat.task.repository.TaskExecutionParamsRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import reactor.test.StepVerifier

class GetTaskParamsUseCaseTest : BehaviorSpec({

    val taskExecutionParamsRepository = mockk<TaskExecutionParamsRepository>()
    val useCase = GetTaskParamsUseCase(taskExecutionParamsRepository)

    afterEach {
        clearAllMocks()
    }

    given("Requesting task execution parameters") {
        val executionId = 100L
        val input = GetTaskParamsUseCaseIn(executionId)

        val params = listOf("param1=value1", "param2=value2", "param3=value3")

        `when`("Parameters exist for the execution") {
            every { taskExecutionParamsRepository.findParamsByExecutionId(executionId) } returns params

            val result = useCase.execute(input)

            then("Parameter list is returned") {
                StepVerifier.create(result.collectList())
                    .expectNextMatches { list ->
                        list.size == 3 &&
                            list[0] == "param1=value1" &&
                            list[1] == "param2=value2" &&
                            list[2] == "param3=value3"
                    }
                    .verifyComplete()
            }
        }

        `when`("No parameters found") {
            every { taskExecutionParamsRepository.findParamsByExecutionId(executionId) } returns emptyList()

            val result = useCase.execute(input)

            then("Empty list is returned") {
                StepVerifier.create(result.collectList())
                    .expectNextMatches { it.isEmpty() }
                    .verifyComplete()
            }
        }
    }
})
