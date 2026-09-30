# Fonte Atkinson Hyperlegible Next — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir `FontFamily.Default` (fonte do fabricante) por Atkinson Hyperlegible Next (OFL 1.1, 4 pesos estáticos 400/500/600/700) em todos os 15 estilos do `Typography` do M3, embutida via `res/font/` — sem dependências novas, sem `INTERNET`, com delta de APK entre +100 KB e +250 KB por variante ABI, e sem quebra de layout em fontScale 1.0/1.3/2.0.

**Architecture:** Um arquivo novo `ui/theme/Font.kt` declara `internal val AtkinsonHyperlegibleNext: FontFamily` a partir de 4 `Font(R.font.x, FontWeight.Y)`. `Theme.kt` ganha um helper privado `Typography.withFontFamily(family)` que reescreve os 15 estilos do M3 (não só os 10 já customizados) e é aplicado por cima do `BigTypography` existente. Os 4 TTFs vão em `res/font/*.ttf` (nomes `snake_case`, exigência do Android), a licença completa em `res/raw/ofl_atkinson_hyperlegible_next.txt`, e um resumo em `NOTICE` na raiz do repo. Nenhum `build.gradle.kts` é tocado.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Android `res/font`, JUnit4 + Robolectric (`AndroidJUnit4`), `androidx.core:core` (`ResourcesCompat`, já no classpath transitivo — confirmado via `androidx.activity:activity-compose`), SIL Open Font License 1.1.

**Spec:** `docs/superpowers/specs/2026-09-29-fonte-atkinson-hyperlegible-next.md`

## Global Constraints

- `minSdk = 24` (`app/build.gradle.kts:22`) — fontes variáveis exigem API 26+; usar **somente** os TTFs estáticos de `fonts/ttf/`, nunca `fonts/variable/AtkinsonHyperlegibleNext[wght].ttf`.
- Os **4 pesos são obrigatórios**: 400 (Regular), 500 (Medium), 600 (SemiBold), 700 (Bold). Faltar o 500 não é sintetizado para 600 — o Compose degrada silenciosamente para 400.
- **Sem itálicos** — zero ocorrências de `FontStyle.Italic` em `app/src/main` (verificado na spec); não baixar nem referenciar os itálicos.
- Delta de APK: **entre +100 KB e +250 KB** por APK de variante ABI (arm64-v8a, armeabi-v7a, x86_64, universal), contra a baseline **18,9 MB** (arm64 release). Fora dessa faixa, investigar antes de prosseguir.
- **Sem `android.permission.INTERNET`** em runtime — os TTFs são baixados uma única vez na máquina de build e **commitados**; nada de Downloadable Fonts / GoogleFont provider.
- **Nenhum `build.gradle.kts` tocado, nenhuma dependência nova, nenhuma permissão nova.**
- **Nenhum tamanho e nenhum peso de texto muda nesta tarefa** — só a família. `fontSize`/`fontWeight` continuam exatamente como em `Theme.kt` hoje.
- **Não modificar os TTFs** (OFL §3/§5, Reserved Font Name) — apenas renomear o arquivo para `snake_case` (`a–z`, `0–9`, `_`, exigência de `res/`). Nada de reabrir em FontForge, subsetar, ou reexportar.
- Arquivos em `res/` só aceitam `a–z`, `0–9`, `_` no nome.
- `.gitignore` não pode bloquear `*.ttf` nem `res/font` — os binários precisam entrar no git (já confirmado limpo).

## Review Focus

- `labelMedium` (peso 500, usado nos rótulos da bottom nav e em "Página 4 de 200") deve renderizar com o peso **Medium real** do arquivo `atkinson_hyperlegible_next_medium.ttf` — não degradar silenciosamente para Regular por falta do arquivo ou weight errado em `Font()`.
- O nome do PDF na barra superior do leitor (`ReaderScreen`) não deve quebrar linha nem estourar o container em orientação paisagem — a Atkinson é mais larga que a fonte de sistema (risco R2 da spec).
- Os botões "Anterior"/"Próxima" (barra inferior do leitor) e os botões de diálogo não devem ganhar reticências (`…`) com a fonte mais larga.
- O rótulo "Todos os PDFs" (aba da bottom nav) deve continuar legível, sem corte, em fontScale 1.0.
- Em fontScale **2.0**, nenhuma tela chave (Home, Reader, diálogos de senha/erro) deve ter texto estourando o container ou sobrepondo outro elemento — cenário mais extremo que o 1.3 já testado no moto g41.

