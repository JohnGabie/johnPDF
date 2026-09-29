# Spec técnica — Vendorizar Material Symbols Rounded em `ui/icons/JohnIcons.kt`

- **Data:** 2026-09-29
- **Status:** aguardando revisão
- **Origem:** `docs/superpowers/specs/2026-09-29-design-d1-proposta.md` §B (opção 3) e tabela de substituições (linhas 49–64)
- **Escopo:** criar `app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt`, o `NOTICE` da licença, o gerador em `tools/icons/` e o teste de sanidade. **Não** altera `Theme.kt`, telas ou diálogos — as trocas de emoji por ícone são tarefas seguintes que apenas consomem este arquivo.
- **Não-objetivos:** ligar R8 (opção 4 do D1), adicionar `material-icons-extended`, trocar tipografia/cores, remover `BigButton`.

---

## 1. Decisão e justificativa

Vendorizar os SVGs do repositório [`google/material-design-icons`](https://github.com/google/material-design-icons) (Apache 2.0), estilo **Material Symbols Rounded, weight 400, grade 0, optical size 24**, como `ImageVector` estáticos em um único arquivo Kotlin.

| Critério | Resultado |
|---|---|
| Dependência nova no Gradle | **Nenhuma** |
| Impacto no APK | ~20–25 KB de `.dex` (22 vetores, majoritariamente strings) — desprezível frente aos 18,9 MB atuais |
| Risco para o MuPDF | Nenhum (sem mudança de build, sem minificação) |
| Consistência visual | Um único estilo (Symbols Rounded), em vez de misturar `Icons.Rounded.*` do core com Symbols |
| Acoplamento a `material3` | Nenhum — sobrevive ao `material3 1.4+`, que deixa de trazer `material-icons-core` |

---

## 2. Inventário de ícones

> **Achado em relação ao D1:** a tabela das linhas 49–64 fala em "~15 ícones", mas ao expandir as células (algumas listam dois ícones) o total é **20 nomes distintos + 2 variantes preenchidas = 22 `ImageVector`**. O número não muda a decisão (o custo continua desprezível), mas a spec adota 22 para não deixar lacuna na hora de implementar.

### 2.1 Núcleo — obrigatórios para eliminar todos os emojis/pseudo-ícones de hoje

Ocorrências atuais confirmadas no código:
`HomeScreen.kt:162` (📂), `:173` (🕘), `:179` (📚), `:216` (🔍), `:279` (📄);
`ReaderScreen.kt:211` (← Voltar), `:240` (🔒/🔓), `:243` (⬆), `:244` (⬇);
`Dialogs.kt:74` (abc/123).

| # | `val` em `JohnIcons` | Nome upstream | Substitui | Onde |
|---|---|---|---|---|
| 1 | `FolderOpen` | `folder_open` | 📂 "Abrir" | header da Home |
| 2 | `History` | `history` | 🕘 Recentes (não selecionado) | bottom nav |
| 3 | `HistoryFilled` | `history` (fill 1) | 🕘 Recentes (selecionado) | bottom nav |
| 4 | `LibraryBooks` | `library_books` | 📚 Todos os PDFs (não selecionado) | bottom nav |
| 5 | `LibraryBooksFilled` | `library_books` (fill 1) | 📚 Todos os PDFs (selecionado) | bottom nav |
| 6 | `PictureAsPdf` | `picture_as_pdf` | 📄 | leading do `ListItem` |
| 7 | `Search` | `search` | 🔍 no placeholder | `leadingIcon` da busca |
| 8 | `Close` | `close` | — (novo) | `trailingIcon` da busca, limpa o texto |
| 9 | `ArrowBack` | `arrow_back` | "← Voltar" | top bar do leitor (**auto-mirrored**) |
| 10 | `KeyboardArrowUp` | `keyboard_arrow_up` | ⬆ Anterior | barra do leitor |
| 11 | `KeyboardArrowDown` | `keyboard_arrow_down` | ⬇ Próxima | barra do leitor |
| 12 | `ScreenRotation` | `screen_rotation` | 🔓 "Gira sozinha" | ação do top bar (§C do D1) |
| 13 | `ScreenLockRotation` | `screen_lock_rotation` | 🔒 "Travada" | ação do top bar (§C do D1) |
| 14 | `Keyboard` | `keyboard` | "abc  Usar letras" | diálogo de senha |
| 15 | `Dialpad` | `dialpad` | "123  Usar números" | diálogo de senha |

### 2.2 Complemento — exigidos pelas telas propostas em §D do D1

| # | `val` | Nome upstream | Uso |
|---|---|---|---|
| 16 | `Visibility` | `visibility` | mostrar senha (novo) |
| 17 | `VisibilityOff` | `visibility_off` | ocultar senha (novo) |
| 18 | `Error` | `error` | ícone do `AlertDialog` de erro |
| 19 | `Folder` | `folder` | ícone da tela de permissão |
| 20 | `SearchOff` | `search_off` | estado vazio "Nenhum PDF com esse nome." |
| 21 | `Lock` | `lock` | título do diálogo "PDF protegido" |
| 22 | `Delete` | `delete` | título do diálogo "Remover da lista?" |

**Se o objetivo for mesmo ficar em ~15**, os cortes possíveis, em ordem de menor prejuízo: `Folder` (reusar `FolderOpen` na permissão), `VisibilityOff` (usar só `Visibility` com `stateDescription`), `SearchOff` (reusar `Search`). Não cortar `Close`, `Error`, `Lock` nem `Delete` — cada um é o único ícone do seu contexto.

---

## 3. Estrutura do arquivo

Caminho: `app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt`

### 3.1 Layout

```
┌─ cabeçalho de licença (Apache 2.0, ~12 linhas)  ← §5
├─ package + imports
├─ private fun symbol(d, autoMirror)              ← construtor único, 12 linhas
├─ object JohnIcons {
│    val FolderOpen by lazy { symbol(D_FOLDER_OPEN) }
│    … 22 vals, ordem alfabética …
│    val All: List<ImageVector>                   ← só para o teste de sanidade
│  }
└─ private const val D_* = "…"                    ← 22 strings `d`, cópia byte-a-byte do SVG
```

### 3.2 Três decisões de implementação (e por quê)

**(a) Guardar o `d` do SVG como `String` e parsear com `addPathNodes`, em vez de transcrever `moveTo`/`curveTo`.**
`androidx.compose.ui.graphics.vector.addPathNodes(String): List<PathNode>` é API pública do `compose-ui` (o mesmo parser que o `vectorResource` usa). Ganhos: a string é **idêntica ao arquivo upstream**, então `diff` contra o SVG vendorizado é trivial e o risco (5) do D1 ("conversão manual dos SVGs pode errar paths") desaparece; o arquivo cai de ~900 para ~250 linhas. Custo: o parse acontece uma vez por ícone, no primeiro acesso (dezenas de microssegundos), amortizado pelo `by lazy`.
*Verificação obrigatória na implementação:* confirmar que `addPathNodes` resolve no compile contra a BOM `2025.05.00`. Se não resolver, o fallback é o gerador emitir os nós explícitos (§6.2) — o resto da spec não muda.

**(b) `viewportWidth/Height = 960f` + `group(translationY = 960f)`.**
Os SVGs de Material Symbols usam `viewBox="0 -960 960 960"` (origem no canto **inferior** esquerdo, Y negativo). `ImageVector` não tem offset de viewBox, então o deslocamento é aplicado por um `group`. A alternativa — reescrever as coordenadas no gerador — quebraria a comparação byte-a-byte com o upstream. O `group` custa um nó a mais na árvore vetorial, irrelevante para 24dp.

**(c) `by lazy` em vez do padrão de backing field do AOSP.**
`private var _x: ImageVector? = null` + getter existe nos ícones gerados pelo Google por razões de tooling. Aqui `by lazy` dá a mesma memoização, é thread-safe por padrão e economiza ~3 linhas por ícone.

### 3.3 Esqueleto

```kotlin
/*
 * Ícones derivados de Material Symbols (Rounded, weight 400, grade 0, optical size 24)
 * de https://github.com/google/material-design-icons
 *
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ARQUIVO GERADO — não editar à mão.
 * Regenerar com: python tools/icons/generate_johnicons.py
 * SVGs de origem: tools/icons/svg/
 */
package com.johngabie.johnpdf.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/**
 * Constrói um Material Symbol 24dp a partir do atributo `d` do SVG.
 *
 * O viewBox dos Symbols é `0 -960 960 960`; o [group] com `translationY = 960f`
 * traz o desenho para o quadrante positivo sem mexer nas coordenadas originais.
 *
 * A cor preta é um marcador: `Icon(...)` sempre aplica um tint
 * (`LocalContentColor` por padrão), então nenhum ícone chega à tela em preto fixo.
 */
private fun symbol(d: String, autoMirror: Boolean = false): ImageVector =
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = autoMirror,
    ).apply {
        group(translationY = 960f) {
            addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
        }
    }.build()
```

---

## 4. Exemplo de um ícone

### 4.1 Caso simples — `folder_open`

```kotlin
object JohnIcons {

    /** `folder_open` — ação "Abrir PDF" no header da Home. */
    val FolderOpen: ImageVector by lazy { symbol(D_FOLDER_OPEN) }

    // …
}

// Copiado do atributo `d` de tools/icons/svg/folder_open.svg — não editar.
private const val D_FOLDER_OPEN =
    "M160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h207q16 0 30.5 6t25.5 17l57 57h280q33 0 56.5 23.5T840-640H447l-80-80H160v480l96-320h684L837-217q-8 27-30 42t-50 15H160Zm84-80h516l72-240H316l-72 240Zm0 0 72-240-72 240Zm-84-320v-80 80Z"
```

> ⚠️ **A string acima é ilustrativa da forma, não uma fonte confiável.** Ela foi escrita para esta spec e **não** foi conferida contra o arquivo upstream. **Regra da spec: nenhuma coordenada é digitada ou copiada de uma spec/exemplo** — a string final vem sempre do gerador (§6), que lê o SVG vendorizado em `tools/icons/svg/`. O teste de idempotência (§7.2) e a conferência visual (§7.3) existem exatamente para impedir que um valor como este chegue ao código.

### 4.2 Caso com auto-mirror — `arrow_back`

RTL não é idioma-alvo do johnPDF, mas `autoMirror` é grátis e evita um bug silencioso se o app for aberto com o sistema em árabe/hebraico:

```kotlin
/** `arrow_back` — botão Voltar do leitor. Espelha em RTL. */
val ArrowBack: ImageVector by lazy { symbol(D_ARROW_BACK, autoMirror = true) }
```

`autoMirror = true` apenas em `ArrowBack`. `KeyboardArrowUp`/`Down` são verticais e `ScreenRotation` é simétrico o bastante — não espelhar.

### 4.3 Caso com variante preenchida — `history`

A bottom nav usa contorno quando não selecionada e preenchido quando selecionada (§D do D1). São dois SVGs diferentes (`history_24px.svg` e `history_fill1_24px.svg`), logo dois `val`:

```kotlin
/** `history` — aba "Recentes", estado não selecionado. */
val History: ImageVector by lazy { symbol(D_HISTORY) }

/** `history` (fill 1) — aba "Recentes", estado selecionado. */
val HistoryFilled: ImageVector by lazy { symbol(D_HISTORY_FILLED) }
```

### 4.4 Lista de verificação

Somente para o teste de §7.1 — não usar em código de produção:

```kotlin
    /** Todos os ícones, para o teste de sanidade. */
    val All: List<ImageVector>
        get() = listOf(
            ArrowBack, Close, Delete, Dialpad, Error, Folder, FolderOpen,
            History, HistoryFilled, Keyboard, KeyboardArrowDown, KeyboardArrowUp,
            LibraryBooks, LibraryBooksFilled, Lock, PictureAsPdf, ScreenLockRotation,
            ScreenRotation, Search, SearchOff, Visibility, VisibilityOff,
        )
```

---

## 5. Licença

O johnPDF é **AGPL-3.0** (exigência do MuPDF). Apache 2.0 é compatível em mão única: código Apache 2.0 pode ser incorporado a uma obra AGPL-3.0, desde que os avisos de copyright e licença sejam preservados. É exatamente o que esta spec exige.

Três obrigações, todas atendidas:

1. **Cabeçalho no arquivo** — o bloco de comentário de §3.3, no topo de `JohnIcons.kt`, com o texto padrão de licença Apache 2.0 e a atribuição ao Google LLC.
2. **`NOTICE` na raiz do repositório** (arquivo novo):

   ```
   johnPDF
   Copyright 2026 Joao Gabriel
   Licensed under the GNU Affero General Public License v3.0 (see LICENSE).

   ---

   Este produto inclui ícones derivados de Material Symbols,
   de https://github.com/google/material-design-icons

   Copyright 2023 Google LLC
   Licensed under the Apache License, Version 2.0.
   Uma cópia da licença está em third_party/material-design-icons/LICENSE.

   Arquivos afetados:
     app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt
     tools/icons/svg/*.svg
   ```

3. **Cópia íntegra da licença** em `third_party/material-design-icons/LICENSE` (texto Apache 2.0 sem modificação, baixado junto com os SVGs pelo gerador).

**Não é necessário:** tela "Sobre"/créditos no app (o `NOTICE` no repositório distribuído junto com o APK já satisfaz o AGPL e o Apache 2.0, e o D1 não pede tela nova). Se uma tela de créditos aparecer depois, ela deve incluir o conteúdo do `NOTICE`.

---

## 6. Gerador (`tools/icons/`)

Segue o padrão já existente no repositório (`tools/make_test_pdfs.py`, `tools/make_layout_pdfs.py`): Python, sem dependências além da stdlib.

```
tools/icons/
  icons.txt                  # lista de ícones: <nome_upstream> <NomeKotlin> [fill] [automirror]
  fetch_svgs.py              # baixa os SVGs uma vez → svg/ (roda só quando a lista muda)
  generate_johnicons.py      # svg/*.svg → JohnIcons.kt (offline, idempotente)
  svg/                       # SVGs vendorizados, versionados no git
```

### 6.1 `fetch_svgs.py`

Baixa de `https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/<nome>/materialsymbolsrounded/<nome>_24px.svg` (variante preenchida: `<nome>_fill1_24px.svg`).

Requisitos:
- **Falhar alto** em HTTP != 200 — um 404 silencioso produziria um ícone vazio que só apareceria em runtime. Se a URL acima não responder, o fallback documentado é `https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/<nome>/default/24px.svg`; registrar no topo do script qual foi usada.
- Fixar o commit upstream (`?ref=<sha>` ou clonar em `--depth 1` e anotar o SHA em `tools/icons/UPSTREAM.txt`) — reprodutibilidade.
- Baixar também o `LICENSE` do repo para `third_party/material-design-icons/LICENSE`.
- Rodar **uma vez**; os SVGs entram no git. O build nunca acessa a rede.

### 6.2 `generate_johnicons.py`

1. Lê cada SVG de `svg/`.
2. Valida: exatamente um `<path>`, `viewBox == "0 -960 960 960"`, `width == height == "24px"`. Qualquer desvio → erro com o nome do arquivo (alguns Symbols têm `fill-rule="evenodd"`; nesse caso o gerador emite `pathFillType = PathFillType.EvenOdd` no `addPath`).
3. Extrai o `d` **sem normalizar** (nem espaços, nem casas decimais).
4. Emite `JohnIcons.kt` com cabeçalho, `symbol()`, os `val` em ordem alfabética e as `const val D_*`.
5. É **idempotente**: rodar duas vezes produz bytes idênticos (CI depende disso — §7.2).

*Fallback se `addPathNodes` não for utilizável (§3.2a):* o passo 4 passa a emitir os nós explícitos (`moveTo(...)`, `curveToRelative(...)`), gerados a partir do mesmo `d` — o arquivo cresce para ~900 linhas, mas continua 100% derivado do SVG e a API pública de `JohnIcons` não muda.

---

## 7. Testes

### 7.1 `JohnIconsTest` (novo, JVM puro — sem Robolectric)

`app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsTest.kt`

| Teste | Asserção |
|---|---|
| `todos_os_icones_tem_24dp_e_viewport_960` | para cada item de `JohnIcons.All`: `defaultWidth == 24.dp`, `defaultHeight == 24.dp`, `viewportWidth == 960f`, `viewportHeight == 960f` |
| `nenhum_icone_esta_vazio` | a raiz de cada `ImageVector` contém ao menos um `VectorPath` com `pathData` não vazio — pega SVG baixado errado (404) e `d` truncado |
| `inventario_completo` | `JohnIcons.All.size == 22` — falha se alguém adicionar um `val` e esquecer de `All`, o que furaria os dois testes acima |
| `arrow_back_espelha_em_rtl` | `JohnIcons.ArrowBack.autoMirror` é `true` e os demais são `false` |

### 7.2 Idempotência (manual ou CI)

```bash
python tools/icons/generate_johnicons.py && git diff --exit-code -- app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt
```

Garante que o `.kt` versionado corresponde aos SVGs versionados — é o que substitui a revisão visual path-a-path.

### 7.3 Conferência visual (mitiga o risco 5 do D1)

O teste de §7.1 prova que o vetor **existe**, não que desenha a coisa certa. Antes de fechar a tarefa, renderizar os 22 ícones uma vez em uma grade e comparar com [fonts.google.com/icons](https://fonts.google.com/icons) (filtro: Rounded). Duas opções:

- **Barata:** um `@Preview` temporário em `app/src/debug/`, exigindo `debugImplementation("androidx.compose.ui:ui-tooling")` — **dependência nova, só no debug, fora do APK release**. Remover o preview e a dependência depois da conferência, ou mantê-los se forem úteis para o resto do refresh de design.
- **Sem dependência nova:** um teste Robolectric que renderiza a grade e salva um PNG via `captureToImage()`, inspecionado à mão uma vez.

Registrar o PNG conferido em `docs/e2e/img/` como evidência.

### 7.4 Regressão

Nenhum teste existente muda nesta tarefa — `JohnIcons.kt` ainda não é consumido por ninguém. As atualizações de `ReaderContentTest`, `HomeContentTest`, `DialogsTest` e `ThemeTest` listadas em §F do D1 pertencem às tarefas de tela.

---

## 8. Como usar nos componentes

Todos os exemplos usam `Icon(...)`, que aplica tint automático a partir de `LocalContentColor` — nunca passar cor dentro do `ImageVector`.

### 8.1 Ícone com ação e rótulo (header da Home)

```kotlin
import com.johngabie.johnpdf.ui.icons.JohnIcons

FilledTonalButton(onClick = onOpenPicker) {
    Icon(JohnIcons.FolderOpen, contentDescription = null)   // o texto ao lado já descreve
    Spacer(Modifier.width(8.dp))
    Text("Abrir PDF")
}
```

`contentDescription = null` quando há texto visível ao lado: com descrição, o TalkBack leria "Abrir PDF, Abrir PDF".

### 8.2 Bottom nav com troca contorno ↔ preenchido

```kotlin
NavigationBarItem(
    selected = selected,
    onClick = onClick,
    icon = {
        Icon(
            imageVector = if (selected) JohnIcons.HistoryFilled else JohnIcons.History,
            contentDescription = null,   // o label do item é o rótulo acessível
        )
    },
    label = { Text("Recentes") },
)
```

### 8.3 Ícone decorativo em lista

```kotlin
ListItem(
    headlineContent = { Text(pdf.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    supportingContent = { Text(pdf.originLabel) },
    leadingContent = {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFDECEA)),   // §D do D1
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = JohnIcons.PictureAsPdf,
                contentDescription = null,        // decorativo: o nome do arquivo já é lido
                tint = Color(0xFFC62828),
                modifier = Modifier.size(24.dp),
            )
        }
    },
)
```

### 8.4 Ícone que É o único alvo de toque (busca)

```kotlin
TextField(
    value = query,
    onValueChange = onQueryChange,
    placeholder = { Text("Buscar PDFs") },
    leadingIcon = { Icon(JohnIcons.Search, contentDescription = null) },
    trailingIcon = {
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(JohnIcons.Close, contentDescription = "Limpar busca")
            }
        }
    },
    singleLine = true,
)
```

Aqui o `contentDescription` é **obrigatório e em pt-BR**: é a única forma de identificar o botão.

### 8.5 Toggle de rotação (§C do D1)

O único caso em que o ícone muda de significado, não só de estilo:

```kotlin
IconToggleButton(
    checked = rotationLocked,
    onCheckedChange = onToggleRotation,
    colors = IconButtonDefaults.iconToggleButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        checkedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        checkedContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    modifier = Modifier.semantics {
        stateDescription = if (rotationLocked) "Travada" else "Automática"
    },
) {
    Icon(
        imageVector = if (rotationLocked) JohnIcons.ScreenLockRotation else JohnIcons.ScreenRotation,
        contentDescription = "Travar rotação da tela",   // fixo: descreve a AÇÃO, não o estado
    )
}
```

`contentDescription` constante + `stateDescription` variável é o que permite o teste de §F do D1:
`onNodeWithContentDescription("Travar rotação da tela").assertIsOff()` / `.assertIsOn()`.

### 8.6 Ícone de diálogo

```kotlin
AlertDialog(
    icon = { Icon(JohnIcons.Delete, contentDescription = null) },  // o título já diz tudo
    title = { Text("Remover da lista?") },
    text = { Text("O arquivo continua no celular.") },
    confirmButton = { Button(onClick = onConfirm) { Text("Remover") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    onDismissRequest = onDismiss,
)
```

### 8.7 Regras de `contentDescription`

| Situação | `contentDescription` |
|---|---|
| Ícone dentro de botão com texto visível | `null` |
| Ícone em `NavigationBarItem` com `label` | `null` |
| Ícone decorativo em `ListItem`/`AlertDialog` | `null` |
| `IconButton` / `IconToggleButton` sem texto | **obrigatório**, em pt-BR, descrevendo a ação |
| Ícone de estado vazio (ilustração) | `null` (o título abaixo é o conteúdo) |

TalkBack está fora do escopo da v1 (§1 da spec de 2026-09-28), mas essas descrições também são o seletor dos testes Compose — não são opcionais.

---

## 9. Build

**Nenhuma mudança em `build.gradle.kts` nem em `gradle/libs.versions.toml`.** `ImageVector`, `addPathNodes`, `group` e `Icon` já vêm de `androidx.compose.ui:ui` e `material3`, presentes via BOM `2025.05.00`.

Única exceção possível: `debugImplementation(libs.androidx.compose.ui.tooling)` se a conferência visual usar `@Preview` (§7.3) — debug-only, zero impacto no APK release.

### 9.1 Passos

```bash
# 1. (uma vez) baixar SVGs + LICENSE e fixar o commit upstream
python tools/icons/fetch_svgs.py

# 2. gerar o arquivo Kotlin
python tools/icons/generate_johnicons.py

# 3. compilar
./gradlew :app:assembleDebug

# 4. testes de sanidade + suíte existente (não deve haver regressão)
./gradlew :app:testDebugUnitTest

# 5. conferir que o gerador é idempotente
python tools/icons/generate_johnicons.py
git diff --exit-code -- app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt

# 6. medir o APK release antes/depois (baseline do D1: arm64 = 18,9 MB)
./gradlew :app:assembleRelease
ls -l app/build/outputs/apk/release/
```

No Windows, trocar `python` por `py` se necessário; o `gradlew.bat` é o wrapper equivalente.

### 9.2 Critério de aceite

- [ ] `JohnIcons.kt` existe, compila, tem cabeçalho Apache 2.0 e marcação "ARQUIVO GERADO".
- [ ] `NOTICE` na raiz + `third_party/material-design-icons/LICENSE` íntegro.
- [ ] `tools/icons/svg/` versionado, com `UPSTREAM.txt` registrando o commit de origem.
- [ ] `./gradlew :app:testDebugUnitTest` verde, incluindo `JohnIconsTest`.
- [ ] Gerador idempotente (`git diff --exit-code` limpo).
- [ ] Os 22 ícones conferidos visualmente contra fonts.google.com/icons, evidência em `docs/e2e/img/`.
- [ ] APK release arm64 dentro de **19,0 MB** (ou seja, delta < 100 KB).
- [ ] Nenhuma dependência nova em release; `isMinifyEnabled = false` inalterado.

### 9.3 Commit sugerido

```
feat(ui): vendoriza 22 Material Symbols Rounded como ImageVector

Cria ui/icons/JohnIcons.kt a partir dos SVGs de google/material-design-icons
(Apache 2.0), gerado por tools/icons/generate_johnicons.py. Sem dependência
nova; substitui a opção material-icons-extended (+8-12 MB) avaliada no D1 §B.

Nenhuma tela alterada ainda — as trocas de emoji vêm nas tarefas seguintes.
```

---

## 10. Riscos

| Risco | Mitigação |
|---|---|
| `addPathNodes` não ser resolvível/estável na BOM atual | Verificar no primeiro compile; fallback documentado em §6.2 (nós explícitos), sem mudar a API de `JohnIcons` |
| Layout de URL do repositório upstream mudar → SVG vazio | `fetch_svgs.py` falha alto em != 200; `nenhum_icone_esta_vazio` pega o resto |
| `viewBox` diferente de `0 -960 960 960` em algum ícone | Gerador valida e recusa; o teste de viewport confirma |
| Ícone desenhar errado apesar de todos os testes passarem | Conferência visual única de §7.3 — é o único passo que não dá para automatizar barato |
| O arquivo virar um monólito de 250+ linhas difícil de revisar | É gerado: revisa-se o gerador e o `git diff` dos SVGs, não o `.kt`. Se passar de ~40 ícones, dividir por domínio (`JohnIcons.Nav`, `JohnIcons.Reader`) |

---

## 11. Referências

- `docs/superpowers/specs/2026-09-29-design-d1-proposta.md` §B (decisão), linhas 49–64 (tabela), §C (rotação), §D (uso por tela), §F (impacto e testes)
- `docs/superpowers/specs/2026-09-28-johnpdf-leitor-android-design.md` §1–§4 (escopo, AGPL, público)
- https://github.com/google/material-design-icons — Apache 2.0
- https://fonts.google.com/icons — conferência visual (estilo Rounded)
