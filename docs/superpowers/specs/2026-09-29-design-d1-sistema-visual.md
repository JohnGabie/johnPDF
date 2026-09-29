# D1 — Sistema visual: tipografia, botões, alvos de toque e espaçamento

- **Data:** 2026-09-29 · **Status:** proposta (nenhum código alterado)
- **Escopo:** os quatro fundamentos que todas as telas herdam — tipografia, hierarquia de botões, alvos de toque e escala de espaçamento.
- **Base:** `docs/superpowers/specs/2026-09-29-design-d1-proposta.md` §D (linhas 80–116) + as respostas do usuário na §G.
- **Fora de escopo aqui:** paleta de cores (§G5 em aberto — o usuário rejeitou o índigo `#3558D4` e pediu um estudo no coolors.co), biblioteca de ícones (§B, spec própria) e o layout de cada tela (§D "Por tela", consumidor deste documento).
- **Arquivos afetados:** `ui/theme/Theme.kt`, `ui/common/BigButton.kt` (removido), `ui/common/Dialogs.kt`, `ui/common/Buttons.kt` (novo). Consumidores: `ui/home/HomeScreen.kt`, `ui/reader/ReaderScreen.kt`.

---

## 1. Princípio

O app não fica "mais bonito ficando menor" — fica mais legível porque passa a ter
**hierarquia**. Hoje tudo é 20sp e todo botão é uma pílula azul de 64dp: nada se
destaca porque tudo se destaca. A troca é:

| Antes (v1) | Depois |
|---|---|
| Um tamanho de texto (20sp) para tudo | 16sp mínimo, três degraus claros (18 SemiBold → 16 regular → 16 secundário) |
| Um botão (`BigButton` filled azul 64dp) para tudo | Quatro pesos (Filled / FilledTonal / Text / Icon) com regra de uso |
| 64dp de altura como prova de acessibilidade | 48dp mínimo (padrão M3, já > WCAG 2.5.5) + 56dp nas ações frequentes + linha de lista de 72dp inteira clicável |
| Paddings ad-hoc (4, 8, 12, 16, 20, 24) | Escala de 4dp com tokens nomeados |

O ganho de acessibilidade real vem de **alvo largo, bem espaçado e com rótulo
claro**, não de altura. E tudo continua em `sp`/`dp`, respeitando a escala de
fonte do sistema (no moto g41 a 1.3, 18sp vira ~23sp).

---

## 2. Tipografia

### 2.1 Tabela de estilos

Mínimo absoluto **16sp**. Todos os estilos derivam de `Typography()` do M3 via
`copy()`, preservando `letterSpacing` e `fontFamily`.

| Estilo | Hoje | Proposta (tamanho / entrelinha / peso) | Uso |
|---|---|---|---|
| `headlineMedium` | 30 Bold | **24 / 32 / Bold** | reservado (nenhum uso após o refresh) |
| `titleLarge` | 24 SemiBold | **22 / 28 / SemiBold** | "johnPDF" no top bar, título de diálogo |
| `titleMedium` | 22 SemiBold | **18 / 24 / SemiBold** | nome do PDF na lista, nome do arquivo no top bar do leitor |
| `titleSmall` | 20 SemiBold | **16 / 20 / SemiBold** | título de estado vazio |
| `bodyLarge` | 20 / 28 | **18 / 26 / Normal** | texto de estado vazio, mensagem de erro, campo de busca, passos da permissão |
| `bodyMedium` | 20 / 28 | **16 / 24 / Normal** | linha secundária do item ("Download · ontem"), texto de apoio do diálogo |
| `bodySmall` | 20 / 28 | **16 / 20 / Normal** | pouco usado |
| `labelLarge` | 20 SemiBold | **16 / 20 / SemiBold** | rótulo de botão (Filled, Tonal, Text) |
| `labelMedium` | 20 | **16 / 20 / Medium** | rótulo da bottom nav, "4 de 200" |
| `labelSmall` | 20 | **16 / 16 / Medium** | pouco usado |

**Regra de hierarquia (invariante testável):**
`titleLarge > titleMedium ≥ bodyLarge > bodyMedium ≥ labelLarge` e nenhum estilo
abaixo de 16sp. A distinção entre os dois últimos degraus é feita por **peso +
cor** (`SemiBold`/`onSurface` vs `Normal`/`onSurfaceVariant`), não por tamanho —
é o que permite escanear a lista sem que nada encolha abaixo do confortável.

