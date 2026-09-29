# Design D1 — Sistema Visual (tipografia, botões, alvos de toque, espaçamento) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refatorar os quatro fundamentos visuais do app — tipografia (10 estilos, mínimo 16sp), hierarquia de botões (Filled/FilledTonal/Text/Icon), alvos de toque (48dp piso M3 / 56dp ações frequentes / 72dp linha de lista) e espaçamento (escala de 4dp nomeada) — e remover `BigButton` por completo.

**Architecture:** `ui/theme/Theme.kt` passa a expor três blocos de tokens (`Space*`/`ScreenPadding`/`MinGap`, `PrimaryTouchTarget`/`ListItemMinHeight`/`MaxActionWidth`, `JohnTypography`) mais um esquema de cor claro/escuro completo; `ui/common/Buttons.kt` (novo) expõe `PrimaryButton`/`SecondaryButton` como os únicos wrappers sobre `Button`/`FilledTonalButton` do M3 (Text e Icon usam os componentes M3 crus, sem wrapper); `ui/common/Dialogs.kt` e as telas (`HomeScreen.kt`, `ReaderScreen.kt`) consomem esses tokens e componentes, substituindo todo uso de `BigButton` (apagado ao final). A migração é feita em ondas que mantêm o build verde a cada commit: tokens primeiro (com aliases depreciados para não quebrar `BigButton` enquanto ele ainda existe), depois `Buttons.kt`, depois cada consumidor, e só então `BigButton.kt` é apagado.

**Tech Stack:** Kotlin 2.1.21, Jetpack Compose, Material 3 (via `androidx.compose:compose-bom:2025.05.00`, `gradle/libs.versions.toml:4`), JUnit 4 + Robolectric + AndroidX Compose UI Test (testes em `app/src/test`, já configurados com `testOptions.unitTests.isIncludeAndroidResources = true`).

**Spec:** `docs/superpowers/specs/2026-09-29-design-d1-sistema-visual.md`

## Global Constraints

- `minSdk = 24` (`app/build.gradle.kts:22`) — nenhum token ou API usada pode exigir SDK maior.
- Material3 via `compose-bom = 2025.05.00` (`gradle/libs.versions.toml:4`) — usar apenas API estável do M3 dessa BOM; não subir a BOM neste plano.
- **Nenhuma quebra de existentes**: a cada task o projeto compila e `./gradlew testDebugUnitTest` passa antes do commit.
- Mínimo absoluto de tipografia: **16sp** em todos os 10 estilos de `JohnTypography` (spec §2.1).
- Hierarquia de tamanho invariante: `titleLarge > titleMedium ≥ bodyLarge > bodyMedium ≥ labelLarge` (spec §2.1).
- `PrimaryTouchTarget = 56.dp`, `ListItemMinHeight = 72.dp`, `MaxActionWidth = 360.dp`, `MinGap = 8.dp` (spec §3.1) — `MinTouchTarget = 64.dp` deixa de existir ao final do plano.
- Nunca dois botões **Filled** (`PrimaryButton`) na mesma tela ou diálogo (spec §3.2, invariante 1); `dismissButton` de diálogo é sempre `TextButton`.
- Ícone sozinho (`IconButton`/`IconToggleButton`) só com `contentDescription` não nulo; qualquer outra ação leva texto (spec §3.2, invariante 2).
- Botão desabilitado precisa continuar visível (usar as cores padrão do M3, nunca sobrescrever para cinza sólido) (spec §3.2, invariante 3).
- Escala de espaçamento de 4dp nomeada (`SpaceXs=4`, `SpaceS=8`, `SpaceM=12`, `SpaceL=16`, `SpaceXl=24`, `SpaceXxl=32`) — nenhum número solto novo no código de tela (spec §4).
- Todo texto de UI em pt-BR (herdado do plano `docs/superpowers/plans/2026-09-28-johnpdf-leitor-android.md:18`).

## Review Focus

- **Hierarquia de tamanho**: `titleLarge > titleMedium ≥ bodyLarge > bodyMedium ≥ labelLarge` e nenhum estilo abaixo de 16sp — uma pessoa com baixa visão espera que títulos sempre pareçam maiores que corpo de texto, nunca o contrário. Pinado em `ThemeTest` (Task 2).
- **Alvos de toque 48/56dp**: qualquer botão precisa medir pelo menos 48dp (piso M3) e as ações frequentes pelo menos 56dp — um usuário com tremor de mão espera não errar o alvo. Pinado em `ButtonsTest` (Task 4) e no par Anterior/Próxima de `ReaderContentTest` (Task 9).
- **Desabilitado continua visível**: um botão "Anterior" desabilitado na primeira página precisa continuar legível (não virar um retângulo cinza sem contraste) — é a correção direta do defeito visto em `docs/e2e/device/img/05`. Pinado em `ButtonsTest` (Task 4) e `ReaderContentTest` (Task 9).
- **Espaçamento de 8dp entre ações**: duas ações vizinhas (as setas empilhadas Anterior/Próxima) precisam ter pelo menos `MinGap = 8dp` de folga — sem isso um toque impreciso ativa a ação errada. Pinado em `ReaderContentTest` (Task 9).
- **Duas ações principais na mesma superfície**: nenhuma tela ou diálogo pode ter dois botões "Filled" ao mesmo tempo — isso é o que torna óbvio qual é *a* ação que o usuário veio fazer. Pinado via a tag `primary_button` em `ButtonsTest` (Task 4), `DialogsTest` (Task 5, `RemoveDialog`/`ErrorDialog`) e `HomeContentTest` (Task 6, tela de permissão).

---

### Task 1: `Theme.kt` — tokens de espaçamento e alvos de toque

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt:12` (remove `val MinTouchTarget = 64.dp`, adiciona tokens novos)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt`

**Interfaces:**
- Consumes: nada (arquivo raiz de tokens).
- Produces: `SpaceXs/S/M/L/Xl/Xxl: Dp`, `ScreenPadding: Dp`, `MinGap: Dp`, `PrimaryTouchTarget: Dp`, `ListItemMinHeight: Dp`, `MaxActionWidth: Dp`, `PageGap: Dp`, `PageElevation: Dp` — usados por todas as tasks seguintes. `MinTouchTarget` e `PageGapColor` continuam existindo (com `@Deprecated`, mesmos valores de hoje) até a Task 10.

- [ ] **Step 1: Escrever o teste que falha**

Adicione ao final de `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt` (mantendo o teste antigo por enquanto — ele será substituído na Task 2):

```kotlin
    @Test fun alvo_primario_acima_do_minimo_do_m3() {
        assertTrue("PrimaryTouchTarget deve ser >= 48dp", PrimaryTouchTarget >= 48.dp)
        assertTrue("ListItemMinHeight deve ser >= 72dp", ListItemMinHeight >= 72.dp)
    }
```

Adicione os imports que faltam no topo do arquivo:

