package com.okestro.okchat.fixture

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

/**
 * Test fixtures for commonly used test data.
 * Provides reusable test data objects to reduce duplication and improve maintainability.
 */
object TestFixtures {

    /**
     * Creates a mock ChatResponse with the given content
     */
    fun createChatResponse(content: String): ChatResponse {
        return ChatResponse(
            listOf(Generation(AssistantMessage(content)))
        )
    }

    /**
     * Creates an empty ChatResponse
     */
    fun createEmptyChatResponse(): ChatResponse {
        return ChatResponse(
            listOf(Generation(AssistantMessage("")))
        )
    }

    object ExtractionService {
        const val KOREAN_QUERY = "백엔드 개발 레포 정보"
        const val ENGLISH_QUERY = "Show me the design document for authentication logic in Mobile App"
        const val TITLE_KOREAN_QUERY = "2025년 기술 부채 보고서 찾아줘"
        const val TITLE_ENGLISH_QUERY = "show me the 'Q3 Performance Review' document"

        val KOREAN_KEYWORDS_RESPONSE = createChatResponse("백엔드, backend, 개발, development, 레포, repository")
        val ENGLISH_KEYWORDS_RESPONSE = createChatResponse("design document, authentication logic, Mobile App")
        val KOREAN_TITLE_RESPONSE = createChatResponse("2025년 기술 부채 보고서, 기술 부채 보고서")
        val ENGLISH_TITLE_RESPONSE = createChatResponse("Q3 Performance Review")
        val EMPTY_RESPONSE = createEmptyChatResponse()

        val SINGLE_CHAR_KEYWORDS_RESPONSE = createChatResponse("a, bb, ccc, dddd")
        val MANY_KEYWORDS_RESPONSE = createChatResponse((1..15).joinToString(", ") { "keyword$it" })
    }

    object DateExtractor {
        const val KOREAN_YEAR_MONTH = "2024년 9월 회의록"
        const val SHORT_YEAR_FORMAT = "25년 8월 회의록"
        const val DASH_FORMAT = "2024-09 보고서"
        const val SLASH_FORMAT = "2024/09 회의록"
        const val SHORT_FORMAT_YYMMDD = "250908 회의록"
        const val KOREAN_MULTIPLE_DATES = "2024년 8월과 2024년 9월 회의록"
        const val KOREAN_SPECIFIC_DAY = "2024년 9월 15일 회의록"
        const val NO_DATE_TEXT = "Spring Boot tutorial"
    }

    object EmailCleaner {
        val EMAIL_WITH_SIGNATURE = """
            This is the main content.
            
            --
            John Doe
            john@example.com
        """.trimIndent()

        val EMAIL_WITH_SENT_FROM = """
            This is the main content.
            
            Sent from my iPhone
        """.trimIndent()

        val EMAIL_WITH_QUOTED_LINES = """
            This is my reply.
            
            > This is a quoted line
            > Another quoted line
        """.trimIndent()

        val COMPLEX_REAL_WORLD_EMAIL = """
            Hi team,
            
            I wanted to follow up on our discussion.
            
            Best regards,
            John
            
            > On 2024-01-15, Jane wrote:
            > Thanks for the update.
            > > On 2024-01-14, Bob wrote:
            > > Here's the original message.
            
            --
            John Doe
            Software Engineer
            john@example.com
            
            Sent from my iPhone
        """.trimIndent()

        const val SUBJECT_MULTIPLE_RE = "Re: Re: Re: Original Subject"
        const val SUBJECT_MULTIPLE_FWD = "Fwd: Fwd: Original Subject"
        const val SUBJECT_MIXED_PREFIXES = "  Re: Fwd: Original Subject  "
    }

    object SearchUtils {
        fun createSearchHitDocument(
            id: String,
            content: String,
            title: String,
            path: String,
            spaceKey: String,
            keywords: String? = null
        ): Map<String, Any> {
            val doc = mutableMapOf<String, Any>(
                "id" to id,
                "content" to content,
                "metadata.title" to title,
                "metadata.path" to path,
                "metadata.spaceKey" to spaceKey
            )
            keywords?.let { doc["metadata.keywords"] = it }
            return doc
        }
    }

    object PdfMetadata {
        const val ATTACHMENT_ID = "att123"
        const val PAGE_TITLE = "Technical Documentation"
        const val ATTACHMENT_TITLE = "Architecture.pdf"
        const val SPACE_KEY = "TECH"
        const val PATH = "Documentation > Technical"
        const val PAGE_ID = "page456"
        const val FILE_SIZE = 1024000L
        const val MEDIA_TYPE = "application/pdf"
    }
}