---

## Arquivos tocados (visão geral)

| Arquivo | Ação |
|---|---|
| `app/src/main/res/font/atkinson_hyperlegible_next_regular.ttf` | Criar (binário) |
| `app/src/main/res/font/atkinson_hyperlegible_next_medium.ttf` | Criar (binário) |
| `app/src/main/res/font/atkinson_hyperlegible_next_semibold.ttf` | Criar (binário) |
| `app/src/main/res/font/atkinson_hyperlegible_next_bold.ttf` | Criar (binário) |
| `app/src/main/res/raw/ofl_atkinson_hyperlegible_next.txt` | Criar |
| `NOTICE` (raiz) | Criar |
| `app/src/main/java/com/johngabie/johnpdf/ui/theme/Font.kt` | Criar |
| `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` | Modificar |
| `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt` | Modificar |
| `app/src/test/java/com/johngabie/johnpdf/ui/theme/FontResourceTest.kt` | Criar |
| `docs/superpowers/specs/2026-09-29-fonte-atkinson-hyperlegible-next.md` | Modificar (preencher tabela SHA-256 da §5) |
| `docs/e2e/2026-09-28-checklist.md` | Modificar (nota) |
| `docs/e2e/device/2026-09-29-moto-g41.md` | Modificar (nota + novas capturas) |
| `docs/superpowers/handoff/sdd-ledger.md` | Modificar (linha da tarefa) |

---

### Task 1: Baixar e verificar os 4 TTFs + OFL.txt

**Files:**
- Create: `app/src/main/res/font/atkinson_hyperlegible_next_regular.ttf`
- Create: `app/src/main/res/font/atkinson_hyperlegible_next_medium.ttf`
- Create: `app/src/main/res/font/atkinson_hyperlegible_next_semibold.ttf`
- Create: `app/src/main/res/font/atkinson_hyperlegible_next_bold.ttf`
- Create: `app/src/main/res/raw/ofl_atkinson_hyperlegible_next.txt`
- Modify: `docs/superpowers/specs/2026-09-29-fonte-atkinson-hyperlegible-next.md:152-157` (tabela de SHA-256)

**Interfaces:**
- Produces: os 4 recursos `R.font.atkinson_hyperlegible_next_{regular,medium,semibold,bold}` e `R.raw.ofl_atkinson_hyperlegible_next`, consumidos por `Font.kt` (Task 3) e `FontResourceTest.kt` (Task 6).

- [ ] **Step 1: Rodar o download fixado no commit do upstream**

Este download é feito uma única vez, na máquina de build. Rodar exatamente (PowerShell, a partir da raiz do repo):

```powershell
$sha  = '7925f50f649b3813257faf2f4c0b381011f434f1'
$base = "https://raw.githubusercontent.com/googlefonts/atkinson-hyperlegible-next/$sha"
$dest = 'app/src/main/res/font'
New-Item -ItemType Directory -Force $dest | Out-Null

$map = @{
  'Regular'  = 'atkinson_hyperlegible_next_regular.ttf'
  'Medium'   = 'atkinson_hyperlegible_next_medium.ttf'
  'SemiBold' = 'atkinson_hyperlegible_next_semibold.ttf'
  'Bold'     = 'atkinson_hyperlegible_next_bold.ttf'
}
foreach ($w in $map.Keys) {
  Invoke-WebRequest "$base/fonts/ttf/AtkinsonHyperlegibleNext-$w.ttf" -OutFile "$dest/$($map[$w])"
}
Invoke-WebRequest "$base/OFL.txt" -OutFile 'app/src/main/res/raw/ofl_atkinson_hyperlegible_next.txt'
```

- [ ] **Step 2: Verificar tamanhos em bytes**

```powershell
Get-ChildItem app/src/main/res/font -Filter *.ttf |
  Select-Object Name, Length | Sort-Object Name
```

Expected (exato, senão é ponteiro LFS/HTML de erro/arquivo truncado):

| Arquivo | Bytes |
|---|---|
| `atkinson_hyperlegible_next_regular.ttf` | 65068 |
| `atkinson_hyperlegible_next_medium.ttf` | 67080 |
| `atkinson_hyperlegible_next_semibold.ttf` | 66680 |
| `atkinson_hyperlegible_next_bold.ttf` | 66792 |

