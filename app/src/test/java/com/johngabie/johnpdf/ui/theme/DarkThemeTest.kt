package com.johngabie.johnpdf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DarkThemeTest {
    @get:Rule val rule = createComposeRule()

    /**
     * Lê o mesmo valor nos dois esquemas numa composição só.
     *
     * `createComposeRule()` aceita **um** `setContent` por teste, então os dois
     * temas entram como irmãos na mesma árvore em vez de duas chamadas.
     */
    private fun <T> bothThemes(read: @Composable () -> T): Pair<T, T> {
        var claro: T? = null
        var escuro: T? = null
        rule.setContent {
            JohnPdfTheme(dark = false) { claro = read() }
            JohnPdfTheme(dark = true) { escuro = read() }
        }
        return claro!! to escuro!!
    }

    @Test fun each_theme_selects_its_own_scheme() {
        val (claro, escuro) = bothThemes { MaterialTheme.colorScheme }
        assertEquals(Color(0xFFF7FAFC), claro.surface)
        assertEquals(Color(0xFF006494), claro.primary)
        assertEquals(Color(0xFF0F1417), escuro.surface)
        assertEquals(Color(0xFF96C4E5), escuro.primary)
    }

    @Test fun surfaces_never_collide_between_schemes() {
        val (claro, escuro) = bothThemes { MaterialTheme.colorScheme }
        assertEquals(false, claro.surface == escuro.surface)
        assertEquals(false, claro.surfaceContainer == escuro.surfaceContainer)
    }
}
