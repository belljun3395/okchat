package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Classify user queries using AI to determine the best response strategy
 * Uses LLM for more accurate and context-aware classification
 */
@Service
class QueryClassifier(
    private val chatModel: ChatModel
) {

    enum class QueryType {
        MEETING_RECORDS, // 회의록 조회
        PROJECT_STATUS, // 프로젝트 현황
        HOW_TO, // 방법/절차 질문
        INFORMATION, // 정보 조회 (누가, 언제, 무엇)
        DOCUMENT_SEARCH, // 문서 찾기
        GENERAL // 일반 질문
    }

    data class QueryAnalysis(
        val type: QueryType,
        val confidence: Double,
        val keywords: List<String>
    )

    /**
     * Classify the user query using AI
     */
    suspend fun classify(query: String): QueryAnalysis {
        val classificationPrompt = """
            Classify the following user query into one of these categories:
            
            1. MEETING_RECORDS - 회의록 관련 질문
               Examples: "주간회의록", "9월 회의 요약", "회의 내용", "meeting minutes"
            
            2. PROJECT_STATUS - 프로젝트 현황/진행상황
               Examples: "프로젝트 현황", "작업 진행 상태", "완료 현황"
            
            3. HOW_TO - 방법/절차/가이드
               Examples: "어떻게 하나요", "방법", "설치 절차", "가이드"
            
            4. INFORMATION - 정보 조회 (누가, 언제, 무엇, 어디, 왜)
               Examples: "누가 담당", "언제 완료", "무엇을 해야", "어디에 있나요"
            
            5. DOCUMENT_SEARCH - 문서 찾기
               Examples: "문서 찾아줘", "자료 검색", "페이지 찾기"
            
            6. GENERAL - 일반 질문 (위 카테고리에 해당하지 않음)
            
            Analyze this query and respond in this EXACT format:
            TYPE: [category name]
            CONFIDENCE: [0.0-1.0]
            REASONING: [brief explanation in Korean]
            
            User Query: "$query"
            
            Classification:
        """.trimIndent()

        return try {
            val response = chatModel.call(Prompt(classificationPrompt))
            val result = response.result.output.text?.trim() ?: ""

            log.debug { "AI Classification result: $result" }

            parseClassificationResult(result, query)
        } catch (e: Exception) {
            log.warn { "Failed to classify query with AI: ${e.message}. Using fallback." }
            // Fallback to simple rule-based classification
            fallbackClassify(query)
        }
    }

    /**
     * Parse AI classification result
     */
    private fun parseClassificationResult(result: String, query: String): QueryAnalysis {
        val lines = result.lines()

        val typeLine = lines.find { it.startsWith("TYPE:") }
        val confidenceLine = lines.find { it.startsWith("CONFIDENCE:") }

        val typeStr = typeLine?.substringAfter("TYPE:")?.trim() ?: "GENERAL"
        val confidenceStr = confidenceLine?.substringAfter("CONFIDENCE:")?.trim() ?: "0.5"

        val type = try {
            QueryType.valueOf(typeStr)
        } catch (e: Exception) {
            log.warn { "Unknown type '$typeStr', defaulting to GENERAL" }
            QueryType.GENERAL
        }

        val confidence = confidenceStr.toDoubleOrNull() ?: 0.5

        log.info { "Query classified as $type with confidence ${"%.2f".format(confidence)}" }

        return QueryAnalysis(
            type = type,
            confidence = confidence,
            keywords = emptyList() // Keywords will be extracted separately
        )
    }

    /**
     * Fallback classification using simple rules
     */
    private fun fallbackClassify(query: String): QueryAnalysis {
        val queryLower = query.lowercase()

        // Meeting records - 더 포괄적인 패턴
        if (queryLower.contains("회의") || queryLower.contains("meeting") || queryLower.contains("미팅") || queryLower.contains("minutes")) {
            return QueryAnalysis(QueryType.MEETING_RECORDS, 0.8, emptyList())
        }

        // Project status
        if (queryLower.contains("현황") || queryLower.contains("상태") || queryLower.contains("status") || queryLower.contains("진행")) {
            return QueryAnalysis(QueryType.PROJECT_STATUS, 0.7, emptyList())
        }

        // How-to
        if (queryLower.contains("어떻게") || queryLower.contains("방법") || queryLower.contains("how") || queryLower.contains("가이드")) {
            return QueryAnalysis(QueryType.HOW_TO, 0.7, emptyList())
        }

        // Information
        if (queryLower.contains("누가") || queryLower.contains("언제") || queryLower.contains("무엇") || queryLower.contains("어디")) {
            return QueryAnalysis(QueryType.INFORMATION, 0.7, emptyList())
        }

        // Document search
        if (queryLower.contains("문서") || queryLower.contains("찾") || queryLower.contains("검색") || queryLower.contains("search")) {
            return QueryAnalysis(QueryType.DOCUMENT_SEARCH, 0.7, emptyList())
        }

        // Default to GENERAL
        return QueryAnalysis(QueryType.GENERAL, 0.5, emptyList())
    }
}