```kotlin
import androidx.compose.ui.unit.dp
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ThemeTest"`
Expected: FAIL — `PrimaryTouchTarget`/`ListItemMinHeight` ainda não existem (erro de compilação "unresolved reference").

- [ ] **Step 3: Adicionar os tokens em `Theme.kt`**

No topo de `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt`, substitua a linha 12 (`val MinTouchTarget = 64.dp`) por:

```kotlin
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
```

`PageGapColor` (linha 13 original) continua como está por enquanto — só sai na Task 10, junto com `MinTouchTarget`. Adicione `@Deprecated` nela também:

```kotlin
@Deprecated("Sai na Task 10 do plano D1; use MaterialTheme.colorScheme.surfaceContainerHigh.")
val PageGapColor = Color(0xFFBDBDBD)
```

- [ ] **Step 4: Rodar o teste e confirmar que passa**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ThemeTest"`
Expected: PASS (o teste antigo `every_typography_style_is_at_least_20sp` continua passando sem mudanças — `BigTypography` ainda não foi tocada).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt
git commit -m "feat(theme): tokens de espaçamento (escala 4dp) e alvos de toque (D1 §3-4)"
```

---

### Task 2: `Theme.kt` — tipografia `JohnTypography` (10 estilos, mínimo 16sp)

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` (substitui `BigTypography` por `JohnTypography`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt` (reescrito — deixa de ser "tudo ≥ 20sp")

**Interfaces:**
- Consumes: nenhum token novo desta task; `Typography()` do M3.
- Produces: `JohnFontFamily: FontFamily` (= `FontFamily.Default` por ora — a troca para Atkinson Hyperlegible Next é um commit isolado fora deste plano, spec §2.2), `JohnTypography: Typography` — consumido pela Task 3 (`JohnPdfTheme`) e por todo o app via `MaterialTheme.typography`.

- [ ] **Step 1: Reescrever `ThemeTest.kt` com as quatro invariantes da spec (§8)**

Substitua o conteúdo inteiro de `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.theme

import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Invariantes de tipografia e alvo de toque da spec D1 (docs/superpowers/specs/2026-09-29-design-d1-sistema-visual.md §8). */
@RunWith(AndroidJUnit4::class)
class ThemeTest {
    private val styles = mapOf(
        "headlineMedium" to JohnTypography.headlineMedium,
        "titleLarge" to JohnTypography.titleLarge,
        "titleMedium" to JohnTypography.titleMedium,
        "titleSmall" to JohnTypography.titleSmall,
        "bodyLarge" to JohnTypography.bodyLarge,
        "bodyMedium" to JohnTypography.bodyMedium,
        "bodySmall" to JohnTypography.bodySmall,
        "labelLarge" to JohnTypography.labelLarge,
        "labelMedium" to JohnTypography.labelMedium,
        "labelSmall" to JohnTypography.labelSmall,
    )

    @Test fun nenhum_estilo_abaixo_de_16sp() {
        styles.forEach { (name, style) ->
            assertTrue("$name tinha ${style.fontSize.value}sp, mínimo é 16sp", style.fontSize.value >= 16f)
        }
    }

    @Test fun hierarquia_e_decrescente() {
        val titleLarge = JohnTypography.titleLarge.fontSize.value
        val titleMedium = JohnTypography.titleMedium.fontSize.value
        val bodyLarge = JohnTypography.bodyLarge.fontSize.value
        val bodyMedium = JohnTypography.bodyMedium.fontSize.value
        val labelLarge = JohnTypography.labelLarge.fontSize.value
        assertTrue("titleLarge ($titleLarge) deve ser > titleMedium ($titleMedium)", titleLarge > titleMedium)
        assertTrue("titleMedium ($titleMedium) deve ser >= bodyLarge ($bodyLarge)", titleMedium >= bodyLarge)
        assertTrue("bodyLarge ($bodyLarge) deve ser > bodyMedium ($bodyMedium)", bodyLarge > bodyMedium)
        assertTrue("bodyMedium ($bodyMedium) deve ser >= labelLarge ($labelLarge)", bodyMedium >= labelLarge)
    }

    @Test fun entrelinha_maior_ou_igual_ao_tamanho() {
        styles.forEach { (name, style) ->
            assertTrue(
                "$name tem lineHeight ${style.lineHeight.value}sp menor que fontSize ${style.fontSize.value}sp",
                style.lineHeight.value >= style.fontSize.value,
            )
        }
    }

    @Test fun alvo_primario_acima_do_minimo_do_m3() {
        assertTrue("PrimaryTouchTarget deve ser >= 48dp", PrimaryTouchTarget >= 48.dp)
        assertTrue("ListItemMinHeight deve ser >= 72dp", ListItemMinHeight >= 72.dp)
    }
}
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ThemeTest"`
Expected: FAIL — `JohnTypography` ainda não existe (só `BigTypography`).

- [ ] **Step 3: Substituir `BigTypography` por `JohnTypography` em `Theme.kt`**

Remova o bloco `private val Base = Typography()` / `internal val BigTypography = Typography(...)` (linhas 29-41 originais) e substitua por:

```kotlin
internal val JohnFontFamily = FontFamily.Default // troca para Atkinson Hyperlegible Next é um commit isolado (spec §2.2), fora deste plano

private val Base = Typography()
internal val JohnTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontFamily = JohnFontFamily, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge     = Base.titleLarge.copy(fontFamily = JohnFontFamily, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium    = Base.titleMedium.copy(fontFamily = JohnFontFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall     = Base.titleSmall.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge      = Base.bodyLarge.copy(fontFamily = JohnFontFamily, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium     = Base.bodyMedium.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall      = Base.bodySmall.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 20.sp),
    labelLarge     = Base.labelLarge.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium    = Base.labelMedium.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelSmall     = Base.labelSmall.copy(fontFamily = JohnFontFamily, fontSize = 16.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)
```

Adicione o import que falta:

```kotlin
import androidx.compose.ui.text.font.FontFamily
```

Atualize `JohnPdfTheme` (linhas 43-46 originais) para referenciar `JohnTypography` em vez de `BigTypography`:

```kotlin
@Composable
fun JohnPdfTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = JohnTypography, content = content)
}
```

(A troca para claro/escuro com parâmetro `dark` acontece na Task 3 — por ora só o nome da tipografia muda.)

- [ ] **Step 4: Rodar o teste e confirmar que passa**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ThemeTest"`
Expected: PASS — as 4 invariantes (`nenhum_estilo_abaixo_de_16sp`, `hierarquia_e_decrescente`, `entrelinha_maior_ou_igual_ao_tamanho`, `alvo_primario_acima_do_minimo_do_m3`) passam.

- [ ] **Step 5: Rodar a suíte completa para garantir que nada mais quebrou**

Run: `./gradlew testDebugUnitTest`
Expected: PASS em todos os testes (os textos visuais ainda são os antigos; só o *tamanho* da fonte mudou, então `DialogsTest`/`HomeContentTest`/`ReaderContentTest` continuam batendo).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt
git commit -m "feat(theme): JohnTypography com 10 estilos e piso de 16sp (D1 §2)"
```

---

### Task 3: `Theme.kt` — esquema de cor claro/escuro completo

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` (substitui `Colors`/`lightColorScheme` único por `LightColors`/`DarkColors`; `JohnPdfTheme` ganha parâmetro `dark`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt` (novo teste de composição)

**Interfaces:**
- Consumes: `JohnTypography` (Task 2).
- Produces: `JohnPdfTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)` — assinatura pública muda (parâmetro novo com default, nenhum call site precisa mudar). Consumido por `MainActivity.kt:38` (inalterado, usa a lambda-trailing) e por todos os testes de Compose do projeto.

- [ ] **Step 1: Escrever o teste que falha**

Adicione a `ThemeTest.kt` (precisa de `createComposeRule`, então vira parte de um teste com regra de Compose — adicione ao mesmo arquivo):

```kotlin
    @get:Rule val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test fun tema_compoe_em_claro_e_escuro_sem_quebrar() {
        composeRule.setContent {
            JohnPdfTheme(dark = false) { androidx.compose.material3.Text("claro") }
        }
        composeRule.onNodeWithText("claro").assertIsDisplayed()
        composeRule.setContent {
            JohnPdfTheme(dark = true) { androidx.compose.material3.Text("escuro") }
        }
        composeRule.onNodeWithText("escuro").assertIsDisplayed()
    }
```

Adicione os imports:

```kotlin
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ThemeTest"`
Expected: FAIL — `JohnPdfTheme` ainda não aceita o parâmetro `dark`.

- [ ] **Step 3: Substituir o esquema de cor único por claro/escuro completos**

Substitua o bloco `private val Colors = lightColorScheme(...)` (linhas 15-27 originais) por:

```kotlin
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
```

Atualize `JohnPdfTheme`:

```kotlin
@Composable
fun JohnPdfTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = JohnTypography,
        content = content,
    )
}
```

Adicione os imports que faltam:

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
```

- [ ] **Step 4: Rodar o teste e confirmar que passa**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ThemeTest"`
Expected: PASS.

- [ ] **Step 5: Rodar a suíte completa**

Run: `./gradlew testDebugUnitTest`
Expected: PASS — `MainActivity.kt:38` chama `JohnPdfTheme { ... }` sem argumento nomeado, então o novo parâmetro com default não quebra nada.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt
git commit -m "feat(theme): esquema de cor claro/escuro completo, JohnPdfTheme(dark=...) (D1 §6)"
```

---

### Task 4: `ui/common/Buttons.kt` — `PrimaryButton` / `SecondaryButton`

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/common/Buttons.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/common/ButtonsTest.kt` (novo)
- Modify: `app/build.gradle.kts:24` e `gradle/libs.versions.toml:24` (dependência `material-icons-extended`, necessária a partir daqui — `FolderOpen`, `PictureAsPdf`, `Lock`/`LockOpen`, `Keyboard`/`Dialpad`, `Visibility` usados nas Tasks 5-9 não existem no pacote core)

**Interfaces:**
- Consumes: `PrimaryTouchTarget`, `MaxActionWidth`, `SpaceL`, `SpaceM`, `SpaceS` (Task 1).
- Produces:
```kotlin
@Composable fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true)
@Composable fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, height: Dp = PrimaryTouchTarget)
```
Todo `PrimaryButton` carrega `Modifier.testTag("primary_button")` — é assim que as Tasks 5/6 pinam a invariante "nunca dois Filled na mesma superfície".

- [ ] **Step 1: Adicionar a dependência `material-icons-extended`**

Em `gradle/libs.versions.toml`, logo abaixo da linha 24 (`androidx-compose-material3 = ...`):

```toml
androidx-compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
```

Em `app/build.gradle.kts`, logo abaixo da linha 72 (`implementation(libs.androidx.compose.material3)`):

```kotlin
    implementation(libs.androidx.compose.material.icons.extended)
```

- [ ] **Step 2: Escrever o teste que falha**

Crie `app/src/test/java/com/johngabie/johnpdf/ui/common/ButtonsTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.common

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ButtonsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun primary_button_is_clickable_and_at_least_56dp_tall() {
        var clicked = false
        rule.setContent { JohnPdfTheme { PrimaryButton("Permitir acesso", onClick = { clicked = true }) } }
        rule.onNodeWithText("Permitir acesso").assertHeightIsAtLeast(56.dp).performClick()
        assertTrue(clicked)
    }

    @Test fun secondary_button_default_height_is_56dp() {
        rule.setContent { JohnPdfTheme { SecondaryButton("Abrir", onClick = {}) } }
        rule.onNodeWithText("Abrir").assertHeightIsAtLeast(56.dp)
    }

    @Test fun secondary_button_accepts_48dp_height_override() {
        rule.setContent { JohnPdfTheme { SecondaryButton("Abrir", onClick = {}, height = 48.dp) } }
        rule.onNodeWithText("Abrir").assertHeightIsAtLeast(48.dp)
    }

    @Test fun disabled_primary_button_stays_visible_and_not_clickable() {
        rule.setContent { JohnPdfTheme { PrimaryButton("Remover", onClick = {}, enabled = false) } }
        rule.onNodeWithText("Remover").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test fun only_one_primary_button_tagged_per_surface() {
        rule.setContent { JohnPdfTheme { PrimaryButton("Remover", onClick = {}) } }
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
    }
}
```

- [ ] **Step 3: Rodar o teste e confirmar que falha**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.common.ButtonsTest"`
Expected: FAIL — `PrimaryButton`/`SecondaryButton` não existem.

- [ ] **Step 4: Criar `Buttons.kt`**

```kotlin
package com.johngabie.johnpdf.ui.common

import androidx.compose.foundation.layout.PaddingValues
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

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(SpaceS))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}
```

`contentDescription = null` no ícone é proposital: o texto ao lado já é o rótulo acessível (spec §3.3).

- [ ] **Step 5: Rodar o teste e confirmar que passa**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.common.ButtonsTest"`
Expected: PASS — as 5 asserções (altura ≥56/48dp, clique, desabilitado visível, uma única tag `primary_button`).

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/johngabie/johnpdf/ui/common/Buttons.kt app/src/test/java/com/johngabie/johnpdf/ui/common/ButtonsTest.kt
git commit -m "feat(ui): PrimaryButton/SecondaryButton — hierarquia Filled/FilledTonal (D1 §3.3)"
```

`BigButton.kt` continua existindo e compilando normalmente (ainda é usado por `Dialogs.kt`, `HomeScreen.kt`, `ReaderScreen.kt`) — ele só é apagado na Task 10.

---

### Task 5: `ui/common/Dialogs.kt` — migrar para `PrimaryButton`/`TextButton`

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt` (arquivo inteiro — `ErrorDialog`, `ConfirmDialog` → `RemoveDialog`, `PasswordDialog`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/common/DialogsTest.kt` (reescrito)

**Interfaces:**
- Consumes: `PrimaryButton` (Task 4), `SpaceM` (Task 1).
- Produces: `ErrorDialog(error: AppError, onDismiss: () -> Unit)` (assinatura igual), `RemoveDialog(onConfirm: () -> Unit, onCancel: () -> Unit)` (**substitui** `ConfirmDialog(question, onYes, onNo)` — a pergunta deixa de ser parâmetro, o texto é fixo), `PasswordDialog(wrongAttempt: Boolean, onSubmit: (String) -> Unit, onCancel: () -> Unit)` (assinatura igual). `HomeScreen.kt:145` (único chamador de `ConfirmDialog`) é ajustado na Task 6.

- [ ] **Step 1: Escrever os testes que falham**

Substitua o conteúdo inteiro de `app/src/test/java/com/johngabie/johnpdf/ui/common/DialogsTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.common

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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

    @Test fun error_dialog_shows_message_ok_dismisses_and_has_no_primary_button() {
        var dismissed = false
        rule.setContent { JohnPdfTheme { ErrorDialog(AppError.GONE) { dismissed = true } } }
        rule.onNodeWithText("Este arquivo não está mais disponível.").assertIsDisplayed()
        rule.onAllNodesWithTag("primary_button").assertCountEquals(0) // é um aviso, não uma ação (spec §7)
        rule.onNodeWithText("OK").performClick()
        assertTrue(dismissed)
    }

    @Test fun remove_dialog_shows_title_and_support_text() {
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = {}) } }
        rule.onNodeWithText("Remover da lista?").assertIsDisplayed()
        rule.onNodeWithText("O arquivo continua no celular.").assertIsDisplayed()
    }

    @Test fun remove_dialog_has_exactly_one_primary_button() {
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = {}) } }
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
    }

    @Test fun remove_dialog_confirm_calls_on_confirm() {
        var confirmed = false
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = { confirmed = true }, onCancel = {}) } }
        rule.onNodeWithText("Remover").performClick()
        assertTrue(confirmed)
    }

    @Test fun remove_dialog_cancel_calls_on_cancel() {
        var cancelled = false
        rule.setContent { JohnPdfTheme { RemoveDialog(onConfirm = {}, onCancel = { cancelled = true }) } }
        rule.onNodeWithText("Cancelar").performClick()
        assertTrue(cancelled)
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

    @Test fun password_dialog_toggles_keyboard_label_without_prefix() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("Usar letras").performClick()
        rule.onNodeWithText("Usar números").assertIsDisplayed()
    }

    @Test fun password_dialog_has_exactly_one_primary_button() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
    }
}
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.common.DialogsTest"`
Expected: FAIL — `RemoveDialog` não existe, textos "Sim"/"Não"/"abc  Usar letras" ainda são os antigos.

