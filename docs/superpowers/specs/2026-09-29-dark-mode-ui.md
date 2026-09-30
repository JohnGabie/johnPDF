# Modo escuro da interface (johnPDF)

- **Data:** 2026-09-29 · **Status:** aguardando revisão · **Tipo:** spec de implementação
- **Origem:** `docs/superpowers/specs/2026-09-29-design-d1-proposta.md` **seção E, linha 121** (ideia opcional "Modo escuro da interface") + **seção G, pergunta 4** — resposta do usuário: *"sim é barato de fazer."*
- **Base de código:** `ui/theme/Theme.kt`, `MainActivity.kt`, `AndroidManifest.xml`, `ui/home/HomeScreen.kt`, `ui/reader/ReaderScreen.kt`, `ui/common/Dialogs.kt`
- **Todos os valores de contraste deste documento foram calculados** (fórmula WCAG 2.1 de luminância relativa), não estimados.

---

## 1. Objetivo

Fazer a **interface** do johnPDF seguir o modo escuro do sistema: barras (topo, inferior,
navegação), listas, campo de busca, diálogos, estados vazios e tela de permissão.
**As páginas do PDF continuam brancas em qualquer modo.**

### Dentro do escopo
1. `darkColorScheme` completo, derivado do mesmo seed do esquema claro.
2. `JohnPdfTheme` passa a escolher claro/escuro por `isSystemInDarkTheme()`.
3. Cor da barra de status/navegação do sistema (ícones) acompanhando o modo.
4. Tema do `AndroidManifest` com variante `-night` (evita clarão branco no arranque a frio).
5. Cor do vão entre páginas do leitor (`PageGapColor`) deixa de ser fixa.
6. Testes automatizados de contraste (≥ 4,5:1 para texto) em **ambos** os esquemas.

### Fora do escopo
- **"Modo noturno" do PDF** (inverter/escurecer o conteúdo da página) — já declarado fora do
  escopo na spec §1 de v1. Esta tarefa não conflita com ele: aqui só muda o *chrome*.
- **Cor dinâmica** (`dynamicDarkColorScheme`, Android 12+) — seção E linha 122, permanece
  desligada; perde identidade própria.
- **Botão "tema claro/escuro/sistema" nas configurações** — v1 segue o sistema e pronto.
  Custo de adicionar depois: ver §10.

### Critérios de aceitação
1. Com o sistema em escuro, nenhuma superfície da interface é branca, exceto a página do PDF.
2. Nenhum par texto/fundo do app fica abaixo de **4,5:1** em nenhum dos dois esquemas.
3. Os ícones da barra de status ficam visíveis nos dois modos (regressão F6 não volta).
4. No arranque a frio em modo escuro não há flash branco.
5. Girar o aparelho ou trocar o tema do sistema com o leitor aberto não perde a página atual.

---

## 2. Decisões

| Tema | Decisão | Motivo |
|---|---|---|
| Gatilho | `isSystemInDarkTheme()` — segue o sistema, sem opção no app | Resposta do usuário na G4; zero UI nova |
| Seed | **O mesmo do esquema claro** (hoje: índigo `#3558D4`) | Um só seed garante que claro e escuro sejam a mesma marca |
| Geração | Material Theme Builder → esquema **completo** (todos os papéis) | A causa do "lilás vazado" (A, linha 20) é esquema parcial; no escuro o sintoma seria pior |
| `tonalElevation` | Removido; superfícies usam `surfaceContainer*` explícito | No escuro `tonalElevation` clareia a superfície com tinta de `primary` de forma imprevisível |
| Página do PDF | `Color.White` **literal**, nunca `colorScheme.surface` | Requisito do usuário; ver §7 |
| Vão entre páginas | Papel de tema, não constante global | `PageGapColor` fixo `#BDBDBD` não funciona no escuro |
| `outlineVariant` | **Sobrescrito** em relação ao padrão do MTB | Padrão do MTB dá divisor de 1,75:1 no escuro — invisível para o público idoso; ver §4.3 |
| Desabilitado | Alpha **0,60** em vez do 0,38 do M3 | 0,38 dá 2,2–3,0:1; "Anterior" desabilitado na página 1 precisa ser lido, não adivinhado |

> **Dependência com a pergunta G5 (paleta).** O usuário respondeu que **não gosta do índigo
> `#3558D4`** e pediu um estudo de paleta (coolors.co) — esse estudo ainda não existe no repo.
> Este documento está escrito com o índigo como seed de trabalho. **Se a paleta mudar, só as
> colunas de hex das famílias `primary`/`secondary`/`tertiary` mudam**; a estrutura do
> `Theme.kt`, os papéis neutros, as regras de contraste e todos os testes da §8 continuam
> válidos sem alteração. Reexecutar o script da §8.3 com os novos hex é o único trabalho extra.

---

## 3. Paleta

### 3.1 Famílias de marca e de erro

