package com.johngabie.johnpdf.ui.reader

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageMathTest {
    @Test fun spread_of_two_fingers_is_half_the_distance_between_them() {
        assertEquals(50f, fingerSpread(listOf(Offset(0f, 0f), Offset(100f, 0f))), 0.01f)
    }

    /** O que importa para o zoom é a razão: afastar os dedos ao dobro dobra a abertura. */
    @Test fun spread_ratio_tracks_finger_distance() {
        val before = fingerSpread(listOf(Offset(-20f, 0f), Offset(20f, 0f)))
        val after = fingerSpread(listOf(Offset(-60f, 0f), Offset(60f, 0f)))
        assertEquals(3f, after / before, 0.001f)
    }

    @Test fun spread_ignores_rotation_of_the_pair() {
        val horizontal = fingerSpread(listOf(Offset(-30f, 0f), Offset(30f, 0f)))
        val diagonal = fingerSpread(listOf(Offset(-30f, -40f), Offset(30f, 40f)))
        assertEquals(30f, horizontal, 0.01f)
        assertEquals(50f, diagonal, 0.01f)
    }

    @Test fun spread_needs_two_fingers() {
        assertEquals(0f, fingerSpread(emptyList()), 0f)
        assertEquals(0f, fingerSpread(listOf(Offset(10f, 10f))), 0f)
    }

    @Test fun centroid_is_the_middle_of_the_fingers() {
        assertEquals(Offset(10f, 20f), centroidOf(listOf(Offset(0f, 0f), Offset(20f, 40f))))
        assertEquals(Offset.Zero, centroidOf(emptyList()))
    }

    @Test fun picks_page_occupying_most_of_viewport() {
        val pages = listOf(VisiblePage(2, offset = -700, size = 1000), VisiblePage(3, offset = 312, size = 1000))
        assertEquals(3, dominantPage(pages, viewportStart = 0, viewportEnd = 1000))
    }
    @Test fun tie_goes_to_upper_page() {
        val pages = listOf(VisiblePage(0, offset = -500, size = 1000), VisiblePage(1, offset = 500, size = 1000))
        assertEquals(0, dominantPage(pages, 0, 1000))
    }
    @Test fun empty_returns_null() = assertNull(dominantPage(emptyList(), 0, 1000))
    @Test fun label_is_one_based() = assertEquals("Página 3 de 12", pageLabel(2, 12))
}