- [ ] **Step 3: Reescrever `Dialogs.kt`**

Substitua o conteúdo inteiro de `app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt`:

```kotlin
package com.johngabie.johnpdf.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.johngabie.johnpdf.ui.theme.SpaceM
import com.johngabie.johnpdf.ui.theme.SpaceXs

@Composable
fun ErrorDialog(error: AppError, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Error, contentDescription = null) },
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
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        title = { Text("Remover da lista?", style = MaterialTheme.typography.titleLarge) },
        text = { Text("O arquivo continua no celular.", style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { PrimaryButton("Remover", onConfirm) },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
    )
}

@Composable
fun PasswordDialog(wrongAttempt: Boolean, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var numeric by rememberSaveable { mutableStateOf(true) }
    var visible by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
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
                                if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
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
                    Icon(if (numeric) Icons.Filled.Keyboard else Icons.Filled.Dialpad, contentDescription = null)
                    Spacer(Modifier.width(SpaceXs))
                    Text(if (numeric) "Usar letras" else "Usar números", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = { PrimaryButton("Abrir", onClick = { onSubmit(password) }, enabled = password.isNotEmpty()) },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
    )
}
```

Note que os dois `Modifier.heightIn(min = MinTouchTarget)` que existiam nos `TextButton` (linhas 73 e 80 originais) somem — `TextButton` já nasce com 48dp de alvo (spec §3.1).

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.common.DialogsTest"`
Expected: PASS — todos os 9 testes.

