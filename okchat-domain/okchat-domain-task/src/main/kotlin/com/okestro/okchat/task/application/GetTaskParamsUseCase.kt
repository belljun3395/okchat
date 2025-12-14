package com.okestro.okchat.task.application

import com.okestro.okchat.task.application.dto.GetTaskParamsUseCaseIn
import com.okestro.okchat.task.repository.TaskExecutionParamsRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class GetTaskParamsUseCase(
    private val taskExecutionParamsRepository: TaskExecutionParamsRepository
) {
    fun execute(input: GetTaskParamsUseCaseIn): Flux<String> {
        return Mono.fromCallable {
            taskExecutionParamsRepository.findParamsByExecutionId(input.executionId)
        }
            .flatMapMany { Flux.fromIterable(it) }
            .subscribeOn(Schedulers.boundedElastic())
    }
}
