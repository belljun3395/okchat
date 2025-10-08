package com.okestro.okchat.chat.pipeline.steps

import com.okestro.okchat.ai.support.extraction.ContentExtractionService
import com.okestro.okchat.ai.support.extraction.DateExtractor
import com.okestro.okchat.ai.support.extraction.KeywordExtractionService
import com.okestro.okchat.ai.support.extraction.PathExtractionService
import com.okestro.okchat.ai.support.QueryClassifier
import com.okestro.okchat.ai.support.extraction.TitleExtractionService
import com.okestro.okchat.chat.pipeline.ChatContext
import com.okestro.okchat.chat.pipeline.FirstChatPipelineStep
import com.okestro.okchat.chat.pipeline.copy
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
    private val keywordExtractionService: KeywordExtractionService,
    private val contentExtractionService: ContentExtractionService,
    private val pathExtractionService: PathExtractionService,
    private val titleExtractionService: TitleExtractionService
) : FirstChatPipelineStep {

    override suspend fun execute(context: ChatContext): ChatContext {
        val userMessage = context.input.message
        log.info { "[${getStepName()}] Analyzing query: $userMessage" }

        // Classify query type using AI
        val queryAnalysis = queryClassifier.classify(userMessage)
        log.info { "[${getStepName()}] Type: ${queryAnalysis.type}, Confidence: ${"%.2f".format(queryAnalysis.confidence)}" }

        // Extract title using AI
        val titles = titleExtractionService.extractTitleKeywords(userMessage)

        // Extract content keywords using AI
        val contents = contentExtractionService.extractContentKeywords(userMessage)

        // Extract location using AI
        val paths = pathExtractionService.extractLocationKeywords(userMessage)

        // Extract keywords using AI (if not provided)
        val providedKeywords = context.input.providedKeywords
        val keywords = providedKeywords.ifEmpty {
            keywordExtractionService.extractQueryKeywords(userMessage)
        }
        log.debug {
            if (providedKeywords.isNotEmpty()) {
                "[${getStepName()}] Keywords provided: $keywords"
            } else {
                "[${getStepName()}] Keywords extracted: $keywords"
            }
        }

        // Extract date keywords
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
                extractedTitles = titles,
                extractedContents = contents,
                extractedPaths = paths,
                extractedKeywords = keywords,
                dateKeywords = dateKeywords
            )
        )
    }

    override fun getStepName(): String = "Query Analysis"
}