- [ ] **Step 5: Rodar a suíte completa**

Run: `./gradlew testDebugUnitTest`
Expected: FAIL apenas em `HomeContentTest` (`long_press_asks_before_removing`, que ainda espera `ConfirmDialog`/"Sim") — confirme que é exatamente essa falha esperada; ela é corrigida na Task 6. Nenhum outro teste deve quebrar.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt app/src/test/java/com/johngabie/johnpdf/ui/common/DialogsTest.kt
git commit -m "feat(ui): Dialogs.kt migra para PrimaryButton/TextButton; ConfirmDialog vira RemoveDialog (D1 §7)"
```

---

### Task 6: `HomeScreen.kt` — header, bottom nav e permissão

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt:55-58` (imports), `:114-152` (`HomeContent`, incluindo o diálogo de remoção), `:154-165` (`HomeHeader`), `:167-183` (`HomeBottomBar`), `:240-258` (`PermissionContent`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt` (3 testes ajustados)

**Interfaces:**
- Consumes: `SecondaryButton`, `PrimaryButton` (Task 4), `RemoveDialog` (Task 5), `SpaceS`/`SpaceL`/`SpaceXl` (Task 1).
- Produces: nenhuma assinatura pública nova nesta task (`HomeContent`/`HomeScreen` mantêm as mesmas assinaturas).

- [ ] **Step 1: Escrever os testes que falham**

Em `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt`, ajuste os três testes afetados:

```kotlin
    @Test fun header_and_bottom_menu_are_visible() {
        show(HomeUiState())
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
        rule.onNodeWithText("Abrir").performClick()
        rule.onNodeWithText("Todos os PDFs").performClick()
        assertEquals(listOf("picker", "tab:ALL"), events)
    }
```

```kotlin
    @Test fun long_press_asks_before_removing() {
        show(HomeUiState(recents = listOf(recent)))
        rule.onNodeWithText("Fatura.pdf").performTouchInput { longClick() }
        rule.onNodeWithText("Remover da lista?").assertIsDisplayed()
        rule.onNodeWithText("Remover").performClick()
        assertEquals(listOf("remove:Fatura.pdf"), events)
    }
```

```kotlin
    @Test fun all_tab_without_access_asks_permission() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = false))
        rule.onNodeWithText("Para mostrar os PDFs do celular, o johnPDF precisa de permissão.").assertIsDisplayed()
        rule.onNodeWithText("Permitir acesso").performClick()
        assertEquals(listOf("permission"), events)
    }
