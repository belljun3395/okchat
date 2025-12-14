package com.okestro.okchat.task.application

import com.okestro.okchat.task.application.dto.GetRecentTaskExecutionsUseCaseIn
import com.okestro.okchat.task.dto.TaskExecutionDto
import com.okestro.okchat.task.dto.toDto
import com.okestro.okchat.task.repository.TaskExecutionRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class GetRecentTaskExecutionsUseCase(
    private val taskExecutionRepository: TaskExecutionRepository
) {
    fun execute(input: GetRecentTaskExecutionsUseCaseIn): Flux<TaskExecutionDto> {
        return Mono.fromCallable {
            taskExecutionRepository.findRecentExecutions() // potentially limit by input.limit if repo supports it
                .map { it.toDto() }
        }
            .flatMapMany { Flux.fromIterable(it) }
            .subscribeOn(Schedulers.boundedElastic())
    }
}
