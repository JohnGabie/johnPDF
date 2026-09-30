# D1 — Spec de layouts por tela (johnPDF)

- **Data:** 2026-09-29 · **Tipo:** spec de implementação (nenhum código alterado ainda)
- **Origem:** decisões do usuário na seção G de `docs/superpowers/specs/2026-09-29-design-d1-proposta.md`; detalha a seção D (linhas 80–116) daquele documento.
- **Leia primeiro:** D1 §A (diagnóstico), §B (biblioteca de ícones — opção 3, vendorizar Material Symbols), §C (controle de rotação).
- **Arquivos afetados:** `ui/theme/Theme.kt`, **novo** `ui/icons/JohnIcons.kt`, `ui/common/BigButton.kt`, `ui/common/Dialogs.kt`, `ui/home/HomeScreen.kt`, `ui/reader/ReaderScreen.kt`, `ui/reader/PageMath.kt` (sem mudança — ver §7.3).

> **Convenção deste documento.** Os wireframes usam caracteres de caixa; cada bloco traz a altura em dp à direita. O código Compose é a forma pretendida, não um patch — nomes de parâmetro e imports seguem o que já existe no repositório (material3 1.3.2, BOM 2025.05.00). Cores aparecem **sempre por papel** (`colorScheme.surfaceContainer`), nunca por hexadecimal, porque a paleta ainda está em aberto (§9).

---

## 1. Decisões travadas

| # | Pergunta (D1 §G) | Decisão do usuário | Efeito nesta spec |
|---|---|---|---|
| 1 | Barra do leitor: texto ou só setas? | **Setas, no canto direito, uma em cima da outra** | §6.3 — `PagerRail` vertical ancorado à direita; a barra inferior fica só com "Página X de Y" |
| 2 | Voltar: seta + texto ou só seta? | **Só a seta** | §6.2 — `IconButton(JohnIcons.ArrowBack, contentDescription = "Voltar")` |
| 3 | "Abrir PDF": FAB ou header? | **Manter no header, com leve ajuste** | §5.1 — `FilledTonalButton` compacto nas `actions` do `TopAppBar`. **Não há FAB.** |
| 4 | Modo escuro da interface? | **Sim** | §3.4 — `darkColorScheme` do mesmo seed; todo layout desta spec usa papéis de cor |
| 5 | Cor: índigo fixo ou dinâmica? | **Nenhuma das duas — refazer estudo de cor** | §9 — pendente; a spec não depende disso |
| 6 | Fonte | **Atkinson Hyperlegible Next** | §3.1 — `FontFamily` própria em `res/font`, aplicada na `Typography` |

### 1.1 Conflitos resolvidos

Duas instruções de entrada se contradiziam. Resolução e justificativa:

**(a) FAB na Home.** O briefing pedia ao mesmo tempo "Abrir PDF: manter no header (sem FAB no canto)" e "Home: … FAB no canto inferior". Prevalece a **primeira**, que é a transcrição literal da resposta do usuário à pergunta 3 ("mantem no header, com um leve ajust."). A menção ao FAB é resíduo da proposta original (D1 linha 108), que a decisão do usuário revogou. **Consequência:** a `LazyColumn` da Home **não** precisa do `contentPadding` inferior de 88dp previsto em D1 linha 108 — volta a 8dp + insets.

**(b) "Barra única de 72dp com setas" vs. "setas empilhadas no canto direito".** Duas setas empilhadas exigem ≥ 96dp (2 × 48dp) e não cabem numa barra de 72dp sem violar o alvo mínimo de toque. Resolução: a **barra inferior continua sendo uma faixa única de 72dp**, mas carrega apenas o indicador de página; as setas viram um **trilho flutuante vertical de 112dp** (`PagerRail`) ancorado na borda direita, transbordando por cima da barra — o mesmo relacionamento visual que um FAB tem com uma bottom bar no M3. Isso atende à decisão 1 ao pé da letra e preserva a altura de 72dp da faixa. Variante recusada e por quê: setas lado a lado dentro dos 72dp violaria "uma em cima da outra".

### 1.2 O que esta spec preserva de propósito

- Toque simples no PDF **esconde/mostra** header, barra e barras do sistema (commit `5de0feb`) — inclusive o `PagerRail` (§6.4).
- Contraste alto, textos em pt-BR simples, permissão com passos numerados, confirmação antes de remover, teclado numérico por padrão na senha, linha inteira clicável.
- Tudo em `sp`, respeitando a escala de fonte do sistema.

---

## 2. Inventário de componentes

| Componente | Arquivo | Situação |
|---|---|---|
| `JohnIcons` (~16 `ImageVector`) | **novo** `ui/icons/JohnIcons.kt` | criar (D1 §B opção 3, Apache 2.0 + `NOTICE`) |
| `PrimaryButton` | `ui/common/BigButton.kt` | `BigButton` renomeado e reduzido a um `Button` fino com ícone opcional |
| `PdfListItem` | `ui/home/HomeScreen.kt` | substitui `PdfCard` |
| `SearchField` | `ui/home/HomeScreen.kt` | substitui o `OutlinedTextField` |
| `EmptyState` | `ui/home/HomeScreen.kt` | substitui `CenteredMessage` |
| `RotationLockAction` | `ui/reader/ReaderScreen.kt` | novo (D1 §C) |
| `PagerRail` | `ui/reader/ReaderScreen.kt` | novo — substitui a linha de `BigButton` |
| `ConfirmDialog` / `ErrorDialog` / `PasswordDialog` | `ui/common/Dialogs.kt` | reescritos (§7) |

