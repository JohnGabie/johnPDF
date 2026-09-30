package com.johngabie.johnpdf.ui.reader

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.PAGE_RENDER_FAILED_MESSAGE
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderContentTest {
    @get:Rule val rule = createComposeRule()

    private val a4 = PageSize(595f, 842f)
    private var backCalls = 0
    private var rotationToggles = 0
    private var submitted: String? = null

    private fun show(initial: ReaderUiState, render: suspend (Int, Int) -> Bitmap? = { _, w -> Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888) }) {
        rule.setContent {
            var state by remember { mutableStateOf(initial) }
            JohnPdfTheme {
                ReaderContent(
                    state = state,
                    onBack = { backCalls++ },
                    onPageVisible = { state = state.copy(currentPage = it) },
                    onZoomChange = { state = state.copy(zoom = it) },
                    onDoubleTap = {},
                    onToggleRotation = { rotationToggles++; state = state.copy(rotationLocked = !state.rotationLocked) },
                    onSubmitPassword = { submitted = it },
                    renderPage = render,
                )
            }
        }
    }

    @Test fun ready_shows_page_label_and_button_states() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithText("doc.pdf").assertIsDisplayed()
        rule.onNodeWithText("Página 1 de 3").assertIsDisplayed()
        // Desabilitado continua visível (não vira retângulo sem contraste) — defeito de docs/e2e/device/img/05.
        rule.onNodeWithContentDescription("Página anterior").assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Próxima página").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun next_button_scrolls_to_next_page() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithContentDescription("Próxima página").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Página 2 de 3").assertIsDisplayed()
    }

    /** Review Focus: alvo de 56dp e 8dp de folga — um toque impreciso não pode virar a página errada. */
    @Test fun setas_tem_56dp_e_pelo_menos_8dp_de_folga() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        val anterior = rule.onNodeWithContentDescription("Página anterior").assertHeightIsAtLeast(56.dp)
        val proxima = rule.onNodeWithContentDescription("Próxima página").assertHeightIsAtLeast(56.dp)
        val folga = proxima.getUnclippedBoundsInRoot().top - anterior.getUnclippedBoundsInRoot().bottom
        assertTrue("folga era $folga, esperado >= 8dp", folga >= 8.dp)
    }

    @Test fun back_button_calls_on_back() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithContentDescription("Voltar").performClick()
        assertEquals(1, backCalls)
    }

    @Test fun acao_de_rotacao_alterna_e_informa_o_estado() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithContentDescription("Travar rotação da tela").assertIsOff()
        rule.onNodeWithContentDescription("Travar rotação da tela").performClick()
        rule.onNodeWithContentDescription("Travar rotação da tela").assertIsOn()
        assertEquals(1, rotationToggles)
    }

    @Test fun travar_rotacao_mostra_snackbar() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        // autoAdvance desligado: com o relógio livre, o Snackbar Short (4s) some antes da asserção.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Travar rotação da tela").performClick()
        rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithText("Tela travada nesta posição").assertIsDisplayed()
    }

    /** Review Focus: voltar (IconButton) e rotação (IconToggleButton) na mesma linha. */
    @Test fun icones_do_top_bar_do_leitor_ficam_alinhados() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        val voltar = rule.onNodeWithContentDescription("Voltar").getBoundsInRoot()
        val rotacao = rule.onNodeWithContentDescription("Travar rotação da tela").getBoundsInRoot()
        assertEquals(
            "centros verticais diferentes: $voltar vs $rotacao",
            voltar.top.value + voltar.height.value / 2f,
            rotacao.top.value + rotacao.height.value / 2f,
            1f,
        )
    }

    @Test fun carregando_nao_mostra_a_acao_de_rotacao() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Loading))
        rule.onNodeWithContentDescription("Travar rotação da tela").assertDoesNotExist()
    }

    @Test fun needs_password_shows_dialog() {
        show(ReaderUiState("doc.pdf", ReaderStatus.NeedsPassword(wrongAttempt = false)))
        rule.onNodeWithText("PDF protegido").assertIsDisplayed()
    }

    @Test fun failed_shows_error_and_ok_goes_back() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Failed(AppError.CORRUPTED)))
        rule.onNodeWithText("Não foi possível abrir este arquivo.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, backCalls)
    }

    @Test fun failed_render_shows_page_message() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })), render = { _, _ -> null })
        rule.onNodeWithText(PAGE_RENDER_FAILED_MESSAGE).assertIsDisplayed()
    }

    @Test fun page_with_zero_size_does_not_crash() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(listOf(PageSize(0f, 0f)))))
        rule.onNodeWithText("Página 1 de 1").assertIsDisplayed()
    }

    @Test fun loading_shows_progress_text() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Loading))
        rule.onNodeWithText("Abrindo…").assertIsDisplayed()
        assertTrue(submitted == null)
    }

    @Test fun tapping_the_page_hides_and_restores_the_bars() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithContentDescription("Página 1").performTouchInput { click() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Próxima página").assertDoesNotExist()
        rule.onNodeWithContentDescription("Voltar").assertDoesNotExist()

        rule.onNodeWithContentDescription("Página 1").performTouchInput { click() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Próxima página").assertIsDisplayed()
        rule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
    }
}
