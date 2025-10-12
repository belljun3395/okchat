package com.okestro.okchat.fixture

import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

/**
 * DSL-style test fixtures for enhanced readability and flexibility.
 * * Design principles:
 * - Fluent API: Natural, readable syntax
 * - Type-safe builders: Compile-time guarantees
 * - Composability: Easy to combine and extend
 * - Immutability: Thread-safe fixtures
 * * Usage examples:
 * ```kotlin
 * val response = chatResponse {
 *     content = "test keywords"
 * }
 * * val result = searchResult {
 *     id = "doc1"
 *     title = "Test Document"
 *     similarity = 0.9
 * }
 * * val email = email {
 *     body = "Main content"
 *     withSignature("John Doe", "john@example.com")
 *     addQuotedLine("Previous message")
 * }
 * ```
 */
object TestFixtures {

    // ==================== ChatResponse DSL ====================

    @DslMarker
    annotation class ChatResponseDsl

    @ChatResponseDsl
    class ChatResponseBuilder {
        var content: String = ""

        fun build(): ChatResponse = ChatResponse(
            listOf(Generation(AssistantMessage(content)))
        )
    }

    fun chatResponse(init: ChatResponseBuilder.() -> Unit): ChatResponse {
        return ChatResponseBuilder().apply(init).build()
    }

    fun emptyChatResponse(): ChatResponse = chatResponse { content = "" }

    // Backward compatibility
    fun createChatResponse(content: String): ChatResponse = chatResponse { this.content = content }
    fun createEmptyChatResponse(): ChatResponse = emptyChatResponse()

    // ==================== SearchResult DSL ====================

    @DslMarker
    annotation class SearchResultDsl

    @SearchResultDsl
    class SearchResultBuilder {
        var id: String = "test-doc-1"
        var title: String = "Test Document"
        var content: String = "Test content"
        var path: String = "/test/path"
        var spaceKey: String = "TEST"
        var similarity: Double = 0.85
        var keywords: String = ""
        var type: String = "confluence-page"
        var pageId: String = ""
        var webUrl: String = ""
        var downloadUrl: String = ""

        infix fun withSimilarity(score: Double) = apply { this.similarity = score }
        infix fun inSpace(key: String) = apply { this.spaceKey = key }
        infix fun atPath(path: String) = apply { this.path = path }

        fun build(): SearchResult = SearchResult.withSimilarity(
            id = id,
            title = title,
            content = content,
            path = path,
            spaceKey = spaceKey,
            keywords = keywords,
            similarity = SearchScore.SimilarityScore(similarity),
            type = type,
            pageId = pageId,
            webUrl = webUrl,
            downloadUrl = downloadUrl
        )
    }

    fun searchResult(init: SearchResultBuilder.() -> Unit = {}): SearchResult {
        return SearchResultBuilder().apply(init).build()
    }

    /**
     * Creates multiple search results with DSL
     */
    fun searchResults(count: Int, init: SearchResultCollectionBuilder.() -> Unit = {}): List<SearchResult> {
        return SearchResultCollectionBuilder(count).apply(init).build()
    }

    @SearchResultDsl
    class SearchResultCollectionBuilder(private val count: Int) {
        var baseId: String = "doc"
        var titlePrefix: String = "Document"
        var maxScore: Double = 0.9
        var minScore: Double = 0.5
        var spaceKey: String = "TEST"

        fun build(): List<SearchResult> {
            val scoreStep = (maxScore - minScore) / count.coerceAtLeast(1)
            val collectionBaseId = baseId
            val collectionTitlePrefix = titlePrefix
            val collectionMaxScore = maxScore
            val collectionSpaceKey = spaceKey

            return (1..count).map { index ->
                searchResult {
                    id = "$collectionBaseId$index"
                    title = "$collectionTitlePrefix $index"
                    content = "Content for document $index"
                    path = "/path$index"
                    this.spaceKey = collectionSpaceKey
                    similarity = collectionMaxScore - (scoreStep * (index - 1))
                }
            }
        }
    }

    // ==================== Email DSL ====================

    @DslMarker
    annotation class EmailDsl

    @EmailDsl
    class EmailBuilder {
        var body: String = "Main content"
        private var signatureName: String? = null
        private var signatureEmail: String? = null
        private var sentFrom: String? = null
        private val quotedLines = mutableListOf<String>()