| Papel M3 | Claro | Escuro | Onde aparece |
|---|---|---|---|
| `primary` | `#3558D4` | `#B9C3FF` | Botão principal do diálogo, indicador da aba, botão "Abrir" |
| `onPrimary` | `#FFFFFF` | `#08218A` | Texto sobre `primary` |
| `primaryContainer` | `#DDE1FF` | `#1B38A3` | Círculo do ícone da tela de permissão |
| `onPrimaryContainer` | `#00105C` | `#DDE1FF` | Ícone dentro do círculo |
| `secondary` | `#5B5D72` | `#C3C5DD` | Acentos neutros |
| `onSecondary` | `#FFFFFF` | `#2C2F42` | — |
| `secondaryContainer` | `#E0E2EC` | `#424659` | **Indicador da aba selecionada** (`NavigationBar`) |
| `onSecondaryContainer` | `#171B2C` | `#DEE1F9` | Ícone/rótulo da aba selecionada |
| `tertiary` | `#77536D` | `#E6BAD7` | Não usado hoje (definido para não vazar o roxo padrão) |
| `onTertiary` | `#FFFFFF` | `#45263D` | — |
| `tertiaryContainer` | `#FFD7F1` | `#5D3C55` | — |
| `onTertiaryContainer` | `#2D1228` | `#FFD7F1` | — |
| `error` | `#BA1A1A` | `#FFB4AB` | "Senha incorreta, tente de novo"; ícone do diálogo de erro |
| `onError` | `#FFFFFF` | `#690005` | — |
| `errorContainer` | `#FFDAD6` | `#93000A` | — |
| `onErrorContainer` | `#410002` | `#FFDAD6` | — |

### 3.2 Neutros e superfícies

| Papel M3 | Claro | Escuro | Onde aparece |
|---|---|---|---|
| `background` | `#FBF8FF` | `#131318` | Fundo da `Scaffold` |
| `onBackground` | `#1B1B21` | `#E4E1E9` | — |
| `surface` | `#FBF8FF` | `#131318` | Fundo da lista de PDFs, `TopAppBar` não rolado |
| `onSurface` | `#1B1B21` | `#E4E1E9` | Nome do PDF, títulos, "4 de 200" |
| `surfaceVariant` | `#E2E1EC` | `#45464F` | Preenchimentos discretos |
| `onSurfaceVariant` | `#45464F` | `#C6C5D0` | "Download · ontem", ícones não selecionados, placeholder da busca |
| `surfaceContainerLowest` | `#FFFFFF` | `#0E0E13` | **Vão entre páginas no escuro** (§7) |
| `surfaceContainerLow` | `#F5F2FA` | `#1B1B21` | — |
| `surfaceContainer` | `#EFEDF4` | `#1F1F25` | `NavigationBar`, `TopAppBar` rolado, barra inferior do leitor |
| `surfaceContainerHigh` | `#E9E7EF` | `#2A2A2F` | `AlertDialog`, campo de busca, **vão entre páginas no claro** |
| `surfaceContainerHighest` | `#E4E1E9` | `#35353A` | Fundo do `IconToggleButton` de rotação quando travado (seção C da D1) |
| `outline` | `#767680` | `#90909A` | Borda de campos |
| `outlineVariant` | `#807F88` ⚠️ | `#85868F` ⚠️ | **Divisores da lista** — ver §4.3 |
| `inverseSurface` | `#303036` | `#E4E1E9` | Fundo do Snackbar |
| `inverseOnSurface` | `#F2F0F7` | `#303036` | Texto do Snackbar |
| `inversePrimary` | `#B9C3FF` | `#3558D4` | Ação do Snackbar |
| `surfaceTint` | `#3558D4` | `#B9C3FF` | (irrelevante: `tonalElevation` removido) |
| `scrim` | `#000000` | `#000000` | Véu atrás dos diálogos |

⚠️ = **desvio proposital** do que o Material Theme Builder gera (`#C6C5D0` no claro,
`#45464F` no escuro). Justificativa medida na §4.3.

### 3.3 Cores fora do `ColorScheme`

| Constante | Claro | Escuro | Regra |
|---|---|---|---|
| Fundo da página do PDF | `#FFFFFF` | `#FFFFFF` | **Nunca muda.** Literal `Color.White` |
| Vão entre páginas | `surfaceContainerHigh` `#E9E7EF` | `surfaceContainerLowest` `#0E0E13` | Papel de tema, não constante |
| Ícone de PDF na lista | `#C62828` sobre `#FDECEA` | `error` `#FFB4AB` sobre `surfaceContainerHigh` | O vermelho claro fica ilegível no escuro |

---

## 4. Contraste

**Regra do projeto:** texto ≥ **4,5:1** (WCAG 2.1 AA, critério 1.4.3); elementos não textuais
que carregam informação (divisores, bordas, ícones sozinhos) ≥ **3:1** (critério 1.4.11);
**estado desabilitado ≥ 3:1** — mais rígido que a WCAG, que isenta desabilitado, porque o
público-alvo inclui pessoas idosas e "Anterior" desabilitado precisa ser reconhecível.

