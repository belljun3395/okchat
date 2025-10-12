package com.okestro.okchat.search.application

import com.okestro.okchat.search.application.dto.MultiSearchUseCaseIn
import com.okestro.okchat.search.application.dto.MultiSearchUseCaseOut
import com.okestro.okchat.search.strategy.MultiSearchStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class MultiSearchUseCase(
    private val searchStrategy: MultiSearchStrategy
) {
    suspend fun execute(useCaseIn: MultiSearchUseCaseIn): MultiSearchUseCaseOut {
        log.info { "[MultiSearchUseCase] Delegating to ${searchStrategy.getStrategyName()} strategy" }

        val (titles, contents, paths, keywords, topK) = useCaseIn

        // Build criteria list (polymorphic approach)
        val criteria = listOfNotNull(keywords, titles, contents, paths)

        val result = searchStrategy.search(
            searchCriteria = criteria,
            topK = topK
        )

        return MultiSearchUseCaseOut(result)
    }
}
