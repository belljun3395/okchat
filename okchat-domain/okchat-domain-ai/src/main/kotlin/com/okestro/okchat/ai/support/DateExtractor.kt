package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val log = KotlinLogging.logger {}

/**
 * Extract and normalize date expressions from text
 * Handles various Korean and English date formats
 * OPTIMIZED: Avoids generating too many day-level variations for month-only queries
 */
object DateExtractor {

    private val yearMonthKoreanRegex = Regex("""(\d{4})년\s*(\d{1,2})월""")
    private val shortYearMonthKoreanRegex = Regex("""(\d{2})년\s*(\d{1,2})월""") // 25년 8월 형식
    private val yearMonthDashRegex = Regex("""(\d{4})-(\d{1,2})""")
    private val yearMonthSlashRegex = Regex("""(\d{4})/(\d{1,2})""")
    private val shortYearMonthRegex = Regex("""(\d{2})(\d{2})(\d{2})""") // 250710 형식
    private val specificDayRegex = Regex("""(\d{1,2})일""") // "28일" 형식

    /**
     * Extract all date-related keywords from text
     * Returns multiple variations for better search coverage
     * * @param text The text to extract dates from
     * @param includeAllDays If true, generate YYMMDD for each day in month (use only for specific day queries)
     */
    fun extractDateKeywords(text: String, includeAllDays: Boolean = false): List<String> {
        val dateKeywords = mutableSetOf<String>()

        // Check if query mentions specific day
        val hasSpecificDay = specificDayRegex.containsMatchIn(text) || text.contains("일", ignoreCase = true)

        val shouldIncludeAllDays = includeAllDays || hasSpecificDay

        // Extract "YYYY년 MM월" format
        yearMonthKoreanRegex.findAll(text).forEach { match ->
            val year = match.groupValues[1]
            val month = match.groupValues[2].padStart(2, '0')
            addDateVariations(dateKeywords, year, month, shouldIncludeAllDays)
        }

        // Extract "YY년 MM월" format (e.g., "25년 8월")
        shortYearMonthKoreanRegex.findAll(text).forEach { match ->
            val shortYear = match.groupValues[1]
            val month = match.groupValues[2].padStart(2, '0')
            // Assume 20XX for years
            val fullYear = "20$shortYear"
            addDateVariations(dateKeywords, fullYear, month, shouldIncludeAllDays)
        }

        // Extract "YYYY-MM" format
        yearMonthDashRegex.findAll(text).forEach { match ->
            val year = match.groupValues[1]
            val month = match.groupValues[2].padStart(2, '0')
            addDateVariations(dateKeywords, year, month, shouldIncludeAllDays)
        }

        // Extract "YYYY/MM" format
        yearMonthSlashRegex.findAll(text).forEach { match ->
            val year = match.groupValues[1]
            val month = match.groupValues[2].padStart(2, '0')
            addDateVariations(dateKeywords, year, month, shouldIncludeAllDays)
        }

        // Extract short format "YYMMDD" or "YYMM"
        shortYearMonthRegex.findAll(text).forEach { match ->
            val shortYear = match.groupValues[1]
            val month = match.groupValues[2]
            val day = match.groupValues[3]

            // Assume 20XX for years
            val fullYear = "20$shortYear"

            // Add YYMMDD if it looks like a specific date
            if (day != "00") {
                dateKeywords.add("$shortYear$month$day") // 250710
            }

            // Always add YYMM
            dateKeywords.add("$shortYear$month") // 2507
            dateKeywords.add("${fullYear}년 ${month}월") // 2025년 07월
            dateKeywords.add("$fullYear-$month") // 2025-07
        }

        log.debug { "Extracted ${dateKeywords.size} date keywords (includeAllDays=$shouldIncludeAllDays)" }

        return dateKeywords.toList()
    }

    /**
     * Add multiple date format variations for a given year and month
     * * @param includeAllDays If true, adds strategic day patterns instead of all 30 days (RRF optimized)
     */
    private fun addDateVariations(set: MutableSet<String>, year: String, month: String, includeAllDays: Boolean) {
        val monthInt = month.toIntOrNull() ?: return
        val yearInt = year.toIntOrNull() ?: return

        // Short year (YY)
        val shortYear = year.substring(2)

        // Korean format
        set.add("${year}년 ${month}월")
        set.add("${year}년${month}월") // No space

        // Dash format
        set.add("$year-$month")

        // Slash format
        set.add("$year/$month")

        // Short formats (YYMM) - for month-level matching
        set.add("$shortYear$month") // 2509

        // RRF-optimized: Add partial patterns for better title matching
        // "2509" matches "250908", "250901", etc. via substring matching
        set.add("$shortYear$month*") // Wildcard pattern (if supported by search)

        // Strategic day samples instead of all days (RRF optimization)
        if (includeAllDays) {
            try {
                val yearMonth = YearMonth.of(yearInt, monthInt)
                val daysInMonth = yearMonth.lengthOfMonth()

                log.debug { "Generating strategic day patterns for $year-$month (RRF optimized)" }

                // Add key dates only (reduces from 30 keywords to ~10)
                val strategicDays = buildList {
                    // Week starts (Mondays typically)
                    add(1) // First day
                    add(8) // Week 2 start
                    add(15) // Week 3 start (mid-month)
                    add(22) // Week 4 start
                    if (daysInMonth >= 29) add(29) // Week 5 start

                    // Common meeting days
                    add(7) // End of first week
                    add(14) // End of second week
                    add(21) // End of third week
                    add(28) // End of fourth week

                    // Last day
                    add(daysInMonth)
                }

                strategicDays.distinct().sorted().forEach { day ->
                    val dayStr = day.toString().padStart(2, '0')
                    set.add("$shortYear$month$dayStr") // e.g., 250901, 250908, 250915, 250922, 250929, 250930
                }

                log.debug { "Generated ${strategicDays.size} strategic day keywords (instead of $daysInMonth)" }
            } catch (e: Exception) {
                log.warn { "Failed to parse date for day generation: $year-$month" }
            }
        }

        // English month name and Korean month
        try {
            val yearMonth = YearMonth.of(yearInt, monthInt)

            val monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))
            set.add(monthName) // September

            val monthNameKr = when (monthInt) {
                1 -> "1월"
                2 -> "2월"
                3 -> "3월"
                4 -> "4월"
                5 -> "5월"
                6 -> "6월"
                7 -> "7월"
                8 -> "8월"
                9 -> "9월"
                10 -> "10월"
                11 -> "11월"
                12 -> "12월"
                else -> null
            }
            monthNameKr?.let { set.add(it) }
        } catch (e: Exception) {
            log.warn { "Failed to parse date for month name: $year-$month" }
        }
    }

    /**
     * Check if text contains any date expressions
     */
    fun containsDateExpression(text: String): Boolean {
        return yearMonthKoreanRegex.containsMatchIn(text) ||
            shortYearMonthKoreanRegex.containsMatchIn(text) ||
            yearMonthDashRegex.containsMatchIn(text) ||
            yearMonthSlashRegex.containsMatchIn(text) ||
            shortYearMonthRegex.containsMatchIn(text)
    }
}
