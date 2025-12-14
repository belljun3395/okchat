package com.okestro.okchat.task.application

import com.okestro.okchat.task.application.dto.GetTaskExecutionByIdUseCaseIn
import com.okestro.okchat.task.dto.TaskExecutionDto
import com.okestro.okchat.task.dto.toDto
import com.okestro.okchat.task.repository.TaskExecutionRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.NoSuchElementException

@Service
class GetTaskExecutionByIdUseCase(
    private val taskExecutionRepository: TaskExecutionRepository
) {
    fun execute(input: GetTaskExecutionByIdUseCaseIn): Mono<TaskExecutionDto> {
        return Mono.fromCallable {
            taskExecutionRepository.findById(input.id)
                .orElseThrow { NoSuchElementException("Task execution not found: ${input.id}") }
                .toDto()
        }
            .subscribeOn(Schedulers.boundedElastic())
    }
}
