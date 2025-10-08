package com.okestro.okchat.ai.support.extraction

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateExtractorTest {

    @Test
    fun `extractDateKeywords should extract Korean year-month format`() {
        // Given
        val text = "2024년 9월 회의록"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.contains("2024년 09월"))
        assertTrue(keywords.contains("2024-09"))
        assertTrue(keywords.contains("2024/09"))
        assertTrue(keywords.contains("2409")) // YYMM format
    }

    @Test
    fun `extractDateKeywords should extract short year format`() {
        // Given
        val text = "25년 8월 회의록"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.contains("2025년 08월"))
        assertTrue(keywords.contains("2025-08"))
        assertTrue(keywords.contains("2508")) // YYMM format
    }

    @Test
    fun `extractDateKeywords should extract dash format`() {
        // Given
        val text = "2024-09 보고서"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.contains("2024년 09월"))
        assertTrue(keywords.contains("2024-09"))
        assertTrue(keywords.contains("2409"))
    }

    @Test
    fun `extractDateKeywords should extract slash format`() {
        // Given
        val text = "2024/09 회의록"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.contains("2024년 09월"))
        assertTrue(keywords.contains("2024-09"))
        assertTrue(keywords.contains("2024/09"))
    }

    @Test
    fun `extractDateKeywords should extract short format YYMMDD`() {
        // Given
        val text = "250908 회의록"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.contains("250908")) // YYMMDD
        assertTrue(keywords.contains("2509")) // YYMM
        assertTrue(keywords.contains("2025년 09월"))
    }

    @Test
    fun `extractDateKeywords should add English month names`() {
        // Given
        val text = "2024년 9월 회의록"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.contains("September"))
        assertTrue(keywords.contains("9월"))
    }

    @Test
    fun `extractDateKeywords should handle multiple date expressions`() {
        // Given
        val text = "2024년 8월과 2024년 9월 회의록"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.any { it.contains("08") || it.contains("8") })
        assertTrue(keywords.any { it.contains("09") || it.contains("9") })
    }

    @Test
    fun `extractDateKeywords should generate strategic days when includeAllDays is true`() {
        // Given
        val text = "2024년 9월"

        // When
        val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = true)

        // Then
        // Should have strategic day patterns
        assertTrue(keywords.any { it.contains("2409") })
        // Should have some YYMMDD patterns for strategic days
        val dayPatterns = keywords.filter { it.matches(Regex("\\d{6}")) }
        assertTrue(dayPatterns.isNotEmpty())
        // Should not have all 30 days (optimized for RRF)
        assertTrue(dayPatterns.size < 30)
    }

    @Test
    fun `extractDateKeywords should detect specific day and include day patterns`() {
        // Given
        val text = "2024년 9월 15일 회의록"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        // Should automatically include day patterns when "일" is detected
        val dayPatterns = keywords.filter { it.matches(Regex("\\d{6}")) }
        assertTrue(dayPatterns.isNotEmpty())
    }

    @Test
    fun `extractDateKeywords should not generate day patterns by default`() {
        // Given
        val text = "2024년 9월"

        // When
        val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = false)

        // Then
        // Should have month-level keywords
        assertTrue(keywords.contains("2024년 09월"))
        assertTrue(keywords.contains("2409"))
        // Month-level keywords should be present without excessive day patterns
        assertTrue(keywords.isNotEmpty())
    }

    @Test
    fun `extractDateKeywords should pad single-digit months`() {
        // Given
        val text = "2024년 8월"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.contains("2024년 08월")) // padded
        assertTrue(keywords.contains("2024-08")) // padded
        assertTrue(keywords.contains("2408")) // padded
    }

    @Test
    fun `containsDateExpression should return true for Korean date format`() {
        // Then
        assertTrue(DateExtractor.containsDateExpression("2024년 9월"))
        assertTrue(DateExtractor.containsDateExpression("25년 8월"))
    }

    @Test
    fun `containsDateExpression should return true for dash format`() {
        // Then
        assertTrue(DateExtractor.containsDateExpression("2024-09"))
    }

    @Test
    fun `containsDateExpression should return true for slash format`() {
        // Then
        assertTrue(DateExtractor.containsDateExpression("2024/09"))
    }

    @Test
    fun `containsDateExpression should return true for short format`() {
        // Then
        assertTrue(DateExtractor.containsDateExpression("250908"))
    }

    @Test
    fun `containsDateExpression should return false for text without dates`() {
        // Then
        assertFalse(DateExtractor.containsDateExpression("Spring Boot tutorial"))
        assertFalse(DateExtractor.containsDateExpression("회의록"))
    }

    @Test
    fun `extractDateKeywords should handle text without dates`() {
        // Given
        val text = "Spring Boot tutorial"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.isEmpty())
    }

    @Test
    fun `extractDateKeywords should handle empty string`() {
        // Given
        val text = ""

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        assertTrue(keywords.isEmpty())
    }

    @Test
    fun `extractDateKeywords should handle February with correct days`() {
        // Given - 2024 is a leap year
        val text = "2024년 2월"

        // When
        val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = true)

        // Then
        // Should have day 29 (leap year) in the strategic days
        assertTrue(keywords.any { it == "240229" })
    }

    @Test
    fun `extractDateKeywords should add wildcard patterns`() {
        // Given
        val text = "2025년 9월"

        // When
        val keywords = DateExtractor.extractDateKeywords(text)

        // Then
        // Should include wildcard pattern for flexible matching
        assertTrue(keywords.contains("2509*"))
    }
}
