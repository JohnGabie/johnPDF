# Paleta de cores Blue Lagoon — Plano de Implementação

> **Para agentes executores:** SUB-SKILL OBRIGATÓRIA: use `superpowers:subagent-driven-development` (recomendado) ou `superpowers:executing-plans` para implementar este plano tarefa a tarefa. Os passos usam caixas de seleção (`- [ ]`) para acompanhamento.

**Goal:** Substituir a paleta atual do johnPDF pela paleta **Blue Lagoon** (`#006494` no claro, `#96C4E5` no escuro) em `ui/theme/Theme.kt`, eliminando o lilás vazado do baseline do Material 3 e garantindo contraste WCAG AA (≥ 4,5:1) em todo par texto/fundo e AAA (≥ 7:1) no texto principal, nos dois esquemas.

**Architecture:** Dois esquemas **completos** (`LightColors` e `DarkColors`) declarados em `Theme.kt` com **todos** os papéis do `ColorScheme` — inclusive `surfaceContainerLowest…Highest`, `secondaryContainer`, `tertiary*`, `outline`, `outlineVariant`, `inverse*`, `scrim`. O lilás vazado hoje existe porque `lightColorScheme()` só define 11 papéis e o resto cai no baseline roxo do M3; preencher todos os papéis é a correção. As cores que não têm papel no M3 (vão entre páginas, ícone de PDF, indicador de aba) vivem numa `data class JohnColors` entregue por `CompositionLocalProvider` e lida via `JohnTheme.colors`. Nenhum `tonalElevation` — no escuro ele tinge a superfície com `primary` e reintroduz exatamente o problema de cor vazada; as barras passam a usar `surfaceContainer*` explícito. A regra de contraste fica travada por testes JVM puros, e nenhum `Color(0x…)` literal pode existir fora de `Theme.kt`.

**Tech Stack:** Kotlin, Jetpack Compose (BOM), Material 3, JUnit4 + Robolectric (testes em `app/src/test/`), Python 3 para o verificador de paleta fora do build (`tools/contrast.py`).

**Spec:**
- Estudo de paleta de cores — agente `a021abb48ee0a08b3`, recomendação **Blue Lagoon** (`#006494` / `#96C4E5`). O estudo não está versionado; **os valores finais, já medidos, estão reproduzidos integralmente na seção "Paleta medida" abaixo** e são a fonte de verdade deste plano.
- `docs/superpowers/specs/2026-09-29-dark-mode-ui.md` — estrutura do `Theme.kt`, papéis, desvios do Material Theme Builder, testes e checklist de E2E. Este plano **mantém a estrutura** dessa spec e **troca as cores** (era o cenário previsto na §2 e no risco 1 dela).
- `docs/superpowers/specs/2026-09-29-design-d1-proposta.md` §A linha 20 (diagnóstico do lilás vazado) e §G pergunta 5 (pedido do usuário por outra paleta; o índigo `#3558D4` foi rejeitado).

## Global Constraints

- **Contraste de texto ≥ 4,5:1** (WCAG 2.1 AA, critério 1.4.3) em **todo** par texto/fundo dos dois esquemas.
- **Contraste do texto principal ≥ 7:1** (WCAG 2.1 AAA, critério 1.4.6): `onSurface`/`onBackground` sobre `surface`, `background` e os cinco níveis `surfaceContainer*`; e `onSurfaceVariant` sobre `surface`.
- **Elementos não textuais que carregam informação ≥ 3:1** (critério 1.4.11): `outline`, `outlineVariant`, indicador de aba selecionada, ícones sem rótulo.
- **Estado desabilitado ≥ 3:1** — regra do projeto, mais rígida que a WCAG (que isenta desabilitado). Alpha **0,60**, não o 0,38 do M3.
- **Toda razão de contraste citada neste plano foi calculada** pela fórmula de luminância relativa da WCAG 2.1, não estimada. Reproduzível por `tools/contrast.py` e por `ContrastTest.kt`.
- **Nenhum `Color(0x…)` ou `Color.<nome>` do Compose literal fora de `ui/theme/Theme.kt`.** O branco da página do PDF fica em `Theme.kt` como `PdfPageBackground` (a página é branca nos dois modos — requisito do usuário). Único arquivo isento: `engine/MuPdfEngine.kt`, cujo `bitmap.eraseColor(Color.WHITE)` usa `android.graphics.Color`, não o do Compose.
- **Nenhum `tonalElevation`** em nenhum `Surface`/componente do app.
- Nenhuma cor dinâmica (`dynamicLightColorScheme`/`dynamicDarkColorScheme`) — decisão da spec de dark mode, §1 "fora do escopo".
- Mínimo de tipografia continua **20sp** (`ThemeTest.kt` atual). Este plano **não** mexe em tipografia; a redução para 16sp pertence ao plano D1.
- `minSdk = 24`, `compileSdk = 36`, Java 17 — não introduzir API acima de 24 sem qualificador de recurso.
- Idioma de commits, comentários e documentação: **pt-BR**.

## Review Focus

Cinco classes de entrada que a spec implica e que precisam de teste explícito, da mais provável de morder o usuário para a menos:

1. **`primary` sobre `surface` e sobre `surfaceContainer*`, nos dois esquemas** — é o rótulo de todo `TextButton` ("Cancelar", "Usar letras") e o texto do botão "Abrir"; o M3 o desenha sobre fundos que vão de `surfaceContainerLowest` a `surfaceContainerHighest`, e o pior caso (claro, sobre `surfaceContainerHighest`) é onde um azul escolhido "no olho" reprova. Medido: **5,05:1** claro / **6,59:1** escuro. Esperado: ≥ 4,5:1 em todos os cinco níveis.
2. **Ícone sem rótulo sobre `surfaceContainer*`** — o botão de rotação travado fica sobre `surfaceContainerHighest`; um ícone sozinho é informação não textual e precisa de ≥ 3:1, não de "parecer visível". Medido: `onSurfaceVariant` sobre `surfaceContainerHighest` = **7,28:1** claro / **7,20:1** escuro.
3. **`outline` e `outlineVariant` ≥ 3:1 sobre os seis fundos possíveis** (`surface` + cinco `surfaceContainer*`) — o divisor recuado da lista e a borda do campo de senha são o único separador visual entre um PDF e o seguinte; o padrão do Material Theme Builder entrega 1,6:1 e some para quem tem visão reduzida. Medido: pior caso **3,23:1** claro / **3,57:1** escuro.
4. **Indicador da aba selecionada contra o fundo da `NavigationBar`** — "qual aba está aberta" é um estado de componente (1.4.11). Nenhum container claro do M3 chega a 3:1 contra uma barra clara (`secondaryContainer` sobre `surfaceContainer` dá **1,12:1**), por isso existe um papel próprio `tabIndicator`. Medido: **6,40:1** claro / **3,28:1** escuro; rótulo dentro do indicador **7,41:1** / **4,97:1**.
5. **Erro e confirmação** — "Senha incorreta, tente de novo" em `error` sobre o fundo do diálogo (`surfaceContainerHigh`) e o Snackbar de confirmação (`inverseOnSurface` sobre `inverseSurface`, ação em `inversePrimary`) precisam ser legíveis **e** não podem depender só do matiz: `error` e `primary` têm razão de luminância 1,00:1 no claro e 1,09:1 no escuro — são indistinguíveis para quem não enxerga cor, então a mensagem de erro sempre acompanha texto. Medido: erro **5,33:1** / **8,42:1**; snackbar **11,58:1** / **10,20:1**; ação do snackbar **7,10:1** / **5,01:1**.

---

## Paleta medida (fonte de verdade)

Todos os valores abaixo foram conferidos pela fórmula WCAG 2.1: **zero falhas**, pior par de texto **5,05:1** no claro e **5,50:1** no escuro.

### Esquema claro — seed `#006494`