```

Adicione também um teste novo, pinando a invariante "nunca dois Filled":

```kotlin
    @Test fun permission_screen_has_exactly_one_primary_button() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = false))
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
    }
```

Adicione os imports que faltam no topo do arquivo:

```kotlin
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.home.HomeContentTest"`
Expected: FAIL — "Abrir"/"Remover"/"Permitir acesso" ainda não existem na tela.

- [ ] **Step 3: Atualizar os imports de `HomeScreen.kt`**

Substitua as linhas 55-58 originais:

```kotlin
import com.johngabie.johnpdf.ui.common.BigButton
import com.johngabie.johnpdf.ui.common.ConfirmDialog
import com.johngabie.johnpdf.ui.common.ErrorDialog
```

por:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import com.johngabie.johnpdf.ui.common.ErrorDialog
import com.johngabie.johnpdf.ui.common.PrimaryButton
import com.johngabie.johnpdf.ui.common.RemoveDialog
import com.johngabie.johnpdf.ui.common.SecondaryButton
import com.johngabie.johnpdf.ui.theme.SpaceS
```

Adicione `@file:OptIn(ExperimentalMaterial3Api::class)` como a primeira linha do arquivo, antes de `package com.johngabie.johnpdf.ui.home` (necessário para `TopAppBar`).

- [ ] **Step 4: Trocar o diálogo de remoção em `HomeContent`**

Substitua o bloco `pendingRemoval?.let { ... }` (linhas 144-150 originais):

```kotlin
    pendingRemoval?.let { item ->
        RemoveDialog(
            onConfirm = { onRemoveRecent(item); pendingRemoval = null },
            onCancel = { pendingRemoval = null },
        )
    }
```

- [ ] **Step 5: Reescrever `HomeHeader` com `TopAppBar` + `SecondaryButton`**

Substitua a função inteira (linhas 154-165 originais):

```kotlin
@Composable
private fun HomeHeader(onOpenPicker: () -> Unit) {
    TopAppBar(
        title = { Text("johnPDF", style = MaterialTheme.typography.titleLarge) },
        actions = {
            SecondaryButton(
                text = "Abrir",
                onClick = onOpenPicker,
                icon = Icons.Filled.FolderOpen,
                height = 48.dp,
                modifier = Modifier.padding(end = SpaceS),
            )
        },
    )
}
```

- [ ] **Step 6: Trocar os emojis do bottom nav por ícones M3**

Substitua a função inteira (linhas 167-183 originais):

```kotlin
@Composable
private fun HomeBottomBar(tab: HomeTab, onSelectTab: (HomeTab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = tab == HomeTab.RECENTS,
            onClick = { onSelectTab(HomeTab.RECENTS) },
            icon = { Icon(Icons.Filled.History, contentDescription = null) },
            label = { Text("Recentes", style = MaterialTheme.typography.labelMedium) },
        )
        NavigationBarItem(
            selected = tab == HomeTab.ALL,
            onClick = { onSelectTab(HomeTab.ALL) },
            icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
            label = { Text("Todos os PDFs", style = MaterialTheme.typography.labelMedium) },
        )
    }
}
```

- [ ] **Step 7: Trocar `BigButton` por `PrimaryButton` em `PermissionContent`**

Substitua a última linha da função (linha 256 original, `BigButton("Permitir", onRequestPermission, Modifier.fillMaxWidth())`):

```kotlin
        PrimaryButton("Permitir acesso", onRequestPermission, Modifier.fillMaxWidth())
```

- [ ] **Step 8: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.home.HomeContentTest"`
Expected: PASS — todos os testes, incluindo o novo `permission_screen_has_exactly_one_primary_button`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat(home): header com TopAppBar+SecondaryButton, bottom nav com ícones, RemoveDialog (D1 §5)"
```

---

### Task 7: `HomeScreen.kt` — lista, busca e estado vazio

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt:185-201` (`RecentsTab`), `:203-238` (`AllPdfsTab`), `:267-286` (`PdfCard` → `PdfListItem`); `HomeContent` (linha ~135, call site de `RecentsTab`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt` (1 teste novo)

**Interfaces:**
- Consumes: `ListItemMinHeight` (Task 1).
- Produces: `RecentsTab` ganha um parâmetro novo `onOpenPicker: () -> Unit` (usado pela ação do estado vazio); nenhuma outra assinatura pública muda.

- [ ] **Step 1: Escrever o teste que falha**

Adicione a `HomeContentTest.kt`:

```kotlin
    @Test fun empty_recents_action_opens_picker() {
        show(HomeUiState())
        rule.onNodeWithText("Abrir PDF").performClick()
        assertEquals(listOf("picker"), events)
    }
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.home.HomeContentTest.empty_recents_action_opens_picker"`
Expected: FAIL — não existe nenhum nó de texto "Abrir PDF" ainda.

- [ ] **Step 3: Adicionar `EmptyState` e migrar `RecentsTab`**

Adicione a nova função privada (perto de `CenteredMessage`, linha 260 original) e reescreva `RecentsTab` (linhas 186-201 originais):

```kotlin
@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(SpaceXxl),
        verticalArrangement = Arrangement.spacedBy(SpaceL, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        TextButton(onClick = onAction) { Text(actionLabel, style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun RecentsTab(
    items: List<RecentItem>,
    nowMillis: Long,
    onOpen: (RecentItem) -> Unit,
    onLongPress: (RecentItem) -> Unit,
    onOpenPicker: () -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.InsertDriveFile,
            title = "Nenhum PDF recente",
            message = "Os PDFs que você abrir vão aparecer aqui.",
            actionLabel = "Abrir PDF",
            onAction = onOpenPicker,
        )
        return
    }
    LazyColumn(Modifier.fillMaxWidth()) {
        itemsIndexed(items, key = { _, item -> item.path }) { index, item ->
            PdfListItem(
                name = item.name,
                subtitle = "${item.origin.label} · ${friendlyDate(item.openedAt, nowMillis)}",
                onClick = { onOpen(item) },
                onLongClick = { onLongPress(item) },
            )
            if (index < items.lastIndex) {
                HorizontalDivider(Modifier.padding(start = ListItemMinHeight), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
```

Ajuste o call site em `HomeContent` (dentro do `when (state.tab)`, linha ~135 original):

```kotlin
                HomeTab.RECENTS -> RecentsTab(state.recents, nowMillis, onOpenRecent, onLongPress = { pendingRemoval = it }, onOpenPicker = onOpenPicker)
```

- [ ] **Step 4: Migrar `AllPdfsTab` (busca em pílula + lista plana)**

Substitua a função inteira (linhas 203-238 originais):

```kotlin
@Composable
private fun AllPdfsTab(
    pdfs: List<PdfFile>,
    query: String,
    loading: Boolean,
    nowMillis: Long,
    onQueryChange: (String) -> Unit,
    onOpen: (PdfFile) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar pelo nome…", style = MaterialTheme.typography.bodyLarge) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(SpaceL).heightIn(min = 56.dp),
        )
        when {
            loading && pdfs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            pdfs.isEmpty() -> CenteredMessage(if (query.isBlank()) "Nenhum PDF encontrado no celular." else "Nenhum PDF com esse nome.")
            else -> LazyColumn(Modifier.fillMaxWidth()) {
                itemsIndexed(pdfs, key = { _, pdf -> pdf.path }) { index, pdf ->
                    PdfListItem(
                        name = pdf.name,
                        subtitle = "${pdf.origin.label} · ${friendlyDate(pdf.modifiedAt, nowMillis)}",
                        onClick = { onOpen(pdf) },
                    )
                    if (index < pdfs.lastIndex) {
                        HorizontalDivider(Modifier.padding(start = ListItemMinHeight), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
```

Adicione os imports que faltam:

```kotlin
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.TextField
import com.johngabie.johnpdf.ui.theme.ListItemMinHeight
import com.johngabie.johnpdf.ui.theme.SpaceXxl
```

- [ ] **Step 5: Migrar `PdfCard` para `PdfListItem` (M3 `ListItem`, 72dp)**

Substitua a função inteira (linhas 267-286 originais):

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfListItem(name: String, subtitle: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(40.dp)) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ListItemMinHeight)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}
```

Remova o import agora não usado `androidx.compose.foundation.shape.RoundedCornerShape`? **Não** — ele continua em uso no `TextField` do Step 4. Remova apenas `androidx.compose.ui.unit.sp` se, após este step, nenhum `.sp` literal restar no arquivo (confira com uma busca antes de remover).

- [ ] **Step 6: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.home.HomeContentTest"`
Expected: PASS — todos os testes, incluindo `empty_recents_action_opens_picker`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt
git commit -m "feat(home): lista plana com ListItem 72dp, busca em pílula, estado vazio com ação (D1 §4-5)"
```

---

### Task 8: `ReaderScreen.kt` — barra superior (voltar, rotação)

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt:76-80` (imports), `:132-153` (call sites de `ReaderTopBar`/`ReaderBottomBar` em `ReaderContent`), `:204-222` (`ReaderTopBar`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt` (2 testes ajustados)

**Interfaces:**
- Consumes: `SpaceS`/`SpaceXs` (Task 1).
- Produces: `ReaderTopBar` ganha os parâmetros `rotationLocked: Boolean` e `onToggleRotation: () -> Unit` (a rotação sai da barra inferior); `ReaderBottomBar` perde `rotationLocked`/`onToggleRotation` (ajustado na Task 9).

- [ ] **Step 1: Escrever os testes que falham**

Em `ReaderContentTest.kt`, ajuste:

```kotlin
    @Test fun back_button_calls_on_back() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithContentDescription("Voltar").performClick()
        assertEquals(1, backCalls)
    }