        fun withSignature(name: String, email: String) {
            this.signatureName = name
            this.signatureEmail = email
        }

        fun withMobileSignature(device: String = "iPhone") {
            this.sentFrom = "Sent from my $device"
        }

        fun addQuotedLine(line: String) {
            quotedLines.add(line)
        }

        fun addQuotedLines(lines: List<String>) {
            quotedLines.addAll(lines)
        }

        fun quotedReply(init: QuotedReplyBuilder.() -> Unit) {
            val reply = QuotedReplyBuilder().apply(init).build()
            quotedLines.addAll(reply)
        }

        fun build(): String {
            val parts = mutableListOf(body)

            if (quotedLines.isNotEmpty()) {
                parts.add("")
                parts.addAll(quotedLines.map { "> $it" })
            }

            if (signatureName != null) {
                parts.add("")
                parts.add("--")
                parts.add(signatureName!!)
                if (signatureEmail != null) {
                    parts.add(signatureEmail!!)
                }
            }

            if (sentFrom != null) {
                parts.add("")
                parts.add(sentFrom!!)
            }

            return parts.joinToString("\n")
        }
    }

    @EmailDsl
    class QuotedReplyBuilder {
        private val lines = mutableListOf<String>()

        fun from(author: String, date: String? = null) {
            val dateStr = date?.let { ", $it" } ?: ""
            lines.add("On$dateStr, $author wrote:")
        }

        fun line(text: String) {
            lines.add(text)
        }

        fun nested(init: QuotedReplyBuilder.() -> Unit) {
            val nestedReply = QuotedReplyBuilder().apply(init).build()
            lines.addAll(nestedReply.map { "> $it" })
        }

        fun build(): List<String> = lines
    }

    fun email(init: EmailBuilder.() -> Unit): String {
        return EmailBuilder().apply(init).build()
    }

    // ==================== PDF Metadata DSL ====================

    @DslMarker
    annotation class PdfMetadataDsl

    @PdfMetadataDsl
    class PdfMetadataBuilder {
        var attachmentId: String = "att123"
        var pageTitle: String = "Technical Documentation"
        var attachmentTitle: String = "Architecture.pdf"
        var spaceKey: String = "TECH"
        var path: String = "Documentation > Technical"
        var pageId: String = "page456"
        var fileSize: Long = 1024000L
        var mediaType: String = "application/pdf"
        var pageNumber: Int = 1
        var totalPages: Int = 10

        infix fun withFileSize(size: Long) = apply { this.fileSize = size }
        infix fun withPages(total: Int) = apply { this.totalPages = total }

        fun asSinglePage() = apply {
            this.totalPages = 1
            this.pageNumber = 1
        }

        fun asPage(number: Int, of: Int) = apply {
            this.pageNumber = number
            this.totalPages = of
        }

        data class PdfMetadataData(
            val attachmentId: String,
            val pageTitle: String,
            val attachmentTitle: String,
            val spaceKey: String,
            val path: String,
            val pageId: String,
            val fileSize: Long,
            val mediaType: String,
            val pageNumber: Int,
            val totalPages: Int
        )

        fun build(): PdfMetadataData = PdfMetadataData(
            attachmentId = attachmentId,
            pageTitle = pageTitle,
            attachmentTitle = attachmentTitle,
            spaceKey = spaceKey,
            path = path,
            pageId = pageId,
            fileSize = fileSize,
            mediaType = mediaType,
            pageNumber = pageNumber,
            totalPages = totalPages
        )
    }

    fun pdfMetadata(init: PdfMetadataBuilder.() -> Unit): PdfMetadataBuilder.PdfMetadataData {
        return PdfMetadataBuilder().apply(init).build()
    }

    /**
     * Creates all pages of a PDF document
     */
    fun pdfPages(totalPages: Int, init: PdfMetadataBuilder.() -> Unit = {}): List<PdfMetadataBuilder.PdfMetadataData> {
        val baseMetadata = PdfMetadataBuilder().apply(init)
        return (1..totalPages).map { page ->
            baseMetadata.apply {
                pageNumber = page
                this.totalPages = totalPages
            }.build()
        }
    }

    // ==================== Search Hit Document DSL ====================

    @DslMarker
    annotation class SearchHitDsl

    @SearchHitDsl
    class SearchHitDocumentBuilder {
        var id: String = "doc1"
        var content: String = "Test content"
        var title: String = "Test Title"
        var path: String = "/test"
        var spaceKey: String = "TEST"
        var keywords: String? = null

