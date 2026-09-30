# D1 — Layouts por tela (Home, Leitor, Diálogos) — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refatorar os layouts das três superfícies do johnPDF — Home (o "Abrir PDF" fica no header, **sem FAB**), Leitor (barra inferior única de 72dp com só o indicador de página + `PagerRail` flutuante de 112dp com as setas empilhadas à direita) e Diálogos (`TextButton` dismissivo + `Button` filled confirmatório, alinhados à direita) — sem alterar nenhum comportamento do produto.

**Architecture:** Três arquivos de UI carregam a mudança. `HomeScreen.kt` troca a faixa `Surface`+`Row` por um `TopAppBar` M3 de 64dp com `FilledTonalButton` nas `actions`, a `NavigationBar` ganha ícones vetoriais (contorno ↔ preenchido), os `PdfCard` viram `ListItem` de 72dp numa lista plana com divisores recuados, e o `OutlinedTextField` vira uma busca em pílula de 56dp. `ReaderScreen.kt` move a rotação para as `actions` do `TopAppBar` (`IconToggleButton` + tooltip + Snackbar), reduz a barra inferior a uma faixa de 72dp que só mostra "Página X de Y", e extrai as setas ▲/▼ para um `PagerRail` — um `Surface` de 56×112dp ancorado em `BottomEnd` **dentro** do conteúdo do `Scaffold`, que entra e sai na mesma animação do modo imersivo. `Dialogs.kt` passa a usar os slots `icon`/`title`/`text` do `AlertDialog` M3 com uma única ação principal. Todas as cores vêm de papéis do `ColorScheme`; nenhum hexadecimal novo entra no código.

**Tech Stack:** Kotlin 2.1.21, Jetpack Compose (BOM 2025.05.00, `material3` 1.3.2), JUnit 4 + Robolectric 4.14.1 (`sdk=35`) + `compose-ui-test-junit4`, Gradle 8.11.1 / AGP 8.10.1, adb (ou mobile-mcp) para o E2E.

**Spec:** `docs/superpowers/specs/2026-09-29-design-d1-layouts.md`

## Pré-requisitos (gate — checados na Task 1)

Este plano implementa **só a seção §5, §6 e §7 da spec** (layout por tela). Três dependências pertencem a specs irmãs e precisam estar no código **antes** da Task 2:

| Dependência | Spec dona | Como este plano a consome |
|---|---|---|
| `ui/icons/JohnIcons.kt` com os 22 `ImageVector` | `docs/superpowers/specs/2026-09-29-icones-vendorizados-spec.md` | `JohnIcons.FolderOpen`, `History`, `HistoryFilled`, `LibraryBooks`, `LibraryBooksFilled`, `PictureAsPdf`, `Search`, `SearchOff`, `Close`, `ArrowBack`, `KeyboardArrowUp`, `KeyboardArrowDown`, `ScreenRotation`, `ScreenLockRotation`, `Keyboard`, `Dialpad`, `Visibility`, `VisibilityOff`, `Error`, `Lock`, `Delete`, `Folder` |
| Tipografia (16sp de piso, `titleLarge` 22, `titleMedium` 18, `bodyLarge` 18, `bodyMedium`/`labelLarge`/`labelMedium` 16) e a fonte Atkinson | `2026-09-29-design-d1-sistema-visual.md` + `2026-09-29-fonte-atkinson-hyperlegible-next.md` | só chama `MaterialTheme.typography.*`; **não** edita `Type`/`Typography` |
| Paleta completa clara/escura (`surfaceContainer*`, `secondaryContainer`, `outlineVariant`, `errorContainer`) | `2026-09-29-dark-mode-ui.md` | só chama `MaterialTheme.colorScheme.*` |

Se `JohnIcons.kt` não existir, **pare na Task 1** e execute antes o plano da spec de ícones — nenhuma task deste plano compila sem ele. Tipografia e paleta **não** bloqueiam: com os valores antigos o layout compila e os testes passam; só o resultado visual fica intermediário.

## Global Constraints

- Todo texto de UI em **pt-BR**, exatamente como escrito neste plano.
- Alvo de toque mínimo **48dp** (`MinTouchTarget`); ações frequentes/primárias **56dp** (`PrimaryTouchTarget`); linha de lista **72dp** (`ListItemHeight`) **inteira clicável**. Espaço mínimo entre alvos adjacentes: **8dp**.
- `PagerRail`: **56dp de largura × 112dp de altura**, `RoundedCornerShape(28.dp)`, `shadowElevation = 3.dp`.
- Barra inferior do leitor: faixa **única** de **72dp** + `navigationBarsPadding()`, contendo **só** o indicador "Página X de Y".
- **Nenhum hardcode de cor.** Toda cor vem de `MaterialTheme.colorScheme.*`. Exceção única e pré-existente: `Color.White` do papel do PDF e `Color.Transparent` de `ListItemDefaults.colors`. Proibido introduzir `Color(0xFF…)` em qualquer arquivo deste plano.
- **Nenhum FAB na Home.** "Abrir PDF" vive nas `actions` do `TopAppBar`; a `LazyColumn` usa `contentPadding = PaddingValues(vertical = 8.dp)` — sem padding inferior reservando espaço de FAB.
- `SnackbarHost` fica no `Scaffold` do leitor, com `SnackbarDuration.Short` obrigatório (o Snackbar cobre o `PagerRail` enquanto está na tela).
- Margem horizontal padrão de conteúdo: **16dp**; blocos centrados (permissão, estado vazio) usam **24dp** e `widthIn(max = 360.dp)`.
- Comportamento preservado, sem exceção: toque simples no PDF esconde/mostra header + barra + barras do sistema (commit `5de0feb`) e agora também o `PagerRail`; toque longo em Recentes confirma antes de remover; teclado numérico por padrão na senha; `testTag("password_field")`; `rememberSaveable` em `password`/`numeric`; rótulo `pageLabel` continua **"Página X de Y"** (`PageMath.kt` e `PageMathTest` **não** mudam).
- Mensagens de `AppError` continuam idênticas: `"Não foi possível abrir este arquivo."`, `"Este arquivo não está mais disponível."`, `"Sem espaço no celular para abrir este arquivo."`, `"Não foi possível mostrar esta página."`.
- Nenhuma dependência nova no Gradle. Nenhuma permissão nova.
- Testes: JUnit 4 + Robolectric (`sdk=35`), rodados com `./gradlew testDebugUnitTest`. Gradle com `-Xmx2g` (já em `gradle.properties`); não rodar dois builds ao mesmo tempo.
- Commits frequentes, um por task, em pt-BR, prefixo `feat(d1):` / `test(d1):` / `refactor(d1):`.

## Review Focus

1. **As setas no canto direito ficam visíveis e não são engolidas pela barra nem pelo Snackbar.** O `PagerRail` tem 112dp e a barra tem 72dp; se ele for parar dentro do `bottomBar` ou for posicionado sem o `padding` do `Scaffold`, metade dele sai da tela ou fica sob a barra de navegação do sistema. Esperado: o trilho fica inteiro na tela, encostado na borda direita, com as setas uma em cima da outra e a base **acima** da barra inferior. *(Teste: Task 10, `pager_rail_fica_acima_da_barra_no_canto_direito`, reconferido na Task 11 contra a faixa final.)*
2. **"Página X de Y" continua legível com números longos.** Um documento de 2000 páginas gera "Página 1000 de 2000"; centralizado numa faixa que tem um trilho de 56dp flutuando por cima do canto direito, o texto passa por baixo do trilho se faltar o `end = 88.dp`. Esperado: o rótulo termina antes de onde o trilho começa. *(Teste: Task 11, `rotulo_de_pagina_nao_passa_por_baixo_do_trilho`.)*
3. **A Home não tem FAB — nem um sobrando de refatoração parcial.** A decisão 3 do usuário revogou o FAB da proposta original; um `FloatingActionButton` esquecido no canto inferior direito duplicaria a ação e voltaria a exigir o `contentPadding` de 88dp que a spec removeu. Esperado: a única ação "Abrir PDF" no topo é a do header, e nada com esse rótulo flutua no canto inferior direito. *(Teste: Task 2, `home_nao_tem_botao_flutuante`.)*
4. **Ícones em cima da mesma linha de base.** Trocar emoji (`Text("🕘", fontSize = 28.sp)`) por `Icon` muda a métrica: um ícone de 24dp ao lado de outro de 28dp, ou um `IconButton` de 48dp ao lado de um `IconToggleButton` de 40dp, deixa a barra visivelmente torta. Esperado: na `NavigationBar` os dois ícones têm o mesmo topo e a mesma altura; no `TopAppBar` do leitor, voltar e rotação compartilham o mesmo centro vertical. *(Testes: Task 3, `icones_da_navigation_bar_ficam_alinhados`; Task 9, `icones_do_top_bar_do_leitor_ficam_alinhados`.)*
5. **Na página 1 a seta ▲ está desabilitada — e visivelmente desabilitada, não sumida nem cinza sólido.** Idem ▼ na última página e **ambas** num PDF de página única. Esperado: `enabled = false` no `IconButton` (o M3 já aplica `onSurface` a 38%), com o alvo de toque preservado. *(Teste: Task 10, `setas_desabilitam_na_primeira_e_na_ultima_pagina`.)*

---

## Estrutura de arquivos

```
app/src/main/java/com/johngabie/johnpdf/
├── ui/theme/Theme.kt              MODIFICA  MinTouchTarget 64→48; +PrimaryTouchTarget; +ListItemHeight; −PageGapColor
├── ui/icons/JohnIcons.kt          CONSOME   (criado pela spec de ícones — gate da Task 1)
├── ui/common/BigButton.kt         REMOVE    (Task 12, depois de zerar os usos)
├── ui/common/Dialogs.kt           REESCREVE ConfirmRemoveDialog · ErrorDialog · PasswordDialog
├── ui/home/HomeScreen.kt          REESCREVE HomeTopBar · HomeBottomBar · PdfListItem · SearchField · EmptyState · PermissionContent
├── ui/reader/ReaderScreen.kt      REESCREVE ReaderTopBar · RotationLockAction · ReaderBottomBar · PagerRail · fundo das páginas
└── ui/reader/PageMath.kt          NÃO MUDA  (pageLabel continua "Página X de Y")

app/src/test/java/com/johngabie/johnpdf/
├── ui/theme/ThemeTokensTest.kt    CRIA      trava os três tokens de alvo de toque
├── ui/common/DialogsTest.kt       ATUALIZA
├── ui/home/HomeContentTest.kt     ATUALIZA
├── ui/reader/ReaderContentTest.kt ATUALIZA
├── ui/theme/ThemeTest.kt          NÃO MUDA  (piso de sp pertence à spec de sistema visual)
└── MainActivitySmokeTest.kt       ATUALIZA  "Permitir" → "Permitir acesso"

docs/e2e/
├── 2026-09-29-d1-layouts.md       CRIA      re-execução E2E depois do refresh
├── img/d1/*.png                   CRIA      capturas da Home e dos diálogos
└── 2026-09-28-checklist.md        ATUALIZA  uma linha de ponteiro para o doc novo
```

