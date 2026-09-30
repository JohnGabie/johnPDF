package com.johngabie.johnpdf.ui.common

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialogsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun error_dialog_shows_message_ok_dismisses_and_has_no_primary_button() {
        var dismissed = false
        rule.setContent { JohnPdfTheme { ErrorDialog(AppError.GONE) { dismissed = true } } }
        rule.onNodeWithText("This file is no longer available.").assertIsDisplayed()
        rule.onAllNodesWithTag("primary_button").assertCountEquals(0) // é um aviso, não uma ação (spec §7)
        rule.onNodeWithText("OK").performClick()
        assertTrue(dismissed)
    }

    @Test fun remove_dialog_shows_title_and_support_text() {
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = {}) } }
        rule.onNodeWithText("Remove from list?").assertIsDisplayed()
        rule.onNodeWithText("The file stays on your phone.").assertIsDisplayed()
    }

    @Test fun remove_dialog_has_exactly_one_primary_button() {
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = {}) } }
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
    }

    /**
     * M3: a ação dismissiva fica à esquerda da confirmatória, ambas na mesma linha (nunca
     * empilhadas). As alturas são de propósito diferentes — 56dp na ação principal, 48dp na
     * secundária (spec §3.1) —, então a comparação é pelo topo, não pelo centro vertical.
     */
    @Test fun acoes_do_dialogo_ficam_alinhadas_a_direita() {
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = {}) } }
        val cancelar = rule.onNodeWithText("Cancel").getBoundsInRoot()
        val remover = rule.onNodeWithText("Remove").getBoundsInRoot()
        assertTrue("'Cancelar' ($cancelar) deveria estar à esquerda de 'Remover' ($remover)", cancelar.right <= remover.left)
        assertEquals("botões em linhas diferentes: $cancelar vs $remover", cancelar.top.value, remover.top.value, 1f)
        assertTrue("'Remover' com ${remover.height}, esperado >= 56dp", remover.height >= 56.dp)
        assertTrue("'Cancelar' com ${cancelar.height}, esperado >= 48dp", cancelar.height >= 48.dp)
    }

    /** O diálogo de senha renderiza inteiro com os ícones vendorizados (sem material-icons-extended). */
    @Test fun password_dialog_renders_title_field_and_actions() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("Protected PDF").assertIsDisplayed()
        rule.onNodeWithTag("password_field").assertIsDisplayed()
        rule.onNodeWithContentDescription("Show password").assertIsDisplayed()
        rule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test fun remove_dialog_confirm_calls_on_confirm() {
        var confirmed = false
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = { confirmed = true }, onCancel = {}) } }
        rule.onNodeWithText("Remove").performClick()
        assertTrue(confirmed)
    }

    @Test fun remove_dialog_cancel_calls_on_cancel() {
        var cancelled = false
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = { cancelled = true }) } }
        rule.onNodeWithText("Cancel").performClick()
        assertTrue(cancelled)
    }

    @Test fun password_dialog_submits_typed_password() {
        var submitted: String? = null
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = { submitted = it }, onCancel = {}) } }
        rule.onNodeWithText("Open").assertIsNotEnabled()
        rule.onNodeWithTag("password_field").performTextInput("1234")
        rule.onNodeWithText("Open").performClick()
        assertEquals("1234", submitted)
    }

    @Test fun password_dialog_shows_wrong_attempt_message() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = true, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("Wrong password, try again").assertIsDisplayed()
    }

    @Test fun password_dialog_toggles_keyboard_label_without_prefix() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("Use letters").performClick()
        rule.onNodeWithText("Use numbers").assertIsDisplayed()
    }

    @Test fun password_dialog_has_exactly_one_primary_button() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
    }
}
