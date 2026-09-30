package com.johngabie.johnpdf.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val SAME_YEAR = DateTimeFormatter.ofPattern("d 'de' MMM", PT_BR)
private val OTHER_YEAR = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", PT_BR)

/** "hoje", "ontem", "12 de set." ou "3 de mar. de 2025". */
fun friendlyDate(epochMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return when {
        date == today -> "hoje"
        date == today.minusDays(1) -> "ontem"
        date.year == today.year -> date.format(SAME_YEAR)
        else -> date.format(OTHER_YEAR)
    }
}