Ícones usados (Material Symbols Rounded, peso 400): `arrow_back` · `folder_open` · `folder` · `history` (+ preenchido) · `library_books` (+ preenchido) · `picture_as_pdf` · `search` · `search_off` · `close` · `keyboard_arrow_up` · `keyboard_arrow_down` · `screen_rotation` · `screen_lock_rotation` · `keyboard` · `dialpad` · `visibility` / `visibility_off` · `error` · `lock` · `delete`.

---

## 3. Tokens compartilhados

### 3.1 Tipografia

Fonte **Atkinson Hyperlegible Next** (OFL, `res/font/atkinson_hyperlegible_next_{regular,medium,semibold}.ttf`, ~150 KB, sem `INTERNET`).

| Estilo | Hoje | Spec | Onde aparece nesta spec |
|---|---|---|---|
| `headlineMedium` | 30 Bold | 24 Bold / 32 | — (deixa de ser usado na Home) |
| `titleLarge` | 24 SemiBold | **22 SemiBold / 28** | "johnPDF", títulos de diálogo, título do estado de permissão |
| `titleMedium` | 22 SemiBold | **18 SemiBold / 24** | nome do PDF na lista, nome no top bar do leitor, título de estado vazio |
| `bodyLarge` | 20 / 28 | **18 / 26** | passos da permissão, mensagens de erro, texto da busca, corpo dos diálogos |
| `bodyMedium` | 20 / 28 | **16 / 24** | "Download · ontem", texto de apoio dos estados vazios |
| `labelLarge` | 20 SemiBold | **16 SemiBold** | rótulos de botão |
| `labelMedium` | 20 | **16 Medium** | rótulos da `NavigationBar`, "Página 4 de 200" |
| `titleSmall` / `bodySmall` / `labelSmall` | 20 | 16 | pouco usados |

**Piso absoluto: 16sp.** `ThemeTest` passa de 20 para 16.

```kotlin
// ui/theme/Type.kt (ou dentro de Theme.kt)
private val Hyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_next_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_next_medium, FontWeight.Medium),
    Font(R.font.atkinson_hyperlegible_next_semibold, FontWeight.SemiBold),
)

private val Base = Typography()
internal val JohnTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontFamily = Hyperlegible, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge     = Base.titleLarge.copy(fontFamily = Hyperlegible, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium    = Base.titleMedium.copy(fontFamily = Hyperlegible, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall     = Base.titleSmall.copy(fontFamily = Hyperlegible, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge      = Base.bodyLarge.copy(fontFamily = Hyperlegible, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium     = Base.bodyMedium.copy(fontFamily = Hyperlegible, fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall      = Base.bodySmall.copy(fontFamily = Hyperlegible, fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge     = Base.labelLarge.copy(fontFamily = Hyperlegible, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium    = Base.labelMedium.copy(fontFamily = Hyperlegible, fontSize = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall     = Base.labelSmall.copy(fontFamily = Hyperlegible, fontSize = 16.sp),
)
```

### 3.2 Alvos de toque e espaçamento

```kotlin
// ui/theme/Theme.kt
val MinTouchTarget = 48.dp        // era 64.dp — piso para tudo que é tocável
val PrimaryTouchTarget = 56.dp    // ações frequentes: ↑/↓, "Permitir acesso", botão principal de diálogo
val ListItemHeight = 72.dp        // linha inteira clicável
```

- Espaço mínimo entre alvos adjacentes: **8dp**.
- Margem horizontal padrão de conteúdo: **16dp**; blocos centrados (permissão, vazio) usam **24dp** e `widthIn(max = 360.dp)`.

### 3.3 Barras e superfícies

| Elemento | Altura | Container |
|---|---|---|
| `TopAppBar` (Home e Leitor) | 64dp + status bar | `surface`, ao rolar `surfaceContainer` |
| `NavigationBar` (Home) | 80dp + navigation bar | `surfaceContainer`, indicador `secondaryContainer` |
| Barra inferior do leitor | **72dp** + navigation bar | `surfaceContainer` |
| `PagerRail` (leitor) | 112dp × 56dp | `surfaceContainerHigh`, `shadowElevation = 3.dp` |
| Fundo entre páginas do PDF | — | `surfaceContainerHigh` (era `#BDBDBD` chapado), gap **8dp** |

Sem `tonalElevation = 3.dp` fixo em lugar nenhum: a separação vem do tom do container.

### 3.4 Cor