**Divisão de responsabilidade dentro de `HomeScreen.kt` e `ReaderScreen.kt`:** os dois arquivos crescem (~290 → ~360 linhas e ~345 → ~430 linhas), o que ainda cabe de folga no padrão do repositório (um arquivo por tela, composables privados no mesmo arquivo, estabelecido em todo o v1). **Não** quebrar em arquivos novos: os composables desta refatoração mudam juntos, e a spec §2 lista `PdfListItem`/`SearchField`/`EmptyState` explicitamente dentro de `HomeScreen.kt` e `RotationLockAction`/`PagerRail` dentro de `ReaderScreen.kt`.

**`testTag` introduzidos** (necessários porque textos passam a se repetir na mesma tela e porque `contentDescription = null` torna ícones decorativos invisíveis para a árvore de semântica):

| Tag | Onde | Por quê |
|---|---|---|
| `open_pdf_header` | `FilledTonalButton` do `HomeTopBar` | "Abrir PDF" passa a existir 2× na tela (header + estado vazio) |
| `empty_state_action` | `TextButton` do `EmptyState` | idem, e permite afirmar a **ausência** da ação na busca sem resultado |
| `nav_icon_recents` / `nav_icon_all` | `Icon` dos `NavigationBarItem` | `contentDescription = null` por decisão de acessibilidade (§8.1) |
| `password_field` | campo de senha | **já existe**, preservar |

---

### Task 1: Tokens de alvo de toque + gate de pré-requisitos

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt:12`
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTokensTest.kt` (criar)

**Interfaces:**
- Consumes: nada.
- Produces: `com.johngabie.johnpdf.ui.theme.MinTouchTarget: Dp = 48.dp`, `PrimaryTouchTarget: Dp = 56.dp`, `ListItemHeight: Dp = 72.dp`. `PageGapColor` **continua existindo** nesta task (removido só na Task 12, quando `ReaderScreen.kt` parar de usá-lo).

- [ ] **Step 1: Checar o gate de pré-requisitos**

Rode, da raiz do repositório:

```bash
F=app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt
ls "$F" || echo "GATE: JohnIcons.kt não existe"
for n in FolderOpen History HistoryFilled LibraryBooks LibraryBooksFilled PictureAsPdf \
         Search SearchOff Close ArrowBack KeyboardArrowUp KeyboardArrowDown \
         ScreenRotation ScreenLockRotation Keyboard Dialpad Visibility VisibilityOff \
         Error Lock Delete Folder; do
  grep -q "val $n\b" "$F" || echo "GATE: falta JohnIcons.$n"
done
```

Esperado: o `ls` imprime o caminho e o laço **não imprime nada**. Qualquer linha começando com `GATE:` significa parar e reportar: o plano da spec `2026-09-29-icones-vendorizados-spec.md` precisa rodar antes. Não invente ícones, não adicione `material-icons-extended`, não substitua por emoji.

- [ ] **Step 2: Escrever o teste que falha**

Crie `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTokensTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Trava os alvos de toque da spec D1 §3.2: 48 / 56 / 72dp. */
class ThemeTokensTest {
    @Test fun min_touch_target_is_48dp() {
        assertEquals(48.dp, MinTouchTarget)
    }

    @Test fun primary_touch_target_is_56dp() {
        assertEquals(56.dp, PrimaryTouchTarget)
    }

    @Test fun list_item_height_is_72dp() {
        assertEquals(72.dp, ListItemHeight)
    }
}
```

- [ ] **Step 3: Rodar o teste e confirmar que falha**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.theme.ThemeTokensTest'`
Expected: FAIL na compilação — `Unresolved reference: PrimaryTouchTarget` e `Unresolved reference: ListItemHeight`.

- [ ] **Step 4: Implementar**

Em `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt`, troque a linha `val MinTouchTarget = 64.dp` por:

```kotlin
/** Piso de toque para qualquer elemento tocável (M3/WCAG 2.5.5). */
val MinTouchTarget = 48.dp

/** Ações frequentes ou primárias: setas do leitor, "Permitir acesso", botão principal de diálogo. */
val PrimaryTouchTarget = 56.dp

/** Altura mínima da linha de lista, inteira clicável. */
val ListItemHeight = 72.dp
```

Deixe `PageGapColor` como está — `ReaderScreen.kt` ainda o usa até a Task 12.

- [ ] **Step 5: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest`
Expected: PASS na suíte inteira. `BigButton` e o `TextButton` do leitor passam de 64dp para 48dp de altura mínima — nenhum teste atual afirma altura, então nada quebra.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTokensTest.kt
git commit -m "feat(d1): alvos de toque de 48/56/72dp em Theme.kt"
```

---

### Task 2: Home — `TopAppBar` com "Abrir PDF" (e nenhum FAB)

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt:154-165` (`HomeHeader` → `HomeTopBar`) e `:130` (o `topBar` do `Scaffold`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt`

**Interfaces:**
- Consumes: `MinTouchTarget` (Task 1); `JohnIcons.FolderOpen`.
- Produces: `private fun HomeTopBar(onOpenPicker: () -> Unit)` em `HomeScreen.kt`; `testTag("open_pdf_header")`; rótulo exato **"Abrir PDF"**. `HomeContent` mantém a assinatura atual, inalterada.

- [ ] **Step 1: Escrever os testes que falham**

Em `HomeContentTest.kt`, **substitua** o teste `header_and_bottom_menu_are_visible` por estes dois e acrescente os imports:

```kotlin
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
```

```kotlin
    @Test fun header_abre_o_seletor_de_arquivos() {
        show(HomeUiState())
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
        rule.onNodeWithTag("open_pdf_header").assertIsDisplayed().performClick()
        assertEquals(listOf("picker"), events)
    }

    /** Review Focus 3: a decisão 3 do usuário revogou o FAB; nada com esse rótulo pode flutuar embaixo. */
    @Test fun home_nao_tem_botao_flutuante() {
        show(HomeUiState())
        val root = rule.onRoot().getBoundsInRoot()
        val header = rule.onNodeWithTag("open_pdf_header").getBoundsInRoot()
        assertTrue("o botão saiu do topo da tela: $header", header.bottom < root.height / 4f)

        val total = rule.onAllNodesWithText("Abrir PDF").fetchSemanticsNodes().size
        repeat(total) { i ->
            val b = rule.onAllNodesWithText("Abrir PDF")[i].getBoundsInRoot()
            val noCantoInferiorDireito = b.top > root.height * 0.75f && b.right > root.width * 0.5f
            assertFalse("há um 'Abrir PDF' flutuando no canto inferior direito: $b", noCantoInferiorDireito)
        }
    }
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: FAIL — `open_pdf_header` não existe na árvore de semântica ("Reason: Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'open_pdf_header')").

- [ ] **Step 3: Implementar**

Em `HomeScreen.kt`, troque `HomeHeader` por `HomeTopBar`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onOpenPicker: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "johnPDF",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            FilledTonalButton(
                onClick = onOpenPicker,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .heightIn(min = MinTouchTarget)
                    .testTag("open_pdf_header"),
                contentPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Icon(JohnIcons.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir PDF", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}
```

No `Scaffold` de `HomeContent`, troque `topBar = { HomeHeader(onOpenPicker) }` por `topBar = { HomeTopBar(onOpenPicker) }`.

Ajuste de imports em `HomeScreen.kt`: **adicione**

```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.testTag
import com.johngabie.johnpdf.ui.icons.JohnIcons
import com.johngabie.johnpdf.ui.theme.MinTouchTarget
```

e **remova** `import androidx.compose.foundation.layout.statusBarsPadding` (o `TopAppBar` já cuida do inset da barra de status). Mantenha `Surface`, `Row` e `BigButton` importados — ainda são usados pelo resto do arquivo até as tasks seguintes.

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: PASS, inclusive `home_nao_tem_botao_flutuante`.

- [ ] **Step 5: Rodar a suíte inteira**