- [ ] **Step 3: Verificar assinatura sfnt (4 primeiros bytes = `00-01-00-00`)**

```powershell
Get-ChildItem app/src/main/res/font -Filter *.ttf | ForEach-Object {
  $b = [System.IO.File]::ReadAllBytes($_.FullName)[0..3]
  "$($_.Name): $([BitConverter]::ToString($b))"
}
```

Expected: as 4 linhas terminam em `00-01-00-00-00`. Se alguma não bater, o arquivo não é uma fonte válida — apagar e repetir o Step 1.

- [ ] **Step 4: Calcular SHA-256 e registrar na spec**

```powershell
Get-ChildItem app/src/main/res/font -Filter *.ttf | Get-FileHash -Algorithm SHA256 |
  Select-Object @{n='File';e={Split-Path $_.Path -Leaf}}, Hash
```

Abrir `docs/superpowers/specs/2026-09-29-fonte-atkinson-hyperlegible-next.md`, localizar a tabela vazia nas linhas 152-157 (§5, "Verificação obrigatória antes de commitar", item 3) e preencher a coluna **SHA-256** com os 4 valores obtidos. Não alterar mais nada nesse arquivo.

- [ ] **Step 5: Confirmar que `.gitignore` não bloqueia os binários**

```powershell
git check-ignore -v app/src/main/res/font/atkinson_hyperlegible_next_regular.ttf
```

Expected: saída vazia (nenhum padrão do `.gitignore` casa o arquivo). Se algo casar, ajustar o `.gitignore` antes de continuar.

- [ ] **Step 6: Conferir a árvore final**

```powershell
Get-ChildItem app/src/main/res/font, app/src/main/res/raw
```

Expected: 4 `.ttf` em `res/font/` e `ofl_atkinson_hyperlegible_next.txt` em `res/raw/` — nenhum outro arquivo novo.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/font/atkinson_hyperlegible_next_regular.ttf \
        app/src/main/res/font/atkinson_hyperlegible_next_medium.ttf \
        app/src/main/res/font/atkinson_hyperlegible_next_semibold.ttf \
        app/src/main/res/font/atkinson_hyperlegible_next_bold.ttf \
        app/src/main/res/raw/ofl_atkinson_hyperlegible_next.txt \
        docs/superpowers/specs/2026-09-29-fonte-atkinson-hyperlegible-next.md
git commit -m "chore(font): adiciona Atkinson Hyperlegible Next (OFL 1.1) em res/font"
```

---

### Task 2: Criar `NOTICE` na raiz do repositório

**Files:**
- Create: `NOTICE`

**Interfaces:**
- Produces: nenhuma interface de código — arquivo de atribuição de licença exigido pela OFL §2.

- [ ] **Step 1: Criar o arquivo com o conteúdo mínimo exigido pela OFL**

```
johnPDF inclui software de terceiros:

