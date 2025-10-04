package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

/**
 * Extract and normalize date expressions from text
 * Handles various Korean and English date formats
 * OPTIMIZED: Avoids generating too many day-level variations for month-only queries
 */
object DateExtractor {

    private val yearMonthKoreanRegex = Regex("""(\d{4})년\s*(\d{1,2})월""")
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
     * * @param includeAllDays If true, adds YYMMDD for all days in the month
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

        // Short formats (YYMM)
        set.add("$shortYear$month") // 2509

        // Generate day-level dates only if needed
        if (includeAllDays) {
            try {
                val yearMonth = YearMonth.of(yearInt, monthInt)
                val daysInMonth = yearMonth.lengthOfMonth()

                log.debug { "Generating day-level dates for $year-$month ($daysInMonth days)" }

                // Add YYMMDD patterns for each day in the month
                for (day in 1..daysInMonth) {
                    val dayStr = day.toString().padStart(2, '0')
                    set.add("$shortYear$month$dayStr") // e.g., 250901, 250902, ..., 250930
                }
            } catch (e: Exception) {
                log.warn { "Failed to parse date for day generation: $year-$month" }
            }
        }

        // English month name and Korean month
        try {
            val yearMonth = YearMonth.of(yearInt, monthInt)

            val monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMMM", java.util.Locale.ENGLISH))
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
            yearMonthDashRegex.containsMatchIn(text) ||
            yearMonthSlashRegex.containsMatchIn(text) ||
            shortYearMonthRegex.containsMatchIn(text)
    }
}