### 4.1 Pares de texto — claro

| Texto | Fundo | Razão | |
|---|---|---:|---|
| `onSurface #1B1B21` | `surface #FBF8FF` | **16,30:1** | ✅ |
| `onSurfaceVariant #45464F` | `surface #FBF8FF` | **8,91:1** | ✅ |
| `onSurfaceVariant #45464F` | `surfaceContainer #EFEDF4` | **8,07:1** | ✅ |
| `onSurface #1B1B21` | `surfaceContainerHigh #E9E7EF` | **13,99:1** | ✅ |
| `primary #3558D4` | `surfaceContainerHigh #E9E7EF` | **4,90:1** | ✅ (pior caso do claro) |
| `onPrimary #FFFFFF` | `primary #3558D4` | **6,00:1** | ✅ |
| `onPrimaryContainer #00105C` | `primaryContainer #DDE1FF` | **13,26:1** | ✅ |
| `onSecondaryContainer #171B2C` | `secondaryContainer #E0E2EC` | **13,22:1** | ✅ |
| `error #BA1A1A` | `surfaceContainerHigh #E9E7EF` | **5,27:1** | ✅ |

### 4.2 Pares de texto — escuro

| Texto | Fundo | Razão | |
|---|---|---:|---|
| `onSurface #E4E1E9` | `surface #131318` | **14,33:1** | ✅ |
| `onSurfaceVariant #C6C5D0` | `surface #131318` | **10,84:1** | ✅ |
| `onSurfaceVariant #C6C5D0` | `surfaceContainer #1F1F25` | **9,60:1** | ✅ |
| `onSurface #E4E1E9` | `surfaceContainerHigh #2A2A2F` | **11,05:1** | ✅ |
| `onSurfaceVariant #C6C5D0` | `surfaceContainerHighest #35353A` | **7,14:1** | ✅ (pior caso do escuro) |
| `onPrimary #08218A` | `primary #B9C3FF` | **7,72:1** | ✅ |
| `onPrimaryContainer #DDE1FF` | `primaryContainer #1B38A3` | **7,61:1** | ✅ |
| `onSecondaryContainer #DEE1F9` | `secondaryContainer #424659` | **7,21:1** | ✅ |
| `primary #B9C3FF` | `surfaceContainerHigh #2A2A2F` | **8,37:1** | ✅ |
| `error #FFB4AB` | `surfaceContainerHigh #2A2A2F` | **8,41:1** | ✅ |

Os 44 pares `onX`/`X` e texto-sobre-`surfaceContainer*` dos dois esquemas foram verificados:
**mínimo absoluto 4,64:1** (`primary` sobre `surfaceContainerHighest` no claro). Zero falhas.

### 4.3 Por que `outlineVariant` foi sobrescrito

O divisor recuado da lista (proposto na seção D da D1) é o único separador visual entre um PDF
e o seguinte. Com os valores padrão do Material Theme Builder ele praticamente some:

| `outlineVariant` | sobre `surface` | sobre `surfaceContainer` | |
|---|---:|---:|---|
| Padrão MTB claro `#C6C5D0` | 1,62:1 | 1,47:1 | ❌ |
| Padrão MTB escuro `#45464F` | 1,98:1 | 1,75:1 | ❌ |
| **Proposta clara `#807F88`** | **3,76:1** | **3,41:1** | ✅ |
| **Proposta escura `#85868F`** | **5,12:1** | **4,53:1** | ✅ |

As propostas ficam ≥ 3:1 sobre **todos** os cinco níveis de `surfaceContainer*` (pior caso:
3,06:1 no claro sobre `surfaceContainerHighest`, 3,37:1 no escuro).

### 4.4 Estado desabilitado

O M3 pinta rótulo desabilitado com `onSurface` a 38% de alpha. Medido sobre
`surfaceContainer`:

| Alpha | Escuro | Claro | |
|---:|---:|---:|---|
| 0,38 (padrão M3) | 3,02:1 | 2,32:1 | ❌ claro reprova |
| **0,60 (proposta)** | **5,41:1** | **4,29:1** | ✅ |

Aplica-se a "Anterior" na primeira página e "Próxima" na última, e ao botão "Abrir" do diálogo
de senha com o campo vazio. Ver `disabledColors()` na §5.

---

## 5. `Theme.kt`

Arquivo completo proposto. A tipografia é a da seção D da D1 (mínimo 16sp); se a D1 entrar
antes, manter o bloco `BigTypography` que ela deixar e trocar apenas as cores.