O esquema passa a ser **completo** (todos os papéis, inclusive `surfaceContainer*`, `secondaryContainer`, `outlineVariant`) em claro **e** escuro, eliminando o lilás do baseline M3. Cor de marca só em: botão principal de diálogo, botão "Permitir acesso", "Abrir PDF" do header (tonal), indicador da aba selecionada. Ícone de PDF na lista em vermelho suave sobre container claro (pista "é PDF"), com par para o escuro.

```kotlin
@Composable
fun JohnPdfTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,  // gerados do mesmo seed — §9
        typography = JohnTypography,
        content = content,
    )
}
```

---

## 4. Mapa de telas

```
HomeScreen ─┬─ aba Recentes ──┬─ lista
            │                 └─ vazio: "Nenhum PDF aberto ainda"
            ├─ aba Todos os PDFs ─┬─ sem permissão → PermissionContent
            │                     ├─ busca + lista
            │                     ├─ vazio: "Nenhum PDF encontrado no celular."
            │                     └─ busca sem resultado: "Nenhum PDF com esse nome."
            ├─ diálogo Remover da lista?        (toque longo em Recentes)
            └─ diálogo Erro                     (state.error)

ReaderScreen ─┬─ Loading  → spinner + "Abrindo…"
              ├─ NeedsPassword → diálogo de senha
              ├─ Failed  → diálogo de erro (OK volta para a Home)
              └─ Ready   → páginas + TopAppBar + barra 72dp + PagerRail + SnackbarHost
```

---

## 5. Home

### 5.1 Shell: `TopAppBar` + `NavigationBar`

#### Wireframe — retrato

```
┌────────────────────────────────────────────┐
│ ▓▓▓ barra de status ▓▓▓                    │
├────────────────────────────────────────────┤
│                                            │
│  johnPDF                 ╭──────────────╮  │ 64dp  TopAppBar (surface)
│  22sp SemiBold           │ 📁 Abrir PDF │  │       FilledTonalButton 48dp
│                          ╰──────────────╯  │
├────────────────────────────────────────────┤
│                                            │
│              conteúdo da aba               │  resto
│                                            │
├────────────────────────────────────────────┤
│        ◷                     ▤             │ 80dp  NavigationBar
│    Recentes            Todos os PDFs       │       (surfaceContainer)
│  ╰── indicador secondaryContainer ──╯      │
├────────────────────────────────────────────┤
│ ▓▓▓ barra de navegação do sistema ▓▓▓      │
└────────────────────────────────────────────┘
```

#### Especificação

| Item | Valor |
|---|---|
| Título | "johnPDF", `titleLarge`, `onSurface`, 1 linha |
| Ação | `FilledTonalButton`, `heightIn(min = 48.dp)`, `contentPadding(start = 16, end = 20, vertical = 10)`, ícone `folder_open` 20dp + espaço 8dp + "Abrir PDF" `labelLarge` |
| Cor do topo | `containerColor = surface`, `scrolledContainerColor = surfaceContainer` |
| Aba selecionada | ícone **preenchido** (`history_filled` / `library_books_filled`); não selecionada, contorno |
| Rótulos | sempre visíveis, `labelMedium` 16sp, `alwaysShowLabel = true` |
| Insets | `Scaffold` cuida; `windowInsets` padrão do `TopAppBar`/`NavigationBar` |

> **O "leve ajuste" da decisão 3, em detalhe:** o botão sai de `Button` cheio de 64dp com emoji `📂` e vira `FilledTonalButton` de 48dp com o ícone `folder_open` — mesma posição, mesmo rótulo alargado ("Abrir" → "Abrir PDF"), peso visual menor. O header deixa de ser uma faixa de ~280px com `headlineMedium` 30sp e passa a um `TopAppBar` de 64dp.

