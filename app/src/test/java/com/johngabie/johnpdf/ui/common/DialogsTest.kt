package com.johngabie.johnpdf.ui.common

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
        rule.onNodeWithText("Este arquivo não está mais disponível.").assertIsDisplayed()
        rule.onAllNodesWithTag("primary_button").assertCountEquals(0) // é um aviso, não uma ação (spec §7)
        rule.onNodeWithText("OK").performClick()
        assertTrue(dismissed)
    }

    @Test fun remove_dialog_shows_title_and_support_text() {
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = {}) } }
        rule.onNodeWithText("Remover da lista?").assertIsDisplayed()
        rule.onNodeWithText("O arquivo continua no celular.").assertIsDisplayed()
    }

    @Test fun remove_dialog_has_exactly_one_primary_button() {
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = {}) } }
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
    }

    @Test fun remove_dialog_confirm_calls_on_confirm() {
        var confirmed = false
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = { confirmed = true }, onCancel = {}) } }
        rule.onNodeWithText("Remover").performClick()
        assertTrue(confirmed)
    }

    @Test fun remove_dialog_cancel_calls_on_cancel() {
        var cancelled = false
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = { cancelled = true }) } }
        rule.onNodeWithText("Cancelar").performClick()
        assertTrue(cancelled)
    }

    @Test fun password_dialog_submits_typed_password() {
        var submitted: String? = null
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = { submitted = it }, onCancel = {}) } }
        rule.onNodeWithText("Abrir").assertIsNotEnabled()
        rule.onNodeWithTag("password_field").performTextInput("1234")
        rule.onNodeWithText("Abrir").performClick()
        assertEquals("1234", submitted)
    }

    @Test fun password_dialog_shows_wrong_attempt_message() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = true, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("Senha incorreta, tente de novo").assertIsDisplayed()
    }

    @Test fun password_dialog_toggles_keyboard_label_without_prefix() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("Usar letras").performClick()
        rule.onNodeWithText("Usar números").assertIsDisplayed()
    }

    @Test fun password_dialog_has_exactly_one_primary_button() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
    }
}