| Papel | Hex | Papel | Hex |
|---|---|---|---|
| `primary` | `#006494` | `background` | `#F7FAFC` |
| `onPrimary` | `#FFFFFF` | `onBackground` | `#171C1F` |
| `primaryContainer` | `#CBE6FF` | `surface` | `#F7FAFC` |
| `onPrimaryContainer` | `#001E30` | `onSurface` | `#171C1F` |
| `secondary` | `#4E616D` | `surfaceVariant` | `#DCE3E9` |
| `onSecondary` | `#FFFFFF` | `onSurfaceVariant` | `#40484D` |
| `secondaryContainer` | `#D2E5F5` | `surfaceContainerLowest` | `#FFFFFF` |
| `onSecondaryContainer` | `#0B1D29` | `surfaceContainerLow` | `#F1F4F7` |
| `tertiary` | `#00677C` | `surfaceContainer` | `#EBEFF2` |
| `onTertiary` | `#FFFFFF` | `surfaceContainerHigh` | `#E5EAEE` |
| `tertiaryContainer` | `#B2EBFF` | `surfaceContainerHighest` | `#DFE4E8` |
| `onTertiaryContainer` | `#001F27` | `outline` | `#70787D` |
| `error` | `#BA1A1A` | `outlineVariant` | `#767E83` ⚠️ |
| `onError` | `#FFFFFF` | `inverseSurface` | `#2C3134` |
| `errorContainer` | `#FFDAD6` | `inverseOnSurface` | `#EDF1F4` |
| `onErrorContainer` | `#410002` | `inversePrimary` | `#96C4E5` |
| `surfaceTint` | `#006494` | `scrim` | `#000000` |

### Esquema escuro — `primary` `#96C4E5`

| Papel | Hex | Papel | Hex |
|---|---|---|---|
| `primary` | `#96C4E5` | `background` | `#0F1417` |
| `onPrimary` | `#003450` | `onBackground` | `#DFE3E7` |
| `primaryContainer` | `#004B70` | `surface` | `#0F1417` |
| `onPrimaryContainer` | `#CBE6FF` | `onSurface` | `#DFE3E7` |
| `secondary` | `#B6C9D8` | `surfaceVariant` | `#40484D` |
| `onSecondary` | `#21333E` | `onSurfaceVariant` | `#C0C8CD` |
| `secondaryContainer` | `#384955` | `surfaceContainerLowest` | `#0A0F12` |
| `onSecondaryContainer` | `#D2E5F5` | `surfaceContainerLow` | `#171C1F` |
| `tertiary` | `#83D3EB` | `surfaceContainer` | `#1B2124` |
| `onTertiary` | `#003641` | `surfaceContainerHigh` | `#262B2F` |
| `tertiaryContainer` | `#004E5D` | `surfaceContainerHighest` | `#31363A` |
| `onTertiaryContainer` | `#B2EBFF` | `outline` | `#8A9297` |
| `error` | `#FFB4AB` | `outlineVariant` | `#848C91` ⚠️ |
| `onError` | `#690005` | `inverseSurface` | `#DFE3E7` |
| `errorContainer` | `#93000A` | `inverseOnSurface` | `#2C3134` |
| `onErrorContainer` | `#FFDAD6` | `inversePrimary` | `#006494` |
| `surfaceTint` | `#96C4E5` | `scrim` | `#000000` |

⚠️ `outlineVariant` é **desvio proposital** do que o Material Theme Builder geraria (um cinza claro de ~1,6:1): o divisor da lista precisa de ≥ 3:1 contra os seis fundos possíveis.

### Papéis fora do `ColorScheme` (`JohnColors`)

| Campo | Claro | Escuro | Onde aparece |
|---|---|---|---|
| `pageGap` | `#E5EAEE` (= `surfaceContainerHigh`) | `#0A0F12` (= `surfaceContainerLowest`) | Vão entre páginas no leitor |
| `pdfIcon` | `#C62828` | `#FF8A80` | Ícone/emoji de PDF no cartão da lista |
| `pdfIconContainer` | `#FDECEA` | `#262B2F` | Fundo do ícone de PDF |
| `tabIndicator` | `#365A6C` | `#5A7385` | Pílula da aba selecionada na `NavigationBar` |
| `onTabIndicator` | `#FFFFFF` | `#FFFFFF` | Ícone/rótulo dentro da pílula |

`PdfPageBackground = Color.White` fica **fora** da `JohnColors` de propósito: é literal e imutável nos dois modos.

### Razões medidas que os testes travam

| Par | Claro | Escuro |
|---|---:|---:|
| `onSurface` / `surface` | 16,39:1 | 14,37:1 |
| `onSurfaceVariant` / `surface` | 8,90:1 | 10,93:1 |
| `onSurface` / `surfaceContainerHighest` | 13,42:1 | 9,47:1 |
| `primary` / `surface` | 6,16:1 | 10,00:1 |
| `primary` / `surfaceContainerHighest` (pior par claro) | **5,05:1** | 6,59:1 |
| `onSurfaceVariant` / `surfaceVariant` (pior par escuro) | 7,20:1 | **5,50:1** |
| `onPrimary` / `primary` | 6,46:1 | 7,05:1 |
| `onPrimaryContainer` / `primaryContainer` | 13,27:1 | 7,28:1 |
| `onSecondaryContainer` / `secondaryContainer` | 13,31:1 | 7,22:1 |
| `error` / `surfaceContainerHigh` | 5,33:1 | 8,42:1 |
| `outlineVariant` / `surfaceContainerHighest` | 3,23:1 | 3,57:1 |
| `outline` / `surfaceContainerHighest` | 3,51:1 | 3,86:1 |
| `tabIndicator` / `surfaceContainer` | 6,40:1 | 3,28:1 |
| `onTabIndicator` / `tabIndicator` | 7,41:1 | 4,97:1 |
| `inverseOnSurface` / `inverseSurface` | 11,58:1 | 10,20:1 |
| `inversePrimary` / `inverseSurface` | 7,10:1 | 5,01:1 |
| `pdfIcon` / `pdfIconContainer` | 4,92:1 | 6,26:1 |
| Rótulo desabilitado (alpha 0,60) sobre container | 3,96:1 | 4,44:1 |
| Página branca / `pageGap` (informativo) | 1,21:1 | 19,27:1 |

---

## Estrutura de arquivos

| Arquivo | Ação | Responsabilidade |
|---|---|---|
| `tools/contrast.py` | Modificar | Verificador de paleta fora do build; espelha os dois esquemas e as quatro regras |
| `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` | Modificar (reescrito) | Única fonte de cor do app: `LightColors`, `DarkColors`, `JohnColors`, helpers de papel, tipografia, `JohnPdfTheme` |
| `app/src/test/java/com/johngabie/johnpdf/ui/theme/ContrastTest.kt` | Criar | Trava AA/AAA/3:1/desabilitado nos dois esquemas (JVM puro, sem Robolectric) |
| `app/src/test/java/com/johngabie/johnpdf/ui/theme/DarkThemeTest.kt` | Criar | Trava a seleção claro/escuro e os papéis expostos por `JohnTheme` (Robolectric) |
| `app/src/test/java/com/johngabie/johnpdf/ui/theme/NoHardcodedColorTest.kt` | Criar | Varre os fontes: nenhum `Color(0x…)` nem `tonalElevation` fora de `Theme.kt` |
| `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt` | Modificar | `PageGapColor` → `JohnTheme.colors.pageGap`; `Color.White` → `PdfPageBackground`; sem `tonalElevation` |
| `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt` | Modificar | Barras e cartões com papéis explícitos; `NavigationBar` com indicador próprio |
| `app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt` | Modificar | `containerColor` explícito nos três `AlertDialog` |
| `app/src/main/java/com/johngabie/johnpdf/ui/common/BigButton.kt` | Modificar | Cores de desabilitado legíveis (alpha 0,60) |
| `app/src/main/java/com/johngabie/johnpdf/MainActivity.kt` | Modificar | `SystemBarStyle.light` → `auto` |
| `app/src/main/res/values/colors.xml`, `values-night/colors.xml` | Criar | `window_background` casando com `surface` de cada esquema |
| `app/src/main/res/values/themes.xml`, `values-v29/themes.xml` | Criar | Tema de plataforma sem clarão no arranque a frio |
| `app/src/main/AndroidManifest.xml` | Modificar | `android:theme` → `@style/Theme.JohnPdf` |
| `docs/e2e/2026-09-28-checklist.md` | Modificar | +8 cenários de verificação visual claro/escuro |

**Ordem das tarefas:** 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9. As tarefas 2 e 3 definem os nomes que todas as outras consomem.

**Comandos** (na raiz do repositório). PowerShell: troque `./gradlew` por `.\gradlew.bat`.

---

### Task 1: Verificador de paleta fora do build

Antes de escrever um único hex em Kotlin, a paleta passa pelo verificador. Ele é mais rápido que o Gradle e é onde qualquer troca futura de seed começa.

**Files:**
- Modify: `tools/contrast.py` (dicionários `LIGHT`/`DARK` nas linhas 17–55; função `check` nas linhas 105–145)

**Interfaces:**
- Consumes: nada.
- Produces: `tools/contrast.py` com os dicionários `LIGHT` e `DARK` contendo, além dos papéis do `ColorScheme`, as chaves `pageGap`, `pdfIcon`, `pdfIconContainer`, `tabIndicator`, `onTabIndicator`; e as regras AAA e de indicador de aba. Os hex daqui são copiados literalmente para `Theme.kt` na Task 2 e 3.

