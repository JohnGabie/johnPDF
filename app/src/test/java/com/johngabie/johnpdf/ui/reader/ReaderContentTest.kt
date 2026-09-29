package com.johngabie.johnpdf.ui.reader

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        rule.onNodeWithText("⬆ Anterior").assertIsNotEnabled()
        rule.onNodeWithText("⬇ Próxima").assertIsEnabled()
    }

    @Test fun next_button_scrolls_to_next_page() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithText("⬇ Próxima").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Página 2 de 3").assertIsDisplayed()
    }

    @Test fun back_button_calls_on_back() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithText("← Voltar").performClick()
        assertEquals(1, backCalls)
    }

    @Test fun rotation_button_toggles_label() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithText("🔓 Gira sozinha").performClick()
        rule.onNodeWithText("🔒 Travada").assertIsDisplayed()
        assertEquals(1, rotationToggles)
    }

    @Test fun needs_password_shows_dialog() {
        show(ReaderUiState("doc.pdf", ReaderStatus.NeedsPassword(wrongAttempt = false)))
        rule.onNodeWithText("Este PDF tem senha").assertIsDisplayed()
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
}