Atkinson Hyperlegible Next
Copyright 2020-2024 The Atkinson Hyperlegible Next Project Authors
(https://github.com/googlefonts/atkinson-hyperlegible-next)
Licenciado sob a SIL Open Font License, Version 1.1.
Texto integral: app/src/main/res/raw/ofl_atkinson_hyperlegible_next.txt
Arquivos: app/src/main/res/font/atkinson_hyperlegible_next_*.ttf (não modificados)
```

- [ ] **Step 2: Verificar que o arquivo existe e não tem extensão**

```powershell
Get-Item NOTICE | Select-Object Name, Length
```

Expected: `Name = NOTICE` (sem `.txt`), `Length` > 0.

- [ ] **Step 3: Commit**

```bash
git add NOTICE
git commit -m "chore(license): adiciona NOTICE com atribuição da OFL 1.1"
```

---

### Task 3: Criar `Font.kt` — declarar a `FontFamily` com os 4 pesos

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Font.kt`

**Interfaces:**
- Consumes: `R.font.atkinson_hyperlegible_next_{regular,medium,semibold,bold}` (Task 1).
- Produces: `internal val AtkinsonHyperlegibleNext: FontFamily` no pacote `com.johngabie.johnpdf.ui.theme`, consumido por `ThemeTest.kt` (Task 4) e `Theme.kt` (Task 5).

- [ ] **Step 1: Escrever o arquivo**

```kotlin
package com.johngabie.johnpdf.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.johngabie.johnpdf.R

/**
 * Atkinson Hyperlegible Next — desenhada pelo Braille Institute of America para
 * baixa visão (0/O, 1/l/I e rn/m desambiguados).
 *
 * Copyright 2020-2024 The Atkinson Hyperlegible Next Project Authors
 * (https://github.com/googlefonts/atkinson-hyperlegible-next)
 * SIL Open Font License 1.1 — texto integral em
 * res/raw/ofl_atkinson_hyperlegible_next.txt. Arquivos não modificados.
 *
 * Usamos as estáticas, não a variável: fontes variáveis exigem API 26 e o
 * minSdk do johnPDF é 24 — num Android 7 o eixo wght seria ignorado.
 *
 * Sem itálicos: nenhum estilo da UI usa FontStyle.Italic (economia de ~280 KB).
 */
internal val AtkinsonHyperlegibleNext = FontFamily(
    Font(R.font.atkinson_hyperlegible_next_regular, FontWeight.Normal),   // 400
    Font(R.font.atkinson_hyperlegible_next_medium, FontWeight.Medium),    // 500
    Font(R.font.atkinson_hyperlegible_next_semibold, FontWeight.SemiBold),// 600
    Font(R.font.atkinson_hyperlegible_next_bold, FontWeight.Bold),        // 700
)
```

- [ ] **Step 2: Compilar para confirmar que os `R.font.*` resolvem**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Se falhar com "unresolved reference: R.font...", os nomes de arquivo da Task 1 não batem com os usados aqui — conferir `app/src/main/res/font/`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/theme/Font.kt
git commit -m "feat(theme): declara FontFamily da Atkinson Hyperlegible Next com 4 pesos"
```

---

### Task 4: `ThemeTest.kt` — escrever o teste que cobre os 15 estilos (falha primeiro)

O teste atual (`app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt`) só cobre os 10 estilos customizados e não checa a família de fonte. Este task escreve a versão final do teste — que vai falhar até a Task 5 aplicar a fonte em `Theme.kt` — e só então o teste passa a orientar a implementação.

**Files:**
- Modify: `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt` (arquivo inteiro, 33 linhas atuais)

**Interfaces:**
- Consumes: `BigTypography` (existente em `Theme.kt`), `AtkinsonHyperlegibleNext` (Task 3).

- [ ] **Step 1: Substituir o conteúdo de `ThemeTest.kt`**

```kotlin
package com.johngabie.johnpdf.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeTest {

    /** Todos os 15 estilos do M3 — não só os customizados. */
    private val allStyles: Map<String, TextStyle> = mapOf(
        "displayLarge" to BigTypography.displayLarge,
        "displayMedium" to BigTypography.displayMedium,
        "displaySmall" to BigTypography.displaySmall,
        "headlineLarge" to BigTypography.headlineLarge,
        "headlineMedium" to BigTypography.headlineMedium,
        "headlineSmall" to BigTypography.headlineSmall,
        "titleLarge" to BigTypography.titleLarge,
        "titleMedium" to BigTypography.titleMedium,
        "titleSmall" to BigTypography.titleSmall,
        "bodyLarge" to BigTypography.bodyLarge,
        "bodyMedium" to BigTypography.bodyMedium,
        "bodySmall" to BigTypography.bodySmall,
        "labelLarge" to BigTypography.labelLarge,
        "labelMedium" to BigTypography.labelMedium,
        "labelSmall" to BigTypography.labelSmall,
    )

    /** Restrição do plano: nenhum texto de UI abaixo de 20sp. */
    @Test fun every_typography_style_is_at_least_20sp() {
        val minSp = 20f
        val customized = allStyles.filterKeys {
            it !in setOf("displayLarge", "displayMedium", "displaySmall",
                         "headlineLarge", "headlineSmall")
        }
        customized.forEach { (name, style) ->
            assertTrue(
                "$name deve ter no mínimo ${minSp}sp, tinha ${style.fontSize.value}sp",
                style.fontSize.value >= minSp,
            )
        }
    }

    /** A fonte é do app, não do fabricante — em TODOS os estilos, sem exceção. */
    @Test fun every_typography_style_uses_atkinson() {
        allStyles.forEach { (name, style) ->
            assertEquals(
                "$name deve usar Atkinson Hyperlegible Next, não a fonte do sistema",
                AtkinsonHyperlegibleNext,
                style.fontFamily,
            )
        }
    }
}
```

- [ ] **Step 2: Rodar e confirmar que `every_typography_style_uses_atkinson` falha**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ThemeTest"`
Expected: FAIL em `every_typography_style_uses_atkinson` — `Theme.kt` ainda não aplica `AtkinsonHyperlegibleNext`, então `style.fontFamily` é `null` (ou `FontFamily.Default` para os 5 estilos não customizados) em vez de `AtkinsonHyperlegibleNext`. `every_typography_style_is_at_least_20sp` continua passando (não depende da fonte).

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt
git commit -m "test(theme): cobre os 15 estilos do M3 e exige Atkinson Hyperlegible Next"
```

---

### Task 5: `Theme.kt` — helper `withFontFamily()` e aplicação em `BigTypography`

Faz o teste da Task 4 passar. Adiciona um `private fun Typography.withFontFamily(family: FontFamily)` que reescreve os 15 estilos do M3 — inclusive os 5 (`displayLarge/Medium/Small`, `headlineLarge/Small`) que hoje vêm do `Typography()` padrão e ficariam em `FontFamily.Default` se não fossem cobertos.

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` (arquivo inteiro, 47 linhas atuais)

**Interfaces:**
- Consumes: `AtkinsonHyperlegibleNext` (Task 3).
- Produces: `private fun Typography.withFontFamily(family: FontFamily): Typography` e `internal val BigTypography: Typography` (atualizado) — `BigTypography` já é consumido por `JohnPdfTheme` no mesmo arquivo e por `ThemeTest.kt` (Task 4).

- [ ] **Step 1: Substituir o conteúdo de `Theme.kt`**

```kotlin
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

val MinTouchTarget = 64.dp
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

/**
 * Aplica [family] aos 15 estilos do M3 — inclusive os que este arquivo não
 * customiza, que senão ficariam em FontFamily.Default e misturariam duas fontes.
 *
 * Ao subir o material3 para 1.4+, acrescentar aqui os estilos *Emphasized.
 */
private fun Typography.withFontFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

private val Base = Typography()

internal val BigTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodySmall = Base.bodySmall.copy(fontSize = 20.sp, lineHeight = 28.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontSize = 20.sp),
    labelSmall = Base.labelSmall.copy(fontSize = 20.sp),
).withFontFamily(AtkinsonHyperlegibleNext)

@Composable
fun JohnPdfTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = BigTypography, content = content)
}
```

- [ ] **Step 2: Rodar `ThemeTest` e confirmar que os dois testes passam**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ThemeTest"`
Expected: PASS — `every_typography_style_is_at_least_20sp` e `every_typography_style_uses_atkinson`, 2/2.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt
git commit -m "feat(theme): aplica Atkinson Hyperlegible Next em todos os estilos de tipografia"
```

---

### Task 6: `FontResourceTest.kt` — teste de que os 4 TTFs carregam de verdade

`ThemeTest` compara objetos `FontFamily` e não abre o arquivo — um TTF corrompido passaria. Este teste força o parse via `ResourcesCompat.getFont`.

**Files:**
- Create: `app/src/test/java/com/johngabie/johnpdf/ui/theme/FontResourceTest.kt`

**Interfaces:**
- Consumes: `R.font.atkinson_hyperlegible_next_{regular,medium,semibold,bold}` (Task 1).

- [ ] **Step 1: Escrever o teste**

```kotlin
package com.johngabie.johnpdf.ui.theme

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.R
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FontResourceTest {

    /**
     * Prova que os 4 recursos existem e são legíveis pelo Android — não que os
     * glifos estão corretos (Robolectric pode não fazer parse real do TTF). A
     * prova visual vem das capturas de tela nas Tasks 9-10.
     */
    @Test fun all_four_weights_load_as_real_typefaces() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        listOf(
            R.font.atkinson_hyperlegible_next_regular,
            R.font.atkinson_hyperlegible_next_medium,
            R.font.atkinson_hyperlegible_next_semibold,
            R.font.atkinson_hyperlegible_next_bold,
        ).forEach { fontRes ->
            assertNotNull(
                "R.font.$fontRes deveria carregar como Typeface",
                ResourcesCompat.getFont(ctx, fontRes),
            )
        }
    }
}
```

- [ ] **Step 2: Rodar e confirmar que passa**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.FontResourceTest"`
Expected: PASS, 1/1. Se falhar com `Resources$NotFoundException`, os arquivos da Task 1 não estão no lugar certo ou o nome não bate com o `R.font.*` referenciado.