#### Compose

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onOpenPicker: () -> Unit) {
    TopAppBar(
        title = { Text("johnPDF", style = MaterialTheme.typography.titleLarge) },
        actions = {
            FilledTonalButton(
                onClick = onOpenPicker,
                modifier = Modifier.heightIn(min = MinTouchTarget).padding(end = 8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Icon(JohnIcons.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir PDF", style = MaterialTheme.typography.labelLarge)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

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
                )
            },
            label = { Text("Todos os PDFs", style = MaterialTheme.typography.labelMedium) },
            alwaysShowLabel = true,
        )
    }
}
```

`contentDescription = null` nos ícones da nav bar é intencional: o rótulo textual ao lado já nomeia o item; descrever o ícone duplicaria o anúncio do TalkBack.

---

### 5.2 Lista de PDFs (Recentes e Todos)

Lista **plana**, sem cartões: `ListItem` de 72dp com divisores recuados. O toque longo continua abrindo "Remover da lista?" (só em Recentes).

#### Wireframe

```
├────────────────────────────────────────────┤
│  ╭────╮                                    │
│  │ 📕 │  Relatorio_anual_2026.pdf          │ 72dp
│  ╰────╯  Download · ontem                  │
│  40dp    18sp SemiBold / 16sp onSurfaceVar │
│        ┌───────────────────────────────────┤  divisor outlineVariant,
│  ╭────╮                                    │  recuado 72dp (alinha ao texto)
│  │ 📕 │  Boleto_energia_marco.pdf          │ 72dp
│  ╰────╯  WhatsApp · há 3 dias              │
│        ┌───────────────────────────────────┤
│  ╭────╮                                    │
│  │ 📕 │  Contrato_locacao_assinado_dig…    │ 72dp (nome corta em 2 linhas)
│  ╰────╯  Documentos · 12/09                │
└────────────────────────────────────────────┘
```

#### Especificação

| Item | Valor |
|---|---|
| Altura mínima | 72dp, **linha inteira** clicável (`combinedClickable` no `Modifier` do `ListItem`) |
| Leading | quadrado 40dp, `RoundedCornerShape(10.dp)`, fundo `pdfIconContainer`, ícone `picture_as_pdf` 24dp em `onPdfIconContainer` |
| Headline | `titleMedium` (18sp SemiBold), `maxLines = 2`, `TextOverflow.Ellipsis` |
| Supporting | `"${origin.label} · ${friendlyDate(...)}"`, `bodyMedium` (16sp), `onSurfaceVariant`, 1 linha |
| Divisor | `HorizontalDivider(color = outlineVariant)` com `Modifier.padding(start = 72.dp)`, **entre** itens (não depois do último) |
| Container | `ListItemDefaults.colors(containerColor = Color.Transparent)` — o fundo é o da tela |
| `contentPadding` da lista | `PaddingValues(vertical = 8.dp)`; sem padding extra inferior (não há FAB — §1.1a) |
| Toque longo | só em Recentes; em "Todos os PDFs", `onLongClick = null` |

#### Compose

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

@Composable
private fun RecentsTab(
    items: List<RecentItem>,
    nowMillis: Long,
    onOpen: (RecentItem) -> Unit,
    onLongPress: (RecentItem) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(
            icon = JohnIcons.History,
            title = "Nenhum PDF aberto ainda",
            message = "Os PDFs que você abrir vão aparecer aqui.",
        )
        return
    }
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
}
```

> **Nota de cor.** `errorContainer` / `onErrorContainer` é usado aqui como o "vermelho suave de PDF" de D1 linha 101, aproveitando um papel que já existe nos dois esquemas (claro e escuro) em vez de inventar um token fora do `ColorScheme`. Se o estudo de cor (§9) devolver um vermelho próprio, trocar por dois `val` em `Theme.kt` (`PdfIconContainer` / `OnPdfIconContainer`) — é o único ponto do layout que depende disso.

---

### 5.3 Busca (aba "Todos os PDFs")

Campo em **pílula**, sem borda, com ícone à esquerda e botão de limpar à direita quando há texto.

#### Wireframe

```
├────────────────────────────────────────────┤
│                                            │  8dp
│  ╭──────────────────────────────────────╮  │
│  │ 🔍  Buscar PDFs                   ✕  │  │ 56dp, raio 28dp
│  ╰──────────────────────────────────────╯  │  surfaceContainerHigh
│                                            │  8dp
├────────────────────────────────────────────┤
│  ╭────╮                                    │
│  │ 📕 │  …                                 │
```

#### Especificação

| Item | Valor |
|---|---|
| Forma | `RoundedCornerShape(28.dp)`, altura 56dp, `fillMaxWidth` com margem 16dp |
| Container | `surfaceContainerHigh` nos três estados (focado, não focado, desabilitado) |
| Bordas | nenhuma — `focusedIndicatorColor`/`unfocusedIndicatorColor` = `Color.Transparent` |
| Leading | `search` 24dp, `onSurfaceVariant` |
| Trailing | `close` 24dp num `IconButton` de 48dp, **só quando `query.isNotEmpty()`**, `contentDescription = "Limpar busca"` |
| Placeholder | "Buscar PDFs", `bodyLarge`, `onSurfaceVariant` |
| Teclado | `singleLine = true`, `imeAction = ImeAction.Search` |

#### Compose

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
                IconButton(onClick = { onQueryChange("") }) {
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

---

### 5.4 Estados vazios

Três variantes, mesmo componente.

| Situação | Ícone | Título (`titleMedium`) | Texto (`bodyMedium`) | Ação |
|---|---|---|---|---|
| Recentes vazio | `history` | "Nenhum PDF aberto ainda" | "Os PDFs que você abrir vão aparecer aqui." | `TextButton` "Abrir PDF" |
| Biblioteca vazia | `folder_open` | "Nenhum PDF no celular" | "Nenhum PDF encontrado no celular." | `TextButton` "Abrir PDF" |
| Busca sem resultado | `search_off` | "Nada encontrado" | "Nenhum PDF com esse nome." | nenhuma |

> Em D1 o `TextButton` "Abrir PDF" era redundante com o FAB e servia de reforço. Sem FAB (§1.1a) ele passa a ser a **segunda** via para a ação, o que o torna mais útil, não menos: o usuário que está olhando a tela vazia não precisa subir até o header.

#### Wireframe

```
├────────────────────────────────────────────┤
│                                            │
│                                            │
│                    ◷                       │  ícone 48dp, onSurfaceVariant
│                                            │  16dp
│        Nenhum PDF aberto ainda             │  titleMedium, centralizado
│                                            │  8dp
│    Os PDFs que você abrir vão aparecer     │  bodyMedium, onSurfaceVariant
│                  aqui.                     │  máx. 360dp de largura
│                                            │  16dp
│               ╭───────────╮                │
│               │ Abrir PDF │                │  TextButton 48dp
│               ╰───────────╯                │
│                                            │
└────────────────────────────────────────────┘
```

#### Compose

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
            TextButton(onClick = onAction, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
```

---

### 5.5 Permissão de arquivos

#### Wireframe

```
├────────────────────────────────────────────┤
│                                            │
│                  ╭─────╮                   │  círculo 72dp primaryContainer
│                  │  📁 │                   │  ícone 36dp onPrimaryContainer
│                  ╰─────╯                   │
│                                            │  20dp
│   Para mostrar os PDFs do celular, o       │  titleLarge 22sp, centralizado
│   johnPDF precisa de permissão.            │  máx. 360dp
│                                            │  20dp
│   1. Toque em Permitir acesso              │
│   2. Ative a opção do johnPDF              │  bodyLarge 18sp, alinhado à esq.
│   3. Volte para o app                      │  dentro do bloco de 360dp
│                                            │  24dp
│   ╭────────────────────────────────────╮   │
│   │        Permitir acesso             │   │  Button (filled) 56dp
│   ╰────────────────────────────────────╯   │  largura máx. 360dp
│                                            │
└────────────────────────────────────────────┘
```

Único botão **filled** com cor de marca em toda a Home — é a ação primária inequívoca da tela.

#### Compose

```kotlin
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

`verticalScroll` é novo e necessário: a 2.0 de escala de fonte o bloco estoura a altura em paisagem.

---

## 6. Leitor

### 6.1 Shell

```
┌────────────────────────────────────────────┐
│ ▓▓▓ barra de status ▓▓▓                    │
├────────────────────────────────────────────┤
│  ←     Relatorio_anual_2026.pdf        ⟳   │ 64dp  TopAppBar
├────────────────────────────────────────────┤
│░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│  ← surfaceContainerHigh (gap)
│░┌────────────────────────────────────────┐░│
│░│                                        │░│
│░│                                        │░│
│░│              página 4                  │░│  branco, shadowElevation 1dp
│░│                                        │░│
│░│                                        │░│              ╭────╮
│░│                                        │░│              │ ▲  │ 56dp
│░└────────────────────────────────────────┘░│              ├────┤ PagerRail
│░░░░░░░░ gap 8dp ░░░░░░░░░░░░░░░░░░░░░░░░░░░│              │ ▼  │ 56dp
│░┌────────────────────────────────────────┐░│              ╰────╯
│░│              página 5                  │░│              ↑ 16dp da borda
├────────────────────────────────────────────┤
│              Página 4 de 200               │ 72dp  barra inferior
├────────────────────────────────────────────┤
│ ▓▓▓ barra de navegação do sistema ▓▓▓      │
└────────────────────────────────────────────┘
```

O `PagerRail` flutua sobre o conteúdo, alinhado à direita, com sua base 8dp **acima** da barra inferior — por isso o desenho o mostra transbordando a faixa de 72dp (§1.1b).

### 6.2 `TopAppBar` — voltar só com seta + rotação

| Item | Valor |
|---|---|
| Navigation | `IconButton` 48dp, ícone `arrow_back` (auto-mirrored), `contentDescription = "Voltar"`. **Sem o texto "Voltar".** |
| Título | nome do arquivo, `titleMedium` 18sp, `maxLines = 1`, `TextOverflow.Ellipsis` |
| Action | `RotationLockAction` — **única** ação, só quando `status is ReaderStatus.Ready` |
| Cor | `containerColor = surface` |

O `RotationLockAction` segue D1 §C integralmente: `IconToggleButton` de 48dp, ícone 24dp, **sem preenchimento azul**; destravado = `screen_rotation` em `onSurfaceVariant` sem fundo; travado = `screen_lock_rotation` em `onSurface` sobre um círculo `surfaceContainerHighest` de 40dp. `TooltipBox` + `PlainTooltip` no toque longo. Cada toque dispara um Snackbar (§6.5).

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
            IconButton(onClick = onBack) {
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

### 6.3 Barra inferior (72dp) + `PagerRail`

#### Barra inferior

| Item | Valor |
|---|---|
| Altura | 72dp + `navigationBarsPadding()` |
| Container | `surfaceContainer`, sem elevação tonal |
| Conteúdo | **só** o indicador: `"Página 4 de 200"`, `labelMedium` 16sp, `onSurfaceVariant`, centralizado |
| Padding final | `end = 88.dp` para o texto não passar por baixo do `PagerRail` quando o número for longo ("Página 1000 de 2000") |
| Visibilidade | só com `status is ReaderStatus.Ready` e `showBars` |

> **Rótulo.** Mantém `"Página 4 de 200"` (`pageLabel` inalterado) em vez do `"4 de 200"` proposto em D1 linha 115. Com as setas fora da faixa sobra largura de sobra, o rótulo completo é mais claro para o público-alvo, e `PageMath.kt`/`PageMathTest`/`ReaderContentTest` não precisam mudar por causa dele.

#### `PagerRail`

| Item | Valor |
|---|---|
| Forma | `RoundedCornerShape(28.dp)`, 56dp de largura × 112dp de altura |
| Container | `surfaceContainerHigh`, `shadowElevation = 3.dp` |
| Ancoragem | `Alignment.BottomEnd` do `Box` de conteúdo; `padding(end = 16.dp, bottom = 8.dp)` |
| Botões | dois `IconButton` de 56dp empilhados: `keyboard_arrow_up` (cima) e `keyboard_arrow_down` (baixo), ícone 28dp |
| Desabilitado | primeira página desabilita ▲; última desabilita ▼ — **tonal esmaecido** (`onSurface.copy(alpha = 0.38f)`), nunca cinza sólido |
| `contentDescription` | "Página anterior" / "Próxima página" |
| Divisor interno | `HorizontalDivider` `outlineVariant` de 1dp entre os dois, com 12dp de recuo de cada lado |
| Visibilidade | acompanha `showBars` (§6.4) |

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
            IconButton(
                onClick = onPrevious,
                enabled = canGoPrevious,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(JohnIcons.KeyboardArrowUp, contentDescription = "Página anterior", modifier = Modifier.size(28.dp))
            }
            HorizontalDivider(
                Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            IconButton(
                onClick = onNext,
                enabled = canGoNext,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(JohnIcons.KeyboardArrowDown, contentDescription = "Próxima página", modifier = Modifier.size(28.dp))
            }
        }
    }
}

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

#### Paisagem

Mesma barra de 72dp e mesmo `PagerRail` — nada muda de forma, o que já resolve o problema de D1 §A (hoje a barra come ~55% da tela em paisagem). Ganho medido pela geometria: a área de leitura sai de ~30% para ~72% da altura em paisagem.

```
┌──────────────────────────────────────────────────────────────────┐
│  ←    Relatorio_anual_2026.pdf                              ⟳    │ 64dp
├──────────────────────────────────────────────────────────────────┤
│░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░╭────╮░░│
│░░┌────────────────────────────────────────────────────┐░░│ ▲  │░░│
│░░│                     página 4                       │░░├────┤░░│
│░░└────────────────────────────────────────────────────┘░░│ ▼  │░░│
├──────────────────────────────────────────────────────────╰────╯──┤
│                        Página 4 de 200                           │ 72dp
└──────────────────────────────────────────────────────────────────┘
```

### 6.4 Modo imersivo (preservar)

O toque simples no PDF alterna `barsVisible`; `showBars = barsVisible || status !is Ready`. O `PagerRail` **entra na mesma animação** que as barras — senão ficaria um controle flutuando sozinho sobre a página em tela cheia, que é exatamente o ruído que o modo imersivo quer eliminar.

```kotlin
Box(Modifier.padding(padding).fillMaxSize()) {
    // … PageList / Loading / diálogos …

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
```

O `PagerRail` fica **dentro** do `content` do `Scaffold` (não no `bottomBar`), alinhado ao `BottomEnd`; com o `padding` do `Scaffold` aplicado, o `bottom = 8.dp` já o posiciona acima da barra de 72dp.

### 6.5 `SnackbarHost`

`Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) })`. O `Scaffold` posiciona o Snackbar **acima** da `bottomBar`, e como as setas agora estão no trilho à direita (56dp de largura), um Snackbar de largura quase total ainda pode cobri-las por ~4s — daí `SnackbarDuration.Short` obrigatório (risco 4 de D1 §F).

| Gatilho | Mensagem | Duração |
|---|---|---|
| Travar rotação | "Tela travada nesta posição" | `Short` |
| Destravar rotação | "Rotação automática" | `Short` |
| Abrir documento | **nenhuma** (evita ruído — D1 §C) | — |

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
val scope = rememberCoroutineScope()

// no callback do IconToggleButton, depois de onToggleRotation():
scope.launch {
    snackbarHostState.currentSnackbarData?.dismiss()   // toques rápidos não enfileiram
    snackbarHostState.showSnackbar(
        message = if (willBeLocked) "Tela travada nesta posição" else "Rotação automática",
        duration = SnackbarDuration.Short,
    )
}
```

`willBeLocked` é o estado **depois** do toque (`!state.rotationLocked` no momento do clique) — o `StateFlow` do ViewModel ainda não recompôs quando o Snackbar é disparado.

### 6.6 Estados não-`Ready`

| Estado | Layout |
|---|---|
| `Loading` | `TopAppBar` **sem** ação de rotação · centro: `CircularProgressIndicator` + `Text("Abrindo…", bodyLarge)` · **sem** barra inferior, **sem** `PagerRail` |
| `NeedsPassword` | diálogo de senha (§7.3) sobre o fundo `surfaceContainerHigh` vazio |
| `Failed` | diálogo de erro (§7.2); `OK` chama `onBack` |

### 6.7 Fundo entre páginas

```kotlin
BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) { … }

LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),      // era 12dp
    contentPadding = PaddingValues(vertical = 8.dp),       // era 12dp
) { … }
```

Cada página ganha `Surface(shadowElevation = 1.dp, color = Color.White)` em volta do `Image` — a página continua **branca** independentemente do modo escuro (o "modo noturno" que inverte o PDF segue fora de escopo). `PageGapColor` sai de `Theme.kt`.

---

## 7. Diálogos

Regras comuns a todos, conforme a decisão "AlertDialog M3 com ações alinhadas":

- `AlertDialog` do M3, `containerColor = surfaceContainerHigh`, forma padrão (28dp).
- **Ações alinhadas à direita**, na ordem M3: dismissiva à esquerda da confirmatória.
- **Uma** ação principal por diálogo — `Button` filled, `heightIn(min = PrimaryTouchTarget)`. As demais são `TextButton` de 48dp.
- Ícone de 24dp no slot `icon` (fica centralizado acima do título no M3).
- Título `titleLarge` 22sp; corpo `bodyLarge` 18sp.
- Nada de `BigButton` de 64dp com cor cheia nos dois lados (o "Não/Sim" azul de hoje).

### 7.1 Remover da lista

```
        ╭──────────────────────────────────────╮
        │                 🗑                    │  ícone 24dp
        │                                      │
        │        Remover da lista?             │  titleLarge, centralizado
        │                                      │
        │   O arquivo continua no celular.     │  bodyLarge
        │                                      │
        │            ╭─────────╮ ╭──────────╮  │
        │            │Cancelar │ │ Remover  │  │  TextButton 48 · Button 56
        │            ╰─────────╯ ╰──────────╯  │  alinhados à direita
        ╰──────────────────────────────────────╯
