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

// ------------------------------------------------------------- esquema claro
// Paleta Blue Lagoon (estudo de cor a021abb48ee0a08b3), seed #006494.
// TODOS os papéis estão preenchidos de propósito: um lightColorScheme() parcial
// deixa surfaceContainer*, secondaryContainer e tertiary* no baseline roxo do M3
// — era daí que vinha o lilás nas barras e nos diálogos.
private val LightColors = lightColorScheme(
    primary = Color(0xFF006494),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF4E616D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E5F5),
    onSecondaryContainer = Color(0xFF0B1D29),
    tertiary = Color(0xFF00677C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2EBFF),
    onTertiaryContainer = Color(0xFF001F27),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF7FAFC),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDCE3E9),
    onSurfaceVariant = Color(0xFF40484D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F7),
    surfaceContainer = Color(0xFFEBEFF2),
    surfaceContainerHigh = Color(0xFFE5EAEE),
    surfaceContainerHighest = Color(0xFFDFE4E8),
    outline = Color(0xFF70787D),
    // Mais escuro que um outlineVariant padrão (~1,6:1): o divisor recuado da
    // lista é o único separador entre um PDF e o seguinte e precisa de >= 3:1.
    outlineVariant = Color(0xFF767E83),
    inverseSurface = Color(0xFF2C3134),
    inverseOnSurface = Color(0xFFEDF1F4),
    inversePrimary = Color(0xFF96C4E5),
    surfaceTint = Color(0xFF006494),
    scrim = Color(0xFF000000),
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

// ------------------------------------------- exposição só para os testes
/** Exposto só para os testes de contraste. */
internal val LightSchemeForTest = LightColors

@Composable
fun JohnPdfTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = JohnTypography,
        content = content,
    )
}
