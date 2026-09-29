package com.johngabie.johnpdf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Espaçamento: escala de 4dp nomeada (spec D1 §4) ---
val SpaceXs = 4.dp
val SpaceS = 8.dp
val SpaceM = 12.dp
val SpaceL = 16.dp
val SpaceXl = 24.dp
val SpaceXxl = 32.dp
val ScreenPadding = SpaceL
val MinGap = SpaceS

// --- Alvos de toque (spec D1 §3.1) ---
val PrimaryTouchTarget = 56.dp
val ListItemMinHeight = 72.dp
val MaxActionWidth = 360.dp

// --- Gap entre páginas do PDF (spec D1 §4) ---
val PageGap = SpaceS
val PageElevation = 1.dp

@Deprecated("Sai na Task 10 do plano D1; use PrimaryTouchTarget (§3.1) ou os componentes de Buttons.kt.")
internal val MinTouchTarget = 64.dp

@Deprecated("Sai na Task 10 do plano D1; use MaterialTheme.colorScheme.surfaceContainerHigh.")
val PageGapColor = Color(0xFFBDBDBD)

private val Colors = lightColorScheme(
    primary = Color(0xFF0B4F9C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F7),
    onPrimaryContainer = Color(0xFF0A2540),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF333333),
    error = Color(0xFFB00020),
)

// A troca para Atkinson Hyperlegible Next é um commit isolado (spec §2.2), fora deste plano.
internal val JohnFontFamily = FontFamily.Default

private val Base = Typography()
internal val JohnTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontFamily = JohnFontFamily, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontFamily = JohnFontFamily, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontFamily = JohnFontFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontFamily = JohnFontFamily, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = Base.bodyMedium.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = Base.bodySmall.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 20.sp),
    labelLarge = Base.labelLarge.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelSmall = Base.labelSmall.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun JohnPdfTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = JohnTypography, content = content)
}