### 2.2 Família tipográfica

§G6 decidido: **Atkinson Hyperlegible Next** (OFL, desenhada para baixa visão).
TTF em `app/src/main/res/font/`, sem `INTERNET`, ~150 KB. Entra como um único
ponto de configuração no `Theme.kt`:

```kotlin
// ui/theme/Type.kt (ou topo de Theme.kt)
private val Atkinson = FontFamily(
    Font(R.font.atkinson_hyperlegible_next_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_next_medium, FontWeight.Medium),
    Font(R.font.atkinson_hyperlegible_next_semibold, FontWeight.SemiBold),
    Font(R.font.atkinson_hyperlegible_next_bold, FontWeight.Bold),
)
```

Enquanto os arquivos não estiverem no repositório, `JohnFontFamily =
FontFamily.Default` e a tabela acima continua válida — **os tamanhos não dependem
da fonte**. A troca da família é um commit isolado (1 linha + `res/font` +
`NOTICE`).

---

## 3. Botões — hierarquia e alvos de toque

### 3.1 Tokens de alvo

| Token | Valor | Onde |
|---|---|---|
| (implícito do M3) | **48dp** | piso de qualquer coisa clicável — o M3 já aplica via `LocalMinimumInteractiveComponentSize`; **não escrever `heightIn` para obter isso** |
| `PrimaryTouchTarget` | **56dp** | ação primária de tela/diálogo e ações frequentes (Anterior/Próxima, "Permitir acesso", "Abrir" do header) |
| `ListItemMinHeight` | **72dp** | linha de lista inteira clicável |
| `MinGap` | **8dp** | folga mínima entre dois alvos vizinhos |
| `MaxActionWidth` | **360dp** | largura máxima de um botão de largura cheia (evita a pílula gigante em tablet/paisagem) |

`MinTouchTarget = 64.dp` **deixa de existir**. Os três `Modifier.heightIn(min =
MinTouchTarget)` atuais (`Dialogs.kt:73`, `Dialogs.kt:80`, `ReaderScreen.kt:210`)
são simplesmente apagados: `TextButton` e `IconButton` já nascem com 48dp de
alvo.

### 3.2 Regra de uso

| Peso | Componente M3 | Altura | Quando |
|---|---|---|---|
| **Filled** | `Button` | 56dp | **uma** por tela ou diálogo — a ação que o usuário veio fazer ("Permitir acesso", "Abrir" na senha, "Remover" na confirmação) |
| **FilledTonal** | `FilledTonalButton` / `FilledTonalIconButton` | 56dp (frequentes) ou 48dp | ações secundárias repetidas: Anterior/Próxima, "Abrir" do header da Home |
| **Text** | `TextButton` | 48dp (padrão) | cancelar, alternativas, ação redundante ("Usar letras", "Abrir PDF" do estado vazio) |
| **Icon** | `IconButton` / `IconToggleButton` | 48dp | ações universais e auto-explicativas por convenção: voltar, travar rotação, limpar busca |

Invariantes:
1. **Nunca dois Filled na mesma superfície.** Em diálogo: `dismissButton` é
   sempre `TextButton`.
2. **Ícone sozinho só nos quatro casos da linha "Icon"** — e sempre com
   `contentDescription`. Qualquer outra ação leva texto.
3. **Botão desabilitado continua visível** (tonal esmaecido), nunca cinza sólido
   — corrige o "Anterior" ilegível visto em `docs/e2e/device/img/05`.
4. Entre dois botões lado a lado: `Arrangement.spacedBy(MinGap)`.

### 3.3 `ui/common/Buttons.kt` (substitui `BigButton.kt`)

Só dois wrappers, e existem por um motivo único: aplicar `PrimaryTouchTarget` e
o slot de ícone. Text e Icon usam os componentes M3 crus.