```

```kotlin
    @Test fun rotation_toggle_switches_state() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithTag("rotation_toggle").assertIsOff()
        rule.onNodeWithTag("rotation_toggle").performClick()
        rule.onNodeWithTag("rotation_toggle").assertIsOn()
        assertEquals(1, rotationToggles)
    }
```

(Isso substitui o antigo `rotation_button_toggles_label`.) Adicione os imports que faltam:

```kotlin
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.reader.ReaderContentTest"`
Expected: FAIL — não existe nó com `contentDescription = "Voltar"` nem `testTag = "rotation_toggle"` ainda (o botão de voltar ainda é um `TextButton("← Voltar")` e a rotação ainda está na barra inferior).

- [ ] **Step 3: Atualizar imports de `ReaderScreen.kt`**

Substitua as linhas 76-80 originais:

```kotlin
import com.johngabie.johnpdf.ui.common.BigButton
import com.johngabie.johnpdf.ui.common.ErrorDialog
import com.johngabie.johnpdf.ui.common.PasswordDialog
import com.johngabie.johnpdf.ui.theme.MinTouchTarget
import com.johngabie.johnpdf.ui.theme.PageGapColor
```

por:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.ui.platform.testTag
import com.johngabie.johnpdf.ui.common.ErrorDialog
import com.johngabie.johnpdf.ui.common.PasswordDialog
import com.johngabie.johnpdf.ui.theme.MinGap
import com.johngabie.johnpdf.ui.theme.PrimaryTouchTarget
import com.johngabie.johnpdf.ui.theme.SpaceL
import com.johngabie.johnpdf.ui.theme.SpaceS
import com.johngabie.johnpdf.ui.theme.SpaceXs
```

(`Icons.AutoMirrored.Filled.ArrowBack`, `KeyboardArrowUp/Down`, `Lock`/`LockOpen` são usados nesta e na próxima task.)

- [ ] **Step 4: Mover a rotação da barra inferior para a superior**

Em `ReaderContent` (linhas 132-153 originais), atualize os dois call sites:

```kotlin
        topBar = {
            AnimatedVisibility(showBars, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                ReaderTopBar(state.title, state.rotationLocked, onBack, onToggleRotation)
            }
        },
        bottomBar = {
            AnimatedVisibility(
                showBars && status is ReaderStatus.Ready,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                ReaderBottomBar(
                    current = state.currentPage,
                    total = state.pageCount,
                    onPrevious = { scope.launch { listState.animateScrollToItem((state.currentPage - 1).coerceAtLeast(0)) } },
                    onNext = { scope.launch { listState.animateScrollToItem((state.currentPage + 1).coerceAtMost(state.pageCount - 1)) } },
                )
            }
        },
```

- [ ] **Step 5: Reescrever `ReaderTopBar`**

Substitua a função inteira (linhas 204-222 originais):

```kotlin
@Composable
private fun ReaderTopBar(title: String, rotationLocked: Boolean, onBack: () -> Unit, onToggleRotation: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = SpaceS, vertical = SpaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = SpaceS),
            )
            IconToggleButton(
                checked = rotationLocked,
                onCheckedChange = { onToggleRotation() },
                modifier = Modifier.testTag("rotation_toggle"),
            ) {
                Icon(
                    if (rotationLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (rotationLocked) "Girar automaticamente" else "Travar rotação",
                )
            }
        }
    }
}
```

- [ ] **Step 6: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.reader.ReaderContentTest"`
Expected: `back_button_calls_on_back` e `rotation_toggle_switches_state` PASSAM. `ready_shows_page_label_and_button_states`, `next_button_scrolls_to_next_page` e `tapping_the_page_hides_and_restores_the_bars` ainda FALHAM (esperado — são corrigidos na Task 9, que muda a barra inferior). `ReaderBottomBar` ainda não compila com a assinatura nova até a Task 9: **por isso o arquivo inteiro não compila ainda** — rode em vez disso um `./gradlew compileDebugKotlin` mental checklist: se preferir manter o build verde a cada task, aplique a Task 9 na sequência antes de rodar a suíte completa. Documente no commit desta task que ela é intermediária.

