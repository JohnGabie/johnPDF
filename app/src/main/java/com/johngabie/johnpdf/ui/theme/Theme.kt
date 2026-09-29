package com.johngabie.johnpdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * Fundo da página renderizada do PDF.
 *
 * Literal e imutável de propósito: o conteúdo do PDF NÃO acompanha o modo escuro
 * (inverter a página é "modo noturno do PDF", fora do escopo da v1). Não trocar
 * por `MaterialTheme.colorScheme.surface`. Coberto por `DarkThemeTest`.
 */
val PdfPageBackground = Color.White

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

// ------------------------------------------------------------ esquema escuro
private val DarkColors = darkColorScheme(
    primary = Color(0xFF96C4E5),
    onPrimary = Color(0xFF003450),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = Color(0xFFB6C9D8),
    onSecondary = Color(0xFF21333E),
    secondaryContainer = Color(0xFF384955),
    onSecondaryContainer = Color(0xFFD2E5F5),
    tertiary = Color(0xFF83D3EB),
    onTertiary = Color(0xFF003641),
    tertiaryContainer = Color(0xFF004E5D),
    onTertiaryContainer = Color(0xFFB2EBFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFDFE3E7),
    surface = Color(0xFF0F1417),
    onSurface = Color(0xFFDFE3E7),
    surfaceVariant = Color(0xFF40484D),
    onSurfaceVariant = Color(0xFFC0C8CD),
    surfaceContainerLowest = Color(0xFF0A0F12),
    surfaceContainerLow = Color(0xFF171C1F),
    surfaceContainer = Color(0xFF1B2124),
    surfaceContainerHigh = Color(0xFF262B2F),
    surfaceContainerHighest = Color(0xFF31363A),
    outline = Color(0xFF8A9297),
    // Mais claro que o padrão, pelo mesmo motivo do esquema claro.
    outlineVariant = Color(0xFF848C91),
    inverseSurface = Color(0xFFDFE3E7),
    inverseOnSurface = Color(0xFF2C3134),
    inversePrimary = Color(0xFF006494),
    surfaceTint = Color(0xFF96C4E5),
    scrim = Color(0xFF000000),
)

// ------------------------------------------- papéis que o M3 não tem

/** Cores do johnPDF sem papel correspondente no Material 3. */
data class JohnColors(
    /** Vão entre as páginas no leitor. Quase preto no escuro, para emoldurar a página branca. */
    val pageGap: Color,
    /** Ícone de PDF no cartão da lista e o quadrado arredondado atrás dele. */
    val pdfIcon: Color,
    val pdfIconContainer: Color,
    /**
     * Pílula da aba selecionada. Papel próprio porque nenhum container claro do M3
     * chega a 3:1 contra uma NavigationBar clara (secondaryContainer dá 1,12:1) —
     * e "qual aba está aberta" é estado de componente (WCAG 1.4.11).
     */
    val tabIndicator: Color,
    val onTabIndicator: Color,
)

private val LightJohnColors = JohnColors(
    pageGap = Color(0xFFE5EAEE),        // = surfaceContainerHigh
    pdfIcon = Color(0xFFC62828),
    pdfIconContainer = Color(0xFFFDECEA),
    tabIndicator = Color(0xFF365A6C),   // 6,40:1 contra surfaceContainer
    onTabIndicator = Color(0xFFFFFFFF), // 7,41:1 dentro da pílula
)

private val DarkJohnColors = JohnColors(
    pageGap = Color(0xFF0A0F12),        // = surfaceContainerLowest; 19,27:1 com a página branca
    pdfIcon = Color(0xFFFF8A80),        // vermelho próprio, para não virar "vermelho de erro"
    pdfIconContainer = Color(0xFF262B2F),
    tabIndicator = Color(0xFF5A7385),   // 3,28:1 contra surfaceContainer
    onTabIndicator = Color(0xFFFFFFFF), // 4,97:1 dentro da pílula
)

private val LocalJohnColors = staticCompositionLocalOf { LightJohnColors }

/**
 * Acesso no estilo `MaterialTheme.colorScheme`.
 *
 * `barColor` e `dialogColor` existem para que a escolha de nível de superfície
 * fique num lugar só e testável, em vez de espalhada pelas telas.
 */
object JohnTheme {
    val colors: JohnColors
        @Composable @ReadOnlyComposable get() = LocalJohnColors.current

    /** Fundo das barras: NavigationBar, top bar rolada, barra inferior do leitor. */
    val barColor: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainer

    /** Fundo dos AlertDialog e do campo de busca. */
    val dialogColor: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHigh
}

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
internal val DarkSchemeForTest = DarkColors
internal val LightJohnForTest = LightJohnColors
internal val DarkJohnForTest = DarkJohnColors

@Composable
fun JohnPdfTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalJohnColors provides if (dark) DarkJohnColors else LightJohnColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = JohnTypography,
            content = content,
        )
    }
}