```kotlin
package com.johngabie.johnpdf.ui.common

/** Ação principal — no máximo uma por tela/diálogo. */
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
    modifier = modifier.heightIn(min = PrimaryTouchTarget).widthIn(max = MaxActionWidth),
    contentPadding = PaddingValues(horizontal = SpaceL, vertical = SpaceM),
) { ButtonContent(text, icon) }

/** Ação secundária frequente (Anterior/Próxima, "Abrir" do header). */
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

`contentDescription = null` no ícone é proposital: o texto ao lado já é o rótulo
acessível; descrever o ícone duplicaria a leitura.

---

## 4. Espaçamento e gaps

Escala de **4dp**, nomeada, sem números soltos no código de tela:

| Token | Valor | Uso |
|---|---|---|
| `SpaceXs` | 4dp | folga interna de ícone, divisor |
| `SpaceS` | 8dp | entre alvos vizinhos (`MinGap`), gap entre páginas do PDF |
| `SpaceM` | 12dp | padding vertical interno de botão, entre campos de um diálogo |
| `SpaceL` | 16dp | **padding padrão de tela** (`ScreenPadding`), padding horizontal de lista/botão |
| `SpaceXl` | 24dp | entre blocos de uma tela (ícone → título → texto → ação) |
| `SpaceXxl` | 32dp | respiro de estado vazio/permissão |

Regras:
- **Lista plana, não cartões espaçados.** `ListItem` de 72dp encostado no
  seguinte, separados por divisor `outlineVariant` recuado 72dp à esquerda
  (alinhado ao texto, não ao ícone). Hoje são `Surface`s de 80dp com
  `spacedBy(12.dp)`, que desperdiça ~15% da altura útil da lista.
- **Padding horizontal único de 16dp** em toda a árvore (top bar, lista, busca,
  diálogo). Nada de 8dp no top bar e 16dp na lista como hoje.
- **Barras do sistema** tratadas por `Scaffold` + `WindowInsets`, nunca por
  `padding` manual somado a `statusBarsPadding()`.
- **Gap entre páginas do PDF: 8dp** (`SpaceS`), cor `surfaceContainerHigh` em
  vez do `#BDBDBD` chapado; a página recebe `shadowElevation = 1.dp`.
- **Elevação:** tons, não sombras. `Surface(tonalElevation = 3.dp)` sai das
  barras; top bar usa `surface` e `surfaceContainer` ao rolar
  (`scrolledContainerColor`).

---

## 5. Componentes — antes / depois

| Componente | Hoje | Depois |
|---|---|---|
| Header da Home | `Surface(tonalElevation=3)` + "johnPDF" `headlineMedium` 30sp + `BigButton("📂 Abrir")` 64dp azul | `TopAppBar` 64dp, título `titleLarge` 22sp; ação = `SecondaryButton(icon = folderOpen, "Abrir")` 48dp tonal (§G3: fica no header, sem FAB) |
| Item de lista | `Surface` 80dp, raio 16, tonal 2, "📄" 32sp, nome 22sp, subtítulo 20sp | `ListItem` 72dp sem cartão, leading 40dp com `picture_as_pdf`, headline `titleMedium` 18 SemiBold (2 linhas), supporting `bodyMedium` 16 `onSurfaceVariant`, divisor recuado |
| Bottom nav | emoji 28sp + rótulo 20sp | `NavigationBar` M3, ícone 24dp (contorno ↔ preenchido), rótulo `labelMedium` 16sp sempre visível |
| Busca | `OutlinedTextField` retangular 64dp, "🔍" dentro do placeholder | `TextField` pílula (raio 28dp, **56dp**), `leadingIcon = search`, `trailingIcon = close` quando há texto, texto `bodyLarge` 18sp |
| Voltar (leitor) | `TextButton("← Voltar")` com `heightIn(64dp)` | `IconButton(arrowBack)` 48dp, `contentDescription = "Voltar"` (§G2: só a seta) |
| Rotação | `BigButton("🔒 Travada"/"🔓 Gira sozinha")` na barra inferior | `IconToggleButton` 48dp no `actions` do top bar, sem preenchimento azul (detalhado na §C da proposta) |
| Anterior / Próxima | dois `BigButton` 64dp com "⬆"/"⬇", 2ª linha da barra (~315px no total) | `FilledTonalIconButton` 56dp **empilhados à direita** (§G1), setas `keyboard_arrow_up`/`down`, `contentDescription` "Página anterior"/"Próxima página"; "4 de 200" (`labelMedium`) permanece visível à esquerda |
| Ação de diálogo | dois `BigButton` filled ("Sim"/"Não") | `TextButton("Cancelar")` + `PrimaryButton("Remover")` |
| Permissão | `BigButton("Permitir", fillMaxWidth)` 64dp | `PrimaryButton("Permitir acesso")` 56dp, largura máx. 360dp |
| Estado vazio | frase solta centralizada 20sp | ícone 48dp + `titleSmall` 16 SemiBold + `bodyLarge` 18sp + `TextButton("Abrir PDF")` |

