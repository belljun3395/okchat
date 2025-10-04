package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.support.DateExtractor
import com.okestro.okchat.ai.support.KeywordExtractionService
import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.FirstChatPipelineStep
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 *  Analyze user query
 * - Classify query type using AI
 * - Extract keywords using AI
 * - Extract date keywords
 */
@Component
class QueryAnalysisStep(
    private val queryClassifier: QueryClassifier,
    private val keywordExtractionService: KeywordExtractionService
) : FirstChatPipelineStep {

    override suspend fun execute(context: ChatContext): ChatContext {
        log.info { "[${getStepName()}] Analyzing query: ${context.userMessage}" }

        // Classify query type using AI
        val queryAnalysis = queryClassifier.classify(context.userMessage)
        log.info { "[${getStepName()}] Type: ${queryAnalysis.type}, Confidence: ${"%.2f".format(queryAnalysis.confidence)}" }

        // Extract keywords using AI (if not provided)
        val keywords = context.providedKeywords ?: keywordExtractionService.extractKeywords(context.userMessage)
        if (context.providedKeywords != null) {
            log.info { "[${getStepName()}] Keywords provided: $keywords" }
        } else {
            log.info { "[${getStepName()}] Keywords extracted: $keywords" }
        }

        // Extract date keywords (month-only by default, unless specific day is mentioned)
        val dateKeywords = DateExtractor.extractDateKeywords(
            context.userMessage,
            includeAllDays = false // 월 단위 검색이 기본
        )
        if (dateKeywords.isNotEmpty()) {
            log.info { "[${getStepName()}] Date keywords: $dateKeywords" }
        }

        val allKeywords = (keywords + dateKeywords).distinct()
        log.info { "[${getStepName()}] Total keywords: ${allKeywords.size}" }

        return context.copy(
            queryAnalysis = queryAnalysis,
            extractedKeywords = keywords,
            dateKeywords = dateKeywords
        )
    }

    override fun getStepName(): String = "Query Analysis"
}