```kotlin
package com.johngabie.johnpdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Alvo de toque das ações frequentes/primárias (seção D da D1). */
val PrimaryTouchTarget = 56.dp

/**
 * Fundo da página renderizada do PDF.
 *
 * Literal e imutável de propósito: o conteúdo do PDF NÃO acompanha o modo escuro
 * (inverter a página é o "modo noturno", fora do escopo da v1 — spec §1).
 * Não trocar por `MaterialTheme.colorScheme.surface`. Coberto por `DarkThemeTest`.
 */
val PdfPageBackground = Color.White

// ---------------------------------------------------------------- esquemas

private val LightColors = lightColorScheme(
    primary = Color(0xFF3558D4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E2EC),
    onSecondaryContainer = Color(0xFF171B2C),
    tertiary = Color(0xFF77536D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F1),
    onTertiaryContainer = Color(0xFF2D1228),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9),
    outline = Color(0xFF767680),
    // Mais escuro que o padrão do MTB (#C6C5D0, 1,62:1): divisor de lista precisa de >= 3:1.
    outlineVariant = Color(0xFF807F88),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2F0F7),
    inversePrimary = Color(0xFFB9C3FF),
    surfaceTint = Color(0xFF3558D4),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF1B38A3),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFDEE1F9),
    tertiary = Color(0xFFE6BAD7),
    onTertiary = Color(0xFF45263D),
    tertiaryContainer = Color(0xFF5D3C55),
    onTertiaryContainer = Color(0xFFFFD7F1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    surfaceContainerLowest = Color(0xFF0E0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF2A2A2F),
    surfaceContainerHighest = Color(0xFF35353A),
    outline = Color(0xFF90909A),
    // Mais claro que o padrão do MTB (#45464F, 1,75:1).
    outlineVariant = Color(0xFF85868F),
    inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036),
    inversePrimary = Color(0xFF3558D4),
    surfaceTint = Color(0xFFB9C3FF),
    scrim = Color(0xFF000000),
)

// ------------------------------------------------- papéis fora do ColorScheme

/** Cores do johnPDF que não têm papel correspondente no M3. */
data class JohnColors(
    /** Vão entre as páginas no leitor. Escuro no dark para emoldurar a página branca. */
    val pageGap: Color,
    /** Ícone de PDF na lista. */
    val pdfIcon: Color,
    val pdfIconContainer: Color,
)

private val LightJohnColors = JohnColors(
    pageGap = Color(0xFFE9E7EF),      // surfaceContainerHigh
    pdfIcon = Color(0xFFC62828),
    pdfIconContainer = Color(0xFFFDECEA),
)

private val DarkJohnColors = JohnColors(
    pageGap = Color(0xFF0E0E13),      // surfaceContainerLowest — 19,25:1 com a página branca
    pdfIcon = Color(0xFFFFB4AB),      // error: 8,41:1 sobre surfaceContainerHigh
    pdfIconContainer = Color(0xFF2A2A2F),
)

private val LocalJohnColors = staticCompositionLocalOf { LightJohnColors }

/** Acesso no estilo `MaterialTheme.colorScheme`: `JohnTheme.colors.pageGap`. */
object JohnTheme {
    val colors: JohnColors
        @Composable @ReadOnlyComposable get() = LocalJohnColors.current
}

// ------------------------------------------------------------- desabilitado

/**
 * O 38% de alpha do M3 dá 2,32:1 no claro. 0,60 dá 4,29:1 (claro) / 5,41:1 (escuro).
 * Usar em Anterior/Próxima e no "Abrir" do diálogo de senha.
 */
@Composable
fun readableTonalButtonColors() = ButtonDefaults.filledTonalButtonColors(
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
)

// -------------------------------------------------------------- tipografia

private val Base = Typography()
internal val BigTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = Base.bodySmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = Base.labelSmall.copy(fontSize = 16.sp),
)

// -------------------------------------------------------------------- tema

/** `darkTheme` é parâmetro (e não só `isSystemInDarkTheme()` interno) para os testes. */
@Composable
fun JohnPdfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme = if (darkTheme) DarkColors else LightColors
    val john = if (darkTheme) DarkJohnColors else LightJohnColors
    CompositionLocalProvider(LocalJohnColors provides john) {
        MaterialTheme(colorScheme = colors, typography = BigTypography, content = content)
    }
}

/** Exposto só para os testes de contraste. */
internal val LightSchemeForTest = LightColors
internal val DarkSchemeForTest = DarkColors
internal val LightJohnForTest = LightJohnColors
internal val DarkJohnForTest = DarkJohnColors
```

> `MinTouchTarget = 64.dp` e `PageGapColor` **deixam de existir**. `MinTouchTarget` é
> substituído por `PrimaryTouchTarget = 56.dp` (seção D da D1) — se esta spec entrar **antes**
> da D1, manter `MinTouchTarget = 64.dp` e trocar só `PageGapColor` por `JohnTheme.colors.pageGap`.

---

## 6. Barras do sistema e tema do manifesto

### 6.1 `MainActivity.kt` — corrigir o `enableEdgeToEdge`

Hoje o código força ícones escuros nas barras do sistema independentemente do modo — foi o
conserto F6 do ciclo anterior, feito justamente **porque não havia esquema escuro**. Com o
`darkColorScheme` no lugar, `SystemBarStyle.light(...)` passa a ser o bug: ícones escuros
sobre barra transparente sobre superfície `#131318` ficam invisíveis.