- [ ] **Step 1: Endurecer as regras antes de trocar as cores**

Em `tools/contrast.py`, logo depois do bloco `ON_CONTAINER = [...]` (linha 72), acrescente:

```python
# Texto principal: AAA (WCAG 1.4.6). onSurface sobre tudo que o app usa como fundo.
AAA_FOREGROUNDS = ["onSurface"]
```

E dentro de `check`, logo depois do bloco `-- texto sobre surfaceContainer* --` (antes de `print("  -- divisores (>= 3.0) --")`), acrescente:

```python
    print("  -- AAA: texto principal (>= 7.0) --")
    for bg in ["surface", "background"] + CONTAINERS:
        for fg in AAA_FOREGROUNDS:
            assert_min(f"AAA {fg}/{bg}", scheme[fg], scheme[bg], 7.0)
    assert_min("AAA onSurfaceVariant/surface",
               scheme["onSurfaceVariant"], scheme["surface"], 7.0)

    print("  -- indicador da aba selecionada (>= 3.0) e seu rótulo (>= 4.5) --")
    assert_min("tabIndicator/surfaceContainer",
               scheme["tabIndicator"], scheme["surfaceContainer"], 3.0)
    assert_min("onTabIndicator/tabIndicator",
               scheme["onTabIndicator"], scheme["tabIndicator"], 4.5)

    print("  -- ícone de PDF (>= 3.0) --")
    assert_min("pdfIcon/pdfIconContainer",
               scheme["pdfIcon"], scheme["pdfIconContainer"], 3.0)
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `py tools/contrast.py` (ou `python tools/contrast.py`)
Expected: **falha** — `KeyError: 'tabIndicator'`, porque os dicionários ainda são os do índigo e não têm os papéis novos.

- [ ] **Step 3: Trocar as duas paletas**

Substitua **todo** o dicionário `LIGHT` (linhas 17–35) por:

```python
LIGHT = {
    "primary": "#006494", "onPrimary": "#FFFFFF",
    "primaryContainer": "#CBE6FF", "onPrimaryContainer": "#001E30",
    "secondary": "#4E616D", "onSecondary": "#FFFFFF",
    "secondaryContainer": "#D2E5F5", "onSecondaryContainer": "#0B1D29",
    "tertiary": "#00677C", "onTertiary": "#FFFFFF",
    "tertiaryContainer": "#B2EBFF", "onTertiaryContainer": "#001F27",
    "error": "#BA1A1A", "onError": "#FFFFFF",
    "errorContainer": "#FFDAD6", "onErrorContainer": "#410002",
    "background": "#F7FAFC", "onBackground": "#171C1F",
    "surface": "#F7FAFC", "onSurface": "#171C1F",
    "surfaceVariant": "#DCE3E9", "onSurfaceVariant": "#40484D",
    "surfaceContainerLowest": "#FFFFFF", "surfaceContainerLow": "#F1F4F7",
    "surfaceContainer": "#EBEFF2", "surfaceContainerHigh": "#E5EAEE",
    "surfaceContainerHighest": "#DFE4E8",
    "outline": "#70787D", "outlineVariant": "#767E83",
    "inverseSurface": "#2C3134", "inverseOnSurface": "#EDF1F4",
    "inversePrimary": "#96C4E5",
    "pageGap": "#E5EAEE",
    "pdfIcon": "#C62828", "pdfIconContainer": "#FDECEA",
    "tabIndicator": "#365A6C", "onTabIndicator": "#FFFFFF",
}
```

E **todo** o dicionário `DARK` (linhas 37–55) por:

```python
DARK = {
    "primary": "#96C4E5", "onPrimary": "#003450",
    "primaryContainer": "#004B70", "onPrimaryContainer": "#CBE6FF",
    "secondary": "#B6C9D8", "onSecondary": "#21333E",
    "secondaryContainer": "#384955", "onSecondaryContainer": "#D2E5F5",
    "tertiary": "#83D3EB", "onTertiary": "#003641",
    "tertiaryContainer": "#004E5D", "onTertiaryContainer": "#B2EBFF",
    "error": "#FFB4AB", "onError": "#690005",
    "errorContainer": "#93000A", "onErrorContainer": "#FFDAD6",
    "background": "#0F1417", "onBackground": "#DFE3E7",
    "surface": "#0F1417", "onSurface": "#DFE3E7",
    "surfaceVariant": "#40484D", "onSurfaceVariant": "#C0C8CD",
    "surfaceContainerLowest": "#0A0F12", "surfaceContainerLow": "#171C1F",
    "surfaceContainer": "#1B2124", "surfaceContainerHigh": "#262B2F",
    "surfaceContainerHighest": "#31363A",
    "outline": "#8A9297", "outlineVariant": "#848C91",
    "inverseSurface": "#DFE3E7", "inverseOnSurface": "#2C3134",
    "inversePrimary": "#006494",
    "pageGap": "#0A0F12",
    "pdfIcon": "#FF8A80", "pdfIconContainer": "#262B2F",
    "tabIndicator": "#5A7385", "onTabIndicator": "#FFFFFF",
}
```

Atualize também o cabeçalho do arquivo: na lista de regras (linhas 7–11), acrescente a linha
`  - texto principal            >= 7.0:1  (WCAG 1.4.6 AAA)`.

- [ ] **Step 4: Rodar e ver passar**

Run: `py tools/contrast.py`
Expected: sai com código 0; as duas seções terminam em `>>> tudo dentro da regra`. Confira no relatório: pior par de texto **5,05:1** no claro (`primary/surfaceContainerHighest`) e **5,50:1** no escuro (`onSurfaceVariant/surfaceVariant`); `tabIndicator/surfaceContainer` **6,40:1** claro e **3,28:1** escuro.

- [ ] **Step 5: Commit**

```bash
git add tools/contrast.py
git commit -m "feat(tema): paleta Blue Lagoon no verificador de contraste

Troca o seed indigo #3558D4 pelo Blue Lagoon #006494 (claro) / #96C4E5
(escuro) e endurece as regras: AAA (>=7:1) para texto principal,
>=3:1 para o indicador de aba e para o icone de PDF.

80+ pares conferidos, zero falhas. Pior par: 5,05:1 claro, 5,50:1 escuro."
```

---

### Task 2: `ContrastTest.kt` + `LightColors` completo

O teste vem primeiro e é JVM puro: `Color` do Compose e `lightColorScheme()` não tocam em Android, então roda sem Robolectric e em milissegundos.

**Files:**
- Create: `app/src/test/java/com/johngabie/johnpdf/ui/theme/ContrastTest.kt`
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` (substitui o `private val Colors` das linhas 15–27)

**Interfaces:**
- Consumes: os hex da Task 1.
- Produces:
  - `internal fun contrast(a: Color, b: Color): Double` em `ContrastTest.kt` — razão WCAG, 1,0 a 21,0. Reutilizada pelo `DarkThemeTest` da Task 3.
  - `private val LightColors: ColorScheme` em `Theme.kt`.
  - `internal val LightSchemeForTest: ColorScheme` em `Theme.kt` — só para os testes.

- [ ] **Step 1: Escrever o teste que falha**

