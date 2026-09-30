package com.johngabie.johnpdf.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The localized pieces [friendlyDate] needs.
 *
 * Passed in rather than read from resources inside the function so the formatting stays a
 * pure function: testable without a Context, and translatable without touching Kotlin.
 * The patterns are [DateTimeFormatter] patterns and live in strings.xml, because "d 'de' MMM"
 * is a Portuguese convention, not a universal one.
 */
data class DateLabels(
    val today: String,
    val yesterday: String,
    val sameYearPattern: String,
    val otherYearPattern: String,
)

/** "today", "yesterday", "Sep 12" or "Mar 3, 2025" — in whatever language [labels] carries. */
fun friendlyDate(
    epochMillis: Long,
    nowMillis: Long,
    labels: DateLabels,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return when {
        date == today -> labels.today
        date == today.minusDays(1) -> labels.yesterday
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern(labels.sameYearPattern, locale))
        else -> date.format(DateTimeFormatter.ofPattern(labels.otherYearPattern, locale))
    }
}
