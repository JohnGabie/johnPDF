package com.johngabie.johnpdf.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.ui.icons.JohnIcons
import com.johngabie.johnpdf.ui.theme.SpaceM
import com.johngabie.johnpdf.ui.theme.SpaceXs

@Composable
fun ErrorDialog(error: AppError, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(JohnIcons.Error, contentDescription = null) },
        text = { Text(error.message, style = MaterialTheme.typography.bodyLarge) },
        // É um aviso, não uma ação — não merece um botão Filled (spec §7).
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", style = MaterialTheme.typography.labelLarge) } },
    )
}

/** Substitui o antigo ConfirmDialog("Sim"/"Não") — texto fixo, só usado para remover um recente (spec §7). */
@Composable
fun RemoveDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(JohnIcons.Delete, contentDescription = null) },
        title = { Text("Remover da lista?", style = MaterialTheme.typography.titleLarge) },
        text = { Text("O arquivo continua no celular.", style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { PrimaryButton("Remover", onConfirm) },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
    )
}

/**
 * Ponte temporária: `HomeScreen.kt` ainda chama esta função e só migra para [RemoveDialog] na
 * Task 6 do plano D1. Mantida sem mudanças para o build continuar verde a cada commit
 * (Global Constraints do plano); sai junto com `BigButton` na Task 10.
 */
@Deprecated("Sai na Task 6 do plano D1; use RemoveDialog.", ReplaceWith("RemoveDialog(onYes, onNo)"))
@Composable
fun ConfirmDialog(question: String, onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        text = { Text(question, style = MaterialTheme.typography.titleMedium) },
        confirmButton = { BigButton("Sim", onYes) },
        dismissButton = { BigButton("Não", onNo) },
    )
}

@Composable
fun PasswordDialog(wrongAttempt: Boolean, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var numeric by rememberSaveable { mutableStateOf(true) }
    var visible by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(JohnIcons.Lock, contentDescription = null) },
        title = { Text("PDF protegido", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SpaceM)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) JohnIcons.VisibilityOff else JohnIcons.Visibility,
                                contentDescription = if (visible) "Ocultar senha" else "Mostrar senha",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("password_field"),
                )
                if (wrongAttempt) {
                    Text(
                        "Senha incorreta, tente de novo",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                TextButton(onClick = { numeric = !numeric }) {
                    Icon(if (numeric) JohnIcons.Keyboard else JohnIcons.Dialpad, contentDescription = null)
                    Spacer(Modifier.width(SpaceXs))
                    Text(if (numeric) "Usar letras" else "Usar números", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = { PrimaryButton("Abrir", onClick = { onSubmit(password) }, enabled = password.isNotEmpty()) },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
    )
}