Run: `./gradlew testDebugUnitTest`
Expected: PASS. `MainActivitySmokeTest.shows_app_name` continua verde ("johnPDF" segue na tela).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt
git commit -m "feat(d1): TopAppBar da Home com FilledTonalButton 'Abrir PDF', sem FAB"
```

---

### Task 3: Home — `NavigationBar` com ícones vetoriais

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt:167-183` (`HomeBottomBar`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt`

**Interfaces:**
- Consumes: `JohnIcons.History`, `HistoryFilled`, `LibraryBooks`, `LibraryBooksFilled`.
- Produces: `private fun HomeBottomBar(tab: HomeTab, onSelectTab: (HomeTab) -> Unit)`; tags `nav_icon_recents` e `nav_icon_all`; rótulos exatos "Recentes" e "Todos os PDFs".

- [ ] **Step 1: Escrever os testes que falham**

Acrescente a `HomeContentTest.kt`:

```kotlin
    @Test fun barra_inferior_troca_de_aba_com_rotulos_sempre_visiveis() {
        show(HomeUiState())
        rule.onNodeWithText("Recentes").assertIsDisplayed()
        rule.onNodeWithText("Todos os PDFs").assertIsDisplayed()
        rule.onNodeWithText("Todos os PDFs").performClick()
        assertEquals(listOf("tab:ALL"), events)
    }

    /** Review Focus 4: trocar emoji por Icon não pode deixar a barra torta. */
    @Test fun icones_da_navigation_bar_ficam_alinhados() {
        show(HomeUiState())
        val recentes = rule.onNodeWithTag("nav_icon_recents", useUnmergedTree = true).getBoundsInRoot()
        val todos = rule.onNodeWithTag("nav_icon_all", useUnmergedTree = true).getBoundsInRoot()
        assertEquals("topos diferentes: $recentes vs $todos", recentes.top.value, todos.top.value, 0.5f)
        assertEquals("alturas diferentes: $recentes vs $todos", recentes.height.value, todos.height.value, 0.5f)
    }
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: FAIL em `icones_da_navigation_bar_ficam_alinhados` — as tags não existem (os ícones ainda são `Text("🕘")`).

- [ ] **Step 3: Implementar**

Substitua `HomeBottomBar` inteiro:

```kotlin
@Composable
private fun HomeBottomBar(tab: HomeTab, onSelectTab: (HomeTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        NavigationBarItem(
            selected = tab == HomeTab.RECENTS,
            onClick = { onSelectTab(HomeTab.RECENTS) },
            icon = {
                Icon(
                    if (tab == HomeTab.RECENTS) JohnIcons.HistoryFilled else JohnIcons.History,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).testTag("nav_icon_recents"),
                )
            },
            label = { Text("Recentes", style = MaterialTheme.typography.labelMedium) },
            alwaysShowLabel = true,
        )
        NavigationBarItem(
            selected = tab == HomeTab.ALL,
            onClick = { onSelectTab(HomeTab.ALL) },
            icon = {
                Icon(
                    if (tab == HomeTab.ALL) JohnIcons.LibraryBooksFilled else JohnIcons.LibraryBooks,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).testTag("nav_icon_all"),
                )
            },
            label = { Text("Todos os PDFs", style = MaterialTheme.typography.labelMedium) },
            alwaysShowLabel = true,
        )
    }
}
```

`contentDescription = null` é intencional: o rótulo textual ao lado já nomeia o item, e descrever o ícone duplicaria o anúncio do TalkBack (spec §8.1). Remova o import `androidx.compose.ui.unit.sp` se nenhum outro ponto do arquivo o usar ainda (o `PdfCard` usa — então só remova na Task 5).

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: PASS.

- [ ] **Step 5: Rodar a suíte inteira**

Run: `./gradlew testDebugUnitTest`
Expected: PASS — `MainActivitySmokeTest.home_starts_on_recents_and_switches_to_all` clica em "Todos os PDFs" por texto, que continua existindo.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt
git commit -m "feat(d1): NavigationBar da Home com ícones contorno/preenchido"
```

---

### Task 4: Home — `EmptyState` e `PermissionContent`

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt` (`PermissionContent`, `CenteredMessage` → `EmptyState`, e os call sites em `RecentsTab`/`AllPdfsTab`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt`, `app/src/test/java/com/johngabie/johnpdf/MainActivitySmokeTest.kt`

**Interfaces:**
- Consumes: `MinTouchTarget`, `PrimaryTouchTarget` (Task 1); `JohnIcons.History`, `FolderOpen`, `SearchOff`, `Folder`.
- Produces:
  - `private fun EmptyState(icon: ImageVector, title: String, message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null)`
  - `private fun PermissionContent(onRequestPermission: () -> Unit)`
  - `RecentsTab` passa a ter a assinatura `(items: List<RecentItem>, nowMillis: Long, onOpen: (RecentItem) -> Unit, onLongPress: (RecentItem) -> Unit, onOpenPicker: () -> Unit)` e `AllPdfsTab` ganha um último parâmetro `onOpenPicker: () -> Unit` — a Task 5 e a Task 6 consomem essas assinaturas.
  - Textos exatos: "Nenhum PDF aberto ainda" / "Os PDFs que você abrir vão aparecer aqui." · "Nenhum PDF no celular" / "Nenhum PDF encontrado no celular." · "Nada encontrado" / "Nenhum PDF com esse nome." · "Permitir acesso".
  - Tag `empty_state_action`.

- [ ] **Step 1: Escrever os testes que falham**

Em `HomeContentTest.kt`, **substitua** `empty_recents_shows_hint`, `all_tab_without_access_asks_permission` e `all_tab_empty_with_query_says_no_match` por:

```kotlin
    @Test fun recentes_vazio_mostra_estado_com_acao() {
        show(HomeUiState())
        rule.onNodeWithText("Nenhum PDF aberto ainda").assertIsDisplayed()
        rule.onNodeWithText("Os PDFs que você abrir vão aparecer aqui.").assertIsDisplayed()
        rule.onNodeWithTag("empty_state_action").performClick()
        assertEquals(listOf("picker"), events)
    }

    @Test fun biblioteca_vazia_mostra_estado_com_acao() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true))
        rule.onNodeWithText("Nenhum PDF no celular").assertIsDisplayed()
        rule.onNodeWithText("Nenhum PDF encontrado no celular.").assertIsDisplayed()
    }

    @Test fun busca_sem_resultado_nao_oferece_acao() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true, query = "zzz"))
        rule.onNodeWithText("Nada encontrado").assertIsDisplayed()
        rule.onNodeWithText("Nenhum PDF com esse nome.").assertIsDisplayed()
        rule.onNodeWithTag("empty_state_action").assertDoesNotExist()
    }

    @Test fun aba_todos_sem_permissao_pede_acesso() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = false))
        rule.onNodeWithText("Para mostrar os PDFs do celular, o johnPDF precisa de permissão.").assertIsDisplayed()
        rule.onNodeWithText("1. Toque em Permitir acesso\n2. Ative a opção do johnPDF\n3. Volte para o app").assertIsDisplayed()
        rule.onNodeWithText("Permitir acesso").performClick()
        assertEquals(listOf("permission"), events)
    }
```

E em `MainActivitySmokeTest.kt`, no teste `home_starts_on_recents_and_switches_to_all`, troque as duas asserções de texto:

```kotlin
        rule.onNodeWithText("Nenhum PDF aberto ainda").assertIsDisplayed()
        rule.onNodeWithText("Todos os PDFs").performClick()
        rule.onNodeWithText("Permitir acesso").assertIsDisplayed()
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest' --tests 'com.johngabie.johnpdf.MainActivitySmokeTest'`
Expected: FAIL — "Nenhum PDF aberto ainda" e "Permitir acesso" não existem na árvore.

- [ ] **Step 3: Implementar**

Substitua `CenteredMessage` por `EmptyState` e reescreva `PermissionContent`:

```kotlin
@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = onAction,
                modifier = Modifier.heightIn(min = MinTouchTarget).testTag("empty_state_action"),
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PermissionContent(onRequestPermission: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                JohnIcons.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Para mostrar os PDFs do celular, o johnPDF precisa de permissão.",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "1. Toque em Permitir acesso\n2. Ative a opção do johnPDF\n3. Volte para o app",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = PrimaryTouchTarget),
        ) {
            Text("Permitir acesso", style = MaterialTheme.typography.labelLarge)
        }
    }
}
```

O `verticalScroll` é **novo e necessário**: a `fontScale` 2.0 o bloco estoura a altura em paisagem (spec §5.5).

Agora ligue os call sites. Em `HomeContent`, passe `onOpenPicker` para as duas abas:

```kotlin
            when (state.tab) {
                HomeTab.RECENTS -> RecentsTab(
                    items = state.recents,
                    nowMillis = nowMillis,
                    onOpen = onOpenRecent,
                    onLongPress = { pendingRemoval = it },
                    onOpenPicker = onOpenPicker,
                )
                HomeTab.ALL ->
                    if (!state.hasFilesAccess) PermissionContent(onRequestPermission)
                    else AllPdfsTab(state.filteredPdfs, state.query, state.loadingAll, nowMillis, onQueryChange, onOpenPdf, onOpenPicker)
            }
```

Em `RecentsTab`, acrescente o parâmetro e troque a mensagem vazia:

```kotlin
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
            icon = JohnIcons.History,
            title = "Nenhum PDF aberto ainda",
            message = "Os PDFs que você abrir vão aparecer aqui.",
            actionLabel = "Abrir PDF",
            onAction = onOpenPicker,
        )
        return
    }
    // … LazyColumn com PdfCard, inalterado nesta task (reescrito na Task 5)
```

Em `AllPdfsTab`, acrescente `onOpenPicker: () -> Unit` como último parâmetro e troque o ramo `pdfs.isEmpty()`:

```kotlin
            pdfs.isEmpty() ->
                if (query.isBlank()) EmptyState(
                    icon = JohnIcons.FolderOpen,
                    title = "Nenhum PDF no celular",
                    message = "Nenhum PDF encontrado no celular.",
                    actionLabel = "Abrir PDF",
                    onAction = onOpenPicker,
                ) else EmptyState(
                    icon = JohnIcons.SearchOff,
                    title = "Nada encontrado",
                    message = "Nenhum PDF com esse nome.",
                )
```

Imports a **adicionar** em `HomeScreen.kt`:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.johngabie.johnpdf.ui.theme.PrimaryTouchTarget
```

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest' --tests 'com.johngabie.johnpdf.MainActivitySmokeTest'`
Expected: PASS. `home_nao_tem_botao_flutuante` agora percorre **dois** nós "Abrir PDF" (header + estado vazio) e continua verde, porque o do estado vazio está centralizado, não no canto direito.

