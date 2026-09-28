package com.johngabie.johnpdf.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageMathTest {
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