- [ ] **Step 7: Commit (intermediário — build só volta a compilar completo após a Task 9)**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt
git commit -m "feat(reader): barra superior com voltar em ícone e rotação via IconToggleButton (D1 §5) [wip: bottom bar na próxima task]"
```

---

### Task 9: `ReaderScreen.kt` — barra inferior (setas empilhadas) e cor do gap entre páginas

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt:224-248` (`ReaderBottomBar`), `:250-289` (`PageList`), `:316-344` (`PdfPage`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt` (3 testes ajustados, 1 novo)

**Interfaces:**
- Consumes: `PrimaryTouchTarget`, `MinGap`, `PageGap`, `PageElevation` (Task 1).
- Produces: `ReaderBottomBar(current: Int, total: Int, onPrevious: () -> Unit, onNext: () -> Unit)` (perde `rotationLocked`/`onToggleRotation`, já movidos na Task 8).

- [ ] **Step 1: Escrever os testes que falham**

Em `ReaderContentTest.kt`, ajuste:

```kotlin
    @Test fun ready_shows_page_label_and_button_states() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithText("doc.pdf").assertIsDisplayed()
        rule.onNodeWithText("Página 1 de 3").assertIsDisplayed()
        rule.onNodeWithContentDescription("Página anterior").assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Próxima página").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun next_button_scrolls_to_next_page() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithContentDescription("Próxima página").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Página 2 de 3").assertIsDisplayed()
    }
```

```kotlin
    @Test fun tapping_the_page_hides_and_restores_the_bars() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithContentDescription("Página 1").performTouchInput { click() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Próxima página").assertDoesNotExist()
        rule.onNodeWithContentDescription("Voltar").assertDoesNotExist()

        rule.onNodeWithContentDescription("Página 1").performTouchInput { click() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Próxima página").assertIsDisplayed()
        rule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
    }
```

Adicione um teste novo pinando o alvo de 56dp e o gap de 8dp entre as setas (Review Focus
"alvos de toque 48/56dp" e "espaçamento de 8dp entre ações"):

```kotlin
    @Test fun previous_and_next_buttons_are_56dp_with_at_least_8dp_gap() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        val previous = rule.onNodeWithContentDescription("Página anterior").assertHeightIsAtLeast(56.dp)
        val next = rule.onNodeWithContentDescription("Próxima página").assertHeightIsAtLeast(56.dp)
        val gap = next.getUnclippedBoundsInRoot().top - previous.getUnclippedBoundsInRoot().bottom
        assertTrue("gap era $gap, esperado >= 8dp", gap >= 8.dp)
    }
```

Adicione os imports que faltam:

```kotlin
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.reader.ReaderContentTest"`
Expected: FAIL — `ReaderBottomBar` ainda espera `rotationLocked`/`onToggleRotation` e não tem `contentDescription "Página anterior"/"Próxima página"`.

- [ ] **Step 3: Reescrever `ReaderBottomBar` com setas empilhadas**

Substitua a função inteira (linhas 224-248 originais):

```kotlin
@Composable
private fun ReaderBottomBar(current: Int, total: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SpaceL, vertical = SpaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(pageLabel(current, total), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(MinGap)) {
                FilledTonalIconButton(
                    onClick = onPrevious,
                    enabled = current > 0,
                    modifier = Modifier.size(PrimaryTouchTarget),
                ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Página anterior") }
                FilledTonalIconButton(
                    onClick = onNext,
                    enabled = current < total - 1,
                    modifier = Modifier.size(PrimaryTouchTarget),
                ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Próxima página") }
            }
        }
    }
}
```

- [ ] **Step 4: Trocar `PageGapColor` por `surfaceContainerHigh` e usar `PageGap`/`PageElevation`**

Em `PageList` (linhas 250-289 originais), troque a linha do `BoxWithConstraints`:

```kotlin
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
```

e troque `verticalArrangement = Arrangement.spacedBy(12.dp)` da `LazyColumn` interna por:

```kotlin
                verticalArrangement = Arrangement.spacedBy(PageGap),
```

Em `PdfPage` (linhas 316-344 originais), envolva o conteúdo num `Surface` com elevação de 1dp em vez de `Box(...background(Color.White))`:

```kotlin
@Composable
private fun PdfPage(index: Int, size: PageSize, widthPx: Int, renderPage: suspend (Int, Int) -> Bitmap?) {
    var image by remember(index) { mutableStateOf<PageImage>(PageImage.Loading) }
    LaunchedEffect(index, widthPx) {
        image = renderPage(index, widthPx)?.let { PageImage.Loaded(it.asImageBitmap()) } ?: PageImage.Failed
    }
    val aspect = (size.width / size.height).takeIf { it.isFinite() && it > 0f } ?: A4_ASPECT
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
        color = Color.White,
        shadowElevation = PageElevation,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val img = image) {
                is PageImage.Loaded -> Image(
                    img.image,
                    contentDescription = "Página ${index + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                PageImage.Failed -> Text(
                    PAGE_RENDER_FAILED_MESSAGE,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
                PageImage.Loading -> Unit
            }
        }
    }
}
```

- [ ] **Step 5: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests "com.johngabie.johnpdf.ui.reader.ReaderContentTest"`
Expected: PASS — todos os testes, incluindo `previous_and_next_buttons_are_56dp_with_at_least_8dp_gap`.

- [ ] **Step 6: Rodar a suíte completa**

Run: `./gradlew testDebugUnitTest`
Expected: PASS em tudo — este é o primeiro ponto desde a Task 8 em que o projeto volta a compilar e passar por completo.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt
git commit -m "feat(reader): setas Anterior/Próxima empilhadas 56dp, gap de página em surfaceContainerHigh (D1 §3.3, §5.2)"
```

---

### Task 10: Remover `BigButton.kt` e os aliases depreciados de `Theme.kt`