Crie `app/src/test/java/com/johngabie/johnpdf/ui/theme/ContrastTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Luminância relativa, WCAG 2.1: https://www.w3.org/TR/WCAG21/#dfn-relative-luminance */
private fun luminance(c: Color): Double {
    fun ch(v: Float): Double {
        val s = v.toDouble()
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
}

/** Razão de contraste WCAG: 1:1 (cores iguais) a 21:1 (preto sobre branco). */
internal fun contrast(a: Color, b: Color): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/** Os cinco níveis de container que o app usa como fundo. */
internal fun ColorScheme.containers(): List<Pair<String, Color>> = listOf(
    "surfaceContainerLowest" to surfaceContainerLowest,
    "surfaceContainerLow" to surfaceContainerLow,
    "surfaceContainer" to surfaceContainer,
    "surfaceContainerHigh" to surfaceContainerHigh,
    "surfaceContainerHighest" to surfaceContainerHighest,
)

/** Todo par texto/fundo que o app realmente desenha. */
internal fun ColorScheme.textPairs(): List<Triple<String, Color, Color>> = listOf(
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
    Triple("inversePrimary/inverseSurface", inversePrimary, inverseSurface),
) + containers().flatMap { (bgName, bg) ->
    // Tudo que o app escreve sobre um container: texto principal, secundário,
    // rótulo de TextButton (primary) e mensagem de erro.
    listOf(
        "onSurface" to onSurface,
        "onSurfaceVariant" to onSurfaceVariant,
        "primary" to primary,
        "error" to error,
    ).map { (fgName, fg) -> Triple("$fgName/$bgName", fg, bg) }
}

class ContrastTest {

    private fun assertAa(schemeName: String, scheme: ColorScheme) {
        scheme.textPairs().forEach { (name, fg, bg) ->
            val r = contrast(fg, bg)
            assertTrue("[$schemeName] $name = %.2f:1, mínimo 4.5:1".format(r), r >= 4.5)
        }
    }

    private fun assertAaa(schemeName: String, scheme: ColorScheme) {
        val backgrounds = listOf("surface" to scheme.surface, "background" to scheme.background) +
            scheme.containers()
        backgrounds.forEach { (bgName, bg) ->
            val r = contrast(scheme.onSurface, bg)
            assertTrue("[$schemeName] AAA onSurface/$bgName = %.2f:1, mínimo 7:1".format(r), r >= 7.0)
        }
        val variant = contrast(scheme.onSurfaceVariant, scheme.surface)
        assertTrue(
            "[$schemeName] AAA onSurfaceVariant/surface = %.2f:1, mínimo 7:1".format(variant),
            variant >= 7.0,
        )
    }

    private fun assertOutlines(schemeName: String, scheme: ColorScheme) {
        val backgrounds = listOf("surface" to scheme.surface) + scheme.containers()
        backgrounds.forEach { (bgName, bg) ->
            listOf("outline" to scheme.outline, "outlineVariant" to scheme.outlineVariant)
                .forEach { (fgName, fg) ->
                    val r = contrast(fg, bg)
                    assertTrue(
                        "[$schemeName] $fgName/$bgName = %.2f:1, mínimo 3:1".format(r),
                        r >= 3.0,
                    )
                }
        }
    }

    /** Regra do projeto, mais rígida que a WCAG (que isenta desabilitado). */
    private fun assertDisabled(schemeName: String, scheme: ColorScheme) {
        val container = scheme.onSurface.copy(alpha = 0.12f).compositeOver(scheme.surfaceContainer)
        val label = scheme.onSurface.copy(alpha = 0.60f).compositeOver(container)
        val r = contrast(label, container)
        assertTrue("[$schemeName] rótulo desabilitado = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
    }

    @Test fun light_text_meets_wcag_aa() = assertAa("claro", LightSchemeForTest)

    @Test fun light_main_text_meets_wcag_aaa() = assertAaa("claro", LightSchemeForTest)

    /** Review Focus 3: divisor da lista e borda do campo em todos os fundos. */
    @Test fun light_outlines_meet_non_text_minimum() = assertOutlines("claro", LightSchemeForTest)

    @Test fun light_disabled_label_stays_readable() = assertDisabled("claro", LightSchemeForTest)

    /** Review Focus 1: o pior par do claro é primary sobre surfaceContainerHighest. */
    @Test fun light_primary_is_readable_on_every_container() {
        LightSchemeForTest.containers().forEach { (name, bg) ->
            val r = contrast(LightSchemeForTest.primary, bg)
            assertTrue("[claro] primary/$name = %.2f:1, mínimo 4.5:1".format(r), r >= 4.5)
        }
    }

    /** Review Focus 2: ícone sem rótulo é informação não textual (WCAG 1.4.11). */
    @Test fun light_lone_icon_is_visible_on_every_container() {
        LightSchemeForTest.containers().forEach { (name, bg) ->
            val r = contrast(LightSchemeForTest.onSurfaceVariant, bg)
            assertTrue("[claro] ícone onSurfaceVariant/$name = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
        }
    }

    /** O seed da marca não pode mudar por acidente. */
    @Test fun light_primary_is_blue_lagoon() {
        assertTrue(
            "primary do claro deve ser #006494",
            LightSchemeForTest.primary == Color(0xFF006494),
        )
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ContrastTest"`
Expected: **falha de compilação** — `Unresolved reference: LightSchemeForTest`.

- [ ] **Step 3: Escrever o esquema claro completo**

Em `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt`, apague o bloco `private val Colors = lightColorScheme(...)` (linhas 15–27) e coloque no lugar:

```kotlin
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
```

E, no fim do arquivo, exponha o esquema para os testes:

```kotlin
/** Exposto só para os testes de contraste. */
internal val LightSchemeForTest = LightColors
```

Troque também a referência em `JohnPdfTheme` (linha 45) de `colorScheme = Colors` para `colorScheme = LightColors` — o parâmetro `darkTheme` entra na Task 3.

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.ContrastTest"`
Expected: PASS, 7 testes.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt app/src/test/java/com/johngabie/johnpdf/ui/theme/ContrastTest.kt
git commit -m "feat(tema): esquema claro Blue Lagoon completo

Todos os papeis do ColorScheme preenchidos (era um lightColorScheme de 11
papeis; o resto caia no baseline roxo do M3 e vazava lilas nas barras,
no indicador de aba e nos dialogos).

ContrastTest trava AA (4,5:1), AAA (7:1) no texto principal, 3:1 em
outline/outlineVariant e no icone sozinho, e 3:1 no desabilitado."
```

---

### Task 3: `DarkColors`, `JohnPdfTheme(darkTheme)` e `DarkThemeTest`

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt`
- Modify: `app/src/test/java/com/johngabie/johnpdf/ui/theme/ContrastTest.kt` (acrescenta os testes do escuro)
- Create: `app/src/test/java/com/johngabie/johnpdf/ui/theme/DarkThemeTest.kt`

**Interfaces:**
- Consumes: `contrast(a, b)`, `ColorScheme.containers()`, `ColorScheme.textPairs()` e `LightSchemeForTest` da Task 2.
- Produces:
  - `private val DarkColors: ColorScheme`
  - `internal val DarkSchemeForTest: ColorScheme`
  - `@Composable fun JohnPdfTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)` — a assinatura com valor padrão mantém todas as chamadas existentes (`JohnPdfTheme { ... }`) compilando.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente ao final da classe `ContrastTest` (antes da última `}`):

```kotlin
    @Test fun dark_text_meets_wcag_aa() = assertAa("escuro", DarkSchemeForTest)

    @Test fun dark_main_text_meets_wcag_aaa() = assertAaa("escuro", DarkSchemeForTest)

    @Test fun dark_outlines_meet_non_text_minimum() = assertOutlines("escuro", DarkSchemeForTest)

    @Test fun dark_disabled_label_stays_readable() = assertDisabled("escuro", DarkSchemeForTest)

    @Test fun dark_primary_is_readable_on_every_container() {
        DarkSchemeForTest.containers().forEach { (name, bg) ->
            val r = contrast(DarkSchemeForTest.primary, bg)
            assertTrue("[escuro] primary/$name = %.2f:1, mínimo 4.5:1".format(r), r >= 4.5)
        }
    }

    @Test fun dark_lone_icon_is_visible_on_every_container() {
        DarkSchemeForTest.containers().forEach { (name, bg) ->
            val r = contrast(DarkSchemeForTest.onSurfaceVariant, bg)
            assertTrue("[escuro] ícone onSurfaceVariant/$name = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
        }
    }

    @Test fun dark_primary_is_blue_lagoon() {
        assertTrue(
            "primary do escuro deve ser #96C4E5",
            DarkSchemeForTest.primary == Color(0xFF96C4E5),
        )
    }

    /**
     * Review Focus 5: erro e confirmação. As duas mensagens precisam ser legíveis
     * sobre a superfície em que aparecem — e, como error e primary têm quase a
     * mesma luminância (1,00:1 no claro), o erro nunca é sinalizado só por cor.
     */
    @Test fun error_and_confirmation_are_readable_in_both_schemes() {
        listOf("claro" to LightSchemeForTest, "escuro" to DarkSchemeForTest).forEach { (n, s) ->
            val erro = contrast(s.error, s.surfaceContainerHigh)
            assertTrue("[$n] error/surfaceContainerHigh = %.2f:1".format(erro), erro >= 4.5)
            val snack = contrast(s.inverseOnSurface, s.inverseSurface)
            assertTrue("[$n] snackbar = %.2f:1".format(snack), snack >= 4.5)
            val acao = contrast(s.inversePrimary, s.inverseSurface)
            assertTrue("[$n] ação do snackbar = %.2f:1".format(acao), acao >= 4.5)
        }
    }

    /** Nenhum papel pode ficar na cor de "não preenchido" (lilás do baseline M3). */
    @Test fun no_role_keeps_the_material_baseline_purple() {
        val lilases = listOf(
            Color(0xFFE8DEF8), Color(0xFFEADDFF), Color(0xFF6750A4), Color(0xFFD0BCFF),
            Color(0xFF4A4458), Color(0xFF21005D), Color(0xFF1D192B), Color(0xFF49454F),
        )
        listOf("claro" to LightSchemeForTest, "escuro" to DarkSchemeForTest).forEach { (n, s) ->
            val usados = s.textPairs().flatMap { listOf(it.second, it.third) } +
                listOf(s.outline, s.outlineVariant, s.surfaceTint, s.scrim)
            lilases.forEach { roxo ->
                assertTrue(
                    "[$n] papel ficou no baseline roxo do M3: $roxo",
                    usados.none { it == roxo },
                )
            }
        }
    }
```

