package com.okestro.okchat.ai.support

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("DateExtractor Enhanced Tests")
class DateExtractorImprovedTest {

    @Nested
    @DisplayName("Korean Date Format Extraction")
    inner class KoreanDateFormatTests {

        @ParameterizedTest(name = "should extract date from: {0}")
        @ValueSource(
            strings = [
                "2024년 9월 회의록",
                "2024년9월 회의록",
                "2024년  9월 회의록"
            ]
        )
        @DisplayName("should handle various spacing in Korean format")
        fun `should handle various spacing in Korean format`(text: String) {
            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords shouldContain "2024년 09월"
            keywords shouldContain "2024-09"
            keywords shouldContain "2409"
        }

        @ParameterizedTest(name = "month {0} should be padded to {1}")
        @CsvSource(
            "1, 01",
            "2, 02",
            "9, 09",
            "10, 10",
            "12, 12"
        )
        @DisplayName("should pad single-digit months correctly")
        fun `should pad single-digit months correctly`(month: String, paddedMonth: String) {
            // given
            val text = "2024년 ${month}월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords shouldContain "2024년 ${paddedMonth}월"
            keywords shouldContain "2024-$paddedMonth"
        }

        @Test
        @DisplayName("should include English month names")
        fun `should include English month names`() {
            // given
            val monthNames = mapOf(
                "2024년 1월" to "January",
                "2024년 2월" to "February",
                "2024년 3월" to "March",
                "2024년 12월" to "December"
            )

            monthNames.forEach { (text, expectedMonth) ->
                // when
                val keywords = DateExtractor.extractDateKeywords(text)

                // then
                keywords shouldContain expectedMonth
            }
        }
    }

    @Nested
    @DisplayName("Short Year Format Tests")
    inner class ShortYearFormatTests {

        @ParameterizedTest(name = "{0} should convert to 20{0}")
        @ValueSource(strings = ["25", "24", "23", "26"])
        @DisplayName("should assume 20XX for short years")
        fun `should assume 20XX for short years`(shortYear: String) {
            // given
            val text = "${shortYear}년 8월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords shouldContain "20${shortYear}년 08월"
            keywords shouldContain "20$shortYear-08"
        }

        @Test
        @DisplayName("should handle YYMMDD format")
        fun `should handle YYMMDD format`() {
            // given
            val text = "250908 회의록"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords shouldContain "250908" // YYMMDD
            keywords shouldContain "2509" // YYMM
            keywords shouldContain "2025년 09월"
        }

        @Test
        @DisplayName("should not generate day patterns for YYMM00 format")
        fun `should not generate day patterns for YYMM00 format`() {
            // given
            val text = "250900 보고서"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords.none { it == "250900" } shouldBe true
            keywords shouldContain "2509"
        }
    }

    @Nested
    @DisplayName("Multiple Format Support")
    inner class MultipleFormatTests {

        @ParameterizedTest(name = "should extract {0} format")
        @CsvSource(
            "2024-09, dash",
            "2024/09, slash",
            "2024-9, dash with single digit",
            "2024/9, slash with single digit"
        )
        @DisplayName("should support dash and slash formats")
        fun `should support dash and slash formats`(dateFormat: String, description: String) {
            // when
            val keywords = DateExtractor.extractDateKeywords(dateFormat)

            // then
            keywords shouldContain "2024년 09월"
            keywords shouldContain "2409"
        }

        @Test
        @DisplayName("should handle mixed formats in one text")
        fun `should handle mixed formats in one text`() {
            // given
            val text = "2024년 8월, 2024-09, 2024/10 보고서"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords shouldContain "2024년 08월"
            keywords shouldContain "2024년 09월"
            keywords shouldContain "2024년 10월"
        }
    }

    @Nested
    @DisplayName("Strategic Day Generation")
    inner class StrategicDayGenerationTests {

        @Test
        @DisplayName("should generate strategic days when includeAllDays is true")
        fun `should generate strategic days when includeAllDays is true`() {
            // given
            val text = "2024년 9월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = true)

            // then
            val dayPatterns = keywords.filter { it.matches(Regex("\\d{6}")) }
            dayPatterns.shouldNotBeEmpty()
            dayPatterns.size shouldBeLessThan 30 // Strategic, not all days
        }

        @Test
        @DisplayName("should include strategic days: 1, 8, 15, 22, 29")
        fun `should include strategic days - 1, 8, 15, 22, 29`() {
            // given
            val text = "2024년 9월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = true)

            // then
            keywords shouldContain "240901"
            keywords shouldContain "240908"
            keywords shouldContain "240915"
            keywords shouldContain "240922"
            keywords shouldContain "240929"
        }

        @Test
        @DisplayName("should include last day of month")
        fun `should include last day of month`() {
            // given
            val testCases = listOf(
                "2024년 9월" to "240930", // 30 days
                "2024년 2월" to "240229", // Leap year
                "2023년 2월" to "230228" // Non-leap year
            )

            testCases.forEach { (text, expectedLastDay) ->
                // when
                val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = true)

                // then
                keywords shouldContain expectedLastDay
            }
        }

        @Test
        @DisplayName("should auto-detect specific day and generate day patterns")
        fun `should auto-detect specific day and generate day patterns`() {
            // given
            val text = "2024년 9월 15일 회의록"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            val dayPatterns = keywords.filter { it.matches(Regex("\\d{6}")) }
            dayPatterns.shouldNotBeEmpty()
        }

