package com.johngabie.johnpdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

// Valores provisórios — a paleta final está pendente do estudo em coolors.co (spec D1 §G5,
// fora de escopo aqui). A estrutura (todos os papéis usados pelo app, incluindo
// surfaceContainer*, secondaryContainer e outlineVariant) já entra completa para acabar
// com o lilás padrão do M3 vazando nos componentes não customizados.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B4F9C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F7),
    onPrimaryContainer = Color(0xFF0A2540),
    secondary = Color(0xFF3A5B7D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E6F5),
    onSecondaryContainer = Color(0xFF12293D),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF44474A),
    surfaceContainer = Color(0xFFF1F1F1),
    surfaceContainerLow = Color(0xFFF6F6F6),
    surfaceContainerHigh = Color(0xFFE6E6E6),
    outline = Color(0xFF74777A),
    outlineVariant = Color(0xFFC4C7CA),
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFBD9DD),
    onErrorContainer = Color(0xFF410E12),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FC6F5),
    onPrimary = Color(0xFF00325C),
    primaryContainer = Color(0xFF0B4F9C),
    onPrimaryContainer = Color(0xFFD6E4F7),
    secondary = Color(0xFFA7C6E5),
    onSecondary = Color(0xFF17324A),
    secondaryContainer = Color(0xFF2C4560),
    onSecondaryContainer = Color(0xFFD9E6F5),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF303336),
    onSurfaceVariant = Color(0xFFC4C7CA),
    surfaceContainer = Color(0xFF1D1D1D),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    outline = Color(0xFF8E9194),
    outlineVariant = Color(0xFF44474A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
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
fun JohnPdfTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = JohnTypography,
        content = content,
    )
}
