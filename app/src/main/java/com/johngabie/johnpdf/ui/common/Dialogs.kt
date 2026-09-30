package com.johngabie.johnpdf.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.johngabie.johnpdf.R
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.RemoteVersion
import com.johngabie.johnpdf.ui.icons.JohnIcons
import com.johngabie.johnpdf.ui.theme.JohnTheme
import com.johngabie.johnpdf.ui.theme.SpaceM
import com.johngabie.johnpdf.ui.theme.SpaceXs

@Composable
fun ErrorDialog(error: AppError, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JohnTheme.dialogColor,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        iconContentColor = MaterialTheme.colorScheme.error,
        icon = { Icon(JohnIcons.Error, contentDescription = null) },
        text = { Text(stringResource(error.messageRes), style = MaterialTheme.typography.bodyLarge) },
        // É um aviso, não uma ação — não merece um botão Filled (spec §7).
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok), style = MaterialTheme.typography.labelLarge) } },
    )
}

/**
 * Substitui o antigo `ConfirmDialog("Sim"/"Não")` — texto fixo, só usado para remover um
 * recente (spec §7). "Remover" diz o que a ação faz; num gesto destrutivo isso é mais seguro
 * do que um "Sim" genérico.
 */
@Composable
fun RemoveDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = JohnTheme.dialogColor,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = { Icon(JohnIcons.Delete, contentDescription = null) },
        title = { Text(stringResource(R.string.remove_from_list_title), style = MaterialTheme.typography.titleLarge) },
        text = { Text(stringResource(R.string.remove_from_list_body), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { PrimaryButton(stringResource(R.string.action_remove), onConfirm) },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge) } },
    )
}

@Composable
fun PasswordDialog(wrongAttempt: Boolean, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var numeric by rememberSaveable { mutableStateOf(true) }
    var visible by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = JohnTheme.dialogColor,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = { Icon(JohnIcons.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.protected_pdf), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SpaceM)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) JohnIcons.VisibilityOff else JohnIcons.Visibility,
                                contentDescription = stringResource(if (visible) R.string.hide_password else R.string.show_password),
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("password_field"),
                )
                if (wrongAttempt) {
                    Text(
                        stringResource(R.string.wrong_password),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                TextButton(onClick = { numeric = !numeric }) {
                    Icon(if (numeric) JohnIcons.Keyboard else JohnIcons.Dialpad, contentDescription = null)
                    Spacer(Modifier.width(SpaceXs))
                    Text(stringResource(if (numeric) R.string.use_letters else R.string.use_numbers), style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = { PrimaryButton(stringResource(R.string.action_open), onClick = { onSubmit(password) }, enabled = password.isNotEmpty()) },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge) } },
    )
}

@Composable
fun UpdateAvailableDialog(
    current: RemoteVersion,
    currentVersionName: String,
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JohnTheme.dialogColor,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = { Icon(JohnIcons.ScreenRotation, contentDescription = null) },
        title = { Text(stringResource(R.string.update_available), style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                stringResource(R.string.update_versions, currentVersionName, current.versionName),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = { PrimaryButton(stringResource(R.string.action_download), onClick = { onOpenLink(current.downloadUrl) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_later), style = MaterialTheme.typography.labelLarge) } },
    )
}

@Composable
fun UpdateSettingsDialog(
    autoCheckUpdates: Boolean,
    onToggleAutoCheck: (Boolean) -> Unit,
    onCheckNow: (onResult: (hasUpdate: Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var isChecking by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    // Resolved up front: the callbacks below are not composable and cannot call stringResource.
    val timeoutMsg = stringResource(R.string.update_timeout)
    val foundMsg = stringResource(R.string.update_found)
    val upToDateMsg = stringResource(R.string.update_up_to_date)

    LaunchedEffect(isChecking) {
        if (isChecking) {
            kotlinx.coroutines.delay(15_000)
            if (isChecking) {
                isChecking = false
                feedback = timeoutMsg
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JohnTheme.dialogColor,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = { Icon(JohnIcons.Schedule, contentDescription = null) },
        title = { Text(stringResource(R.string.update_check_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SpaceM)) {
                if (isChecking) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(SpaceM))
                        Text(stringResource(R.string.update_checking), style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (feedback != null) {
                    Text(feedback!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                } else {
                    PrimaryButton(stringResource(R.string.update_check_now), onClick = {
                        isChecking = true
                        onCheckNow { hasUpdate ->
                            isChecking = false
                            feedback = if (hasUpdate) {
                                foundMsg
                            } else {
                                upToDateMsg
                            }
                        }
                    })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.update_auto_check), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = autoCheckUpdates, onCheckedChange = onToggleAutoCheck)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close), style = MaterialTheme.typography.labelLarge) } },
    )
}