E crie `app/src/test/java/com/johngabie/johnpdf/ui/theme/DarkThemeTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DarkThemeTest {
    @get:Rule val rule = createComposeRule()

    /**
     * Lê o mesmo valor nos dois esquemas numa composição só.
     *
     * `createComposeRule()` aceita **um** `setContent` por teste, então os dois
     * temas entram como irmãos na mesma árvore em vez de duas chamadas.
     */
    private fun <T> bothThemes(read: @Composable () -> T): Pair<T, T> {
        var claro: T? = null
        var escuro: T? = null
        rule.setContent {
            JohnPdfTheme(darkTheme = false) { claro = read() }
            JohnPdfTheme(darkTheme = true) { escuro = read() }
        }
        return claro!! to escuro!!
    }

    @Test fun each_theme_selects_its_own_scheme() {
        val (claro, escuro) = bothThemes { MaterialTheme.colorScheme }
        assertEquals(Color(0xFFF7FAFC), claro.surface)
        assertEquals(Color(0xFF006494), claro.primary)
        assertEquals(Color(0xFF0F1417), escuro.surface)
        assertEquals(Color(0xFF96C4E5), escuro.primary)
    }

    @Test fun surfaces_never_collide_between_schemes() {
        val (claro, escuro) = bothThemes { MaterialTheme.colorScheme }
        assertEquals(false, claro.surface == escuro.surface)
        assertEquals(false, claro.surfaceContainer == escuro.surfaceContainer)
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.*"`
Expected: **falha de compilação** — `Unresolved reference: DarkSchemeForTest` e `No value passed for parameter 'darkTheme'`.

- [ ] **Step 3: Escrever o esquema escuro e o seletor de tema**

Em `Theme.kt`, acrescente os imports:

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
```

Logo depois de `LightColors`, acrescente:

```kotlin
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
```

Substitua `JohnPdfTheme` (linhas 43–46 do arquivo original) por:

```kotlin
/**
 * `darkTheme` é parâmetro (e não só `isSystemInDarkTheme()` interno) para que os
 * testes consigam forçar cada esquema. O valor padrão mantém as chamadas
 * existentes `JohnPdfTheme { ... }` funcionando.
 */
@Composable
fun JohnPdfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = BigTypography, content = content)
}
```

E acrescente ao bloco de exposição para testes:

```kotlin
internal val DarkSchemeForTest = DarkColors
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.*"`
Expected: PASS — `ContrastTest` com 16 testes (7 do claro, 7 do escuro, erro/confirmação e a trava anti-lilás), `DarkThemeTest` com 2, `ThemeTest` com 1.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt app/src/test/java/com/johngabie/johnpdf/ui/theme/
git commit -m "feat(tema): esquema escuro Blue Lagoon e JohnPdfTheme(darkTheme)

darkColorScheme completo derivado do mesmo seed; JohnPdfTheme escolhe por
isSystemInDarkTheme() e aceita darkTheme explicito para os testes.

Trava tambem que nenhum papel ficou no roxo do baseline M3 e que erro e
confirmacao sao legiveis nos dois esquemas."
```

---

### Task 4: `JohnColors`, `PdfPageBackground` e o vão entre páginas

`PageGapColor = Color(0xFFBDBDBD)` é uma constante global fixa: no escuro vira uma faixa cinza-clara e, no claro, a faixa grossa cinza já foi apontada como problema (§A linha 22 da D1). Vira papel de tema.

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt` (linhas 80, 262, 325)
- Modify: `app/src/test/java/com/johngabie/johnpdf/ui/theme/DarkThemeTest.kt`

**Interfaces:**
- Consumes: `JohnPdfTheme(darkTheme, content)` da Task 3; `contrast(a, b)` da Task 2.
- Produces:
  - `data class JohnColors(val pageGap: Color, val pdfIcon: Color, val pdfIconContainer: Color, val tabIndicator: Color, val onTabIndicator: Color)`
  - `object JohnTheme` com `colors: JohnColors`, `barColor: Color` (= `surfaceContainer`) e `dialogColor: Color` (= `surfaceContainerHigh`), todos `@Composable @ReadOnlyComposable`.
  - `val PdfPageBackground: Color` (= `Color.White`)
  - `internal val LightJohnForTest: JohnColors`, `internal val DarkJohnForTest: JohnColors`
  - `PageGapColor` **deixa de existir**.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente a `DarkThemeTest.kt` (reutilizando o helper `bothThemes` da Task 3):

```kotlin
    @Test fun page_gap_follows_the_theme() {
        val (claro, escuro) = bothThemes { JohnTheme.colors }
        assertEquals(Color(0xFFE5EAEE), claro.pageGap)
        assertEquals(Color(0xFF0A0F12), escuro.pageGap)
    }

    /** Requisito do usuário: a página do PDF é branca nos dois modos. */
    @Test fun pdf_page_stays_white_in_both_modes() {
        assertEquals(Color.White, PdfPageBackground)
        val (claro, escuro) = bothThemes { JohnTheme.colors }
        assertNotEquals(Color.White, claro.pageGap)
        assertNotEquals(Color.White, escuro.pageGap)
    }

    /** No escuro o vão precisa emoldurar a página branca. */
    @Test fun page_gap_frames_the_white_page_in_dark() {
        val (_, escuro) = bothThemes { JohnTheme.colors }
        val r = contrast(Color.White, escuro.pageGap)
        assertTrue("vão/página no escuro = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
    }

    /** As barras e os diálogos têm um papel só, e ele muda com o tema. */
    @Test fun bar_and_dialog_colors_come_from_the_scheme() {
        val (claro, escuro) = bothThemes { JohnTheme.barColor to JohnTheme.dialogColor }
        assertEquals(Color(0xFFEBEFF2) to Color(0xFFE5EAEE), claro)
        assertEquals(Color(0xFF1B2124) to Color(0xFF262B2F), escuro)
    }
```

Acrescente os imports `import org.junit.Assert.assertTrue` e `import org.junit.Assert.assertNotEquals` ao topo do arquivo.

E acrescente a `ContrastTest.kt`, dentro da classe (Review Focus 4 e o ícone de PDF):

```kotlin
    /** Review Focus 4: "qual aba está aberta" é estado de componente (WCAG 1.4.11). */
    @Test fun tab_indicator_stands_out_from_the_navigation_bar() {
        listOf(
            Triple("claro", LightSchemeForTest, LightJohnForTest),
            Triple("escuro", DarkSchemeForTest, DarkJohnForTest),
        ).forEach { (n, s, j) ->
            val pilula = contrast(j.tabIndicator, s.surfaceContainer)
            assertTrue("[$n] indicador/NavigationBar = %.2f:1, mínimo 3:1".format(pilula), pilula >= 3.0)
            val rotulo = contrast(j.onTabIndicator, j.tabIndicator)
            assertTrue("[$n] rótulo no indicador = %.2f:1, mínimo 4.5:1".format(rotulo), rotulo >= 4.5)
            val naoSelecionada = contrast(s.onSurfaceVariant, s.surfaceContainer)
            assertTrue("[$n] aba não selecionada = %.2f:1, mínimo 4.5:1".format(naoSelecionada), naoSelecionada >= 4.5)
        }
    }

    @Test fun pdf_icon_is_visible_on_its_container() {
        listOf("claro" to LightJohnForTest, "escuro" to DarkJohnForTest).forEach { (n, j) ->
            val r = contrast(j.pdfIcon, j.pdfIconContainer)
            assertTrue("[$n] ícone de PDF = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
        }
    }
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.*"`
Expected: **falha de compilação** — `Unresolved reference: JohnTheme`, `PdfPageBackground`, `LightJohnForTest`, `DarkJohnForTest`.

- [ ] **Step 3: Implementar `JohnColors` e trocar o vão**

Em `Theme.kt`, acrescente os imports:

```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
```

Apague a linha 13 (`val PageGapColor = Color(0xFFBDBDBD)`) e, no lugar do bloco de constantes do topo, deixe:

