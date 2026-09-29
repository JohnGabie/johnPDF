# Spec — Adotar a fonte Atkinson Hyperlegible Next

- **Data:** 2026-09-29 · **Tipo:** spec de implementação
- **Origem:** `docs/superpowers/specs/2026-09-29-design-d1-proposta.md` §E (linhas 120–128), item da linha 125 — *"Fonte própria: Atkinson Hyperlegible Next (OFL, feita para baixa visão, visual moderno) | Baixo (~1h, +~150 KB, sem INTERNET — TTF no `res/font`)"*.
- **Decisão do usuário:** §G, pergunta 6 → **"Atkinson Hyperlegible Next"** (linhas 158–159). Opcional aprovado; sai da lista de ideias e vira tarefa.
- **Arquivos tocados:** `app/src/main/res/font/*` (novo), `app/src/main/res/raw/ofl_atkinson_hyperlegible_next.txt` (novo), `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt`, `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTest.kt`, `NOTICE` (novo, raiz).
- **Não toca:** nenhum `build.gradle.kts`, nenhuma dependência nova, nenhuma permissão nova.

---

## 1. Por que esta fonte

| Motivo | Detalhe |
|---|---|
| Feita para baixa visão | Projetada pelo Braille Institute of America; formas de letra desambiguadas — `0`/`O`, `1`/`l`/`I`, `5`/`S`, `rn`/`m` são distinguíveis. Interessa direto ao público-alvo do johnPDF (§1 da spec v1) e ao rótulo **"Página 4 de 200"**, que é numérico. |
| Visual moderno | Grotesca humanista de 2024/2025, não parece "fonte de acessibilidade". Atende o pedido do refresh: moderno para o público amplo sem perder a facilidade para idosos. |
| Consistência entre aparelhos | Hoje a UI usa `FontFamily.Default` → a fonte do fabricante. Nas capturas em `docs/e2e/device/` o moto g41 renderiza com a fonte da Motorola, diferente do emulador. Embutir o TTF elimina essa variação. |
| Licença permissiva | SIL Open Font License 1.1 — pode ser embutida e redistribuída num APK. |
| Sem rede | O TTF vai no APK. **Nada de Downloadable Fonts** (`GoogleFont` provider), que exigiria Play Services e tráfego de rede. |

---

## 2. Decisões técnicas (e o porquê)

### 2.1 Estáticas, não a variável

O repositório oferece `fonts/variable/AtkinsonHyperlegibleNext[wght].ttf`. **Não usar.** Fontes variáveis no Android só são suportadas a partir da **API 26**, e o `minSdk` do johnPDF é **24** (`app/build.gradle.kts`). Num Android 7 o eixo `wght` seria ignorado e todos os pesos sairiam iguais. Usamos as **estáticas** de `fonts/ttf/`.

### 2.2 Quais pesos embutir

Pesos realmente usados na UI:

| Peso | Onde | Hoje (`Theme.kt`) | Depois do D1 §D |
|---|---|---|---|
| 400 Normal | `bodyLarge/Medium/Small`, `labelSmall` | sim | sim |
| 500 Medium | `labelMedium` | não | **sim** (rótulos da bottom nav, "Página 4 de 200") |
| 600 SemiBold | `titleLarge/Medium/Small`, `labelLarge` | sim | sim |
| 700 Bold | `headlineMedium` | sim | não (D1 desce para SemiBold) |

**Embutir os 4** (400/500/600/700). Motivo: a spec vale com ou sem o D1 aprovado, e um peso faltando **não** é sintetizado de forma confiável. O Compose segue o algoritmo de casamento do CSS: pedir 500 sem ter 500 resolve para **400** (não 600), e o negrito sintético só entra quando o peso pedido é ≥ 600 e o arquivo casado está abaixo disso. Ou seja, faltar o Medium degrada silenciosamente o rótulo para Regular.

**Sem itálicos.** Nenhum estilo da UI usa `FontStyle.Italic` (verificado: zero ocorrências em `app/src/main`). Os 7 itálicos custariam ~+280 KB. Se algum dia surgir um itálico, o Compose aplica oblíquo sintético — aceitável.

### 2.3 `res/font` e não `assets/`

`res/font` gera IDs `R.font.*`, é a via idiomática do Compose (`Font(R.font.x, FontWeight.X)`) e funciona nos testes Robolectric porque o módulo já tem `testOptions { unitTests.isIncludeAndroidResources = true }`.