Se este teste quebrar `HomeContentTest` ou `ReaderContentTest` (que renderizam de verdade sob Robolectric), isso é sinal de problema real no carregamento da fonte — investigar, não silenciar (R4 da spec).

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/johngabie/johnpdf/ui/theme/FontResourceTest.kt
git commit -m "test(theme): verifica que os 4 TTFs da Atkinson carregam como Typeface"
```

---

### Task 7: Verificação — suíte JVM completa sem regressão

**Files:** nenhum arquivo novo — checkpoint de verificação (spec §9, passo 1).

**Interfaces:** nenhuma.

- [ ] **Step 1: Rodar a suíte JVM inteira**

Run: `./gradlew testDebugUnitTest`
Expected: **BUILD SUCCESSFUL**, contagem de testes = baseline anterior (94, conforme `sdd-ledger.md`) **+ 2** (Task 4: 2 testes em `ThemeTest`, sendo 1 já existente e 1 novo — na prática o total sobe pelos testes novos de `ThemeTest.every_typography_style_uses_atkinson` e `FontResourceTest.all_four_weights_load_as_real_typefaces`, ou seja **+2** sobre a baseline). Zero FAIL, zero regressão em `HomeContentTest`, `ReaderContentTest`, `DialogsTest`, `MainActivitySmokeTest`.

- [ ] **Step 2: Se algo além de `ThemeTest`/`FontResourceTest` quebrar, tratar como achado (R2/R3)**

Casar por texto/`contentDescription` que falhar após a troca de fonte é sinal de largura de texto mudando comportamento — não ignorar, documentar no `sdd-ledger.md` (Task 11) e decidir se cabe ajuste de `lineHeight` (ver Task 10, risco R3) antes de prosseguir.

- [ ] **Step 3: Nenhum commit neste task** (é um checkpoint; nada muda no código).

---

### Task 8: `assembleRelease` — medir tamanho do APK, permissões e conteúdo

**Files:** nenhum arquivo de código — verificação de build (spec §9, passo 3).

**Interfaces:** nenhuma.

- [ ] **Step 1: Gerar o release**

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL, gera `app/build/outputs/apk/release/app-arm64-v8a-release.apk` (e os splits `armeabi-v7a`, `x86_64`, universal).

- [ ] **Step 2: Medir o delta de tamanho contra a baseline**

```powershell
(Get-Item app/build/outputs/apk/release/app-arm64-v8a-release.apk).Length / 1MB
```

Expected: baseline conhecida é **18,9 MB**. O novo tamanho deve ficar entre **19,0 MB e 19,15 MB** (delta de +100 KB a +250 KB). Fora dessa faixa: investigar antes de seguir — provável causa é a fonte variável ter entrado junto, ou itálicos.

- [ ] **Step 3: Confirmar ausência de `INTERNET`**

```powershell
aapt dump permissions app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

