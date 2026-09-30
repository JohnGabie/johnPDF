package com.johngabie.johnpdf.util

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FriendlyDateTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun millis(y: Int, m: Int, d: Int, h: Int = 12) =
        LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()
    private val now = millis(2026, 9, 28, 15)

    @Test fun same_day_is_hoje() = assertEquals("hoje", friendlyDate(millis(2026, 9, 28, 0), now, zone))
    @Test fun previous_day_is_ontem() = assertEquals("ontem", friendlyDate(millis(2026, 9, 27, 23), now, zone))
    @Test fun same_year_shows_day_and_month() = assertEquals("12 de set.", friendlyDate(millis(2026, 9, 12), now, zone))
    @Test fun other_year_includes_year() = assertEquals("3 de mar. de 2025", friendlyDate(millis(2025, 3, 3), now, zone))
}
