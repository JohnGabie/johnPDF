package com.johngabie.johnpdf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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

    @Test fun page_gap_follows_the_theme() {
        val (claro, escuro) = bothThemes { JohnTheme.colors }
        assertEquals(Color(0xFFE5EAEE), claro.pageGap)
        assertEquals(Color(0xFF0A0F12), escuro.pageGap)
    }

    /** Requisito do usuário: a página do PDF é branca nos dois modos. */
    @Test fun pdf_page_stays_white_in_both_modes() {
        assertEquals(Color.White, PdfPageBackground)
        val (claro, escuro) = bothThemes { JohnTheme.colors }
        assertNotEquals(Color.White, claro.pageGap)
        assertNotEquals(Color.White, escuro.pageGap)
    }

    /** No escuro o vão precisa emoldurar a página branca. */
    @Test fun page_gap_frames_the_white_page_in_dark() {
        val (_, escuro) = bothThemes { JohnTheme.colors }
        val r = contrast(Color.White, escuro.pageGap)
        assertTrue("vão/página no escuro = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
    }

    /**
     * O indicador da aba vem de JohnColors, não do secondaryContainer do M3
     * (que dá 1,12:1 contra a barra no claro).
     */
    @Test fun navigation_bar_item_colors_use_the_tab_indicator_role() {
        val (claro, escuro) = bothThemes {
            // Compor o helper aqui garante que ele existe e não estoura na composição.
            johnNavigationBarItemColors()
            JohnTheme.colors.tabIndicator to MaterialTheme.colorScheme.onSurfaceVariant
        }
        assertEquals(Color(0xFF365A6C), claro.first)
        assertEquals(Color(0xFF5A7385), escuro.first)
        assertNotEquals(claro.first, claro.second)
        assertNotEquals(escuro.first, escuro.second)
    }

    /** As barras e os diálogos têm um papel só, e ele muda com o tema. */
    @Test fun bar_and_dialog_colors_come_from_the_scheme() {
        val (claro, escuro) = bothThemes { JohnTheme.barColor to JohnTheme.dialogColor }
        assertEquals(Color(0xFFEBEFF2) to Color(0xFFE5EAEE), claro)
        assertEquals(Color(0xFF1B2124) to Color(0xFF262B2F), escuro)
    }
}
