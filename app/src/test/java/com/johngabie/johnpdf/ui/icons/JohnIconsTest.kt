package com.johngabie.johnpdf.ui.icons

import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Sanidade dos 22 Material Symbols vendorizados em JohnIcons.kt. */
@RunWith(AndroidJUnit4::class)
class JohnIconsTest {

    /**
     * Ordem alfabética, igual à de JohnIcons.All. Escrita à mão de propósito.
     *
     * `Schedule`/`ScheduleFilled` no lugar do `History`/`HistoryFilled` que o
     * plano pedia: upstream publica `history_fill1_24px.svg` byte a byte
     * idêntico ao contorno, o que deixaria o estado selecionado da bottom nav
     * indistinguível do não selecionado. `schedule` tem contorno e fill1 de
     * verdade. Ver o comentário em tools/icons/icons.txt.
     */
    private val nomesEsperados = listOf(
        "ArrowBack", "BrightnessAuto", "Close", "DarkMode", "Delete", "Dialpad", "Error",
        "Folder", "FolderOpen",
        "Keyboard", "KeyboardArrowDown", "KeyboardArrowUp",
        "LibraryBooks", "LibraryBooksFilled", "LightMode", "Lock", "PictureAsPdf",
        "Schedule", "ScheduleFilled",
        "ScreenLockRotation", "ScreenRotation", "Search", "SearchOff",
        "Visibility", "VisibilityOff",
    )

    @Test fun inventario_completo() {
        assertEquals(25, nomesEsperados.size)
        assertEquals(
            "JohnIcons.All não bate com o inventário; alguém criou um val e esqueceu de All",
            25,
            JohnIcons.All.size,
        )
    }

    @Test fun todos_os_icones_tem_24dp_e_viewport_960() {
        JohnIcons.All.forEach { icone ->
            assertEquals("${icone.name}: defaultWidth", 24.dp, icone.defaultWidth)
            assertEquals("${icone.name}: defaultHeight", 24.dp, icone.defaultHeight)
            assertEquals("${icone.name}: viewportWidth", 960f, icone.viewportWidth, 0f)
            assertEquals("${icone.name}: viewportHeight", 960f, icone.viewportHeight, 0f)
        }
    }

    /** Review Focus 4: sem nome próprio, o ícone não dá para ser identificado em lugar nenhum. */
    @Test fun todo_icone_tem_nome_unico_e_legivel() {
        val nomes = JohnIcons.All.map { it.name }
        assertEquals(
            "cada ImageVector deve se chamar JohnIcons.<NomeDoVal>",
            nomesEsperados.map { "JohnIcons.$it" },
            nomes,
        )
        assertEquals("nomes duplicados em JohnIcons", nomes.size, nomes.toSet().size)
        nomes.forEach { assertTrue("nome em branco", it.isNotBlank()) }
    }

    /** Review Focus 3: só a seta de voltar espelha em RTL. */
    @Test fun apenas_arrow_back_espelha_em_rtl() {
        assertTrue("ArrowBack deve espelhar em RTL", JohnIcons.ArrowBack.autoMirror)
        JohnIcons.All.filter { it.name != "JohnIcons.ArrowBack" }.forEach { icone ->
            assertFalse("${icone.name} não deve espelhar em RTL", icone.autoMirror)
        }
    }
}