```kotlin
val MinTouchTarget = 64.dp

/**
 * Fundo da página renderizada do PDF.
 *
 * Literal e imutável de propósito: o conteúdo do PDF NÃO acompanha o modo escuro
 * (inverter a página é "modo noturno do PDF", fora do escopo da v1). Não trocar
 * por `MaterialTheme.colorScheme.surface`. Coberto por `DarkThemeTest`.
 */
val PdfPageBackground = Color.White
```

Depois de `DarkColors`, acrescente:

```kotlin
// ------------------------------------------- papéis que o M3 não tem

/** Cores do johnPDF sem papel correspondente no Material 3. */
data class JohnColors(
    /** Vão entre as páginas no leitor. Quase preto no escuro, para emoldurar a página branca. */
    val pageGap: Color,
    /** Ícone de PDF no cartão da lista e o círculo atrás dele. */
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
```

Em `JohnPdfTheme`, embrulhe o `MaterialTheme`:

```kotlin
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
```

E acrescente à exposição de testes:

```kotlin
internal val LightJohnForTest = LightJohnColors
internal val DarkJohnForTest = DarkJohnColors
```

Em `ReaderScreen.kt`: troque o import da linha 80
`import com.johngabie.johnpdf.ui.theme.PageGapColor` por
`import com.johngabie.johnpdf.ui.theme.JohnTheme` e
`import com.johngabie.johnpdf.ui.theme.PdfPageBackground`;
na linha 262, `BoxWithConstraints(Modifier.fillMaxSize().background(PageGapColor))` vira
`BoxWithConstraints(Modifier.fillMaxSize().background(JohnTheme.colors.pageGap))`;
na linha 325, `.background(Color.White)` vira `.background(PdfPageBackground)` (e apague o import de `androidx.compose.ui.graphics.Color` se ele ficar sem uso).

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.*"`
Expected: PASS, incluindo `ReaderContentTest` (que não referencia `PageGapColor`; se referenciar, troque por `JohnTheme.colors.pageGap` dentro de um `JohnPdfTheme { }`).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/ app/src/test/java/com/johngabie/johnpdf/ui/theme/
git commit -m "feat(tema): JohnColors (vao, icone de PDF, indicador de aba) e PdfPageBackground

PageGapColor #BDBDBD fixo deixa de existir: o vao vira papel de tema
(surfaceContainerHigh no claro, surfaceContainerLowest no escuro).
A pagina do PDF continua branca nos dois modos, por PdfPageBackground.

Indicador de aba ganha papel proprio: nenhum container claro do M3 chega
a 3:1 contra uma NavigationBar clara."
```

---

### Task 5: Home sem `tonalElevation` e sem lilás

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt` (linhas 156, 169–183, 213–220, 270–286)
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` (helper de cores da `NavigationBar`)
- Modify: `app/src/test/java/com/johngabie/johnpdf/ui/theme/DarkThemeTest.kt`

**Interfaces:**
- Consumes: `JohnTheme.colors`, `JohnTheme.barColor`, `JohnTheme.dialogColor` da Task 4.
- Produces: `@Composable fun johnNavigationBarItemColors(): NavigationBarItemColors` em `Theme.kt`.

- [ ] **Step 1: Escrever o teste que falha**

Acrescente a `DarkThemeTest.kt`:

```kotlin
    /**
     * O indicador da aba vem de JohnColors, não do secondaryContainer do M3
     * (que dá 1,12:1 contra a barra no claro).
     */
    @Test fun navigation_bar_item_colors_use_the_tab_indicator_role() {
        val (claro, escuro) = bothThemes {
            // Compor o helper aqui garante que ele existe e não estoura na composição.
            johnNavigationBarItemColors()
            JohnTheme.colors.tabIndicator to MaterialTheme.colorScheme.onSurfaceVariant
        }
        assertEquals(Color(0xFF365A6C), claro.first)
        assertEquals(Color(0xFF5A7385), escuro.first)
        assertNotEquals(claro.first, claro.second)
        assertNotEquals(escuro.first, escuro.second)
    }
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.DarkThemeTest"`
Expected: **falha de compilação** — `Unresolved reference: johnNavigationBarItemColors`.

- [ ] **Step 3: Implementar o helper e aplicar na Home**

Em `Theme.kt`, acrescente os imports `import androidx.compose.material3.NavigationBarItemColors` e `import androidx.compose.material3.NavigationBarItemDefaults`, e depois do `object JohnTheme`:

```kotlin
/**
 * Cores dos itens da NavigationBar. Sem isto, o M3 usa `secondaryContainer`
 * como indicador — 1,12:1 contra a barra no claro, ou seja, invisível.
 */
@Composable
fun johnNavigationBarItemColors(): NavigationBarItemColors = NavigationBarItemDefaults.colors(
    selectedIconColor = JohnTheme.colors.onTabIndicator,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    indicatorColor = JohnTheme.colors.tabIndicator,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

Em `HomeScreen.kt`:

`HomeHeader` (linha 156) — `Surface(tonalElevation = 3.dp)` vira:

```kotlin
    Surface(color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
```

`HomeBottomBar` (linhas 169–183) — `NavigationBar { ... }` vira:

```kotlin
    NavigationBar(
        containerColor = JohnTheme.barColor,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        NavigationBarItem(
            selected = tab == HomeTab.RECENTS,
            onClick = { onSelectTab(HomeTab.RECENTS) },
            icon = { Text("🕘", fontSize = 28.sp) },
            label = { Text("Recentes", style = MaterialTheme.typography.labelMedium) },
            colors = johnNavigationBarItemColors(),
        )
        NavigationBarItem(
            selected = tab == HomeTab.ALL,
            onClick = { onSelectTab(HomeTab.ALL) },
            icon = { Text("📚", fontSize = 28.sp) },
            label = { Text("Todos os PDFs", style = MaterialTheme.typography.labelMedium) },
            colors = johnNavigationBarItemColors(),
        )
    }
```

`AllPdfsTab` — o `OutlinedTextField` da busca (linhas 213–220) ganha cores explícitas, senão a borda e o cursor saem do baseline:

```kotlin
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("🔍 Buscar pelo nome…", style = MaterialTheme.typography.bodyLarge) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = JohnTheme.dialogColor,
                unfocusedContainerColor = JohnTheme.dialogColor,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 64.dp),
        )
```

`PdfCard` (linhas 270–286) — `tonalElevation = 2.dp` sai; o cartão passa a ter cor e borda explícitas, e o emoji de PDF ganha a cor do papel:

```kotlin
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📄", fontSize = 32.sp, color = JohnTheme.colors.pdfIcon, modifier = Modifier.padding(end = 16.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
```

Imports novos em `HomeScreen.kt`: `androidx.compose.foundation.BorderStroke`, `androidx.compose.material3.OutlinedTextFieldDefaults`, `com.johngabie.johnpdf.ui.theme.JohnTheme`, `com.johngabie.johnpdf.ui.theme.johnNavigationBarItemColors`. Remova o import de `androidx.compose.ui.unit.dp` só se ele ficar sem uso (não ficará).

> O emoji `📄` é colorido por fonte em vários aparelhos e pode ignorar `color`; isso é aceitável aqui porque a troca dos emojis por ícones vetoriais é tarefa do plano D1. O importante é que a cor **existe** como papel e o `JohnColors.pdfIcon` já está medido para quando o ícone entrar.

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.DarkThemeTest" --tests "com.johngabie.johnpdf.ui.home.*"`
Expected: PASS em `DarkThemeTest` e em `HomeContentTest`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/ app/src/test/java/com/johngabie/johnpdf/ui/theme/DarkThemeTest.kt
git commit -m "refactor(home): cores explicitas no header, NavigationBar, busca e cartoes