- [ ] **Step 5: Rodar a suíte inteira e commitar**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt app/src/test/java/com/johngabie/johnpdf/MainActivitySmokeTest.kt
git commit -m "feat(d1): estados vazios com ícone e tela de permissão rolável"
```

---

### Task 5: Home — `PdfListItem` de 72dp e listas planas

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt` (`PdfCard` → `PdfListItem`, `RecentsTab`, `AllPdfsTab`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt`

**Interfaces:**
- Consumes: `ListItemHeight` (Task 1); `JohnIcons.PictureAsPdf`; `EmptyState` (Task 4); `friendlyDate` (já existe).
- Produces: `private fun PdfListItem(name: String, subtitle: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null)`.

- [ ] **Step 1: Escrever o teste que falha**

Acrescente a `HomeContentTest.kt` (e mantenha `recent_card_shows_origin_and_date_and_opens` e `all_tab_lists_filtered_pdfs` como estão — eles continuam válidos):

```kotlin
    @Test fun linha_da_lista_tem_72dp_e_e_clicavel_inteira() {
        show(HomeUiState(recents = listOf(recent)))
        val root = rule.onRoot().getBoundsInRoot()
        val linha = rule.onNodeWithText("Fatura.pdf").assertIsDisplayed().getBoundsInRoot()
        assertTrue("linha com ${linha.height}, esperado >= 72dp", linha.height >= 72.dp)
        assertTrue("linha não ocupa a largura toda: ${linha.width} de ${root.width}", linha.width >= root.width - 1.dp)
    }
```

Import a acrescentar no teste: `import androidx.compose.ui.unit.dp`.

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: FAIL — o `PdfCard` de hoje tem `contentPadding` de 16dp na `LazyColumn`, então a largura da linha é `root.width - 32dp`, e a mensagem do assert mostra a diferença.

- [ ] **Step 3: Implementar**

Substitua `PdfCard` por `PdfListItem`:

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfListItem(
    name: String,
    subtitle: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    JohnIcons.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ListItemHeight)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}
```

`errorContainer`/`onErrorContainer` é o "vermelho suave de PDF" da spec §5.2: aproveita um papel que já existe em claro **e** escuro em vez de inventar um token fora do `ColorScheme` — respeitando a regra de "nenhum hardcode de cor".

Na `RecentsTab`, troque a `LazyColumn`:

```kotlin
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(items, key = { _, it -> it.path }) { index, item ->
            if (index > 0) HorizontalDivider(
                Modifier.padding(start = 72.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            PdfListItem(
                name = item.name,
                subtitle = "${item.origin.label} · ${friendlyDate(item.openedAt, nowMillis)}",
                onClick = { onOpen(item) },
                onLongClick = { onLongPress(item) },
            )
        }
    }
```

Na `AllPdfsTab`, o ramo `else` (mesma forma, **sem** `onLongClick` — toque longo só em Recentes, spec §5.2):

```kotlin
            else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                itemsIndexed(pdfs, key = { _, it -> it.path }) { index, pdf ->
                    if (index > 0) HorizontalDivider(
                        Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    PdfListItem(
                        name = pdf.name,
                        subtitle = "${pdf.origin.label} · ${friendlyDate(pdf.modifiedAt, nowMillis)}",
                        onClick = { onOpen(pdf) },
                    )
                }
            }
```

Sem `contentPadding` inferior extra: não há FAB (spec §1.1a).

Imports: **adicione** `androidx.compose.foundation.lazy.itemsIndexed`, `androidx.compose.material3.HorizontalDivider`, `androidx.compose.material3.ListItem`, `androidx.compose.material3.ListItemDefaults`, `androidx.compose.ui.graphics.Color`, `com.johngabie.johnpdf.ui.theme.ListItemHeight`. **Remova** `androidx.compose.foundation.lazy.items`, `androidx.compose.material3.Surface`, `androidx.compose.ui.unit.sp` e `androidx.compose.foundation.layout.Arrangement` **se** nenhum outro ponto do arquivo os usar (confira com `grep -n 'Surface\|Arrangement\|\.sp\b\|items(' app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt` — `Arrangement` ainda é usado pelo `EmptyState`/`PermissionContent`, então **fica**).

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: PASS, inclusive `linha_da_lista_tem_72dp_e_e_clicavel_inteira` e `long_press_asks_before_removing` (que ainda clica em "Sim" — o diálogo só muda na Task 7).

- [ ] **Step 5: Rodar a suíte inteira e commitar**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt
git commit -m "feat(d1): lista plana de PDFs com ListItem de 72dp e divisores recuados"
```

---

### Task 6: Home — busca em pílula com botão de limpar

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt` (`AllPdfsTab`, `OutlinedTextField` → `SearchField`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt`

**Interfaces:**
- Consumes: `JohnIcons.Search`, `JohnIcons.Close`.
- Produces: `private fun SearchField(query: String, onQueryChange: (String) -> Unit)`; `contentDescription = "Limpar busca"`; placeholder "Buscar PDFs".

- [ ] **Step 1: Escrever os testes que falham**

Acrescente a `HomeContentTest.kt`:

```kotlin
    @Test fun busca_vazia_nao_mostra_botao_de_limpar() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true))
        rule.onNodeWithText("Buscar PDFs").assertIsDisplayed()
        rule.onNodeWithContentDescription("Limpar busca").assertDoesNotExist()
    }

    @Test fun botao_de_limpar_zera_a_busca() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true, query = "bol"))
        rule.onNodeWithContentDescription("Limpar busca").performClick()
        assertEquals(listOf("query:"), events)
    }
```

Import a acrescentar: `import androidx.compose.ui.test.onNodeWithContentDescription`.

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: FAIL — o placeholder ainda é "🔍 Buscar pelo nome…" e não existe nó com a descrição "Limpar busca".

- [ ] **Step 3: Implementar**

Acrescente `SearchField` e troque o `OutlinedTextField` de `AllPdfsTab` por `SearchField(query, onQueryChange)`:

```kotlin
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar PDFs", style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(JohnIcons.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(MinTouchTarget)) {
                    Icon(JohnIcons.Close, contentDescription = "Limpar busca")
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(min = 56.dp),
    )
}
```

Imports a **adicionar**: `androidx.compose.foundation.text.KeyboardOptions`, `androidx.compose.material3.IconButton`, `androidx.compose.material3.TextField`, `androidx.compose.material3.TextFieldDefaults`, `androidx.compose.ui.text.input.ImeAction`. **Remova** `androidx.compose.material3.OutlinedTextField`.

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: PASS.

- [ ] **Step 5: Rodar a suíte inteira e commitar**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt
git commit -m "feat(d1): busca em pílula com ícone e botão de limpar"
```

---

### Task 7: Diálogos — confirmação de remoção e erro

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt:26-43`
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt` (call site de `ConfirmDialog`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/common/DialogsTest.kt`, `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt`

**Interfaces:**
- Consumes: `MinTouchTarget`, `PrimaryTouchTarget` (Task 1); `JohnIcons.Delete`, `JohnIcons.Error`.
- Produces: `fun ConfirmRemoveDialog(onConfirm: () -> Unit, onCancel: () -> Unit)` (substitui `ConfirmDialog(question, onYes, onNo)`, que **deixa de existir**) e `fun ErrorDialog(error: AppError, onDismiss: () -> Unit)` (mesma assinatura de hoje). Rótulos exatos: "Remover da lista?", "O arquivo continua no celular.", "Cancelar", "Remover", "OK".

- [ ] **Step 1: Escrever os testes que falham**

Em `DialogsTest.kt`, **substitua** `confirm_dialog_yes` por:

```kotlin
    @Test fun confirm_remove_dialog_confirma() {
        var removeu = false
        rule.setContent { JohnPdfTheme { ConfirmRemoveDialog(onConfirm = { removeu = true }, onCancel = {}) } }
        rule.onNodeWithText("Remover da lista?").assertIsDisplayed()
        rule.onNodeWithText("O arquivo continua no celular.").assertIsDisplayed()
        rule.onNodeWithText("Cancelar").assertIsDisplayed()
        rule.onNodeWithText("Remover").performClick()
        assertTrue(removeu)
    }

    /** M3: a ação dismissiva fica à esquerda da confirmatória, ambas na mesma linha, à direita. */
    @Test fun acoes_do_dialogo_ficam_alinhadas_a_direita() {
        rule.setContent { JohnPdfTheme { ConfirmRemoveDialog(onConfirm = {}, onCancel = {}) } }
        val cancelar = rule.onNodeWithText("Cancelar").getBoundsInRoot()
        val remover = rule.onNodeWithText("Remover").getBoundsInRoot()
        assertTrue("'Cancelar' ($cancelar) deveria estar à esquerda de 'Remover' ($remover)", cancelar.right <= remover.left)
        assertEquals(
            "botões em linhas diferentes: $cancelar vs $remover",
            cancelar.top.value + cancelar.height.value / 2f,
            remover.top.value + remover.height.value / 2f,
            1f,
        )
    }
```

Imports a acrescentar em `DialogsTest.kt`: `androidx.compose.ui.test.getBoundsInRoot`.

Em `HomeContentTest.kt`, no teste `long_press_asks_before_removing`, troque `rule.onNodeWithText("Sim")` por `rule.onNodeWithText("Remover")` e acrescente a asserção do corpo:

```kotlin
    @Test fun long_press_asks_before_removing() {
        show(HomeUiState(recents = listOf(recent)))
        rule.onNodeWithText("Fatura.pdf").performTouchInput { longClick() }
        rule.onNodeWithText("Remover da lista?").assertIsDisplayed()
        rule.onNodeWithText("O arquivo continua no celular.").assertIsDisplayed()
        rule.onNodeWithText("Remover").performClick()
        assertEquals(listOf("remove:Fatura.pdf"), events)
    }
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.common.DialogsTest' --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: FAIL na compilação — `Unresolved reference: ConfirmRemoveDialog`.

- [ ] **Step 3: Implementar**

Em `Dialogs.kt`, substitua `ErrorDialog` e `ConfirmDialog`:

```kotlin
@Composable
fun ConfirmRemoveDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(JohnIcons.Delete, contentDescription = null) },
        title = { Text("Remover da lista?", style = MaterialTheme.typography.titleLarge) },
        text = { Text("O arquivo continua no celular.", style = MaterialTheme.typography.bodyLarge) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.heightIn(min = PrimaryTouchTarget)) {
                Text("Remover", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
fun ErrorDialog(error: AppError, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(JohnIcons.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        text = {
            Text(error.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.heightIn(min = PrimaryTouchTarget)) {
                Text("OK", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
```

"Sim"/"Não" saem de cena de propósito: um rótulo que diz o que a ação faz ("Remover") é mais seguro numa ação destrutiva do que um "Sim" genérico. O `ErrorDialog` **não** tem título — a mensagem é o título (spec §7.2), e as três mensagens de `AppError` continuam idênticas.

Em `HomeScreen.kt`, troque o call site:

```kotlin
    pendingRemoval?.let { item ->
        ConfirmRemoveDialog(
            onConfirm = { onRemoveRecent(item); pendingRemoval = null },
            onCancel = { pendingRemoval = null },
        )
    }
```

e o import `com.johngabie.johnpdf.ui.common.ConfirmDialog` → `com.johngabie.johnpdf.ui.common.ConfirmRemoveDialog`.

Imports a **adicionar** em `Dialogs.kt`: `androidx.compose.material3.Button`, `androidx.compose.material3.Icon`, `androidx.compose.ui.text.style.TextAlign`, `com.johngabie.johnpdf.ui.icons.JohnIcons`, `com.johngabie.johnpdf.ui.theme.PrimaryTouchTarget`.

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.common.DialogsTest' --tests 'com.johngabie.johnpdf.ui.home.HomeContentTest'`
Expected: PASS. `error_dialog_is_shown` (Home) e `error_dialog_shows_message_and_ok_dismisses` continuam verdes — o texto do erro e o "OK" não mudaram.

- [ ] **Step 5: Rodar a suíte inteira e commitar**

Run: `./gradlew testDebugUnitTest`
Expected: PASS. `ReaderContentTest.failed_shows_error_and_ok_goes_back` também continua verde.

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/common/DialogsTest.kt app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt
git commit -m "feat(d1): diálogos de remoção e erro com ações alinhadas à direita"
```

---

### Task 8: Diálogos — senha ("PDF protegido")

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt:45-85`
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/common/DialogsTest.kt`, `app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt`

**Interfaces:**
- Consumes: `MinTouchTarget`, `PrimaryTouchTarget`; `JohnIcons.Lock`, `Visibility`, `VisibilityOff`, `Keyboard`, `Dialpad`.
- Produces: `fun PasswordDialog(wrongAttempt: Boolean, onSubmit: (String) -> Unit, onCancel: () -> Unit)` — **assinatura inalterada**. Rótulos exatos: "PDF protegido", "Senha", "Senha incorreta, tente de novo", "Usar letras", "Usar números", "Abrir", "Cancelar"; descrições "Mostrar senha" / "Ocultar senha".

- [ ] **Step 1: Escrever os testes que falham**

Em `DialogsTest.kt`, **substitua** `password_dialog_toggles_keyboard_label` e acrescente dois testes:

```kotlin
    @Test fun password_dialog_alterna_o_teclado() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("PDF protegido").assertIsDisplayed()
        rule.onNodeWithText("Usar letras").performClick()
        rule.onNodeWithText("Usar números").assertIsDisplayed()
    }

    @Test fun password_dialog_alterna_a_visibilidade_da_senha() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithContentDescription("Mostrar senha").performClick()
        rule.onNodeWithContentDescription("Ocultar senha").assertIsDisplayed()
    }

    @Test fun password_dialog_cancela() {
        var cancelou = false
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = { cancelou = true }) } }
        rule.onNodeWithText("Cancelar").performClick()
        assertTrue(cancelou)
    }