**Regra de nome:** arquivos em `res/` só aceitam `a–z`, `0–9` e `_`. Os nomes originais em CamelCase **precisam** ser renomeados.

---

## 3. Tamanho — orçamento e medição

Tamanhos exatos no repositório upstream (`fonts/ttf/`):

| Arquivo upstream | Bytes | Embutir? |
|---|---|---|
| `AtkinsonHyperlegibleNext-Regular.ttf` | 65.068 | ✅ |
| `AtkinsonHyperlegibleNext-Medium.ttf` | 67.080 | ✅ |
| `AtkinsonHyperlegibleNext-SemiBold.ttf` | 66.680 | ✅ |
| `AtkinsonHyperlegibleNext-Bold.ttf` | 66.792 | ✅ |
| **Total em disco** | **265.620 B ≈ 259 KiB** | |
| `OFL.txt` (em `res/raw`) | ~4 KB | ✅ |
| Os 7 itálicos + Light/ExtraLight/ExtraBold | ~+560 KB | ❌ |

**Impacto no APK: estimado +~150 KB** — alinhado com o número do D1 §E. O aapt2 **comprime** (deflate) arquivos `.ttf` em `res/`, pois `ttf` não está na lista de `noCompress` padrão; TTFs deflatam para ~55–60% do original → `265 KB × ~0,57 ≈ 151 KB`.

> **Estimativa, não medida.** A verificação da §8 exige medir de verdade contra a linha de base conhecida: **APK release arm64 = 18,9 MB** (D1 §B). Um delta acima de **+250 KB** significa que algo saiu errado (itálicos entraram, ou a variável foi copiada junto).

> **Atenção aos splits por ABI:** o build gera `arm64-v8a`, `armeabi-v7a`, `x86_64` e o universal. A fonte não é específica de ABI → os ~150 KB entram em **cada um dos 4 APKs**.

Se depois for preciso apertar o tamanho, a redução óbvia é cair para 3 pesos (400/500/600, ≈199 KB em disco) **junto com** a mudança do D1 que tira o Bold do `headlineMedium` — não antes.

---

## 4. Licença (OFL 1.1)

- **Licença:** SIL Open Font License, Version 1.1.
- **Aviso de copyright (verbatim do `OFL.txt` upstream):**
  `Copyright 2020-2024 The Atkinson Hyperlegible Next Project Authors (https://github.com/googlefonts/atkinson-hyperlegible-next)`

**Obrigações que a OFL impõe e como cumprimos:**

| Cláusula | O que exige | Como cumprimos |
|---|---|---|
| §2 | *"cada cópia deve conter o aviso de copyright acima e esta licença"* | Copiar `OFL.txt` para **`app/src/main/res/raw/ofl_atkinson_hyperlegible_next.txt`** — assim a licença viaja **dentro de cada APK distribuído**, não só no repositório. Mais um arquivo `NOTICE` na raiz para quem lê o código. |
| §2 | Não vender a fonte isoladamente | Não se aplica: o app é gratuito e a fonte é um componente. |
| §3/§5 | *Reserved Font Name* — não usar o nome em versões modificadas | **Não modificamos os TTFs.** Renomear o **arquivo** para `snake_case` (exigência do `res/`) não é modificar o Font Software nem o nome interno da fonte — a restrição vale para o nome tipográfico embutido, que fica intacto. Não abrir/reexportar no FontForge, não subsetar. |

**Compatibilidade com a AGPL-3.0 do johnPDF:** sem conflito. A OFL 1.1 é permissiva e a fonte é uma obra agregada, não derivada do código do app; a AGPL não se propaga para ela nem a OFL para o app. Ambas convivem no mesmo APK, cada uma com seu texto de licença.

**`NOTICE` na raiz — conteúdo mínimo:**

```
johnPDF inclui software de terceiros:

Atkinson Hyperlegible Next
Copyright 2020-2024 The Atkinson Hyperlegible Next Project Authors
(https://github.com/googlefonts/atkinson-hyperlegible-next)
Licenciado sob a SIL Open Font License, Version 1.1.
Texto integral: app/src/main/res/raw/ofl_atkinson_hyperlegible_next.txt
Arquivos: app/src/main/res/font/atkinson_hyperlegible_next_*.ttf (não modificados)
```

> Quando o `ui/icons/JohnIcons.kt` do D1 §B for implementado, o `NOTICE` ganha a entrada Apache 2.0 dos Material Symbols no mesmo arquivo.

---

## 5. Passo 1 — baixar os TTF