### 5.1 Exemplo — ações de diálogo

```kotlin
// antes (Dialogs.kt:40-41)
confirmButton = { BigButton("Sim", onYes) },
dismissButton  = { BigButton("Não", onNo) },

// depois
confirmButton = { PrimaryButton("Remover", onYes) },
dismissButton = { TextButton(onCancel) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) } },
```

### 5.2 Exemplo — navegação de páginas (uma linha, setas à direita)

```kotlin
Row(
    Modifier.fillMaxWidth().padding(horizontal = SpaceL, vertical = SpaceS),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(pageLabel(current, total), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
    Column(verticalArrangement = Arrangement.spacedBy(MinGap)) {
        FilledTonalIconButton(
            onClick = onPrevious,
            enabled = current > 0,
            modifier = Modifier.size(PrimaryTouchTarget),
        ) { Icon(JohnIcons.ArrowUp, contentDescription = "Página anterior") }
        FilledTonalIconButton(
            onClick = onNext,
            enabled = current < total - 1,
            modifier = Modifier.size(PrimaryTouchTarget),
        ) { Icon(JohnIcons.ArrowDown, contentDescription = "Próxima página") }
    }
}
```

Nota de risco: §G1 pede setas empilhadas sem texto — mais econômico em altura,
porém menos explícito para quem não domina o gesto de rolagem. Mitigação: alvo
de 56dp, `contentDescription` completo e o rótulo "4 de 200" sempre presente. Se
o teste no aparelho da família mostrar hesitação, a alternativa é a linha única
com `SecondaryButton("Anterior")` / `SecondaryButton("Próxima")` da §D.

---

## 6. O que muda em `ui/theme/Theme.kt`

Hoje: 47 linhas com 3 responsabilidades misturadas (2 tokens soltos, um
`lightColorScheme` de 11 papéis, `BigTypography`). Depois:

1. **`MinTouchTarget = 64.dp` → removido.** Entram `PrimaryTouchTarget = 56.dp`,
   `ListItemMinHeight = 72.dp`, `MinGap = 8.dp`, `MaxActionWidth = 360.dp`.
2. **Escala de espaçamento nova:** `SpaceXs/S/M/L/Xl/Xxl` + `ScreenPadding`.
3. **`PageGapColor = Color(0xFFBDBDBD)` → removido.** O gap passa a ler
   `MaterialTheme.colorScheme.surfaceContainerHigh` (segue o tema, inclusive no
   escuro). Entra `PageGap = SpaceS` e `PageElevation = 1.dp`.
4. **`BigTypography` → `JohnTypography`**, com os 10 estilos da tabela §2.1 e
   `fontFamily = JohnFontFamily`. O nome `BigTypography` some (é o rótulo do
   problema que estamos corrigindo); `ThemeTest` acompanha.
5. **Esquema de cor completo** (todos os papéis, incluindo `surfaceContainer*`,
   `secondaryContainer`, `outlineVariant`) — acaba com o lilás vazado. **Os
   valores ficam pendentes da §G5** (estudo de paleta); a estrutura já entra
   neste refactor com `LightColors`/`DarkColors`.
6. **Modo escuro** (§G4 aprovado): `isSystemInDarkTheme()` escolhe o esquema.
   Cor dinâmica fica desligada (§G5: identidade própria).

```kotlin
// ui/theme/Theme.kt — forma final
val SpaceXs = 4.dp; val SpaceS = 8.dp; val SpaceM = 12.dp
val SpaceL = 16.dp; val SpaceXl = 24.dp; val SpaceXxl = 32.dp
val ScreenPadding = SpaceL
val MinGap = SpaceS

val PrimaryTouchTarget = 56.dp
val ListItemMinHeight = 72.dp
val MaxActionWidth = 360.dp

val PageGap = SpaceS
val PageElevation = 1.dp

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

@Composable
fun JohnPdfTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = JohnTypography,
        content = content,
    )
}
```

---

## 7. O que muda em `ui/common/`