```

Import a acrescentar: `androidx.compose.ui.test.onNodeWithContentDescription`.

Em `ReaderContentTest.kt`, no teste `needs_password_shows_dialog`, troque `"Este PDF tem senha"` por `"PDF protegido"`.

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.common.DialogsTest' --tests 'com.johngabie.johnpdf.ui.reader.ReaderContentTest'`
Expected: FAIL — "PDF protegido" e "Mostrar senha" não existem; o botão de teclado ainda se chama "abc  Usar letras".

- [ ] **Step 3: Implementar**

Substitua `PasswordDialog`:

```kotlin
@Composable
fun PasswordDialog(wrongAttempt: Boolean, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var numeric by rememberSaveable { mutableStateOf(true) }
    var visible by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(JohnIcons.Lock, contentDescription = null) },
        title = { Text("PDF protegido", style = MaterialTheme.typography.titleLarge) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    isError = wrongAttempt,
                    visualTransformation =
                        if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password,
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { visible = !visible },
                            modifier = Modifier.size(MinTouchTarget),
                        ) {
                            Icon(
                                if (visible) JohnIcons.VisibilityOff else JohnIcons.Visibility,
                                contentDescription = if (visible) "Ocultar senha" else "Mostrar senha",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("password_field"),
                )
                if (wrongAttempt) {
                    Text(
                        "Senha incorreta, tente de novo",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(onClick = { numeric = !numeric }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                    Icon(
                        if (numeric) JohnIcons.Keyboard else JohnIcons.Dialpad,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (numeric) "Usar letras" else "Usar números",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(password) },
                enabled = password.isNotEmpty(),
                modifier = Modifier.heightIn(min = PrimaryTouchTarget),
            ) {
                Text("Abrir", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
```

Preservado de propósito: `numeric = true` por padrão (teclado numérico primeiro), `testTag("password_field")`, `rememberSaveable` em `password` e `numeric`, `enabled = password.isNotEmpty()`, cancelar volta para a Home.

Imports a **adicionar** em `Dialogs.kt`: `androidx.compose.foundation.layout.Spacer`, `androidx.compose.foundation.layout.size`, `androidx.compose.foundation.layout.width`, `androidx.compose.material3.IconButton`, `androidx.compose.ui.text.input.VisualTransformation`.

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.common.DialogsTest' --tests 'com.johngabie.johnpdf.ui.reader.ReaderContentTest'`
Expected: PASS. `password_dialog_submits_typed_password` e `password_dialog_shows_wrong_attempt_message` continuam verdes.

- [ ] **Step 5: Rodar a suíte inteira e commitar**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt app/src/test/java/com/johngabie/johnpdf/ui/common/DialogsTest.kt app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt
git commit -m "feat(d1): diálogo de senha com 'PDF protegido', ver senha e ícones de teclado"
```

---

### Task 9: Leitor — `TopAppBar` com voltar só em ícone, rotação e Snackbar

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt:113-174` (`ReaderContent`), `:203-222` (`ReaderTopBar`), `:224-248` (`ReaderBottomBar` — só remove o botão de rotação)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt`

**Interfaces:**
- Consumes: `MinTouchTarget` (Task 1); `JohnIcons.ArrowBack`, `ScreenRotation`, `ScreenLockRotation`.
- Produces:
  - `private fun ReaderTopBar(title: String, showRotation: Boolean, rotationLocked: Boolean, onBack: () -> Unit, onToggleRotation: () -> Unit)`
  - `private fun RotationLockAction(locked: Boolean, onToggle: () -> Unit)`
  - `ReaderBottomBar` passa a ter a assinatura `(current: Int, total: Int, onPrevious: () -> Unit, onNext: () -> Unit)` — a Task 10 a reduz de novo para `(current: Int, total: Int)`.
  - `ReaderContent` mantém a assinatura pública atual (o `SnackbarHostState` é interno).
  - Descrições: `"Voltar"`, `"Travar rotação da tela"` + `stateDescription` "Travada"/"Automática". Mensagens: "Tela travada nesta posição" / "Rotação automática".

- [ ] **Step 1: Escrever os testes que falham**

Em `ReaderContentTest.kt`, **substitua** `back_button_calls_on_back` e `rotation_button_toggles_label` e acrescente dois testes:

```kotlin
    @Test fun back_button_calls_on_back() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithContentDescription("Voltar").performClick()
        assertEquals(1, backCalls)
    }

    @Test fun acao_de_rotacao_alterna_e_informa_o_estado() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithContentDescription("Travar rotação da tela").assertIsOff()
        rule.onNodeWithContentDescription("Travar rotação da tela").performClick()
        rule.onNodeWithContentDescription("Travar rotação da tela").assertIsOn()
        assertEquals(1, rotationToggles)
    }

    @Test fun travar_rotacao_mostra_snackbar() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        // autoAdvance desligado: com o relógio livre, o Snackbar Short (4s) some antes da asserção.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Travar rotação da tela").performClick()
        rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithText("Tela travada nesta posição").assertIsDisplayed()
    }

    /** Review Focus 4: voltar (IconButton 48dp) e rotação (IconToggleButton) na mesma linha. */
    @Test fun icones_do_top_bar_do_leitor_ficam_alinhados() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        val voltar = rule.onNodeWithContentDescription("Voltar").getBoundsInRoot()
        val rotacao = rule.onNodeWithContentDescription("Travar rotação da tela").getBoundsInRoot()
        assertEquals(
            "centros verticais diferentes: $voltar vs $rotacao",
            voltar.top.value + voltar.height.value / 2f,
            rotacao.top.value + rotacao.height.value / 2f,
            1f,
        )
    }

    @Test fun carregando_nao_mostra_a_acao_de_rotacao() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Loading))
        rule.onNodeWithContentDescription("Travar rotação da tela").assertDoesNotExist()
    }
```

Imports a acrescentar em `ReaderContentTest.kt`: `androidx.compose.ui.test.assertIsOn`, `androidx.compose.ui.test.assertIsOff`, `androidx.compose.ui.test.getBoundsInRoot`.

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.reader.ReaderContentTest'`
Expected: FAIL — não existe nó com `contentDescription` "Voltar" nem "Travar rotação da tela" (hoje são `Text("← Voltar")` e `BigButton("🔓 Gira sozinha")`).

- [ ] **Step 3: Implementar**