Download feito **uma vez, na máquina de build**. O app continua **sem `INTERNET`** no `AndroidManifest.xml` — nada é baixado em runtime. Os TTFs são **commitados** no repositório (o build tem de funcionar offline).

**Fonte:** `github.com/googlefonts/atkinson-hyperlegible-next`, pasta `fonts/ttf/`.
**Pin:** o repositório **não tem tags**. Fixar pelo commit **`7925f50f649b3813257faf2f4c0b381011f434f1`** (main em 2025-02-21) em vez de `main`, para que o download seja reproduzível.

### PowerShell (ambiente do usuário)

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

Get-ChildItem $dest -Filter *.ttf | Get-FileHash -Algorithm SHA256 |
  Select-Object @{n='File';e={Split-Path $_.Path -Leaf}}, Hash, `
                @{n='Bytes';e={(Get-Item $_.Path).Length}}
```

### Verificação obrigatória antes de commitar

1. **Tamanhos batem** com a tabela da §3 (65.068 / 67.080 / 66.680 / 66.792). Um arquivo de ~130 bytes é um ponteiro LFS ou uma página de erro HTML, não uma fonte.
2. **Assinatura sfnt:** os 4 primeiros bytes têm de ser `00 01 00 00`.
   ```powershell
   Get-ChildItem app/src/main/res/font -Filter *.ttf | ForEach-Object {
     $b = [System.IO.File]::ReadAllBytes($_.FullName)[0..3]
     "$($_.Name): $([BitConverter]::ToString($b))"   # esperado 00-01-00-00
   }
   ```
3. **Registrar os SHA-256** obtidos na tabela abaixo, dentro desta spec, no commit da implementação:

   | Arquivo | SHA-256 | Bytes |
   |---|---|---|
   | `atkinson_hyperlegible_next_regular.ttf` | _(preencher)_ | 65.068 |
   | `atkinson_hyperlegible_next_medium.ttf` | _(preencher)_ | 67.080 |
   | `atkinson_hyperlegible_next_semibold.ttf` | _(preencher)_ | 66.680 |
   | `atkinson_hyperlegible_next_bold.ttf` | _(preencher)_ | 66.792 |

4. **`.gitignore`:** conferir que nada ignora `*.ttf` ou `res/font` — os binários **precisam** entrar no git.

### Estado final da árvore

```
app/src/main/res/
├── drawable/ic_launcher.xml
├── font/
│   ├── atkinson_hyperlegible_next_regular.ttf    (400)
│   ├── atkinson_hyperlegible_next_medium.ttf     (500)
│   ├── atkinson_hyperlegible_next_semibold.ttf   (600)
│   └── atkinson_hyperlegible_next_bold.ttf       (700)
└── raw/
    └── ofl_atkinson_hyperlegible_next.txt
NOTICE
```

---

## 6. Passo 2 — declarar a família no Compose