| Arquivo | Mudança |
|---|---|
| `BigButton.kt` | **Apagado.** 3 chamadas em `Dialogs.kt`, 2 em `HomeScreen.kt`, 3 em `ReaderScreen.kt` migram. |
| `Buttons.kt` (novo) | `PrimaryButton` e `SecondaryButton` (§3.3). Text e Icon não ganham wrapper. |
| `Dialogs.kt` | `ErrorDialog`: ícone `error`, texto `bodyLarge`, `confirmButton = TextButton("OK")` (é um aviso, não uma ação — não merece Filled). `ConfirmDialog`: vira `RemoveDialog` com ícone `delete`, título "Remover da lista?", texto de apoio "O arquivo continua no celular.", ações `TextButton("Cancelar")` + `PrimaryButton("Remover")` — "Sim"/"Não" somem. `PasswordDialog`: ícone `lock`, título "PDF protegido", `visibility` no campo, `TextButton` com ícone `keyboard`/`dialpad` e rótulo "Usar letras"/"Usar números" (sem o prefixo "abc "/"123 "), `PrimaryButton("Abrir")`. Os dois `heightIn(min = MinTouchTarget)` saem; `spacedBy(12.dp)` vira `spacedBy(SpaceM)`. |

Assinaturas públicas que mudam (chamadores a ajustar): `BigButton(...)` →
`PrimaryButton(...)`/`SecondaryButton(...)`; `ConfirmDialog(question, onYes,
onNo)` → `RemoveDialog(onConfirm, onCancel)` (a pergunta deixa de ser parâmetro
— há um único uso, em `HomeScreen.kt:145`).

---

## 8. Verificação

**`ThemeTest` — reescrito** (deixa de ser "tudo ≥ 20sp"):
```kotlin
@Test fun nenhum_estilo_abaixo_de_16sp()          // piso
@Test fun hierarquia_e_decrescente()               // titleLarge > titleMedium >= bodyLarge > bodyMedium >= labelLarge
@Test fun entrelinha_maior_que_o_tamanho()         // lineHeight >= fontSize em todos
@Test fun alvo_primario_acima_do_minimo_do_m3()    // PrimaryTouchTarget >= 48.dp; ListItemMinHeight >= 72.dp
```

**Testes de tela a ajustar** (já mapeados na §F da proposta): `HomeContentTest`
("📂 Abrir" → "Abrir"; "Sim" → "Remover"), `DialogsTest` ("abc  Usar letras" →
"Usar letras"; "Sim" → "Remover"), `ReaderContentTest` ("← Voltar" →
`onNodeWithContentDescription("Voltar")`; "⬆ Anterior"/"⬇ Próxima" →
`contentDescription` "Página anterior"/"Próxima página"; rotação por
`assertIsOff()/assertIsOn()`).

**Manual, no aparelho:** escala de fonte do sistema a 1.0 / 1.3 / 2.0 — top bar,
bottom nav e rótulos de botão não podem truncar nem quebrar em duas linhas
(`maxLines` + `Ellipsis` onde couber). É a mitigação direta do risco (1) da §F:
regressão de legibilidade ao descer de 20 para 16–18sp.

**Documentos a atualizar** — as três regras "mínimo 20sp", "mínimo 64dp" e
"ícone sempre acompanhado de texto" mudam com esta spec:
- `docs/superpowers/specs/2026-09-28-johnpdf-leitor-android-design.md:99-101` (§4, "Padrões visuais")
- `docs/superpowers/plans/2026-09-28-johnpdf-leitor-android.md:19` (restrições) e `:2625` (layout da barra do leitor em duas linhas)
- `docs/e2e/2026-09-28-checklist.md` (rótulos dos cenários)

---

## 9. Ordem de implementação

1. `Theme.kt` (tokens + tipografia + esquema claro/escuro) e `ThemeTest` — nada
   quebra: `BigButton` continua compilando até o passo 2.
2. `Buttons.kt` + apagar `BigButton.kt` + migrar os 8 call sites.
3. `Dialogs.kt` + `DialogsTest`.
4. Telas (`HomeScreen.kt`, `ReaderScreen.kt`) — **junto com a task do modo
   imersivo**, que toca o mesmo `ReaderScreen.kt` (ver handoff).
5. Fonte Atkinson (`res/font` + `NOTICE`) — commit isolado, 1 linha de código.

Passos 1–3 não dependem da decisão de paleta (§G5): usam os papéis do
`colorScheme`, não cores literais. Só o passo 1 fica com `LightColors`/
`DarkColors` provisórios até a paleta ser escolhida.