```

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
```

"Sim"/"Não" saem de cena: rótulos que dizem o que a ação faz ("Remover") são mais seguros para ação destrutiva do que um "Sim" genérico.

### 7.2 Erro

```
        ╭──────────────────────────────────────╮
        │                 ⚠                    │  ícone 24dp, tint error
        │                                      │
        │  Não foi possível abrir este         │  bodyLarge, centralizado
        │  arquivo.                            │  (sem título — a mensagem é o título)
        │                                      │
        │                          ╭────────╮  │
        │                          │   OK   │  │  Button 56dp
        │                          ╰────────╯  │
        ╰──────────────────────────────────────╯
```

```kotlin
@Composable
fun ErrorDialog(error: AppError, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(JohnIcons.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
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

As três mensagens de `AppError` continuam **idênticas** (`CORRUPTED`, `GONE`, `NO_SPACE`) — a spec §4.3 do produto não muda.

### 7.3 Senha

```
        ╭──────────────────────────────────────╮
        │                 🔒                   │  ícone 24dp
        │                                      │
        │          PDF protegido               │  titleLarge
        │                                      │
        │  ╭────────────────────────────────╮  │
        │  │ Senha                     👁   │  │  OutlinedTextField 56dp
        │  ╰────────────────────────────────╯  │  trailing = visibility toggle
        │                                      │
        │  Senha incorreta, tente de novo      │  só se wrongAttempt, cor error
        │                                      │
        │  ╭──────────────────╮                │
        │  │ ⌨  Usar letras   │                │  TextButton 48dp com ícone
        │  ╰──────────────────╯                │  (alterna teclado num/alfa)
        │                                      │
        │            ╭─────────╮ ╭──────────╮  │
        │            │Cancelar │ │  Abrir   │  │
        │            ╰─────────╯ ╰──────────╯  │
        ╰──────────────────────────────────────╯