        infix fun withKeywords(kw: String) = apply { this.keywords = kw }
        infix fun withId(id: String) = apply { this.id = id }

        fun build(): Map<String, Any> {
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

    fun searchHitDocument(init: SearchHitDocumentBuilder.() -> Unit = {}): Map<String, Any> {
        return SearchHitDocumentBuilder().apply(init).build()
    }

    // Backward compatibility for SearchUtils
    object SearchUtils {
        fun createSearchHitDocument(
            id: String = "doc1",
            content: String = "Test content",
            title: String = "Test Title",
            path: String = "/test",
            spaceKey: String = "TEST",
            keywords: String? = null
        ): Map<String, Any> = searchHitDocument {
            this.id = id
            this.content = content
            this.title = title
            this.path = path
            this.spaceKey = spaceKey
            this.keywords = keywords
        }
    }

    // ==================== Predefined Test Data ====================

    /**
     * Commonly used test queries and responses
     */
    object ExtractionService {
        const val KOREAN_QUERY = "백엔드 개발 레포 정보"
        const val ENGLISH_QUERY = "Show me the design document for authentication logic in Mobile App"
        const val TITLE_KOREAN_QUERY = "2025년 기술 부채 보고서 찾아줘"
        const val TITLE_ENGLISH_QUERY = "show me the 'Q3 Performance Review' document"

        val KOREAN_KEYWORDS_RESPONSE = chatResponse {
            content = "백엔드, backend, 개발, development, 레포, repository"
        }

        val ENGLISH_KEYWORDS_RESPONSE = chatResponse {
            content = "design document, authentication logic, Mobile App"
        }

        val KOREAN_TITLE_RESPONSE = chatResponse {
            content = "2025년 기술 부채 보고서, 기술 부채 보고서"
        }

        val ENGLISH_TITLE_RESPONSE = chatResponse {
            content = "Q3 Performance Review"
        }

        val EMPTY_RESPONSE = emptyChatResponse()

        val SINGLE_CHAR_KEYWORDS_RESPONSE = chatResponse {
            content = "a, bb, ccc, dddd"
        }

        val MANY_KEYWORDS_RESPONSE = chatResponse {
            content = (1..15).joinToString(", ") { "keyword$it" }
        }
    }

    /**
     * Date-related test data
     */
    object DateExtractor {
        const val KOREAN_YEAR_MONTH = "2024년 9월 회의록"
        const val SHORT_YEAR_FORMAT = "25년 8월 회의록"
        const val DASH_FORMAT = "2024-09 보고서"
        const val SLASH_FORMAT = "2024/09 회의록"
        const val SHORT_FORMAT_YYMMDD = "250908 회의록"
        const val KOREAN_SPECIFIC_DAY = "2024년 9월 15일 회의록"
        const val KOREAN_MULTIPLE_DATES = "2024년 8월과 2024년 9월 회의록"
        const val NO_DATE_TEXT = "Spring Boot tutorial"
    }

    /**
     * Email test samples
     */
    object EmailCleaner {
        val EMAIL_WITH_SIGNATURE = email {
            body = "This is the main content."
            withSignature("John Doe", "john@example.com")
        }

        val EMAIL_WITH_SENT_FROM = email {
            body = "This is the main content."
            withMobileSignature("iPhone")
        }

        val EMAIL_WITH_QUOTED_LINES = email {
            body = "This is my reply."
            addQuotedLine("This is a quoted line")
            addQuotedLine("Another quoted line")
        }

        val COMPLEX_REAL_WORLD_EMAIL = email {
            body = """
                Hi team,
                
                I wanted to follow up on our discussion.
                
                Best regards,
                John
            """.trimIndent()

            quotedReply {
                from("Jane", "2024-01-15")
                line("Thanks for the update.")
                nested {
                    from("Bob", "2024-01-14")
                    line("Here's the original message.")
                }
            }

            withSignature("John Doe", "john@example.com")
            withMobileSignature("iPhone")
        }

        const val SUBJECT_MULTIPLE_RE = "Re: Re: Re: Original Subject"
        const val SUBJECT_MULTIPLE_FWD = "Fwd: Fwd: Original Subject"
        const val SUBJECT_MIXED_PREFIXES = "  Re: Fwd: Original Subject  "
    }

    /**
     * PDF metadata test data
     */
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
