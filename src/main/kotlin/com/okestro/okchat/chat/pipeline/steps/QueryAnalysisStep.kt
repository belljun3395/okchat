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
        val userMessage = context.input.message
        log.info { "[${getStepName()}] Analyzing query: $userMessage" }

        // Classify query type using AI
        val queryAnalysis = queryClassifier.classify(userMessage)
        log.info { "[${getStepName()}] Type: ${queryAnalysis.type}, Confidence: ${"%.2f".format(queryAnalysis.confidence)}" }

        // Extract keywords using AI (if not provided)
        val providedKeywords = context.input.providedKeywords
        val keywords = providedKeywords.ifEmpty {
            keywordExtractionService.extractKeywords(userMessage)
        }
        log.debug {
            if (providedKeywords.isNotEmpty()) {
                "[${getStepName()}] Keywords provided: $keywords"
            } else {
                "[${getStepName()}] Keywords extracted: $keywords"
            }
        }

        // Extract date keywords
        // Always include strategic day patterns for better date matching (e.g., "250804" in titles)
        // This ensures month-level queries like "2025년 8월" can match day-specific titles
        val dateKeywords = DateExtractor.extractDateKeywords(
            userMessage,
            includeAllDays = true // Always include strategic days for YYMMDD title matching
        )
        log.debug { "[${getStepName()}] Date keywords: $dateKeywords" }

        val allKeywords = (keywords + dateKeywords).distinct()
        log.info { "[${getStepName()}] Analysis complete: ${allKeywords.size} keywords (${keywords.size} semantic, ${dateKeywords.size} date)" }

        return context.copy(
            analysis = ChatContext.Analysis(
                queryAnalysis = queryAnalysis,
                extractedKeywords = keywords,
                dateKeywords = dateKeywords
            )
        )
    }

    override fun getStepName(): String = "Query Analysis"
}
