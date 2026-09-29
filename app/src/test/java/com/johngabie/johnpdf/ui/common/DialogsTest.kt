package com.johngabie.johnpdf.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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

    @Test fun error_dialog_shows_message_and_ok_dismisses() {
        var dismissed = false
        rule.setContent { JohnPdfTheme { ErrorDialog(AppError.GONE) { dismissed = true } } }
        rule.onNodeWithText("Este arquivo não está mais disponível.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertTrue(dismissed)
    }

    @Test fun confirm_dialog_yes() {
        var yes = false
        rule.setContent { JohnPdfTheme { ConfirmDialog("Remover da lista?", onYes = { yes = true }, onNo = {}) } }
        rule.onNodeWithText("Remover da lista?").assertIsDisplayed()
        rule.onNodeWithText("Sim").performClick()
        assertTrue(yes)
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

    @Test fun password_dialog_toggles_keyboard_label() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("abc  Usar letras").performClick()
        rule.onNodeWithText("123  Usar números").assertIsDisplayed()
    }
}
