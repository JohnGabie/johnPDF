package com.johngabie.johnpdf.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FriendlyDateTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun millis(y: Int, m: Int, d: Int, h: Int = 12) =
        LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()
    private val now = millis(2026, 9, 28, 15)

    private val en = DateLabels("today", "yesterday", "MMM d", "MMM d, yyyy")
    private val pt = DateLabels("hoje", "ontem", "d 'de' MMM", "d 'de' MMM 'de' yyyy")

    private fun en(at: Long) = friendlyDate(at, now, en, zone, Locale.ENGLISH)
    private fun pt(at: Long) = friendlyDate(at, now, pt, zone, Locale.forLanguageTag("pt-BR"))

    @Test fun same_day_is_today() = assertEquals("today", en(millis(2026, 9, 28, 0)))
    @Test fun previous_day_is_yesterday() = assertEquals("yesterday", en(millis(2026, 9, 27, 23)))
    @Test fun same_year_shows_day_and_month() = assertEquals("Sep 12", en(millis(2026, 9, 12)))
    @Test fun other_year_includes_year() = assertEquals("Mar 3, 2025", en(millis(2025, 3, 3)))

    // The same instants, formatted with the Portuguese labels and locale.
    @Test fun same_day_is_hoje() = assertEquals("hoje", pt(millis(2026, 9, 28, 0)))
    @Test fun previous_day_is_ontem() = assertEquals("ontem", pt(millis(2026, 9, 27, 23)))
    @Test fun same_year_in_portuguese() = assertEquals("12 de set.", pt(millis(2026, 9, 12)))
    @Test fun other_year_in_portuguese() = assertEquals("3 de mar. de 2025", pt(millis(2025, 3, 3)))
}