Expected: a lista de permissões **não** contém `android.permission.INTERNET`.

- [ ] **Step 4: Confirmar exatamente 4 TTFs, nenhum itálico, nenhuma variável**

```powershell
# Requer unzip (Git Bash) ou equivalente; alternativa: Expand-Archive + Get-ChildItem
unzip -l app/build/outputs/apk/release/app-arm64-v8a-release.apk | grep font
```

Expected: exatamente 4 linhas terminando em `.ttf`, todas com `atkinson_hyperlegible_next_` no nome (regular/medium/semibold/bold) — nenhum `italic`, nenhum `wght`, nenhum arquivo extra.

- [ ] **Step 5: Nenhum commit neste task** (é um checkpoint de build; nenhum arquivo versionado muda). Se o delta ou o conteúdo do zip estiverem fora do esperado, voltar à Task 1 ou 3 antes de prosseguir para os testes visuais.

---

### Task 9: Capturas no aparelho — fontScale 1.0 (baseline pós-fonte)

Repete o protocolo já usado em `docs/e2e/device/2026-09-29-moto-g41.md` (mesmo aparelho — moto g41, Android 12 — que hoje renderiza com a fonte da Motorola), agora com o release gerado na Task 8, para provar visualmente que a fonte mudou.

**Files:** nenhum arquivo de código. Produz screenshots em `docs/e2e/device/img/` (ou pasta equivalente nova, ver Step 5).

**Interfaces:** nenhuma.

- [ ] **Step 1: Instalar o release no aparelho**

