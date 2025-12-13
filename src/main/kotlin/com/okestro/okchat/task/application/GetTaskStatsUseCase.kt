package com.okestro.okchat.task.application

import com.okestro.okchat.task.application.dto.GetTaskStatsUseCaseIn
import com.okestro.okchat.task.dto.TaskStatsDto
import com.okestro.okchat.task.repository.TaskExecutionRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class GetTaskStatsUseCase(
    private val taskExecutionRepository: TaskExecutionRepository
) {
    fun execute(input: GetTaskStatsUseCaseIn): Mono<TaskStatsDto> {
        return Mono.fromCallable {
            val stats = taskExecutionRepository.getStatistics()
            val lastExecution = taskExecutionRepository.findRecentExecutions()
                .firstOrNull()
                ?.startTime

            TaskStatsDto(
                total = stats["total"] ?: 0L,
                success = stats["success"] ?: 0L,
                failure = stats["failure"] ?: 0L,
                lastExecution = lastExecution
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
    }
}