```kotlin
// MainActivity.onCreate
// auto(): ícones escuros no claro, claros no escuro, acompanhando o sistema.
// Os dois argumentos são as cores de "scrim" usadas quando o contraste é insuficiente;
// TRANSPARENT em ambos mantém o edge-to-edge real.
enableEdgeToEdge(
    statusBarStyle = SystemBarStyle.auto(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
    ),
    navigationBarStyle = SystemBarStyle.auto(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
    ),
)
```

Contraste dos ícones: claros `#FFFFFF` sobre `surface` escuro `#131318` = **18,52:1**;
escuros `#000000` sobre `surface` claro `#FBF8FF` = **19,97:1**.

`ApplySystemBarsVisibility` em `ReaderScreen.kt` só liga/desliga a visibilidade das barras
(modo imersivo) e **não precisa mudar** — ela não mexe na aparência dos ícones.

### 6.2 `AndroidManifest.xml` + `res/values-night`

Hoje: `android:theme="@android:style/Theme.Material.Light.NoActionBar"` — tema de plataforma
**fixo no claro**. Em modo escuro isso produz um flash branco no arranque a frio, antes do
Compose desenhar. Hoje não existe `app/src/main/res/values/` (só `res/drawable/`); criar:

`app/src/main/res/values/themes.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- DayNight: o sistema escolhe entre values/ e values-night/. -->
    <style name="Theme.JohnPdf" parent="@android:style/Theme.DeviceDefault.DayNight">
        <item name="android:windowActionBar">false</item>
        <item name="android:windowNoTitle">true</item>
        <!-- Casa com colorScheme.surface do esquema claro: sem flash branco/preto. -->
        <item name="android:windowBackground">@color/window_background</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
```

`app/src/main/res/values/colors.xml` → `<color name="window_background">#FBF8FF</color>`
`app/src/main/res/values-night/colors.xml` → `<color name="window_background">#131318</color>`

E no manifesto: `android:theme="@style/Theme.JohnPdf"`.

> `Theme.DeviceDefault.DayNight` existe desde a API 29. Como `minSdk` é 24, adicionar
> `app/src/main/res/values-v29/themes.xml` com o parent DayNight e deixar
> `values/themes.xml` com parent `@android:style/Theme.DeviceDefault.Light.NoActionBar`.
> Em API 24–28 o `windowBackground` continua resolvido por `values-night/colors.xml`
> (qualificador `night` existe desde a API 8), então o flash continua correto; só o tema
> base da plataforma fica claro — invisível, porque toda a UI é Compose.

---

## 7. Componentes afetados

| Componente | Arquivo | Hoje | Depois | Papel claro → escuro |
|---|---|---|---|---|
| **`TopAppBar`** (Home) | `HomeScreen.kt:156` | `Surface(tonalElevation = 3.dp)` | `TopAppBar` com `containerColor = surface`, `scrolledContainerColor = surfaceContainer` | `#FBF8FF` → `#131318` / `#EFEDF4` → `#1F1F25` |
| **`TopAppBar`** (leitor) | `ReaderScreen.kt:205` | `Surface(tonalElevation = 3.dp)` | idem; título `onSurface`, ícones `onSurfaceVariant` | idem |
| **`NavigationBar`** | `HomeScreen.kt:169` | `NavigationBar { }` (fundo lilás padrão) | `containerColor = surfaceContainer`; `NavigationBarItemDefaults.colors(indicatorColor = secondaryContainer, selectedIconColor = onSecondaryContainer, unselectedIconColor = onSurfaceVariant, ...)` | Indicador `#E0E2EC` → `#424659`; rótulo não selecionado `#45464F` → `#C6C5D0` |
| **`ListItem`** (cartão de PDF) | `HomeScreen.kt:269` `PdfCard` | `Surface(tonalElevation = 2.dp)` | `ListItem` com `containerColor = surface`, headline `onSurface`, supporting `onSurfaceVariant`, divisor `outlineVariant` recuado | `#FBF8FF` → `#131318`; divisor `#807F88` → `#85868F` |
| **`AlertDialog`** ×3 | `Dialogs.kt:28,37,49` | `AlertDialog` padrão (fundo lilás) | `containerColor = surfaceContainerHigh`, `iconContentColor`/`titleContentColor`/`textContentColor` explícitos | `#E9E7EF` → `#2A2A2F` |
| Campo de senha | `Dialogs.kt:54` | `OutlinedTextField` | Borda `outline`, rótulo `onSurfaceVariant`, erro `error` | `#BA1A1A` → `#FFB4AB` |
| Campo de busca | `HomeScreen.kt` (AllPdfsTab) | `OutlinedTextField` | `TextField` pílula `surfaceContainerHigh` | `#E9E7EF` → `#2A2A2F` |
| Barra inferior do leitor | `ReaderScreen.kt:233` | `Surface(tonalElevation = 3.dp)` | `BottomAppBar`/`Surface(color = surfaceContainer)`; botões com `readableTonalButtonColors()` | `#EFEDF4` → `#1F1F25` |
| **Vão entre páginas** | `ReaderScreen.kt:262` | `.background(PageGapColor)` `#BDBDBD` fixo | `.background(JohnTheme.colors.pageGap)` | `#E9E7EF` → `#0E0E13` |
| **Página do PDF** | `ReaderScreen.kt:325` | `.background(Color.White)` | `.background(PdfPageBackground)` — **branco nos dois modos** | `#FFFFFF` → `#FFFFFF` |
| Bitmap da página | `MuPdfEngine.kt:88` | `bitmap.eraseColor(Color.WHITE)` | **inalterado** | — |
| Tela de permissão | `HomeScreen.kt` `PermissionContent` | texto puro | ícone em círculo `primaryContainer`/`onPrimaryContainer` | `#DDE1FF`/`#00105C` → `#1B38A3`/`#DDE1FF` |
| Estados vazios | `HomeScreen.kt` | texto solto | ícone + título `onSurface` + corpo `onSurfaceVariant` | — |
| Snackbar (rotação) | `ReaderScreen.kt` (D1 seção C) | não existe | `inverseSurface`/`inverseOnSurface` (o padrão já inverte certo) | `#303036` → `#E4E1E9` |