Sai o tonalElevation (que no escuro tinge a superficie com primary e
reintroduz cor vazada) e entram papeis explicitos. O indicador da aba
passa a usar JohnColors.tabIndicator: 6,40:1 no claro, 3,28:1 no escuro,
contra 1,12:1 do secondaryContainer padrao."
```

---

### Task 6: Leitor e diálogos — barras, desabilitado legível e fundo de diálogo

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` (helper de botão)
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt` (linhas 205, 233)
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt` (linhas 28, 37, 49, 54)
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/common/BigButton.kt`
- Modify: `app/src/test/java/com/johngabie/johnpdf/ui/theme/DarkThemeTest.kt`

**Interfaces:**
- Consumes: `JohnTheme.barColor`, `JohnTheme.dialogColor` da Task 4.
- Produces: `@Composable fun readableButtonColors(): ButtonColors` em `Theme.kt` — alpha 0,60 no conteúdo desabilitado (o padrão M3 é 0,38 e dá 2,3:1 no claro).

- [ ] **Step 1: Escrever o teste que falha**

Acrescente a `DarkThemeTest.kt` (e o import `import androidx.compose.ui.graphics.compositeOver`):

```kotlin
    /**
     * "Anterior" desabilitado na página 1 precisa ser lido, não adivinhado.
     * O 38% do M3 dá 2,32:1 no claro; 60% dá 3,96:1.
     */
    @Test fun disabled_button_label_is_readable_in_both_modes() {
        val (claro, escuro) = bothThemes {
            val cores = readableButtonColors()
            cores.disabledContainerColor to cores.disabledContentColor
        }
        listOf(
            Triple("claro", claro, Color(0xFFEBEFF2)),   // surfaceContainer do claro
            Triple("escuro", escuro, Color(0xFF1B2124)), // surfaceContainer do escuro
        ).forEach { (nome, cores, barra) ->
            val fundo = cores.first.compositeOver(barra)
            val rotulo = cores.second.compositeOver(fundo)
            val r = contrast(rotulo, fundo)
            assertTrue("[$nome] desabilitado = %.2f:1, mínimo 3:1".format(r), r >= 3.0)
        }
    }
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.DarkThemeTest"`
Expected: **falha de compilação** — `Unresolved reference: readableButtonColors`.

- [ ] **Step 3: Implementar e aplicar**

Em `Theme.kt`, acrescente os imports `import androidx.compose.material3.ButtonColors` e `import androidx.compose.material3.ButtonDefaults`, e depois de `johnNavigationBarItemColors`:

```kotlin
/**
 * O M3 pinta rótulo desabilitado com `onSurface` a 38% — 2,32:1 no claro, que o
 * público-alvo (pessoas idosas) não consegue ler. Com 60% dá 3,96:1 no claro e
 * 4,44:1 no escuro. Usar em Anterior/Próxima e no "Abrir" do diálogo de senha.
 */
@Composable
fun readableButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
)
```

Em `BigButton.kt`, passe `colors = readableButtonColors()` ao `Button` e acrescente o import
`import com.johngabie.johnpdf.ui.theme.readableButtonColors`.

Em `ReaderScreen.kt`, `ReaderTopBar` (linha 205) e `ReaderBottomBar` (linha 233): troque
`Surface(tonalElevation = 3.dp) {` por, respectivamente,

```kotlin
    Surface(color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
```

```kotlin
    Surface(color = JohnTheme.barColor, contentColor = MaterialTheme.colorScheme.onSurface) {
```

Em `Dialogs.kt`, os três `AlertDialog` ganham cores explícitas — é aqui que o lilás aparecia com mais força:

```kotlin
@Composable
fun ErrorDialog(error: AppError, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JohnTheme.dialogColor,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        iconContentColor = MaterialTheme.colorScheme.error,
        text = { Text(error.message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { BigButton("OK", onDismiss) },
    )
}
```

O mesmo trio `containerColor`/`titleContentColor`/`textContentColor` em `ConfirmDialog` (linha 37) e em `PasswordDialog` (linha 49). No `OutlinedTextField` da senha (linha 54), acrescente:

```kotlin
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
```

Imports novos em `Dialogs.kt`: `androidx.compose.material3.OutlinedTextFieldDefaults` e `com.johngabie.johnpdf.ui.theme.JohnTheme`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.*"`
Expected: PASS, incluindo `DialogsTest` e `ReaderContentTest`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/ app/src/test/java/com/johngabie/johnpdf/ui/theme/DarkThemeTest.kt
git commit -m "refactor(leitor,dialogos): containerColor explicito e desabilitado legivel

Os tres AlertDialog e as duas barras do leitor saem do baseline (fundo
lilas) e passam a usar surfaceContainerHigh/surfaceContainer.
Rotulo desabilitado sobe de alpha 0,38 (2,32:1) para 0,60 (3,96:1)."
```

---

### Task 7: Barras do sistema e tema de plataforma

Com esquema escuro existindo, `SystemBarStyle.light(...)` (conserto F6 do ciclo anterior) vira o bug: ícones escuros sobre uma `surface` `#0F1417` somem. E o tema fixo `Theme.Material.Light.NoActionBar` do manifesto produz clarão branco no arranque a frio em modo escuro.

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/MainActivity.kt` (linhas 29–35)
- Create: `app/src/main/res/values/colors.xml`, `app/src/main/res/values-night/colors.xml`
- Create: `app/src/main/res/values/themes.xml`, `app/src/main/res/values-v29/themes.xml`
- Modify: `app/src/main/AndroidManifest.xml` (linha 19)
- Modify: `app/src/test/java/com/johngabie/johnpdf/MainActivitySmokeTest.kt`

**Interfaces:**
- Consumes: os hex de `surface` dos dois esquemas (`#F7FAFC` e `#0F1417`).
- Produces: `@style/Theme.JohnPdf` e `@color/window_background`.

- [ ] **Step 1: Escrever o teste que falha**

Acrescente dois imports ao topo de `app/src/test/java/com/johngabie/johnpdf/MainActivitySmokeTest.kt`:

```kotlin
import org.junit.Assert.assertEquals
import org.robolectric.annotation.Config
```

E, ao final da classe (o arquivo já tem `@get:Rule val rule = createAndroidComposeRule<MainActivity>()`, que é o que este teste usa):

```kotlin
    /**
     * Arranque a frio em modo escuro: a Activity sobe e o windowBackground do tema
     * de plataforma é a surface escura — é ele que evita o clarão branco antes de o
     * Compose desenhar.
     */
    @Test
    @Config(qualifiers = "+night")
    fun activity_starts_in_night_mode() {
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
        assertEquals(0xFF0F1417.toInt(), rule.activity.getColor(R.color.window_background))
    }
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.MainActivitySmokeTest"`
Expected: **falha de compilação** — `Unresolved reference: color` (o módulo ainda não tem nenhum recurso de cor, então `R.color` não existe).

- [ ] **Step 3: Criar recursos, trocar manifesto e barras**

`app/src/main/res/values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Casa com colorScheme.surface do esquema claro (Theme.kt). -->
    <color name="window_background">#F7FAFC</color>
</resources>
```

`app/src/main/res/values-night/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Casa com colorScheme.surface do esquema escuro (Theme.kt). -->
    <color name="window_background">#0F1417</color>
</resources>
```

