package com.johngabie.johnpdf.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.ui.theme.MinTouchTarget

@Composable
fun ErrorDialog(error: AppError, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(error.message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { BigButton("OK", onDismiss) },
    )
}

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
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Este PDF tem senha", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    visualTransformation = PasswordVisualTransformation(),
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
                TextButton(onClick = { numeric = !numeric }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                    Text(if (numeric) "abc  Usar letras" else "123  Usar números", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = { BigButton("Abrir", onClick = { onSubmit(password) }, enabled = password.isNotEmpty()) },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