Arquivo novo: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Font.kt`

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

Notas sobre a API:
- `Font(resId, weight)` devolve um `ResourceFont`. **Carregamento é preguiçoso** — o TTF só é lido na primeira composição que usa o estilo, então declarar a família como `val` de topo não custa nada no start-up.
- Declarar o peso correto em cada `Font()` é o que faz o casamento funcionar. Se os 4 fossem declarados como `FontWeight.Normal`, todo texto sairia em Regular.
- Não usar a sobrecarga com `variationSettings` — ela é API 26+.

---

## 7. Passo 3 — aplicar em **todos** os estilos de texto (`Theme.kt`)

### O problema que o helper resolve

O `Theme.kt` atual customiza **10** dos **15** estilos do `Typography` do M3. Os 5 restantes (`displayLarge`, `displayMedium`, `displaySmall`, `headlineLarge`, `headlineSmall`) vêm do `Typography()` padrão e continuariam em `FontFamily.Default`. Hoje eles não aparecem na tela, mas componentes do M3 podem puxá-los e o resultado seria **duas fontes misturadas** — exatamente o tipo de inconsistência que o D1 está corrigindo.

Solução: um `Typography.withFontFamily()` aplicado **depois** das customizações, cobrindo os 15 de uma vez. Assim qualquer estilo novo já nasce com a fonte certa.

> **Se o `material3` for atualizado para 1.4+:** a classe `Typography` ganha os estilos `*Emphasized`. O helper precisa ser estendido para incluí-los, senão eles voltam a `FontFamily.Default`. Deixar o comentário no código.

### `Theme.kt` — versão final

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

**Só isso basta para a UI inteira.** Verificado: não existe nenhum `fontFamily` ou `TextStyle` literal em `app/src/main` — todo texto herda de `MaterialTheme.typography`. Os três `Text` com emoji (`HomeScreen.kt:173`, `:179`, `:279`) passam a herdar a família também; emoji não existe no Atkinson, então o Android cai no fallback de emoji do sistema e eles continuam iguais. (O D1 §B troca esses três por ícones de qualquer forma.)

### Escopo — o que **não** muda

- **Nenhum tamanho e nenhum peso** muda nesta tarefa. Só a família. A mudança de 20sp → 16/18sp é do D1 §D e é uma tarefa separada, com risco próprio.
- **O conteúdo do PDF não muda.** As páginas são rasterizadas pelo MuPDF com as fontes embutidas no próprio documento; a Atkinson só afeta o *chrome* do app.
- **O `android:theme` do manifest** (`@android:style/Theme.Material.Light.NoActionBar`) continua com a fonte do sistema. Ele só cobre a janela antes do Compose montar, onde não há texto.

---

## 8. Passo 4 — testes

### 8.1 `ThemeTest.kt` — novo teste de cobertura da família

O teste existente cobre só os 10 estilos customizados. O novo tem de cobrir os **15**, senão não pega justamente o bug que o helper previne.

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

> Manter a lista de exclusão do teste de 20sp igual à lista de estilos não customizados. Os 5 estilos de display/headline grandes já vêm bem acima de 20sp no padrão do M3; se a exclusão incomodar, o teste pode simplesmente rodar sobre os 15 — mas aí ele passa a depender dos defaults do M3, que mudam entre versões.

### 8.2 Teste de que o TTF realmente carrega (recomendado)

O teste acima compara objetos e **não abre o arquivo** — um TTF corrompido passaria. Um teste que força o parse:

```kotlin
// app/src/test/java/com/johngabie/johnpdf/ui/theme/FontResourceTest.kt
@RunWith(AndroidJUnit4::class)
class FontResourceTest {
    @Test fun all_four_weights_load_as_real_typefaces() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        listOf(
            R.font.atkinson_hyperlegible_next_regular,
            R.font.atkinson_hyperlegible_next_medium,
            R.font.atkinson_hyperlegible_next_semibold,
            R.font.atkinson_hyperlegible_next_bold,
        ).forEach { assertNotNull(ResourcesCompat.getFont(ctx, it)) }
    }
}
```

> **Ressalva Robolectric:** o shadow de `Typeface` pode não fazer o parse real do TTF e devolver um typeface padrão sem falhar — ou seja, este teste prova que o **recurso existe e é legível**, não que os glifos estão corretos. A prova visual real vem da §9.3. Se o teste quebrar o suíte de UI (`HomeContentTest`/`ReaderContentTest` renderizam de verdade sob Robolectric), isso é sinal de problema no carregamento da fonte e deve ser investigado, não silenciado.

### 8.3 Testes existentes

Não deve ser preciso mudar nada em `HomeContentTest`, `ReaderContentTest`, `DialogsTest` ou `MainActivitySmokeTest` — todos casam por texto e `contentDescription`, não por métrica. **Se algum quebrar, é achado, não ruído:** quase certamente largura de texto (ver risco R2).

---

## 9. Passo 5 — verificação

Ordem, com o critério de aceite de cada passo:

1. **Suíte JVM:** `./gradlew testDebugUnitTest` → **96/96** (os 94 atuais + os 2 novos). Zero regressão.
2. **Suíte instrumentada:** `./gradlew connectedDebugAndroidTest` → **13/13**. (Toca MuPDF, não a fonte; roda para confirmar que nada foi arrastado junto.)
3. **Tamanho e permissões do release:**
   ```
   ./gradlew assembleRelease
   ```
   - `app/build/outputs/apk/release/app-arm64-v8a-release.apk` — comparar com a linha de base **18,9 MB**. Aceite: delta entre **+100 KB e +250 KB**. Fora disso, investigar antes de seguir.
   - `aapt dump permissions <apk>` → confirmar que continua **sem `android.permission.INTERNET`** (invariante do plano v1, linha 20).
   - `unzip -l <apk> | grep font` → exatamente **4** TTFs, nenhum itálico, nenhuma variável.
4. **Prova visual no aparelho** (moto g41, Android 12 — o aparelho que mostrava a fonte da Motorola): instalar e **refazer as capturas** de `docs/e2e/device/img/`, comparando lado a lado. É aqui que se confirma que a fonte mudou de verdade. Conferir em especial:
   - **"Página 4 de 200"** — os dígitos são o ganho principal;
   - **nome do PDF** no top bar do leitor (reticências);
   - **rótulos da bottom nav** ("Todos os PDFs");
   - botões "Anterior"/"Próxima" na barra inferior.
5. **Escala de fonte 1.3 e 2.0** (D1 §E, linha 128) — repetir o passo 4 com a escala do sistema aumentada. Restaurar `font_scale` ao final, como no protocolo de `docs/e2e/device/2026-09-29-moto-g41.md`.
6. **Atualizar a documentação:** `docs/e2e/2026-09-28-checklist.md` e `docs/e2e/device/2026-09-29-moto-g41.md` ganham a nota de que as capturas foram refeitas com a nova fonte; `docs/superpowers/handoff/sdd-ledger.md` ganha a linha da tarefa.

---

## 10. Riscos

| # | Risco | Probabilidade | Mitigação |
|---|---|---|---|
| R1 | **Download errado** (ponteiro LFS, HTML de erro, arquivo truncado) passa despercebido e o app renderiza com a fonte de sistema silenciosamente | Baixa | Verificação de bytes + assinatura sfnt na §5; SHA-256 registrado nesta spec; teste da §8.2 |
| R2 | **Texto mais largo.** Atkinson tem x-height alto e glifos mais largos que Roboto → rótulos que hoje cabem podem quebrar linha ou ganhar reticências (bottom nav "Todos os PDFs", nome do PDF no top bar, botões dos diálogos) | **Média** | Passos 4 e 5 da §9, incluindo escala 1.3/2.0. Cabe lembrar que o D1 §D **reduz** tamanhos, o que folga a largura — se algo estourar, pode ser motivo para trazer o D1 junto |
| R3 | **Entrelinha.** Ascendentes/descendentes mais altos podem deixar `bodyLarge` (20sp / 28sp de `lineHeight`) apertado | Média | Olhar os estados vazios e as mensagens de erro nas capturas; se necessário, subir `lineHeight` — ajuste de uma linha, sem mudar `fontSize` |
| R4 | **Robolectric** não carrega fontes de recurso e quebra `HomeContentTest`/`ReaderContentTest` | Baixa | Passo 1 da §9 pega na hora. Plano B: manter a família num `val` que os testes possam substituir — só se acontecer, não preventivamente |
| R5 | **APK cresce mais que o previsto** (ex.: alguém copia a pasta `fonts/ttf` inteira) | Baixa | Aceite numérico no passo 3 da §9 + checagem do `unzip -l` |
| R6 | **Gosto.** O usuário pode achar a fonte estranha depois de ver no aparelho | Baixa | Reversão é trivial: apagar `Font.kt`, tirar o `.withFontFamily(...)` de `Theme.kt`, apagar `res/font`. Um commit isolado, revertível com `git revert` |

---

## 11. Esforço e ordem de commits

**~1 h** (bate com a estimativa do D1 §E), dividido em:

| Etapa | Tempo |
|---|---|
| Download, verificação, `NOTICE` + `res/raw/OFL` | 15 min |
| `Font.kt` + `Theme.kt` | 10 min |
| Testes (§8.1 e §8.2) | 15 min |
| `assembleRelease`, medição, capturas no aparelho, escala 1.3/2.0 | 20 min |

**Commits sugeridos** (separados, para que a reversão de R6 seja limpa):

1. `chore(font): adiciona Atkinson Hyperlegible Next (OFL 1.1) em res/font + NOTICE`
2. `feat(theme): aplica Atkinson Hyperlegible Next em todos os estilos de tipografia`
3. `test(theme): garante que os 15 estilos usam a fonte do app`
4. `docs(e2e): recaptura as telas com a nova fonte`

---

## 12. Dependência com o D1

Esta tarefa é **independente** e pode ir antes do D1 — só troca a família, não mexe em tamanho, peso, cor, ícone ou layout. Porém:

- Se o **D1 §D** (tipografia 16/18sp) for aprovado depois, `Theme.kt` muda de novo, mas o `.withFontFamily(...)` e o `Font.kt` continuam intactos — só os `fontSize` mudam.
- Se o D1 for implementado **antes**, `headlineMedium` deixa de usar Bold e o peso 700 passa a ser descartável (economia de ~65 KB). Reavaliar a §2.2 nesse momento, **não antes**.
- O `NOTICE` criado aqui é o mesmo arquivo onde entra a licença Apache 2.0 dos Material Symbols do D1 §B.
