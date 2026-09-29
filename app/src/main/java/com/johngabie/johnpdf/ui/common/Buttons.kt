package com.johngabie.johnpdf.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.johngabie.johnpdf.ui.theme.MaxActionWidth
import com.johngabie.johnpdf.ui.theme.PrimaryTouchTarget
import com.johngabie.johnpdf.ui.theme.SpaceL
import com.johngabie.johnpdf.ui.theme.SpaceM
import com.johngabie.johnpdf.ui.theme.SpaceS

/** Ação principal — no máximo uma por tela/diálogo (spec D1 §3.2, invariante 1). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) = Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
        .testTag("primary_button")
        .heightIn(min = PrimaryTouchTarget)
        .widthIn(max = MaxActionWidth),
    contentPadding = PaddingValues(horizontal = SpaceL, vertical = SpaceM),
) { ButtonContent(text, icon) }

/** Ação secundária frequente (Anterior/Próxima, "Abrir" do header da Home). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = PrimaryTouchTarget,
) = FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.heightIn(min = height),
    contentPadding = PaddingValues(horizontal = SpaceL, vertical = SpaceM),
) { ButtonContent(text, icon) }

/** `contentDescription = null` é proposital: o texto ao lado já é o rótulo acessível (spec §3.3). */
@Composable
private fun RowScope.ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(SpaceS))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}
