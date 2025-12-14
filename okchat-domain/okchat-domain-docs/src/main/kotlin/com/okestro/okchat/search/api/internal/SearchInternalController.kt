package com.okestro.okchat.search.api.internal

import com.okestro.okchat.search.api.internal.dto.InternalMultiSearchRequest
import com.okestro.okchat.search.api.internal.dto.InternalMultiSearchResponse
import com.okestro.okchat.search.application.MultiSearchUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/internal/api/v1/search")
class SearchInternalController(
    private val multiSearchUseCase: MultiSearchUseCase
) {

    @PostMapping("/multi")
    suspend fun multiSearch(
        @RequestBody request: InternalMultiSearchRequest
    ): ResponseEntity<InternalMultiSearchResponse> {
        log.debug { "[Internal] Multi search request: $request" }

        val useCaseIn = request.toUseCaseIn()
        val result = multiSearchUseCase.execute(useCaseIn)

        return ResponseEntity.ok(InternalMultiSearchResponse.from(result.result))
    }
}
