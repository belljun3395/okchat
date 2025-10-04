package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Classify user queries to determine the best response strategy
 */
object QueryClassifier {

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
     * Classify the user query
     */
    fun classify(query: String): QueryAnalysis {
        val queryLower = query.lowercase()

        // Meeting records
        val meetingScore = calculateScore(
            queryLower,
            listOf(
                "회의", "meeting", "주간회의", "월간회의", "회의록", "minutes",
                "미팅", "논의", "안건", "결정사항"
            )
        )

        // Project status
        val statusScore = calculateScore(
            queryLower,
            listOf(
                "현황", "상태", "status", "진행", "progress", "작업",
                "진척", "완료", "예정", "블로커", "이슈"
            )
        )

        // How-to/Procedures
        val howToScore = calculateScore(
            queryLower,
            listOf(
                "어떻게", "how to", "방법", "절차", "procedure",
                "어떡하", "하는법", "가이드", "guide", "매뉴얼"
            )
        )

        // Information lookup
        val infoScore = calculateScore(
            queryLower,
            listOf(
                "누가", "who", "언제", "when", "무엇", "what",
                "어디", "where", "왜", "why"
            )
        )

        // Document search
        val docScore = calculateScore(
            queryLower,
            listOf(
                "문서",
                "자료",
                "document",
                "페이지",
                "page",
                "찾",
                "search",
                "검색"
            )
        )

        // Determine type based on highest score
        val scores = mapOf(
            QueryType.MEETING_RECORDS to meetingScore,
            QueryType.PROJECT_STATUS to statusScore,
            QueryType.HOW_TO to howToScore,
            QueryType.INFORMATION to infoScore,
            QueryType.DOCUMENT_SEARCH to docScore
        )

        val maxEntry = scores.maxByOrNull { it.value }
        val type = maxEntry?.key ?: QueryType.GENERAL
        val confidence = maxEntry?.value ?: 0.0

        // Extract relevant keywords based on type
        val keywords = extractRelevantKeywords(query, type)

        log.debug { "Query classified as $type with confidence $confidence" }

        return QueryAnalysis(
            type = if (confidence > 0.3) type else QueryType.GENERAL,
            confidence = confidence,
            keywords = keywords
        )
    }

    /**
     * Calculate score based on keyword matches
     */
    private fun calculateScore(query: String, keywords: List<String>): Double {
        var score = 0.0
        keywords.forEach { keyword ->
            if (query.contains(keyword)) {
                // Exact match gets higher score
                score += if (query.split(Regex("\\s+")).contains(keyword)) 1.0 else 0.5
            }
        }
        return score / keywords.size // Normalize
    }

    /**
     * Extract keywords relevant to the query type
     */
    private fun extractRelevantKeywords(query: String, type: QueryType): List<String> {
        return when (type) {
            QueryType.MEETING_RECORDS -> listOf("회의", "meeting", "회의록")
            QueryType.PROJECT_STATUS -> listOf("현황", "status", "진행")
            QueryType.HOW_TO -> listOf("방법", "절차", "가이드")
            QueryType.INFORMATION -> listOf("정보", "조회")
            QueryType.DOCUMENT_SEARCH -> listOf("문서", "검색")
            QueryType.GENERAL -> emptyList()
        }
    }
}