**Regra transversal:** nenhum `tonalElevation` e nenhum `Color(0x…)` literal fora de
`Theme.kt`, com a única exceção de `PdfPageBackground`. No escuro `tonalElevation` clareia a
superfície com uma tinta de `primary`, o que reintroduz exatamente o problema de "cor vazada"
diagnosticado na seção A da D1.

### 7.1 A página branca no escuro

No modo escuro a página branca (`#FFFFFF`) fica sobre um vão `#0E0E13` — **19,25:1**. É um
salto de luminância grande e **é intencional**: o usuário pediu explicitamente que os PDFs
continuem brancos, e o vão mais escuro possível é o que melhor emoldura a página, deixando
claro onde uma acaba e a outra começa (no claro, `#E9E7EF` contra a página branca dá só
1,23:1 — o vão praticamente some, que é o efeito desejado lá). Quem quiser a página escura
precisa do "modo noturno do PDF", fora do escopo da v1.

---

## 8. Testes

### 8.1 `ContrastTest.kt` (novo, JVM puro — `app/src/test/java/com/johngabie/johnpdf/ui/theme/`)

Trava a regra de contraste nos dois esquemas. Roda sem Robolectric: `Color` do Compose é
multiplataforma e `lightColorScheme()`/`darkColorScheme()` não tocam em Android.

```kotlin
package com.johngabie.johnpdf.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Luminância relativa, WCAG 2.1 (https://www.w3.org/TR/WCAG21/#dfn-relative-luminance). */
private fun luminance(c: Color): Double {
    fun ch(v: Float): Double {
        val s = v.toDouble()
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
}

/** Razão de contraste WCAG: 1:1 (igual) a 21:1 (preto/branco). */
internal fun contrast(a: Color, b: Color): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

class ContrastTest {

    private fun ColorScheme.textPairs(): List<Triple<String, Color, Color>> = listOf(
        Triple("onPrimary/primary", onPrimary, primary),
        Triple("onSecondary/secondary", onSecondary, secondary),
        Triple("onTertiary/tertiary", onTertiary, tertiary),
        Triple("onError/error", onError, error),
        Triple("onBackground/background", onBackground, background),
        Triple("onSurface/surface", onSurface, surface),
        Triple("onSurfaceVariant/surfaceVariant", onSurfaceVariant, surfaceVariant),
        Triple("onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer),
        Triple("onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer),
        Triple("onTertiaryContainer/tertiaryContainer", onTertiaryContainer, tertiaryContainer),
        Triple("onErrorContainer/errorContainer", onErrorContainer, errorContainer),
        Triple("inverseOnSurface/inverseSurface", inverseOnSurface, inverseSurface),
    ) + listOf(
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    ).flatMap { (bgName, bg) ->
        // Tudo que o app escreve sobre um container: texto principal, secundário,
        // rótulo de TextButton (primary) e mensagem de erro.
        listOf("onSurface" to onSurface, "onSurfaceVariant" to onSurfaceVariant,
               "primary" to primary, "error" to error)
            .map { (fgName, fg) -> Triple("$fgName/$bgName", fg, bg) }
    }

    private fun assertText(schemeName: String, scheme: ColorScheme) {
        scheme.textPairs().forEach { (name, fg, bg) ->
            val r = contrast(fg, bg)
            assertTrue("[$schemeName] $name = %.2f:1, mínimo 4.5:1".format(r), r >= 4.5)
        }
    }

    @Test fun light_text_pairs_meet_wcag_aa() = assertText("claro", LightSchemeForTest)

    @Test fun dark_text_pairs_meet_wcag_aa() = assertText("escuro", DarkSchemeForTest)

    /** Divisores e bordas: WCAG 1.4.11 (não textual) pede 3:1. */
    @Test fun dividers_are_visible_on_every_surface_level() {
        listOf("claro" to LightSchemeForTest, "escuro" to DarkSchemeForTest).forEach { (n, s) ->
            listOf(s.surface, s.surfaceContainerLowest, s.surfaceContainerLow, s.surfaceContainer,
                   s.surfaceContainerHigh, s.surfaceContainerHighest).forEach { bg ->
                val r = contrast(s.outlineVariant, bg)
                assertTrue("[$n] outlineVariant = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
            }
        }
    }

    /** Regra do projeto (mais rígida que a WCAG): desabilitado continua legível. */
    @Test fun disabled_label_stays_readable() {
        listOf("claro" to LightSchemeForTest, "escuro" to DarkSchemeForTest).forEach { (n, s) ->
            val container = s.onSurface.copy(alpha = 0.12f).compositeOver(s.surfaceContainer)
            val label = s.onSurface.copy(alpha = 0.60f).compositeOver(container)
            val r = contrast(label, container)
            assertTrue("[$n] rótulo desabilitado = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
        }
    }

    /** O contraste medido não pode piorar sem alguém perceber. */
    @Test fun worst_case_is_documented() {
        val worstLight = LightSchemeForTest.textPairs().minOf { contrast(it.second, it.third) }
        val worstDark = DarkSchemeForTest.textPairs().minOf { contrast(it.second, it.third) }
        assertTrue("pior caso claro %.2f".format(worstLight), worstLight >= 4.5)
        assertTrue("pior caso escuro %.2f".format(worstDark), worstDark >= 4.5)
    }
}
```