```

| Mudança | De | Para |
|---|---|---|
| Título | "Este PDF tem senha" | **"PDF protegido"** + ícone `lock` |
| Alternar teclado | `"abc  Usar letras"` / `"123  Usar números"` (texto) | `TextButton` com ícone `keyboard`/`dialpad` + `"Usar letras"`/`"Usar números"` |
| Ver senha | não existia | `IconButton` trailing `visibility`/`visibility_off`, `contentDescription = "Mostrar senha"`/`"Ocultar senha"` |
| Confirmar | `BigButton("Abrir")` 64dp | `Button("Abrir")` 56dp, `enabled = password.isNotEmpty()` (inalterado) |
| Cancelar | `TextButton` 64dp | `TextButton` 48dp |

Comportamento preservado: `numeric = true` por padrão (teclado numérico primeiro), `testTag("password_field")`, `rememberSaveable` em `password` e `numeric`, cancelar volta para a Home.

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
                        IconButton(onClick = { visible = !visible }) {
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

---

## 8. Acessibilidade e testabilidade

### 8.1 Tabela de `contentDescription`

| Elemento | `contentDescription` | Observação |
|---|---|---|
| Ícones da `NavigationBar` | `null` | o rótulo textual já nomeia |
| Ícone do `FilledTonalButton` "Abrir PDF" | `null` | o texto do botão nomeia |
| Ícone de PDF na lista | `null` | decorativo |
| Voltar (leitor) | `"Voltar"` | **substitui** o texto "← Voltar" |
| Rotação | `"Travar rotação da tela"` + `stateDescription` "Travada"/"Automática" | `assertIsOn()`/`assertIsOff()` |
| Seta ▲ | `"Página anterior"` | |
| Seta ▼ | `"Próxima página"` | |
| Limpar busca | `"Limpar busca"` | |
| Mostrar/ocultar senha | `"Mostrar senha"` / `"Ocultar senha"` | |
| Ícones de diálogo e de estado vazio | `null` | o título adjacente carrega o significado |
| Página renderizada | `"Página ${index + 1}"` | inalterado |

### 8.2 Escala de fonte

Validar a **1.0, 1.3 e 2.0** (`fontScale`), nestes pontos de risco:

1. `TopAppBar` da Home — "johnPDF" + botão "Abrir PDF" na mesma linha de 64dp. A 2.0 o botão deve encolher o texto antes de cortar o título; se colidir, o botão vira `IconButton` só com `folder_open` a partir de `fontScale >= 1.8` (fallback documentado, não implementado por padrão).
2. `NavigationBar` — "Todos os PDFs" a 2.0.
3. Título do leitor — já tem `maxLines = 1` + ellipsis.
4. `PermissionContent` — resolvido pelo `verticalScroll` (§5.5).
5. `PagerRail` — imune (só ícones).

### 8.3 Testes a ajustar

| Teste | Mudança |
|---|---|
| `ThemeTest` | piso 20sp → **16sp** |
| `ReaderContentTest` | `"← Voltar"` → `onNodeWithContentDescription("Voltar")`; `"⬆ Anterior"`/`"⬇ Próxima"` → `onNodeWithContentDescription("Página anterior")`/`("Próxima página")`; `rotation_button_toggles_label` → `onNodeWithContentDescription("Travar rotação da tela").assertIsOff()/assertIsOn()`; `"Este PDF tem senha"` → `"PDF protegido"` |
| `ReaderContentTest` (novo) | `PagerRail` some junto com as barras no toque simples; ▲ desabilitado na página 1, ▼ na última |
| `HomeContentTest` | `"📂 Abrir"` → `"Abrir PDF"`; `"Sim"` → `"Remover"`; novo: "Limpar busca" aparece só com texto na busca |
| `DialogsTest` | `"abc  Usar letras"`/`"123  Usar números"` → `"Usar letras"`/`"Usar números"`; `"Sim"` → `"Remover"`; novo: alternar `visibility` |
| `MainActivitySmokeTest` | `"Todos os PDFs"` continua válido |
| `PageMathTest` / `PageMath.kt` | **sem mudança** — o rótulo continua "Página X de Y" (§6.3) |
| E2E | refazer todas as capturas de `docs/e2e/img/` e `docs/e2e/device/img/`; atualizar `docs/e2e/2026-09-28-checklist.md` |

### 8.4 Spec do produto

A §4 da spec ("mínimo 20sp/64dp", "ícone sempre acompanhado de texto") precisa ser reescrita: o piso vira **16sp / 48dp (56dp para ações frequentes)** e passa a existir a categoria de **ação universal só com ícone** (voltar, rotação, ▲/▼, limpar busca, mostrar senha), sempre com `contentDescription` e, nas menos óbvias, tooltip e/ou Snackbar.

---

## 9. Pendências

| # | Item | Bloqueia |
|---|---|---|
| 1 | **Paleta** — o usuário recusou o índigo `#3558D4` e pediu um estudo em coolors.co com 1 ou 3 paletas com tom de azul (D1 §G-5) | Só o preenchimento de `LightColors`/`DarkColors`. Como toda esta spec usa papéis de cor, **nenhum layout muda** quando a paleta for definida. Exceção: o container do ícone de PDF (§5.2, nota). |
| 2 | Esquema **escuro** completo do mesmo seed | Idem |
| 3 | Vendorizar os ~16 Material Symbols em `JohnIcons.kt` + `NOTICE` (Apache 2.0) | Tudo — é o primeiro passo da implementação |
| 4 | Arquivos TTF de Atkinson Hyperlegible Next em `res/font` (OFL, +`NOTICE`) | §3.1 |
| 5 | Fallback do header a `fontScale >= 1.8` (§8.2 item 1) — implementar ou aceitar o corte | Nada; decidir na validação no aparelho |

### Ordem sugerida de implementação

1. `JohnIcons.kt` + fonte + `Theme.kt` (tipografia, alvos, esquema claro/escuro, remover `PageGapColor`)
2. `BigButton.kt` → `PrimaryButton` · `Dialogs.kt` (§7)
3. `HomeScreen.kt` (§5) — shell, lista, busca, vazios, permissão
4. `ReaderScreen.kt` (§6) — top bar, barra de 72dp, `PagerRail`, `SnackbarHost`, fundo das páginas; **juntar com o modo imersivo do commit `5de0feb`**, que mexe no mesmo arquivo
5. Testes (§8.3) e recaptura do E2E

**Riscos herdados de D1 §F** que esta spec ainda carrega: regressão de legibilidade ao descer de 20sp para 16–18sp (validar no aparelho da família a 1.3); rotação só com ícone ser menos óbvia (mitigada por tooltip + Snackbar); mudança de layout do leitor alterar a área do `LazyColumn` (revalidar "página dominante" e a preservação de posição ao girar).