`app/src/main/res/values/themes.xml` (base para API 24–28, que não tem `DayNight` no `DeviceDefault`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.JohnPdf" parent="@android:style/Theme.DeviceDefault.Light.NoActionBar">
        <item name="android:windowActionBar">false</item>
        <item name="android:windowNoTitle">true</item>
        <!-- Sem isto, o arranque a frio em modo escuro pisca branco. -->
        <item name="android:windowBackground">@color/window_background</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
```

`app/src/main/res/values-v29/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- DayNight existe a partir da API 29; o sistema escolhe entre values/ e values-night/. -->
    <style name="Theme.JohnPdf" parent="@android:style/Theme.DeviceDefault.DayNight">
        <item name="android:windowActionBar">false</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowBackground">@color/window_background</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
```

No `AndroidManifest.xml`, linha 19: `android:theme="@android:style/Theme.Material.Light.NoActionBar"` → `android:theme="@style/Theme.JohnPdf"`.

Em `MainActivity.kt`, substitua o comentário e a chamada (linhas 29–35) por:

```kotlin
        // auto(): ícones escuros no claro e claros no escuro, acompanhando o sistema.
        // Era light() enquanto não existia darkColorScheme (conserto F6); agora que
        // existe, light() seria o bug — ícones escuros sobre surface #0F1417 somem.
        // Os dois argumentos são os scrims usados quando falta contraste; TRANSPARENT
        // nos dois mantém o edge-to-edge real.
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

Contraste dos ícones do sistema: brancos `#FFFFFF` sobre `surface` escura `#0F1417` = **18,54:1**; pretos `#000000` sobre `surface` clara `#F7FAFC` = **20,03:1**.

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.MainActivitySmokeTest"`
Expected: PASS nos dois testes (claro e `+night`).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/ app/src/main/AndroidManifest.xml app/src/main/java/com/johngabie/johnpdf/MainActivity.kt app/src/test/java/com/johngabie/johnpdf/MainActivitySmokeTest.kt
git commit -m "feat(tema): barras do sistema em auto e windowBackground DayNight

SystemBarStyle.light -> auto (so e seguro agora que existe darkColorScheme:
antes os icones sumiam no escuro, conserto F6). Tema de plataforma proprio
com window_background casando com surface de cada esquema, acabando com o
clarao branco no arranque a frio em modo escuro."
```

---

### Task 8: Guarda contra hardcode de cor e `tonalElevation`

Sem esta trava, o lilás volta na primeira tela nova: basta alguém escrever `Surface(tonalElevation = 2.dp)` ou `Color(0xFF…)` fora do tema.

**Files:**
- Create: `app/src/test/java/com/johngabie/johnpdf/ui/theme/NoHardcodedColorTest.kt`

**Interfaces:**
- Consumes: nada de Kotlin — lê os fontes do disco. O diretório de trabalho dos testes unitários é o módulo (`app/`), então `File("src/main/java")` resolve.
- Produces: nada consumido por outras tarefas.

- [ ] **Step 1: Escrever o teste**

```kotlin
package com.johngabie.johnpdf.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Cor fica num lugar só. O lilás vazado do baseline M3 voltou toda vez que um
 * componente escolheu a própria cor — ou deixou o M3 escolher por `tonalElevation`.
 */
class NoHardcodedColorTest {

    private val fontes: List<File> =
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()

    /** Theme.kt é a única fonte de cor; MuPdfEngine usa android.graphics.Color. */
    private val isentos = setOf("Theme.kt", "MuPdfEngine.kt")

    @Test fun sources_exist() {
        assertTrue(
            "nenhum .kt encontrado — diretório de trabalho inesperado: ${File(".").absolutePath}",
            fontes.size > 5,
        )
    }

    @Test fun no_literal_compose_color_outside_theme() {
        val literal = Regex("""Color\(0x|Color\.(White|Black|Red|Blue|Gray|LightGray|DarkGray|Green|Yellow|Cyan|Magenta)""")
        val ofensores = fontes.filter { it.name !in isentos }
            .flatMap { f ->
                f.readLines().mapIndexedNotNull { i, linha ->
                    if (literal.containsMatchIn(linha)) "${f.name}:${i + 1}: ${linha.trim()}" else null
                }
            }
        assertTrue(
            "cor literal fora de Theme.kt — use um papel do tema:\n" + ofensores.joinToString("\n"),
            ofensores.isEmpty(),
        )
    }

    @Test fun no_tonal_elevation_anywhere() {
        val ofensores = fontes.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, linha ->
                if (linha.contains("tonalElevation")) "${f.name}:${i + 1}: ${linha.trim()}" else null
            }
        }
        assertTrue(
            "tonalElevation tinge a superfície com primary e reintroduz cor vazada;\n" +
                "use surfaceContainer* explícito:\n" + ofensores.joinToString("\n"),
            ofensores.isEmpty(),
        )
    }
}
```

- [ ] **Step 2: Rodar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.johngabie.johnpdf.ui.theme.NoHardcodedColorTest"`
Expected: PASS se as Tasks 4–6 estiverem completas. Se falhar, a mensagem traz arquivo:linha — corrija trocando pelo papel de tema correspondente (a tabela da seção "Estrutura de arquivos" diz qual papel cada lugar usa) e rode de novo. `PdfPageBackground` vive em `Theme.kt`, então não aparece como ofensor.

- [ ] **Step 3: Rodar a bateria inteira**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS em todos os testes JVM do módulo. Anote o total no commit.

- [ ] **Step 4: Compilar o APK de debug**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/johngabie/johnpdf/ui/theme/NoHardcodedColorTest.kt
git commit -m "test(tema): trava cor literal e tonalElevation fora de Theme.kt

Varre src/main/java e falha com arquivo:linha. Sem isto o lilas volta na
primeira tela nova. Isentos: Theme.kt (fonte de cor) e MuPdfEngine.kt
(android.graphics.Color no bitmap da pagina)."
```

---

### Task 9: Verificação visual no aparelho, claro e escuro

Contraste medido não prova que a tela ficou boa: prova que ela é legível. Esta tarefa é o olho humano — e é ela que fecha o pedido original do usuário ("me retorne uma paleta que goste").

**Files:**
- Modify: `docs/e2e/2026-09-28-checklist.md`
- Create: capturas em `docs/e2e/img/` (nomes abaixo)

**Interfaces:**
- Consumes: APK das Tasks 1–8.
- Produces: 8 capturas e o checklist preenchido. Nada de código.

- [ ] **Step 1: Instalar e preparar**

```bash
./gradlew :app:installDebug
adb shell cmd uimode night no
```

- [ ] **Step 2: Capturar os 4 cenários do modo claro**

Abra o app e capture (`adb exec-out screencap -p > docs/e2e/img/blue-lagoon-claro-01-home.png`, e assim por diante):

1. `blue-lagoon-claro-01-home.png` — Home/Recentes: header, cartões e **barra inferior sem nenhum lilás**; a aba selecionada tem pílula azul-ardósia com rótulo branco.
2. `blue-lagoon-claro-02-busca.png` — aba "Todos os PDFs" com texto digitado: borda, placeholder e cursor em azul do tema.
3. `blue-lagoon-claro-03-dialogo-senha.png` — diálogo de senha com "Senha incorreta, tente de novo": fundo cinza-azulado (não lilás), mensagem em vermelho, "Abrir" desabilitado **legível** com o campo vazio.
4. `blue-lagoon-claro-04-leitor.png` — leitor: página branca, vão claro, barras sem tinta.

- [ ] **Step 3: Capturar os 4 cenários do modo escuro**

```bash
adb shell cmd uimode night yes
```

5. `blue-lagoon-escuro-05-home.png` — nenhuma superfície branca fora da página do PDF; ícones da barra de status **claros e visíveis**.
6. `blue-lagoon-escuro-06-aba.png` — indicador da aba distinguível do fundo da barra (é o par medido em 3,28:1; se no aparelho ele sumir, é evidência real e deve ser anotada).
7. `blue-lagoon-escuro-07-dialogo.png` — "Remover da lista?" com fundo `#262B2F`, nem preto puro nem lilás.
8. `blue-lagoon-escuro-08-leitor.png` — página **branca** emoldurada por vão quase preto; "Anterior" desabilitado na página 1 ainda dá para ler.

Feche o app pela tela de recentes e reabra com o sistema em escuro: **não pode piscar branco** (é o `window_background` da Task 7). Anote o resultado; se piscar, o tema do manifesto não está sendo aplicado.

- [ ] **Step 4: Restaurar o aparelho e escrever o checklist**

```bash
adb shell cmd uimode night auto
```

Em `docs/e2e/2026-09-28-checklist.md`, acrescente ao final uma seção `## Paleta Blue Lagoon (claro/escuro) — 2026-09-29` com os 8 cenários acima, cada um com PASS/FAIL, o nome do arquivo da captura e uma linha de observação. Qualquer FAIL vira uma linha explícita de "o que se viu", não "ajustar depois".

- [ ] **Step 5: Commit**

```bash
git add docs/e2e/
git commit -m "test(e2e): capturas da paleta Blue Lagoon nos dois modos

8 cenarios no aparelho (4 claro, 4 escuro): home, busca, dialogo de senha
com erro, leitor, indicador de aba, arranque a frio sem clarao branco."
```

---

## Notas de risco

1. **O indicador de aba no escuro tem folga pequena** (3,28:1 contra o mínimo de 3:1). Se o cenário 6 da Task 9 mostrar que ele some no aparelho real, clareie `DarkJohnColors.tabIndicator` até `#5E7A8C` (3,60:1) — o rótulo branco dentro cai para 4,53:1, ainda acima de 4,5:1. Rode `tools/contrast.py` antes de mexer no Kotlin.
2. **Desfazer o conserto F6** (`SystemBarStyle.light` → `auto`) só é seguro porque agora existe `darkColorScheme`. O cenário 5 da Task 9 é obrigatório antes de dar a tarefa por pronta.
3. **A página branca no escuro ofusca.** É o comportamento pedido pelo usuário; o vão quase preto (19,27:1) é o que melhor emoldura a página. Quem quiser a página escura precisa do "modo noturno do PDF", fora do escopo da v1.
4. **Conflito com o plano D1 e com o modo imersivo**: os três mexem em `ReaderScreen.kt` e `HomeScreen.kt`. Ordem sugerida: esta paleta (cores) → D1 (estrutura, ícones, tipografia). Se a D1 entrar antes, `MinTouchTarget` vira `PrimaryTouchTarget = 56.dp` e os emojis viram ícones vetoriais — nesse caso, aplique `JohnTheme.colors.pdfIcon` ao `Icon` em vez de ao `Text("📄")`.
5. **`tonalElevation` removido muda também o modo claro** — as barras ficam lisas em vez de levemente tingidas. É desejado pela D1, mas é uma mudança visível que sai "de carona" aqui.