> `compositeOver` vem de `androidx.compose.ui.graphics.compositeOver`.

### 8.2 `DarkThemeTest.kt` (novo, Robolectric — `app/src/test/…/ui/theme/`)

```kotlin
@RunWith(AndroidJUnit4::class)
class DarkThemeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun dark_theme_selects_dark_scheme() {
        var scheme: ColorScheme? = null
        rule.setContent { JohnPdfTheme(darkTheme = true) { scheme = MaterialTheme.colorScheme } }
        assertEquals(Color(0xFF131318), scheme!!.surface)
    }

    @Test fun light_theme_selects_light_scheme() {
        var scheme: ColorScheme? = null
        rule.setContent { JohnPdfTheme(darkTheme = false) { scheme = MaterialTheme.colorScheme } }
        assertEquals(Color(0xFFFBF8FF), scheme!!.surface)
    }

    /** O requisito do usuário: "PDFs continuam brancos". */
    @Test fun pdf_page_background_is_white_in_both_modes() {
        assertEquals(Color.White, PdfPageBackground)
        listOf(true, false).forEach { dark ->
            var gap: Color? = null
            rule.setContent { JohnPdfTheme(darkTheme = dark) { gap = JohnTheme.colors.pageGap } }
            // O vão muda com o tema; a página, não.
            assertNotEquals(Color.White, gap)
        }
    }

    @Test fun page_gap_frames_the_white_page_in_dark() {
        var gap: Color? = null
        rule.setContent { JohnPdfTheme(darkTheme = true) { gap = JohnTheme.colors.pageGap } }
        assertTrue(contrast(Color.White, gap!!) >= 3.0)
    }
}
```

### 8.3 Script de verificação fora do Kotlin

`tools/contrast.py` (criado junto com esta spec, mesma fórmula do `ContrastTest.kt`) confere
uma paleta **antes** de escrevê-la no `Theme.kt` — é o caminho rápido para quando a resposta da
G5 trouxer o novo seed. Não entra no build; sai com código 1 se algum par reprovar.

```
$ .venv-tools/Scripts/python tools/contrast.py
===== CLARO =====  ... >>> tudo dentro da regra
===== ESCURO ===== ... >>> tudo dentro da regra
```

Executado contra a paleta da §3: **80 pares conferidos, zero falhas** (pior caso 4,64:1).

### 8.4 Testes existentes a ajustar

| Teste | Mudança |
|---|---|
| `ThemeTest.kt` | Mínimo de 20sp → **16sp** (já previsto na seção F da D1) |
| `ReaderContentTest` | Se usar `PageGapColor`, trocar pelo papel de tema |
| Qualquer teste que construa `JohnPdfTheme { }` | Continua compilando: `darkTheme` tem valor padrão |
| `MainActivitySmokeTest` | Sem mudança de texto; rodar também com `qualifiers = "+night"` do Robolectric para pegar crash de tema |

### 8.5 E2E no aparelho (checklist — `docs/e2e/`)

Refazer as capturas **nos dois modos** (Configurações → Tela → Tema escuro), no moto g41:

1. Home/Recentes escuro — barra de status com ícones claros e visíveis.
2. Home/Todos + busca com texto escuro — placeholder e ícone legíveis.
3. Aba selecionada escuro — indicador `#424659` distinguível do fundo `#1F1F25`.
4. Diálogo "Remover da lista?" escuro — fundo `#2A2A2F`, não preto puro nem lilás.
5. Diálogo de senha escuro + estado de erro ("Senha incorreta") em `#FFB4AB`.
6. Leitor escuro — **página branca**, vão quase preto, barras escuras.
7. Leitor escuro com "Anterior" desabilitado na página 1 — o rótulo tem que dar para ler.
8. Arranque a frio em modo escuro — **sem flash branco**.
9. Trocar o tema do sistema com o leitor aberto — a página atual é mantida.
10. Escala de fonte 1,3 em modo escuro (a D1 pede 1,3 e 2,0 para o claro).

---

## 9. Arquivos afetados

| Arquivo | Ação |
|---|---|
| `ui/theme/Theme.kt` | Reescrito: dois esquemas completos, `JohnColors`, `PdfPageBackground`, `readableTonalButtonColors` |
| `MainActivity.kt` | `SystemBarStyle.light(...)` → `SystemBarStyle.auto(...)`; atualizar o comentário do F6 |
| `AndroidManifest.xml` | `android:theme` → `@style/Theme.JohnPdf` |
| `res/values/themes.xml`, `res/values-v29/themes.xml` | **novos** |
| `res/values/colors.xml`, `res/values-night/colors.xml` | **novos** |
| `ui/home/HomeScreen.kt` | Remover `tonalElevation`; cores explícitas em `TopAppBar`, `NavigationBar`, `ListItem` |
| `ui/reader/ReaderScreen.kt` | Remover `tonalElevation`; `PageGapColor` → `JohnTheme.colors.pageGap`; `Color.White` → `PdfPageBackground` |
| `ui/common/Dialogs.kt` | `containerColor` explícito nos três `AlertDialog` |
| `ui/common/BigButton.kt` | Cores de desabilitado (ou já removido pela D1) |
| `test/…/ui/theme/ContrastTest.kt` | **novo** |
| `test/…/ui/theme/DarkThemeTest.kt` | **novo** |
| `test/…/ui/theme/ThemeTest.kt` | 20sp → 16sp |
| `docs/e2e/2026-09-28-checklist.md` | +10 cenários da §8.5 |

**Esforço:** ~2h30 (esquema + `Theme.kt` 45min; barras do sistema e manifesto 30min;
componentes 30min; testes 45min), fora as recapturas de e2e. Bate com a estimativa "~2h" da
seção E da D1.

---

## 10. Riscos

1. **A paleta ainda não está decidida** (G5). Implementar com o índigo hoje significa refazer
   as duas tabelas de hex depois. *Mitigação:* fechar a G5 antes, ou aceitar que a troca custa
   ~20 min (só os `Color(0x…)` das famílias de marca) porque a estrutura e os testes não mudam.
2. **Regressão F6 ao contrário.** Trocar `light` por `auto` no `enableEdgeToEdge` é exatamente
   desfazer um conserto anterior — só é seguro **porque** agora existe esquema escuro. Cenário
   8 da §8.5 é obrigatório antes de dar como pronto.
3. **A página branca ofusca no escuro.** É o comportamento pedido, mas vale observar com um
   familiar; se incomodar, a saída é o "modo noturno do PDF" (v1.1), não escurecer o vão.
4. **Conflito com a D1 e com o modo imersivo.** Os três mexem em `ReaderScreen.kt`. Ordem
   sugerida: D1 (estrutura e componentes) → modo escuro (cores) → imersivo (já commitado em
   `5de0feb`, pode precisar de rebase).
5. **`tonalElevation` removido** muda também a aparência do modo claro (barras ficam lisas em
   vez de levemente tingidas). É desejado pela D1, mas é uma mudança visível no claro que sai
   "de carona" nesta tarefa.
6. **API 24–28 sem `Theme.DeviceDefault.DayNight`.** Mitigado por `values-v29/`; o
   `windowBackground` continua correto via `values-night/colors.xml`.

---

## 11. Perguntas em aberto

1. **Seed definitivo** (G5): mantém `#3558D4` ou espera o estudo de paleta do coolors.co?
2. **Alternar tema dentro do app** (Claro / Escuro / Do sistema em uma tela de ajustes)?
   Custo: +1h (`ThemeMode` no `SettingsRepository` — já existe DataStore, hoje só guarda
   `rotation_locked`) + uma tela de ajustes, que o app ainda não tem. Recomendação: **não**
   na v1; seguir o sistema é o que o usuário pediu.
3. **Ícone de PDF no escuro**: reusar `error` `#FFB4AB` (8,41:1, mas é a cor de erro) ou
   definir um vermelho próprio na `JohnColors`? Recomendação: cor própria em `JohnColors`,
   para "vermelho de PDF" nunca se confundir com "vermelho de erro".