Substitua `ReaderTopBar` e acrescente `RotationLockAction`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    title: String,
    showRotation: Boolean,
    rotationLocked: Boolean,
    onBack: () -> Unit,
    onToggleRotation: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.size(MinTouchTarget)) {
                Icon(JohnIcons.ArrowBack, contentDescription = "Voltar")
            }
        },
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        actions = {
            if (showRotation) RotationLockAction(rotationLocked, onToggleRotation)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RotationLockAction(locked: Boolean, onToggle: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(if (locked) "Destravar rotação" else "Travar rotação") } },
        state = rememberTooltipState(),
    ) {
        IconToggleButton(
            checked = locked,
            onCheckedChange = { onToggle() },
            modifier = Modifier
                .size(MinTouchTarget)
                .semantics { stateDescription = if (locked) "Travada" else "Automática" },
            colors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContentColor = MaterialTheme.colorScheme.onSurface,
                checkedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        ) {
            Icon(
                if (locked) JohnIcons.ScreenLockRotation else JohnIcons.ScreenRotation,
                contentDescription = "Travar rotação da tela",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
```

**Sem preenchimento azul**: destravado = ícone `onSurfaceVariant` sem fundo; travado = `onSurface` sobre `surfaceContainerHighest` (spec §6.2 / D1 §C).

Em `ReaderContent`, acrescente o `SnackbarHostState`, ligue o novo top bar e o `snackbarHost`, e tire o botão de rotação da barra inferior:

```kotlin
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val status = state.status
    var barsVisible by rememberSaveable { mutableStateOf(true) }
    val showBars = barsVisible || status !is ReaderStatus.Ready
    ApplySystemBarsVisibility(showBars)

    Scaffold(
        topBar = {
            AnimatedVisibility(showBars, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                ReaderTopBar(
                    title = state.title,
                    showRotation = status is ReaderStatus.Ready,
                    rotationLocked = state.rotationLocked,
                    onBack = onBack,
                    onToggleRotation = {
                        // O StateFlow do ViewModel ainda não recompôs aqui: o estado "depois do
                        // toque" é o inverso do atual.
                        val willBeLocked = !state.rotationLocked
                        onToggleRotation()
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()   // toques rápidos não enfileiram
                            snackbarHostState.showSnackbar(
                                message = if (willBeLocked) "Tela travada nesta posição" else "Rotação automática",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
    ) { padding -> /* inalterado nesta task */ }
```

E em `ReaderBottomBar`, remova o parâmetro `rotationLocked`/`onToggleRotation` e a primeira `Row` vira só o rótulo (as setas continuam ali até a Task 10):

```kotlin
@Composable
private fun ReaderBottomBar(current: Int, total: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(pageLabel(current, total), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("⬆ Anterior", onPrevious, Modifier.weight(1f), enabled = current > 0)
                BigButton("⬇ Próxima", onNext, Modifier.weight(1f), enabled = current < total - 1)
            }
        }
    }
}
```

Imports a **adicionar** em `ReaderScreen.kt`: `androidx.compose.material3.ExperimentalMaterial3Api`, `Icon`, `IconButton`, `IconButtonDefaults`, `IconToggleButton`, `PlainTooltip`, `SnackbarDuration`, `SnackbarHost`, `SnackbarHostState`, `TooltipBox`, `TooltipDefaults`, `TopAppBar`, `TopAppBarDefaults`, `rememberTooltipState` (todos em `androidx.compose.material3.*`), `androidx.compose.ui.semantics.semantics`, `androidx.compose.ui.semantics.stateDescription`, `androidx.compose.foundation.layout.size`, `com.johngabie.johnpdf.ui.icons.JohnIcons`. **Remova** `androidx.compose.material3.TextButton` e `androidx.compose.foundation.layout.statusBarsPadding`.

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.reader.ReaderContentTest'`
Expected: PASS nos cinco testes novos. `tapping_the_page_hides_and_restores_the_bars` **falha** neste ponto porque ainda procura `"← Voltar"` — corrija-o agora para `rule.onNodeWithContentDescription("Voltar")` nas quatro ocorrências, mantendo `"⬇ Próxima"` (que só muda na Task 10).

- [ ] **Step 5: Rodar a suíte inteira e commitar**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt
git commit -m "feat(d1): top bar do leitor com voltar em ícone, trava de rotação e Snackbar"
```

---

### Task 10: Leitor — `PagerRail` flutuante de 56×112dp

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt` (`ReaderContent` — bloco de conteúdo do `Scaffold`; `ReaderBottomBar` — remove as setas; novo `PagerRail`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt`

**Interfaces:**
- Consumes: `JohnIcons.KeyboardArrowUp`, `JohnIcons.KeyboardArrowDown`.
- Produces: `private fun PagerRail(canGoPrevious: Boolean, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier)`. `ReaderBottomBar` volta à assinatura `(current: Int, total: Int)` — consumida pela Task 11. Descrições `"Página anterior"` / `"Próxima página"`.

- [ ] **Step 1: Escrever os testes que falham**

Em `ReaderContentTest.kt`, **substitua** `ready_shows_page_label_and_button_states` e `next_button_scrolls_to_next_page`, e acrescente dois testes:

```kotlin
    @Test fun ready_shows_page_label_and_button_states() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithText("doc.pdf").assertIsDisplayed()
        rule.onNodeWithText("Página 1 de 3").assertIsDisplayed()
        rule.onNodeWithContentDescription("Página anterior").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Próxima página").assertIsEnabled()
    }

    @Test fun next_button_scrolls_to_next_page() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithContentDescription("Próxima página").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Página 2 de 3").assertIsDisplayed()
    }

    /** Review Focus 5: página 1 desabilita ▲; documento de página única desabilita as duas. */
    @Test fun setas_desabilitam_na_primeira_e_na_ultima_pagina() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithContentDescription("Página anterior").assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Próxima página").assertIsDisplayed().assertIsNotEnabled()
    }

    /** Review Focus 1: o trilho tem 112dp e a barra 72dp — ele tem que caber inteiro, à direita e acima da barra. */
    @Test fun pager_rail_fica_acima_da_barra_no_canto_direito() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        val root = rule.onRoot().getBoundsInRoot()
        val cima = rule.onNodeWithContentDescription("Página anterior").assertIsDisplayed().getBoundsInRoot()
        val baixo = rule.onNodeWithContentDescription("Próxima página").assertIsDisplayed().getBoundsInRoot()
        val rotulo = rule.onNodeWithText("Página 1 de 3").assertIsDisplayed().getBoundsInRoot()

        assertTrue("o trilho vazou pela direita: $baixo em $root", baixo.right <= root.right)
        assertTrue("o trilho não está encostado na direita: $baixo", baixo.left > root.width / 2f)
        assertTrue("as setas não estão uma em cima da outra: $cima / $baixo", cima.bottom <= baixo.top)
        assertTrue("o trilho invadiu a barra inferior: $baixo vs $rotulo", baixo.bottom <= rotulo.top)
    }

    @Test fun o_trilho_some_junto_com_as_barras_no_toque_simples() {
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

E **apague** o antigo `tapping_the_page_hides_and_restores_the_bars` — `o_trilho_some_junto_com_as_barras_no_toque_simples` o substitui cobrindo exatamente o mesmo caminho, já com os nomes novos.

Imports a acrescentar em `ReaderContentTest.kt`: `androidx.compose.ui.test.onRoot` (`getBoundsInRoot` já entrou na Task 9).

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.reader.ReaderContentTest'`
Expected: FAIL nos cinco — não existe nó com `contentDescription` "Página anterior"/"Próxima página" (hoje são `BigButton("⬆ Anterior")` e `BigButton("⬇ Próxima")`, que só têm texto). A mensagem é "Expected exactly '1' node but could not find any node that satisfies: (ContentDescription = 'Próxima página')".

- [ ] **Step 3: Implementar**

Acrescente `PagerRail` e reduza `ReaderBottomBar`:

```kotlin
@Composable
private fun PagerRail(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = modifier.width(56.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onPrevious, enabled = canGoPrevious, modifier = Modifier.size(56.dp)) {
                Icon(JohnIcons.KeyboardArrowUp, contentDescription = "Página anterior", modifier = Modifier.size(28.dp))
            }
            HorizontalDivider(
                Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(56.dp)) {
                Icon(JohnIcons.KeyboardArrowDown, contentDescription = "Próxima página", modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(current: Int, total: Int) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp)) {
            Text(pageLabel(current, total), style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

Nada de cinza sólido no desabilitado: o `IconButton` com `enabled = false` já aplica `onSurface` a 38% (o "tonal esmaecido" da spec §6.3) e preserva o alvo de 56dp.

No `Scaffold`, troque a chamada do `bottomBar` para `ReaderBottomBar(current = state.currentPage, total = state.pageCount)` e acrescente o trilho **dentro** do conteúdo:

```kotlin
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (status) {
                // … Loading / NeedsPassword / Failed / Ready, inalterados …
            }

            AnimatedVisibility(
                visible = showBars && status is ReaderStatus.Ready,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 8.dp),
            ) {
                PagerRail(
                    canGoPrevious = state.currentPage > 0,
                    canGoNext = state.currentPage < state.pageCount - 1,
                    onPrevious = { scope.launch { listState.animateScrollToItem((state.currentPage - 1).coerceAtLeast(0)) } },
                    onNext = { scope.launch { listState.animateScrollToItem((state.currentPage + 1).coerceAtMost(state.pageCount - 1)) } },
                )
            }
        }
    }
```

O trilho fica **no `content`, não no `bottomBar`**: com o `padding` do `Scaffold` aplicado no `Box`, `bottom = 8.dp` já o posiciona 8dp acima da barra inferior, e ele entra na mesma `AnimatedVisibility` do modo imersivo — senão ficaria um controle flutuando sozinho sobre a página em tela cheia, que é exatamente o ruído que o modo imersivo elimina.

Imports a **adicionar**: `androidx.compose.animation.scaleIn`, `androidx.compose.animation.scaleOut`, `androidx.compose.foundation.shape.RoundedCornerShape`, `androidx.compose.material3.HorizontalDivider`, `androidx.compose.foundation.layout.width`. **Remova** `com.johngabie.johnpdf.ui.common.BigButton` — este era o último uso no arquivo (o `BigButton.kt` em si só é apagado na Task 12, quando a Home também tiver zerado os usos).

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.reader.ReaderContentTest'`
Expected: PASS nos quatro testes.

- [ ] **Step 5: Rodar a suíte inteira e commitar**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt
git commit -m "feat(d1): PagerRail flutuante com as setas empilhadas à direita"
```

---

### Task 11: Leitor — barra inferior única de 72dp

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt` (`ReaderBottomBar`)
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt`

**Interfaces:**
- Consumes: `pageLabel(current, total)` de `PageMath.kt` (**inalterado**); `PagerRail` (Task 10).
- Produces: `private fun ReaderBottomBar(current: Int, total: Int)` em sua forma final — `Surface(color = surfaceContainer)` + `Box` de 72dp mínimos, `navigationBarsPadding()`, `padding(start = 16.dp, end = 88.dp)`, conteúdo centralizado em `labelMedium`/`onSurfaceVariant`.

- [ ] **Step 1: Escrever os testes que falham**

Acrescente a `ReaderContentTest.kt`:

```kotlin
    /**
     * O rótulo fica centralizado na faixa. A altura da `Surface` não tem nó próprio na árvore
     * de semântica, então o que dá para afirmar é a distância do centro do texto até a base da
     * tela: numa faixa de 72dp centralizada, são 36dp. A altura visual de 72dp é conferida na
     * captura do cenário 7 (Task 14).
     */
    @Test fun rotulo_de_pagina_fica_centralizado_numa_faixa_de_72dp() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        val root = rule.onRoot().getBoundsInRoot()
        val rotulo = rule.onNodeWithText("Página 1 de 3").assertIsDisplayed().getBoundsInRoot()
        val centro = rotulo.top + (rotulo.bottom - rotulo.top) / 2f
        val metadeDaFaixa = root.bottom - centro
        assertTrue("centro do rótulo a $metadeDaFaixa da base, esperado >= 36dp", metadeDaFaixa >= 36.dp)

        val meioDaTela = root.left + root.width / 2f
        val centroDoRotulo = rotulo.left + rotulo.width / 2f
        assertTrue("rótulo não está centralizado: $centroDoRotulo vs $meioDaTela", centroDoRotulo < meioDaTela)
    }

    /**
     * Review Focus 2: "Página 1000 de 2000" não pode correr por baixo do trilho.
     * Guarda de regressão, não teste vermelho-primeiro: antes desta task o rótulo está alinhado
     * à esquerda e nunca chega perto do trilho. Numa tela de 320dp o trilho começa em 248dp, e é
     * o `end = 88.dp` que mantém o texto centralizado longe dali.
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun rotulo_de_pagina_nao_passa_por_baixo_do_trilho() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(2000) { a4 })))
        val rotulo = rule.onNodeWithText("Página 1 de 2000").assertIsDisplayed().getBoundsInRoot()
        val trilho = rule.onNodeWithContentDescription("Próxima página").getBoundsInRoot()
        assertTrue("o rótulo (${rotulo.right}) invade o trilho (${trilho.left})", rotulo.right <= trilho.left)
    }
```

Import a acrescentar em `ReaderContentTest.kt`: `org.robolectric.annotation.Config`.

- [ ] **Step 2: Rodar os testes e confirmar que o primeiro falha**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.reader.ReaderContentTest'`
Expected: FAIL em `rotulo_de_pagina_fica_centralizado_numa_faixa_de_72dp` — a faixa de hoje é uma `Column` com `padding(8.dp)` em volta do texto, então o centro do rótulo fica a ~23dp da base, não a ≥36dp, e o texto está alinhado à esquerda em vez de centralizado.

`rotulo_de_pagina_nao_passa_por_baixo_do_trilho` **passa** já neste ponto, e isso é esperado: ele é a guarda do `end = 88.dp`. Depois do Step 3, confirme que ele discrimina — troque temporariamente `end = 88.dp` por `end = 16.dp`, rode só esse teste (deve **falhar**), e restaure os 88dp. Registre no commit que a checagem foi feita.

- [ ] **Step 3: Implementar**

Substitua `ReaderBottomBar` pela forma final da spec §6.3:

```kotlin
@Composable
private fun ReaderBottomBar(current: Int, total: Int) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, end = 88.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                pageLabel(current, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

`end = 88.dp` = 56dp do trilho + 16dp de margem da borda + 16dp de folga: é o que impede o rótulo longo de correr por baixo do `PagerRail`. Sem `tonalElevation` — a separação vem do tom do container (spec §3.3). Em paisagem nada muda de forma: a mesma faixa de 72dp e o mesmo trilho, o que tira a barra dos ~55% da tela de hoje.

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.reader.ReaderContentTest'`
Expected: PASS nos dois testes novos e nos anteriores — inclusive `pager_rail_fica_acima_da_barra_no_canto_direito` (Task 10), que agora mede contra a faixa final.

- [ ] **Step 5: Rodar a suíte inteira e commitar**

Run: `./gradlew testDebugUnitTest`
Expected: PASS. `PageMathTest` continua intocado — `pageLabel` não mudou.

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt
git commit -m "feat(d1): barra inferior do leitor reduzida a uma faixa de 72dp"
```

---

### Task 12: Leitor — fundo entre páginas e remoção do `BigButton`

**Files:**
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt` (`PageList`, `PdfPage`)
- Modify: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt` (remover `PageGapColor`)
- Delete: `app/src/main/java/com/johngabie/johnpdf/ui/common/BigButton.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt`

**Interfaces:**
- Consumes: nada novo.
- Produces: nenhuma API nova. Remove `PageGapColor` e `BigButton` do código.

- [ ] **Step 1: Confirmar que nada mais usa `BigButton` nem `PageGapColor`**

```bash
grep -rn 'BigButton\|PageGapColor' app/src --include=*.kt
```

Esperado: só as definições (`BigButton.kt`, `Theme.kt:13`) e os imports em `HomeScreen.kt`/`ReaderScreen.kt`. Se aparecer **qualquer uso real** (chamada de composable), volte e conclua a task que o deveria ter removido — não apague nada com uso pendente.

- [ ] **Step 2: Escrever o teste que falha**

Acrescente a `ReaderContentTest.kt`:

```kotlin
    @Test fun pagina_renderizada_continua_acessivel_por_indice() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(2) { a4 })))
        rule.onNodeWithContentDescription("Página 1").assertIsDisplayed()
    }
```

E crie um teste de regressão contra hardcode de cor, em `app/src/test/java/com/johngabie/johnpdf/ui/theme/ThemeTokensTest.kt`:

```kotlin
    @Test fun nenhuma_cor_hardcoded_nas_telas_do_refresh() {
        val arquivos = listOf(
            "ui/home/HomeScreen.kt",
            "ui/reader/ReaderScreen.kt",
            "ui/common/Dialogs.kt",
        ).map { java.io.File("src/main/java/com/johngabie/johnpdf/$it") }

        arquivos.forEach { f ->
            assertTrue("arquivo não encontrado: ${f.absolutePath}", f.exists())
            val hex = Regex("""Color\(0x[0-9A-Fa-f]{8}\)""").findAll(f.readText()).map { it.value }.toList()
            assertEquals("${f.name} tem cor hardcoded: $hex", emptyList<String>(), hex)
        }
    }
```

> O diretório de trabalho dos testes unitários do AGP é o do módulo (`app/`), daí o caminho relativo `src/main/...`. Se o assert de existência falhar, imprima `java.io.File(".").absolutePath` e ajuste o prefixo — não relaxe a regex.

- [ ] **Step 3: Rodar os testes e confirmar o estado de cada um**

Run: `./gradlew testDebugUnitTest --tests 'com.johngabie.johnpdf.ui.theme.ThemeTokensTest' --tests 'com.johngabie.johnpdf.ui.reader.ReaderContentTest'`

Expected: `pagina_renderizada_continua_acessivel_por_indice` **passa** (o `contentDescription` da página não muda nesta task — ele existe para provar que envolver a página num `Surface` não quebra a semântica, e o valor dele é justamente falhar no Step 5 se a refatoração do `PdfPage` errar).

`nenhuma_cor_hardcoded_nas_telas_do_refresh` também **passa** de primeira: nenhum dos três arquivos tem literal hexadecimal hoje (`ReaderScreen.kt` importa `PageGapColor` de `Theme.kt`, e `Color.White` não casa com a regex). É uma guarda de regressão, não um teste vermelho-primeiro. Confirme que ele **discrimina**: acrescente `val x = Color(0xFF123456)` no topo de `Dialogs.kt`, rode só esse teste (deve **falhar**, citando `[Color(0xFF123456)]`), e desfaça.

- [ ] **Step 4: Implementar**

Em `PageList`, troque o fundo e os espaçamentos:

```kotlin
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
```

```kotlin
            LazyColumn(
                state = listState,
                modifier = Modifier.width(contentWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),      // era 12dp
                contentPadding = PaddingValues(vertical = 8.dp),       // era 12dp
            ) {
```

Em `PdfPage`, envolva a página num `Surface` para ganhar a sombra de 1dp, mantendo o papel branco:

```kotlin
    Surface(shadowElevation = 1.dp, color = Color.White) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(aspect),
            contentAlignment = Alignment.Center,
        ) {
            // … when (val img = image) { … } inalterado …
        }
    }
```

A página continua **branca** independentemente do modo escuro — o "modo noturno" que inverte o PDF segue fora de escopo. `Color.White` aqui é papel físico, não cor de tema: é a exceção declarada nas Global Constraints.

Em `Theme.kt`, apague a linha `val PageGapColor = Color(0xFFBDBDBD)` (e o import `androidx.compose.ui.graphics.Color` **se** as cores do esquema ainda não o usarem — na paleta atual usam, então o import fica).

Em `ReaderScreen.kt`, remova `import com.johngabie.johnpdf.ui.theme.PageGapColor` e `import androidx.compose.foundation.layout.Row` (se não houver mais nenhuma `Row`), e em `HomeScreen.kt` remova `import com.johngabie.johnpdf.ui.common.BigButton`. Apague o arquivo:

```bash
git rm app/src/main/java/com/johngabie/johnpdf/ui/common/BigButton.kt
```

> Coordenação: a spec de sistema visual prevê um `PrimaryButton` em `ui/common/Buttons.kt` substituindo o `BigButton`. Depois deste plano **nenhuma tela chama `BigButton`**, então mantê-lo seria código morto. Se o plano de sistema visual já tiver criado `Buttons.kt`, não mexa nele — só apague o `BigButton.kt`.

- [ ] **Step 5: Rodar os testes e confirmar que passam**

Run: `./gradlew testDebugUnitTest`
Expected: PASS na suíte inteira, incluindo `nenhuma_cor_hardcoded_nas_telas_do_refresh` e `failed_render_shows_page_message`.

- [ ] **Step 6: Build de debug para provar que o app ainda compila e empacota**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add -A app/src/main/java/com/johngabie/johnpdf app/src/test/java/com/johngabie/johnpdf
git commit -m "refactor(d1): fundo das páginas por papel de tema e remoção do BigButton"
```

---

### Task 13: E2E da Home e dos diálogos, com capturas

**Files:**
- Create: `docs/e2e/2026-09-29-d1-layouts.md`
- Create: `docs/e2e/img/d1/*.png`

**Interfaces:**
- Consumes: o APK de debug produzido na Task 12.
- Produces: o documento `docs/e2e/2026-09-29-d1-layouts.md` com a tabela de cenários 1–6 preenchida (o leitor, cenários 7–11, entra na Task 14).

- [ ] **Step 1: Instalar e preparar o aparelho/emulador**

Prefira o **mobile-mcp** se ele estiver conectado (`mobile_list_available_devices` → use o `device id` em todas as chamadas). Se não estiver, use adb diretamente — foi o caminho do v1 e cobre os mesmos cenários:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-x86_64-debug.apk
adb push tools/fixtures/normal.pdf tools/fixtures/long.pdf tools/fixtures/password.pdf tools/fixtures/corrupted.pdf /sdcard/Download/
adb shell content call --method scan_volume --uri content://media --arg external_primary
mkdir -p docs/e2e/img/d1
```

Se as fixtures não estiverem em `tools/fixtures/`, gere-as com `python tools/make_test_pdfs.py` (reportlab, pypdf, cryptography, pillow).

- [ ] **Step 2: Capturar os cenários da Home**

Para cada cenário, capture com `adb exec-out screencap -p > docs/e2e/img/d1/<nome>.png` (ou `mobile_save_screenshot`) e confira na imagem o que está na coluna "o que verificar":

| # | Cenário | Como chegar | O que verificar | Arquivo |
|---|---|---|---|---|
| 1 | Home vazia | primeira abertura, aba Recentes | `TopAppBar` de 64dp com "johnPDF" à esquerda e o botão tonal "Abrir PDF" (ícone de pasta) à direita; **nenhum botão flutuante no canto inferior direito**; estado vazio com ícone, "Nenhum PDF aberto ainda" e o `TextButton` "Abrir PDF" | `01-home-vazia.png` |
| 2 | Permissão | tocar "Todos os PDFs" | círculo com ícone de pasta, título em 2–3 linhas, os 3 passos numerados citando "Permitir acesso", botão filled de 56dp | `02-permissao.png` |
| 3 | Lista | conceder acesso e voltar | linhas de 72dp, quadrado vermelho-suave com o ícone de PDF, nome em SemiBold e "Download · ontem" abaixo em cinza, divisores finos recuados alinhados ao texto, **sem cartões** | `03-lista.png` |
| 4 | Busca | digitar "pas" na busca | pílula arredondada de 56dp com lupa à esquerda e "✕" à direita; só `password.pdf` na lista | `04-busca.png` |
| 5 | Busca sem resultado | digitar "zzz" | ícone `search_off`, "Nada encontrado", "Nenhum PDF com esse nome.", **sem** botão de ação | `05-busca-vazia.png` |
| 6 | Remover da lista | abrir um PDF, voltar, aba Recentes, toque longo | diálogo com ícone de lixeira, "Remover da lista?", "O arquivo continua no celular.", "Cancelar" (texto) à esquerda e "Remover" (filled) à direita, **os dois na mesma linha, à direita** | `06-remover.png` |

- [ ] **Step 3: Repetir os cenários 1, 3 e 4 com a escala de fonte em 1.3 e 2.0**

```bash
adb shell settings put system font_scale 1.3   # capturar 01/03/04 como 07a-*.png
adb shell settings put system font_scale 2.0   # capturar 01/03/04 como 07b-*.png
adb shell settings put system font_scale 1.0   # RESTAURAR sempre
```

Pontos de risco da spec §8.2 a conferir na captura a 2.0: (1) "johnPDF" + "Abrir PDF" na mesma linha de 64dp — o botão deve encolher o texto antes de cortar o título; (2) "Todos os PDFs" na `NavigationBar`. Se houver corte, **registre no documento** e não conserte agora: o fallback (`IconButton` só com `folder_open` a partir de `fontScale >= 1.8`) é uma pendência declarada da spec, a decidir com o usuário.

- [ ] **Step 4: Escrever o documento**

Crie `docs/e2e/2026-09-29-d1-layouts.md` com: cabeçalho (data, aparelho/emulador, ferramenta usada, commit SHA testado), os comandos exatos do Step 1, e uma tabela `| # | Cenário | Resultado | Evidência |` com os 6 cenários + as 6 capturas de escala de fonte. Escreva **PASS/FAIL com o que foi observado na imagem**, nunca de memória; qualquer afirmação sobre log precisa do arquivo de log salvo junto.

- [ ] **Step 5: Commit**

```bash
git add docs/e2e/2026-09-29-d1-layouts.md docs/e2e/img/d1
git commit -m "test(d1): E2E da Home e dos diálogos com capturas do novo layout"
```

---

### Task 14: E2E do leitor, escala de fonte e fechamento do checklist

**Files:**
- Modify: `docs/e2e/2026-09-29-d1-layouts.md`
- Create: `docs/e2e/img/d1/0[7-9]*.png`, `docs/e2e/img/d1/1*.png`
- Modify: `docs/e2e/2026-09-28-checklist.md`

**Interfaces:**
- Consumes: o documento e o setup da Task 13.
- Produces: o documento E2E completo (cenários 7–11) e o ponteiro no checklist do v1.

- [ ] **Step 1: Capturar os cenários do leitor**

| # | Cenário | Como chegar | O que verificar | Arquivo |
|---|---|---|---|---|
| 7 | Leitor em retrato | abrir `long.pdf` | `TopAppBar` de 64dp: seta de voltar sozinha à esquerda (**sem o texto "Voltar"**), nome do arquivo com reticências no meio, ícone de rotação à direita; faixa inferior fina com "Página 1 de 200" centralizado; trilho de 56×112dp encostado na direita, **acima** da faixa, com ▲ esmaecido e ▼ ativo | `07-leitor-retrato.png` |
| 8 | Navegação | tocar ▼ três vezes | "Página 4 de 200"; ▲ agora ativo | `08-leitor-pagina4.png` |
| 9 | Leitor em paisagem | girar a tela | mesma faixa de 72dp e mesmo trilho; a área de leitura ocupa a maior parte da altura (contraste com `docs/e2e/img/07a-rotated-landscape.png`, do v1) | `09-leitor-paisagem.png` |
| 10 | Trava de rotação | tocar no ícone de rotação | ícone muda para o cadeado sobre um círculo cinza neutro (**sem azul**) e aparece o Snackbar "Tela travada nesta posição" acima da faixa; tocar de novo → "Rotação automática" | `10a-travado.png`, `10b-destravado.png` |
| 11 | Modo imersivo | tocar na página | header, faixa **e trilho** somem juntos; tocar de novo traz os três de volta | `11a-imersivo.png`, `11b-restaurado.png` |
| 12 | Senha | abrir `password.pdf` | diálogo com cadeado, "PDF protegido", campo com o olho à direita, `TextButton` com ícone de teclado "Usar letras", "Cancelar"/"Abrir" à direita; teclado que aparece é **numérico** | `12-senha.png` |
| 13 | Erro | abrir `corrupted.pdf` | diálogo com ícone de erro em vermelho, "Não foi possível abrir este arquivo." centralizado, um único "OK" filled à direita | `13-erro.png` |

- [ ] **Step 2: Checar o cenário de risco 1 e 2 no aparelho**

Com `long.pdf` aberto, confirme **na imagem** de `07-leitor-retrato.png`: (a) o trilho está inteiro dentro da tela, nada cortado pela borda direita nem pela barra de navegação do sistema; (b) o rótulo "Página 1 de 200" não encosta no trilho. Repita com a escala de fonte em 2.0 (`adb shell settings put system font_scale 2.0`, capturar, **restaurar para 1.0**) — salve como `14-leitor-fontscale2.png`.

- [ ] **Step 3: Checar que o Snackbar não esconde as setas permanentemente**

Toque na rotação e, **enquanto o Snackbar está na tela**, capture (`10a-travado.png`). O Snackbar pode cobrir o trilho — é esperado —, mas some sozinho em ~4s (`SnackbarDuration.Short`). Capture de novo ~5s depois (`10c-snackbar-sumiu.png`) e confirme que o trilho voltou a aparecer. Se o Snackbar ficar preso na tela, isso é um FAIL e precisa ser reportado, não contornado.

- [ ] **Step 4: Completar o documento e apontar o checklist do v1**

Acrescente os cenários 7–14 à tabela de `docs/e2e/2026-09-29-d1-layouts.md` e feche com uma seção "Pendências observadas" (por exemplo, cortes a `fontScale` 2.0).

Em `docs/e2e/2026-09-28-checklist.md`, acrescente **uma linha** logo abaixo do título, sem reescrever o histórico daquela execução:

```markdown
> **Atualização (2026-09-29):** este checklist documenta o layout do v1. Depois do refresh de design D1, as capturas de referência são as de `docs/e2e/2026-09-29-d1-layouts.md` — os textos de UI mudaram ("📂 Abrir" → "Abrir PDF", "Sim" → "Remover", "← Voltar" → ícone, "Este PDF tem senha" → "PDF protegido").
```

- [ ] **Step 5: Verificação final antes de declarar pronto**

```bash
./gradlew testDebugUnitTest
grep -rn 'BigButton\|PageGapColor\|📂\|🕘\|📚\|📄\|🔍\|🔓\|🔒\|⬆\|⬇\|← Voltar' app/src/main --include=*.kt
adb shell settings get system font_scale
```

Expected: suíte verde; o `grep` **sem nenhum resultado** (todos os emojis e pseudo-ícones saíram da UI); `font_scale` de volta em `1.0` (ou `null`, que é o padrão).

- [ ] **Step 6: Commit**

```bash
git add docs/e2e/2026-09-29-d1-layouts.md docs/e2e/img/d1 docs/e2e/2026-09-28-checklist.md
git commit -m "test(d1): E2E do leitor e fechamento do checklist de layouts"
```

---

## Fora do escopo deste plano

Está na spec de layouts mas pertence a outra spec/plano — **não implemente aqui**:

| Item | Spec dona |
|---|---|
| `JohnIcons.kt`, `NOTICE` Apache 2.0, gerador em `tools/icons/` | `2026-09-29-icones-vendorizados-spec.md` |
| Tipografia (piso 16sp, `ThemeTest` de 20 → 16), fonte Atkinson Hyperlegible Next em `res/font`, `PrimaryButton`/`Buttons.kt` | `2026-09-29-design-d1-sistema-visual.md` + `2026-09-29-fonte-atkinson-hyperlegible-next.md` |
| `LightColors`/`DarkColors` completos, `darkColorScheme`, `JohnPdfTheme(darkTheme)` | `2026-09-29-dark-mode-ui.md` |
| Reescrita da §4 da spec do produto ("mínimo 20sp/64dp" → "16sp/48dp + ação universal só com ícone") | spec do produto, `2026-09-28-johnpdf-leitor-android-design.md` |
| Recaptura de `docs/e2e/device/img/` no aparelho físico da família | execução separada, precisa do moto g41 em mãos |
| Ligar R8, `material-icons-extended`, modo noturno que inverte o PDF | fora do refresh de design |