```bash
adb install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

Expected: `Success`, sem avisos.

- [ ] **Step 2: Confirmar `font_scale` do sistema em 1.0**

```bash
adb shell settings get system font_scale
```

Expected: `1.0` ou `null` (default). Se estiver diferente, restaurar: `adb shell settings put system font_scale 1.0`.

- [ ] **Step 3: Capturar as telas-chave do Review Focus**

Abrir o app e navegar manualmente (ou via `adb shell input tap/swipe`, como no protocolo do moto-g41) até:
1. Home com a bottom nav visível (rótulo "Todos os PDFs").
2. Leitor em `long.pdf` navegado até "Página 4 de 200" (peso Medium do `labelMedium`).
3. Leitor com um PDF de nome longo, em **paisagem**, mostrando a barra superior.
4. Barra inferior do leitor com os botões "Anterior"/"Próxima".

Para cada uma, `adb shell screencap -p /sdcard/atkinson-1.0-NN.png` seguido de `adb pull /sdcard/atkinson-1.0-NN.png docs/e2e/device/img/`.

- [ ] **Step 4: Conferir os 5 itens do Review Focus contra as capturas**

Verificar manualmente, para cada captura do Step 3:
- "Página 4 de 200" tem o peso Medium visivelmente mais forte que o Regular ao redor (não parece Regular).
- Nome do PDF na paisagem não quebra linha nem corta.
- "Anterior"/"Próxima" sem reticências.
- "Todos os PDFs" legível, sem corte.

Se qualquer item falhar, registrar como achado (não corrigir silenciosamente) — decidir se é caso do risco R2 (texto mais largo) da spec e se cabe um ajuste de layout fora do escopo desta tarefa.

- [ ] **Step 5: Sem commit de código.** As imagens entram no commit de documentação da Task 11.

---

### Task 10: Capturas no aparelho — fontScale 1.3 e 2.0

Mesma mecânica da Task 9, para os dois níveis de escala do D1 §E linha 128. O nível 2.0 é o teste mais extremo do Review Focus e não foi coberto na sessão anterior do moto g41 (que só testou 1.3).

**Files:** nenhum arquivo de código. Produz screenshots em `docs/e2e/device/img/`.

**Interfaces:** nenhuma.

- [ ] **Step 1: Setar `font_scale` para 1.3**

```bash
adb shell settings put system font_scale 1.3
```

- [ ] **Step 2: Repetir as 4 capturas da Task 9 Step 3, com sufixo `-1.3`**

Focar especialmente no risco R3 (entrelinha): olhar `bodyLarge` (20sp / 28sp de `lineHeight`) em estados vazios e mensagens de erro/diálogos — ascendentes/descendentes mais altos da Atkinson podem deixar a linha apertada.

- [ ] **Step 3: Setar `font_scale` para 2.0**

```bash
adb shell settings put system font_scale 2.0
```

- [ ] **Step 4: Repetir as 4 capturas, com sufixo `-2.0`, e cobrir também os diálogos**

Além das 4 telas da Task 9, capturar:
5. Diálogo de senha incorreta ("Senha incorreta, tente de novo").
6. Diálogo de arquivo corrompido ("Não foi possível abrir este arquivo.").

Verificar o item 5 do Review Focus: nenhuma dessas 6 capturas deve ter texto estourando o container ou sobrepondo outro elemento em 2.0 — esta é a escala mais extrema testada até agora (a sessão anterior do moto g41 só cobriu 1.3).

- [ ] **Step 5: Restaurar `font_scale` para 1.0**

```bash
adb shell settings put system font_scale 1.0
adb shell settings get system font_scale
```

Expected: `1.0` — obrigatório antes de encerrar a sessão no aparelho (protocolo de `docs/e2e/device/2026-09-29-moto-g41.md`).

- [ ] **Step 6: Sem commit de código.** As imagens entram no commit de documentação da Task 11.

---

### Task 11: Atualizar documentação e ledger, commit final

**Files:**
- Modify: `docs/e2e/2026-09-28-checklist.md`
- Modify: `docs/e2e/device/2026-09-29-moto-g41.md`
- Modify: `docs/superpowers/handoff/sdd-ledger.md`

**Interfaces:** nenhuma.

- [ ] **Step 1: Anotar em `docs/e2e/2026-09-28-checklist.md`**

Adicionar uma nota (seção ou linha, seguindo o formato existente do arquivo) registrando que as capturas relevantes foram refeitas com a Atkinson Hyperlegible Next em `<data de hoje>`, com link para a nova seção em `docs/e2e/device/2026-09-29-moto-g41.md`.

- [ ] **Step 2: Anotar em `docs/e2e/device/2026-09-29-moto-g41.md`**

Adicionar uma seção nova (após a tabela de cenários existente, seguindo o mesmo estilo de `## Problemas visuais em font_scale 1.3`) descrevendo:
- Que a fonte mudou de "fonte da Motorola" para Atkinson Hyperlegible Next embutida.
- Os resultados da Task 9 (fontScale 1.0) e Task 10 (1.3 e 2.0), cenário por cenário do Review Focus, com os caminhos das novas imagens em `img/`.
- Qualquer achado do Step 4 da Task 9 ou do Step 2/4 da Task 10 que não tenha sido corrigido nesta tarefa (registrar como risco aberto, não esconder).

