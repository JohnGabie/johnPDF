package com.johngabie.johnpdf.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.ui.home.HomeTab
import com.johngabie.johnpdf.ui.home.HomeUiState
import com.johngabie.johnpdf.ui.home.HomeContent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A troca manual de tema: o modo salvo manda, o botão cicla, e a escolha sobrevive. */
@RunWith(AndroidJUnit4::class)
class ThemeSwitchTest {

    @get:Rule val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun limpaPreferencia() {
        context.getSharedPreferences(ThemePreferences.FILE, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    /** Devolve o `primary` que o tema aplicou — é o jeito mais curto de dizer qual esquema subiu. */
    private fun primaryAoAbrir(): Color {
        var primary: Color? = null
        composeRule.setContent {
            JohnPdfTheme { primary = MaterialTheme.colorScheme.primary; Text("x") }
        }
        composeRule.waitForIdle()
        return primary!!
    }

    // ---------------------------------------------------------------- T2: lê a preferência

    @Test fun modo_escuro_salvo_abre_no_escuro() {
        ThemePreferences(context).write(ThemeMode.DARK)
        assertEquals(DarkSchemeForTest.primary, primaryAoAbrir())
    }

    /** O sistema está no escuro e o usuário pediu claro: quem manda é o usuário. */
    @Test
    @Config(qualifiers = "night")
    fun modo_claro_salvo_ganha_do_sistema_escuro() {
        ThemePreferences(context).write(ThemeMode.LIGHT)
        assertEquals(LightSchemeForTest.primary, primaryAoAbrir())
    }

    @Test
    @Config(qualifiers = "night")
    fun seguir_o_sistema_abre_no_escuro_quando_o_sistema_esta_escuro() {
        ThemePreferences(context).write(ThemeMode.SYSTEM)
        assertEquals(DarkSchemeForTest.primary, primaryAoAbrir())
    }

    // ------------------------------------------------------- T4: o botão troca em tempo real

    private fun showHome(mode: ThemeMode = ThemeMode.SYSTEM): MutableList<Color> {
        ThemePreferences(context).write(mode)
        val vistos = mutableListOf<Color>()
        composeRule.setContent {
            JohnPdfTheme {
                vistos += MaterialTheme.colorScheme.primary
                HomeContent(
                    state = HomeUiState(tab = HomeTab.RECENTS),
                    onOpenPicker = {},
                    onSelectTab = {},
                    onQueryChange = {},
                    onOpenRecent = {},
                    onRemoveRecent = {},
                    onOpenPdf = {},
                    onRequestPermission = {},
                    onDismissError = {},
                )
            }
        }
        composeRule.waitForIdle()
        return vistos
    }

    @Test fun o_botao_existe_no_header_da_home() {
        showHome()
        composeRule.onNodeWithTag("theme_switch").assertIsDisplayed()
    }

    @Test fun um_toque_troca_o_esquema_sem_reabrir_a_tela() {
        // De LIGHT o próximo é DARK: é a travessia que tem de repintar na hora.
        val vistos = showHome(ThemeMode.LIGHT)
        assertEquals(LightSchemeForTest.primary, vistos.last())
        composeRule.onNodeWithTag("theme_switch").performClick()
        composeRule.waitForIdle()
        assertEquals(
            "o toque tem de repintar a árvore, não só salvar a preferência",
            DarkSchemeForTest.primary,
            vistos.last(),
        )
    }

    /**
     * Um ciclo de três com "seguir o sistema" dentro sempre tem um passo que não muda cor:
     * SYSTEM num celular claro e LIGHT pintam igual. Não é bug e não tem conserto — o que
     * muda é o ícone e o que o TalkBack anuncia. O teste existe para ninguém "consertar"
     * isso embaralhando a ordem do ciclo, que é o que deixaria o botão imprevisível.
     */
    @Test fun de_seguir_o_sistema_para_claro_o_celular_claro_nao_muda_de_cor() {
        val vistos = showHome(ThemeMode.SYSTEM)
        val antes = vistos.last()
        composeRule.onNodeWithTag("theme_switch").performClick()
        composeRule.waitForIdle()
        assertEquals(antes, vistos.last())
        assertEquals(ThemeMode.LIGHT, ThemePreferences(context).read())
    }

    @Test fun tres_toques_voltam_ao_esquema_inicial() {
        val vistos = showHome()
        val antes = vistos.last()
        repeat(3) {
            composeRule.onNodeWithTag("theme_switch").performClick()
            composeRule.waitForIdle()
        }
        assertEquals(antes, vistos.last())
    }

    @Test fun a_escolha_do_botao_fica_salva() {
        showHome()
        composeRule.onNodeWithTag("theme_switch").performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.SYSTEM.next(), ThemePreferences(context).read())
    }

    // ------------------------------------------------------- T5: os três modos compõem

    @Test fun modo_seguir_o_sistema_compoe() = renderizaCom(ThemeMode.SYSTEM)

    @Test fun modo_claro_compoe() = renderizaCom(ThemeMode.LIGHT)

    @Test fun modo_escuro_compoe() = renderizaCom(ThemeMode.DARK)

    private fun renderizaCom(mode: ThemeMode) {
        ThemePreferences(context).write(mode)
        composeRule.setContent { JohnPdfTheme { Text(mode.name) } }
        composeRule.onNodeWithText(mode.name).assertIsDisplayed()
    }
}