**Files:**
- Delete: `app/src/main/java/com/johngabie/johnpdf/ui/common/BigButton.kt`
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` (remove `MinTouchTarget` e `PageGapColor`)

**Interfaces:**
- Consumes: nada novo.
- Produces: nada novo — esta task só remove código morto. Nenhum arquivo de produção deve mais referenciar `BigButton`, `MinTouchTarget` ou `PageGapColor` depois dela.

- [ ] **Step 1: Confirmar que não há mais nenhuma referência (deve dar zero resultados)**

Run: `grep -rn "BigButton\|MinTouchTarget\|PageGapColor" app/src/main app/src/test`
Expected: nenhuma linha impressa (as Tasks 6, 7, 8 e 9 já migraram os três consumidores).

Se algo aparecer, pare aqui e volte à task correspondente — não prossiga com a remoção.

- [ ] **Step 2: Apagar `BigButton.kt`**

```bash
git rm app/src/main/java/com/johngabie/johnpdf/ui/common/BigButton.kt
```

- [ ] **Step 3: Remover os aliases depreciados de `Theme.kt`**

Remova as duas declarações:

```kotlin
@Deprecated("Sai na Task 10 do plano D1; use PrimaryTouchTarget (§3.1) ou os componentes de Buttons.kt.")
internal val MinTouchTarget = 64.dp
```

```kotlin
@Deprecated("Sai na Task 10 do plano D1; use MaterialTheme.colorScheme.surfaceContainerHigh.")
val PageGapColor = Color(0xFFBDBDBD)
```

Se `Color` (import `androidx.compose.ui.graphics.Color`) não for mais usado em nenhum outro lugar do arquivo, remova o import.

- [ ] **Step 4: Rodar a suíte completa de testes unitários**

Run: `./gradlew testDebugUnitTest`
Expected: PASS em 100% — nenhum teste referenciava esses símbolos diretamente (todos passavam por telas/diálogos, já migrados).

- [ ] **Step 5: Confirmar que o app compila e empacota**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Lembrete de verificação manual no aparelho (spec §8, não automatizável)**

Instale o APK gerado no Step 5 num aparelho físico e, com a escala de fonte do
sistema em 1.0, depois 1.3, depois 2.0 (Ajustes → Acessibilidade → Tamanho da
fonte), confira que a barra superior, a barra inferior e os rótulos de botão
não truncam nem quebram em duas linhas inesperadamente. É a mitigação direta
do risco "regressão de legibilidade ao descer de 20 para 16-18sp" (spec §8).
Isto não é um `- [ ]` de código — registre o resultado como comentário no PR
ou na task de review, não há comando de terminal que substitua essa checagem.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "chore: remove BigButton.kt e os aliases depreciados MinTouchTarget/PageGapColor (D1 §7)"
```

---

### Task 11: Atualizar os documentos de referência

**Files:**
- Modify: `docs/superpowers/specs/2026-09-28-johnpdf-leitor-android-design.md:100-102`
- Modify: `docs/superpowers/plans/2026-09-28-johnpdf-leitor-android.md:19`
- Modify: `docs/superpowers/plans/2026-09-28-johnpdf-leitor-android.md:2625`

**Interfaces:** nenhuma — task somente de documentação, sem código.

- [ ] **Step 1: Atualizar "Padrões visuais" na spec do leitor**

Em `docs/superpowers/specs/2026-09-28-johnpdf-leitor-android-design.md`, substitua as linhas 100-102:

```markdown
**Padrões visuais:** texto com no mínimo 20sp, alvos de toque com no mínimo
64dp, alto contraste (fundo claro, texto quase preto), ícone sempre acompanhado
de texto e nada escondido atrás de menus ⋮.
```

por:

```markdown
**Padrões visuais** (superseded por `docs/superpowers/specs/2026-09-29-design-d1-sistema-visual.md`):
texto com no mínimo 16sp e hierarquia de tamanho (`titleLarge > titleMedium ≥
bodyLarge > bodyMedium ≥ labelLarge`); alvos de toque com no mínimo 48dp
(piso M3) e 56dp nas ações frequentes; alto contraste (fundo claro, texto quase
preto); ícone sozinho só nos quatro casos de convenção universal (voltar,
travar rotação, limpar busca, ação com rótulo redundante ao lado) — qualquer
outra ação leva texto; nada escondido atrás de menus ⋮.
```

- [ ] **Step 2: Atualizar a linha de Global Constraints no plano original**

Em `docs/superpowers/plans/2026-09-28-johnpdf-leitor-android.md`, substitua a linha 19:

```markdown
- Texto com no mínimo **20sp**; alvos de toque com no mínimo **64dp** (`MinTouchTarget`); ícone sempre acompanhado de texto; nenhum menu ⋮.
```

por:

```markdown
- Texto com no mínimo **16sp**, com hierarquia decrescente (`titleLarge > titleMedium ≥ bodyLarge > bodyMedium ≥ labelLarge`); alvos de toque com no mínimo **48dp** (piso M3) e **56dp** nas ações frequentes (`PrimaryTouchTarget`, ver `docs/superpowers/specs/2026-09-29-design-d1-sistema-visual.md`); ícone sozinho só nos casos de convenção universal, com `contentDescription`; nenhum menu ⋮.
```

- [ ] **Step 3: Anotar a nota de layout da Task 11 (barra do leitor) como superada**

Em `docs/superpowers/plans/2026-09-28-johnpdf-leitor-android.md:2625`, logo após a linha que descreve o layout em duas linhas da barra inferior, adicione uma nota (não apague a linha original — ela documenta o que foi implementado naquele momento):

```markdown
> **Atualizado por D1 (2026-09-29):** o layout da barra inferior mudou para uma
> única linha (rótulo "X de N" + setas Anterior/Próxima empilhadas à direita,
> 56dp cada) e a rotação foi para a barra superior como `IconToggleButton`. Ver
> `docs/superpowers/specs/2026-09-29-design-d1-sistema-visual.md` §5.2.
```

- [ ] **Step 4: Não alterar `docs/e2e/2026-09-28-checklist.md`**

Este arquivo é um relatório de evidência de uma execução já concluída (com capturas de tela e logs reais do rótulo antigo, ex. "📂 Abrir", "⬇ Próxima"). Reescrever os rótulos ali para os novos nomes falsificaria o que foi de fato observado naquele teste. Não edite este arquivo nesta task — qualquer novo checklist de E2E para a versão com D1 aplicado deve ser um arquivo novo (`docs/e2e/<data>-checklist.md`), fora do escopo deste plano.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-09-28-johnpdf-leitor-android-design.md docs/superpowers/plans/2026-09-28-johnpdf-leitor-android.md
git commit -m "docs: atualiza regras de 20sp/64dp para 16sp/48-56dp após D1 (sistema visual)"
```

---

## Fora de escopo deste plano

- **Paleta de cores final** (spec §G5): `LightColors`/`DarkColors` na Task 3 usam valores provisórios; a estrutura já está completa, só os valores exatos ficam pendentes do estudo em coolors.co.
- **Fonte Atkinson Hyperlegible Next** (spec §2.2): `JohnFontFamily = FontFamily.Default` (Task 2) já é o único ponto de configuração — trocar por `FontFamily(Font(R.font.atkinson_...))` é um commit isolado de 1 linha + arquivos `.ttf` em `res/font/` + `NOTICE`, que não pode ser escrito aqui porque os binários da fonte não existem no repositório ainda.
- **Biblioteca de ícones própria (`JohnIcons`)** (spec §B): este plano usa `androidx.compose.material.icons.Icons` diretamente (com a dependência `material-icons-extended` adicionada na Task 4); uma camada de abstração própria sobre os ícones é assunto de uma spec separada.
- **Layout específico de cada tela além do que está no §5 da spec D1** (spec §D "Por tela" do documento-mãe): este plano cobre exatamente os componentes listados em "Arquivos afetados" (Theme, Buttons, Dialogs, HomeScreen, ReaderScreen); qualquer redesenho adicional de tela é consumidor deste trabalho, não parte dele.