- [ ] **Step 3: Adicionar linha ao `docs/superpowers/handoff/sdd-ledger.md`**

Seguindo o formato das linhas existentes (`Task N: ...`), adicionar algo como:

```
Fonte Atkinson Hyperlegible Next: implementada (Font.kt, Theme.kt com withFontFamily(), NOTICE, 4 TTFs em res/font); JVM 96+/96+ (baseline + ThemeTest/FontResourceTest novos); assembleRelease delta dentro de +100-250 KB; capturas 1.0/1.3/2.0 refeitas no moto g41
```

Ajustar os números exatos (contagem de testes, delta medido) para os valores reais obtidos nas Tasks 7 e 8.

- [ ] **Step 4: Commit final**

```bash
git add docs/e2e/2026-09-28-checklist.md \
        docs/e2e/device/2026-09-29-moto-g41.md \
        docs/e2e/device/img/ \
        docs/superpowers/handoff/sdd-ledger.md
git commit -m "docs(e2e): recaptura as telas com Atkinson Hyperlegible Next em 1.0/1.3/2.0"
```

---

## Self-Review

**1. Cobertura da spec:** §1 (motivação) e §2 (decisões técnicas: estáticas não variável, 4 pesos, sem itálico, `res/font` não `assets/`) → refletidas nas Global Constraints e na Task 1/3. §3 (orçamento de tamanho) → Global Constraints + Task 8. §4 (OFL) → Task 1 (`res/raw`) + Task 2 (`NOTICE`). §5 (download) → Task 1. §6 (`Font.kt`) → Task 3. §7 (`Theme.kt`/helper) → Tasks 4-5. §8 (testes) → Tasks 4, 6. §9 (verificação) → Tasks 7, 8, 9, 10, 11 (passo 6 da spec). §10 (riscos R1-R6) → R1 coberto por Task 1 Steps 2-4 e Task 6; R2/R3 cobertos pelo Review Focus e Tasks 9-10; R4 coberto pela nota na Task 6 Step 2; R5 coberto pela Task 8 Step 4; R6 é só documentado (reversão trivial, sem task própria — não é trabalho a fazer, é uma opção futura). §11 (ordem de commits) → seguida nas Tasks 1, 3-6, 11, com granularidade um pouco maior (11 tasks vs. 4 commits sugeridos, mas os commits de código coincidem). §12 (dependência com D1) → não gera task; é uma nota para o futuro, já registrada na spec.

**2. Placeholders:** nenhum "TBD"/"implementar depois" — todo código é literal, todos os comandos são exatos, cada critério de aceite tem número.

**3. Consistência de tipos:** `AtkinsonHyperlegibleNext: FontFamily` (Task 3) é o mesmo símbolo usado em `Theme.kt` (Task 5) e `ThemeTest.kt` (Task 4). `Typography.withFontFamily(family: FontFamily): Typography` (Task 5) bate com a chamada `.withFontFamily(AtkinsonHyperlegibleNext)` no mesmo arquivo. Os 4 `R.font.*` (Task 1) são referenciados de forma idêntica em `Font.kt` (Task 3) e `FontResourceTest.kt` (Task 6).

**4. Review Focus:** os 5 itens (labelMedium 500, nome do PDF em paisagem, botões sem reticências, "Todos os PDFs" legível, fontScale 2.0 sem estouro) têm verificação explícita na Task 9 Step 4 (itens 1-4, fontScale 1.0) e Task 10 Step 4 (item 5, fontScale 2.0, incluindo diálogos). Nenhum ficou só na lista sem task correspondente.
