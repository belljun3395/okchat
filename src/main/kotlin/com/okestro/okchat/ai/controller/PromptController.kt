package com.okestro.okchat.ai.controller

import com.okestro.okchat.ai.model.Prompt
import com.okestro.okchat.ai.service.PromptService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/prompts")
class PromptController(
    private val promptService: PromptService,
    private val promptExecutionService: com.okestro.okchat.ai.service.PromptExecutionService
) {

    data class CreatePromptRequest(
        val type: String,
        val content: String
    )

    data class UpdatePromptRequest(
        val content: String
    )

    data class AnalyzePromptRequest(
        val content: String,
        val type: String? = null
    )

    data class PromptAnalysisResponse(
        val score: Int,
        val improvements: List<String>,
        val strengths: List<String>,
        val suggestions: String,
        val similarPrompts: List<SimilarPrompt>? = null
    )

    data class SimilarPrompt(
        val type: String,
        val version: Int,
        val similarity: Double,
        val preview: String
    )

    data class ImprovePromptRequest(
        val content: String,
        val focusAreas: List<String>? = null
    )

    data class ImprovedPromptResponse(
        val original: String,
        val improved: String,
        val changes: List<String>
    )

    data class PromptResponse(
        val id: Long?,
        val type: String,
        val version: Int,
        val content: String,
        val isActive: Boolean
    ) {
        companion object {
            fun from(prompt: Prompt) = PromptResponse(
                id = prompt.id,
                type = prompt.type,
                version = prompt.version,
                content = prompt.content,
                isActive = prompt.active
            )
        }
    }

    data class PromptContentResponse(
        val type: String,
        val version: Int?,
        val content: String
    )

    @GetMapping("/{type}")
    suspend fun getPrompt(
        @PathVariable type: String,
        @RequestParam(required = false) version: Int?
    ): PromptContentResponse {
        log.info { "Getting prompt: type=$type, version=$version" }
        val content = promptService.getPrompt(type, version)
            ?: throw PromptNotFoundException("Prompt not found: type=$type, version=$version")

        val actualVersion = version ?: promptService.getLatestVersion(type)

        return PromptContentResponse(
            type = type,
            version = actualVersion,
            content = content
        )
    }

    @GetMapping("/{type}/versions")
    suspend fun getAllVersions(@PathVariable type: String): List<PromptResponse> {
        log.info { "Getting all versions of prompt: type=$type" }
        return promptService.getAllVersions(type).map { PromptResponse.from(it) }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createPrompt(@RequestBody request: CreatePromptRequest): PromptResponse {
        log.info { "Creating new prompt: type=${request.type}" }
        val prompt = promptService.createPrompt(request.type, request.content)
        return PromptResponse.from(prompt)
    }

    @PutMapping("/{type}")
    suspend fun updatePrompt(
        @PathVariable type: String,
        @RequestBody request: UpdatePromptRequest
    ): PromptResponse {
        log.info { "Updating prompt: type=$type" }
        val prompt = promptService.updateLatestPrompt(type, request.content)
        return PromptResponse.from(prompt)
    }

    @DeleteMapping("/{type}/versions/{version}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deactivatePrompt(
        @PathVariable type: String,
        @PathVariable version: Int
    ) {
        log.info { "Deactivating prompt: type=$type, version=$version" }
        promptService.deactivatePrompt(type, version)
    }

    /**
     * Analyze prompt quality and provide recommendations
     */
    @PostMapping("/analyze")
    suspend fun analyzePrompt(@RequestBody request: AnalyzePromptRequest): PromptAnalysisResponse {
        log.info { "Analyzing prompt quality" }
        
        val content = request.content
        val improvements = mutableListOf<String>()
        val strengths = mutableListOf<String>()
        var score = 50 // Base score
        
        // Analyze prompt structure and quality
        if (content.length > 100) {
            score += 10
            strengths.add("충분한 길이의 프롬프트")
        } else {
            improvements.add("프롬프트를 더 구체적으로 작성하세요")
        }
        
        if (content.contains("{{" ) && content.contains("}}")) {
            score += 15
            strengths.add("변수를 적절히 사용하고 있습니다")
        } else {
            improvements.add("동적 콘텐츠를 위해 {{변수}} 사용을 고려하세요")
        }
        
        if (content.contains("예:") || content.contains("Example:") || content.contains("예시")) {
            score += 15
            strengths.add("예시를 포함하여 명확성을 높였습니다")
        } else {
            improvements.add("구체적인 예시를 추가하면 더 좋은 결과를 얻을 수 있습니다")
        }
        
        if (content.lines().size > 5) {
            score += 10
            strengths.add("구조화된 프롬프트")
        } else {
            improvements.add("프롬프트를 여러 섹션으로 나누어 구조화하세요")
        }
        
        // Check for clear instructions
        val hasInstructions = content.contains("다음") || content.contains("아래") || 
                             content.contains("following") || content.contains("please")
        if (hasInstructions) {
            score += 10
            strengths.add("명확한 지시사항이 포함되어 있습니다")
        } else {
            improvements.add("명확한 지시사항(예: '다음을 수행하세요')을 추가하세요")
        }
        
        // Cap score at 100
        score = score.coerceAtMost(100)
        
        // Find similar prompts if type is provided
        val similarPrompts = if (request.type != null) {
            try {
                val allPrompts = promptService.getAllVersions(request.type)
                    .filter { it.content != content }
                    .take(3)
                    .map { prompt ->
                        SimilarPrompt(
                            type = prompt.type,
                            version = prompt.version,
                            similarity = calculateSimilarity(content, prompt.content),
                            preview = prompt.content.take(100) + "..."
                        )
                    }
                    .filter { it.similarity > 0.3 }
                    .sortedByDescending { it.similarity }
                allPrompts
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            null
        }
        
        val suggestions = buildString {
            append("프롬프트 품질 점수: $score/100\n\n")
            if (score >= 80) {
                append("훌륭합니다! 이 프롬프트는 잘 작성되었습니다.")
            } else if (score >= 60) {
                append("좋은 시작입니다. 아래 개선사항을 참고하여 더 나은 프롬프트를 만들어보세요.")
            } else {
                append("프롬프트를 개선할 여지가 많습니다. 아래 제안사항을 참고하세요.")
            }
        }
        
        return PromptAnalysisResponse(
            score = score,
            improvements = improvements,
            strengths = strengths,
            suggestions = suggestions,
            similarPrompts = similarPrompts
        )
    }

    /**
     * Get AI-powered suggestions to improve prompt
     */
    @PostMapping("/improve")
    suspend fun improvePrompt(@RequestBody request: ImprovePromptRequest): ImprovedPromptResponse {
        log.info { "Getting improvement suggestions for prompt" }
        
        // This would typically call an AI service, but for now we'll provide rule-based improvements
        val original = request.content
        val changes = mutableListOf<String>()
        var improved = original
        
        // Add structure if missing
        if (!improved.contains(":") && improved.length > 50) {
            improved = "당신은 전문적인 AI 어시스턴트입니다.\n\n$improved"
            changes.add("프롬프트 시작 부분에 역할 정의 추가")
        }
        
        // Add output format if missing
        if (!improved.contains("JSON") && !improved.contains("형식") && !improved.contains("format")) {
            improved += "\n\n출력 형식을 명확히 지정해주세요."
            changes.add("출력 형식 지정 섹션 추가")
        }
        
        // Suggest adding examples
        if (!improved.contains("예:") && !improved.contains("Example")) {
            changes.add("구체적인 예시 추가 권장")
        }
        
        return ImprovedPromptResponse(
            original = original,
            improved = improved,
            changes = changes.ifEmpty { listOf("프롬프트가 이미 잘 작성되어 있습니다") }
        )
    }

    /**
     * Get autocomplete suggestions based on current prompt content
     */
    @PostMapping("/autocomplete")
    suspend fun getAutocompleteSuggestions(
        @RequestBody request: Map<String, String>
    ): List<String> {
        val content = request["content"] ?: ""
        val suggestions = mutableListOf<String>()
        
        val lastLine = content.lines().lastOrNull()?.trim() ?: ""
        
        // Context-aware suggestions
        when {
            lastLine.endsWith("당신은") || lastLine.endsWith("You are") -> {
                suggestions.addAll(listOf(
                    "전문적인 AI 어시스턴트입니다.",
                    "도움이 되는 비서입니다.",
                    "문서 분석 전문가입니다."
                ))
            }
            lastLine.contains("분석") || lastLine.contains("analyze") -> {
                suggestions.addAll(listOf(
                    "다음 항목을 분석하세요:",
                    "주요 패턴과 트렌드를 파악하세요.",
                    "데이터에서 인사이트를 추출하세요."
                ))
            }
            lastLine.contains("출력") || lastLine.contains("output") || lastLine.contains("결과") -> {
                suggestions.addAll(listOf(
                    "결과를 JSON 형식으로 반환하세요.",
                    "결과를 구조화된 형식으로 제공하세요.",
                    "다음 형식으로 응답하세요:"
                ))
            }
            content.length < 50 -> {
                suggestions.addAll(listOf(
                    "명확하고 구체적인 지시사항을 작성하세요.",
                    "예시를 포함하면 더 좋은 결과를 얻을 수 있습니다.",
                    "원하는 출력 형식을 명시하세요."
                ))
            }
        }
        
        return suggestions.take(5)
    }

    /**
     * Calculate similarity between two prompts (simple implementation)
     */
    private fun calculateSimilarity(text1: String, text2: String): Double {
        val words1 = text1.lowercase().split("\\s+".toRegex()).toSet()
        val words2 = text2.lowercase().split("\\s+".toRegex()).toSet()
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return if (union > 0) intersection.toDouble() / union else 0.0
    }

    // ========== Execution Tracking APIs ==========

    /**
     * Get execution history for a prompt type
     */
    @GetMapping("/{type}/executions")
    suspend fun getExecutionHistory(
        @PathVariable type: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ExecutionHistoryResponse {
        log.info { "Getting execution history for prompt: $type" }
        val history = promptExecutionService.getExecutionHistory(type, page, size)
        
        return ExecutionHistoryResponse(
            executions = history.content.map { ExecutionDto.from(it) },
            totalElements = history.totalElements,
            totalPages = history.totalPages,
            currentPage = page
        )
    }

    /**
     * Get execution statistics for a prompt type
     */
    @GetMapping("/{type}/statistics")
    suspend fun getExecutionStatistics(@PathVariable type: String): StatisticsResponse {
        log.info { "Getting statistics for prompt: $type" }
        val stats = promptExecutionService.getStatistics(type)
        
        return StatisticsResponse(
            totalExecutions = stats.totalExecutions,
            successCount = stats.successCount,
            failureCount = stats.failureCount,
            successRate = stats.successRate,
            averageRating = stats.averageRating,
            averageResponseTimeMs = stats.averageResponseTimeMs
        )
    }

    /**
     * Get version comparison
     */
    @GetMapping("/{type}/versions/compare")
    suspend fun compareVersions(@PathVariable type: String): VersionComparisonResponse {
        log.info { "Comparing versions for prompt: $type" }
        val comparison = promptExecutionService.compareVersions(type)
        
        return VersionComparisonResponse(
            versions = comparison.map { VersionStatsDto.from(it) }
        )
    }

    /**
     * Get execution trends
     */
    @GetMapping("/{type}/trends")
    suspend fun getExecutionTrends(
        @PathVariable type: String,
        @RequestParam(defaultValue = "30") days: Int
    ): TrendsResponse {
        log.info { "Getting execution trends for prompt: $type" }
        val trends = promptExecutionService.getDailyTrends(type, days)
        
        return TrendsResponse(
            trends = trends.map { TrendDto(it.date, it.count) }
        )
    }

    /**
     * Get executions with user feedback
     */
    @GetMapping("/{type}/feedback")
    suspend fun getExecutionsWithFeedback(
        @PathVariable type: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ExecutionHistoryResponse {
        log.info { "Getting executions with feedback for prompt: $type" }
        val history = promptExecutionService.getExecutionsWithFeedback(type, page, size)
        
        return ExecutionHistoryResponse(
            executions = history.content.map { ExecutionDto.from(it) },
            totalElements = history.totalElements,
            totalPages = history.totalPages,
            currentPage = page
        )
    }

    /**
     * Add feedback to an execution
     */
    @PostMapping("/executions/{executionId}/feedback")
    suspend fun addFeedback(
        @PathVariable executionId: Long,
        @RequestBody request: FeedbackRequest
    ): ExecutionDto {
        log.info { "Adding feedback to execution: $executionId" }
        val execution = promptExecutionService.addFeedback(
            executionId,
            request.rating,
            request.feedback
        )
        
        return ExecutionDto.from(execution)
    }

    // DTOs for execution tracking
    data class ExecutionHistoryResponse(
        val executions: List<ExecutionDto>,
        val totalElements: Long,
        val totalPages: Int,
        val currentPage: Int
    )

    data class ExecutionDto(
        val id: Long,
        val promptType: String,
        val promptVersion: Int,
        val sessionId: String?,
        val userEmail: String?,
        val userInput: String?,
        val generatedOutput: String?,
        val responseTimeMs: Long?,
        val totalTokens: Int?,
        val status: String,
        val userRating: Int?,
        val userFeedback: String?,
        val createdAt: String
    ) {
        companion object {
            fun from(execution: com.okestro.okchat.ai.model.PromptExecution) = ExecutionDto(
                id = execution.id!!,
                promptType = execution.promptType,
                promptVersion = execution.promptVersion,
                sessionId = execution.sessionId,
                userEmail = execution.userEmail,
                userInput = execution.userInput,
                generatedOutput = execution.generatedOutput,
                responseTimeMs = execution.responseTimeMs,
                totalTokens = execution.totalTokens,
                status = execution.status.name,
                userRating = execution.userRating,
                userFeedback = execution.userFeedback,
                createdAt = execution.createdAt.toString()
            )
        }
    }

    data class StatisticsResponse(
        val totalExecutions: Long,
        val successCount: Long,
        val failureCount: Long,
        val successRate: Double,
        val averageRating: Double,
        val averageResponseTimeMs: Double
    )

    data class VersionComparisonResponse(
        val versions: List<VersionStatsDto>
    )

    data class VersionStatsDto(
        val version: Int,
        val totalExecutions: Long,
        val successCount: Long,
        val successRate: Double,
        val averageRating: Double,
        val averageResponseTimeMs: Double,
        val averageTokens: Double
    ) {
        companion object {
            fun from(stats: com.okestro.okchat.ai.service.VersionStatistics) = VersionStatsDto(
                version = stats.version,
                totalExecutions = stats.totalExecutions,
                successCount = stats.successCount,
                successRate = stats.successRate,
                averageRating = stats.averageRating,
                averageResponseTimeMs = stats.averageResponseTimeMs,
                averageTokens = stats.averageTokens
            )
        }
    }

    data class TrendsResponse(
        val trends: List<TrendDto>
    )

    data class TrendDto(
        val date: String,
        val count: Long
    )

    data class FeedbackRequest(
        val rating: Int?,
        val feedback: String?
    )

    data class ErrorResponse(
        val message: String,
        val type: String
    )

    @ExceptionHandler(PromptNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlePromptNotFound(e: PromptNotFoundException): ErrorResponse {
        return ErrorResponse(
            message = e.message ?: "Prompt not found",
            type = "PROMPT_NOT_FOUND"
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(e: IllegalArgumentException): ErrorResponse {
        return ErrorResponse(
            message = e.message ?: "Invalid request",
            type = "INVALID_REQUEST"
        )
    }
}

class PromptNotFoundException(message: String) : RuntimeException(message)