        @Test
        @DisplayName("should not generate day patterns by default")
        fun `should not generate day patterns by default`() {
            // given
            val text = "2024년 9월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = false)

            // then
            keywords shouldContain "2024년 09월"
            keywords shouldContain "2409"
            val sixDigitPatterns = keywords.filter { it.matches(Regex("\\d{6}")) && it != "240900" }
            sixDigitPatterns.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("Wildcard and Special Patterns")
    inner class WildcardPatternTests {

        @Test
        @DisplayName("should include wildcard pattern for flexible matching")
        fun `should include wildcard pattern for flexible matching`() {
            // given
            val text = "2025년 9월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords shouldContain "2509*"
        }

        @Test
        @DisplayName("should include Korean month names")
        fun `should include Korean month names`() {
            // given
            val text = "2024년 9월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords shouldContain "9월"
        }

        @Test
        @DisplayName("should include no-space Korean format")
        fun `should include no-space Korean format`() {
            // given
            val text = "2024년 9월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords shouldContain "2024년09월" // No space variant
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty string")
        fun `should handle empty string`() {
            // when
            val keywords = DateExtractor.extractDateKeywords("")

            // then
            keywords.shouldBeEmpty()
        }

        @Test
        @DisplayName("should handle text without dates")
        fun `should handle text without dates`() {
            // given
            val text = "Spring Boot tutorial"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords.shouldBeEmpty()
        }

        @Test
        @DisplayName("should handle invalid month values gracefully")
        fun `should handle invalid month values gracefully`() {
            // given
            val text = "2024년 13월" // Invalid month

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            // Should not crash, may return empty or handle gracefully
            // The actual behavior depends on implementation
        }

        @Test
        @DisplayName("should handle February leap year correctly")
        fun `should handle February leap year correctly`() {
            // given
            val leapYear = "2024년 2월"
            val nonLeapYear = "2023년 2월"

            // when
            val leapKeywords = DateExtractor.extractDateKeywords(leapYear, includeAllDays = true)
            val nonLeapKeywords = DateExtractor.extractDateKeywords(nonLeapYear, includeAllDays = true)

            // then
            leapKeywords shouldContain "240229" // Leap year has day 29
            nonLeapKeywords shouldContain "230228" // Non-leap year ends on 28
        }

        @Test
        @DisplayName("should handle multiple date expressions in one text")
        fun `should handle multiple date expressions in one text`() {
            // given
            val text = "2024년 8월과 2024년 9월 그리고 2024-10 회의록"

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            keywords shouldContain "2024년 08월"
            keywords shouldContain "2024년 09월"
            keywords shouldContain "2024년 10월"
        }

        @Test
        @DisplayName("should not duplicate keywords")
        fun `should not duplicate keywords`() {
            // given
            val text = "2024년 9월 2024년 9월" // Duplicate dates

            // when
            val keywords = DateExtractor.extractDateKeywords(text)

            // then
            val uniqueKeywords = keywords.toSet()
            keywords.size shouldBe uniqueKeywords.size
        }
    }

    @Nested
    @DisplayName("containsDateExpression Tests")
    inner class ContainsDateExpressionTests {

        @ParameterizedTest(name = "should detect: {0}")
        @ValueSource(
            strings = [
                "2024년 9월",
                "25년 8월",
                "2024-09",
                "2024/09",
                "250908"
            ]
        )
        @DisplayName("should return true for valid date expressions")
        fun `should return true for valid date expressions`(text: String) {
            // when & then
            DateExtractor.containsDateExpression(text) shouldBe true
        }

        @ParameterizedTest(name = "should not detect: {0}")
        @ValueSource(
            strings = [
                "Spring Boot tutorial",
                "회의록",
                "Documentation",
                "API Guide",
                ""
            ]
        )
        @DisplayName("should return false for text without dates")
        fun `should return false for text without dates`(text: String) {
            // when & then
            DateExtractor.containsDateExpression(text) shouldBe false
        }

        @Test
        @DisplayName("should detect date expression in mixed content")
        fun `should detect date expression in mixed content`() {
            // given
            val text = "Please review the 2024년 9월 meeting notes"

            // when & then
            DateExtractor.containsDateExpression(text) shouldBe true
        }
    }

    @Nested
    @DisplayName("Performance and Optimization")
    inner class PerformanceTests {

        @Test
        @DisplayName("should limit day generation for performance")
        fun `should limit day generation for performance`() {
            // given
            val text = "2024년 9월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = true)

            // then
            val dayPatterns = keywords.filter { it.matches(Regex("\\d{6}")) }
            dayPatterns.size shouldBeLessThan 15 // Much less than 30 days
        }

        @Test
        @DisplayName("should generate reasonable number of keywords")
        fun `should generate reasonable number of keywords`() {
            // given
            val text = "2024년 9월"

            // when
            val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = false)

            // then
            keywords.size shouldBeLessThan 20 // Reasonable number without day patterns
        }

        @Test
        @DisplayName("should handle multiple months efficiently")
        fun `should handle multiple months efficiently`() {
            // given
            val text = "2024년 1월부터 12월까지"

            // when
            val keywords = DateExtractor.extractDateKeywords(text, includeAllDays = false)

            // then
            keywords.shouldNotBeEmpty()
            // Should have variations for multiple months but not exponential growth
            keywords.size shouldBeLessThan 100
        }
    }
}
