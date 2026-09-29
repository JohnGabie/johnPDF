# D1 — Análise do refresh de design (johnPDF)

- **Data:** 2026-09-29 · **Tipo:** análise (nenhum código alterado)
- **Pedido:** `design-task.md` (ícones de biblioteca, rotação no header só com ícone e sem azul, visual moderno para público amplo sem perder a facilidade para idosos).
- **Base:** spec §1/§2/§4, `Theme.kt`, `BigButton.kt`, `Dialogs.kt`, `HomeScreen.kt`, `ReaderScreen.kt`, capturas em `docs/e2e/img/` e `docs/e2e/device/img/`.

---

## A. Diagnóstico

### O que faz parecer "app para idoso"
| Sintoma | Onde | Causa no código |
|---|---|---|
| Tudo em 20sp, inclusive rótulos, legendas e o "Download · ontem" — não há hierarquia, tudo "grita" | 02, 11a | `BigTypography`: todos os estilos = 20sp (labelSmall/bodySmall inclusive) |
| Botões azuis cheios e enormes (pílula de 64dp) em toda ação, até "Não"/"Sim"/"OK" | 01 ("📂 Abrir"), 04b, 10a, 11b | `BigButton` = `Button` filled + `heightIn(64.dp)` usado em todo lugar |
| Emojis coloridos como ícones (📂 🕘 📚 📄 🔍 🔓 🔒) — estilo "WhatsApp de família", inconsistentes com o sistema e renderizados diferente por fabricante | 01, 02, 04b | `Text("🕘", fontSize = 28.sp)` etc. |
| Barra do leitor ocupa ~16% da tela em retrato e **~55% em paisagem** (só ~30% da altura sobra para o PDF) | 04b, 07a, device/05 | Duas linhas de `BigButton` 64dp + rótulo 22sp + paddings |
| Três botões azuis iguais competindo (rotação tem o mesmo peso que navegar) | 04b, device/03 | `BigButton` para "Gira sozinha" |
| "Não" e "Sim" com o mesmo botão azul cheio — nenhum destaque para a ação principal, visual de "teclado de telefone para idoso" | 11b | `ConfirmDialog` com dois `BigButton` |
| Paleta incompleta: header azul-acinzentado, mas barra inferior, indicador da aba e diálogos saem **lilás** (baseline roxo do M3 vazando) | 01, 09b, 10a, 11b | `lightColorScheme` só define 11 papéis; `surfaceContainer*`, `secondaryContainer` ficam no padrão roxo |
| Pseudo-ícones em texto ("← Voltar", "⬆", "abc  Usar letras") | 04b, 09b | strings com setas/`abc` |
| Espaço cinza chapado (#BDBDBD) entre páginas, faixa grossa colada no header | 04b | `PageGapColor` |

### O que é acessibilidade boa e deve ficar
- **Contraste alto** (texto quase preto sobre fundo claro) — 02, 11a.
- **Textos em pt-BR simples e sem jargão**: "Os PDFs que você abrir vão aparecer aqui.", erros da §4.3 — 01, 10a.
- **Tela de permissão com passos numerados** e um único botão de ação — 02a. Só precisa de acabamento visual.
- **Bottom navigation com rótulo sempre visível** (Recentes / Todos os PDFs) — 01.
- **Nome do arquivo grande no cartão + origem amigável** ("Download · ontem") — 02.
- **"Página 4 de 200" sempre visível** e botões Anterior/Próxima explícitos — 04b (útil para quem não domina rolagem).
- **Confirmação antes de remover** e **teclado numérico por padrão na senha** — 11b, 09b.
- **Texto em sp** (respeita a escala de fonte do sistema; o run no aparelho com 1.3 continua legível — device/02, device/03).

---

## B. Biblioteca de ícones

Contexto medido: APK release arm64 = **18,9 MB** (dex ≈ 22 MB descomprimido → 7,5 MB no APK; `libmupdf_java.so` 10,8 MB). `material3 1.3.2` (BOM 2025.05.00) **já traz `material-icons-core` como dependência transitiva** (POM: `material-icons-core-android`, escopo compile) — custo zero hoje.

| Opção | Prós | Contras |
|---|---|---|
| 1. `material-icons-extended` | Todos os ícones, API `Icons.Rounded.X` | Sem R8 nada é removido: **estimativa +8 a +12 MB no APK** (≈ +25–30 MB de dex; ~5 estilos × ~2.000 ícones, uma classe Kotlin por ícone), APK arm64 ~27–31 MB. Também deixa o build/dex mais lento. Estimativa, não medida. |
| 2. Só `material-icons-core` | Já está no classpath, 0 KB extra | **Faltam:** `FolderOpen`, `History`, `LibraryBooks`/`FolderCopy`, `PictureAsPdf`/`Description`, `LockOpen`, `ScreenRotation`, `ScreenLockRotation`, `Keyboard`, `Dialpad`, `Visibility`/`VisibilityOff`, `ErrorOutline`, `SearchOff`. Tem: `AutoMirrored.ArrowBack`, `KeyboardArrowUp/Down`, `Search`, `Clear`, `Close`, `Lock`, `Delete`, `Warning`, `Info`. Não resolve a rotação. Além disso o `material3 1.4+` deixa de trazê-lo. |
| 3. Vendorizar só os Material Symbols usados | ~15 ícones × ~0,5–1 KB; estilo único e moderno (Symbols Rounded, peso 400, sem preenchimento; preenchido só no estado selecionado); nenhuma dependência nova | Precisa baixar os SVGs uma vez (repo `google/material-design-icons`, **Apache 2.0**) e converter; manter um `NOTICE`/cabeçalho de licença |
| 4. Ligar R8 para limpar o extended | Menor APK de todos (também encolhe o resto do app) | MuPDF (`com.artifex.mupdf:fitz` 1.28.5) **não traz regras consumer** (o AAR só tem `R.txt`); o JNI instancia/chama classes Java por nome → R8 quebra sem `-keep class com.artifex.mupdf.fitz.** { *; }`. Exige rodar todo o e2e de novo (senha, corrompido, zoom) em release. Mudança de build fora do escopo "design". |

**Recomendação: opção 3** — vendorizar ~15 Material Symbols Rounded como `ImageVector` em `ui/icons/JohnIcons.kt` (um `val` por ícone, cabeçalho Apache 2.0 + linha no `NOTICE`). Motivos: custo de APK desprezível, sem risco para o MuPDF, um único estilo visual coerente (em vez de misturar Icons clássicos do core com Symbols), e independe da versão do material3. R8 pode vir depois como tarefa separada, com keep rules e e2e próprio.

### Emoji/pseudo-ícone → ícone
| Hoje | Onde | Substituto (Material Symbols Rounded) |
|---|---|---|
| 📂 (Abrir) | header da Home | `folder_open` |
| 🕘 (Recentes) | bottom nav | `history` (preenchido quando selecionado) |
| 📚 (Todos os PDFs) | bottom nav | `library_books` (preenchido quando selecionado) |
| 📄 | cartão de PDF | `picture_as_pdf` |
| 🔍 (placeholder) | busca | `search` (leading) + `close` (trailing, limpa a busca — novo) |
| ← (Voltar) | top bar leitor | `arrow_back` (auto-mirrored) |
| ⬆ (Anterior) | barra do leitor | `keyboard_arrow_up` |
| ⬇ (Próxima) | barra do leitor | `keyboard_arrow_down` |
| 🔓 (Gira sozinha) | leitor → header | `screen_rotation` |
| 🔒 (Travada) | leitor → header | `screen_lock_rotation` |
| "abc" / "123" | diálogo de senha | `keyboard` / `dialpad` |
| (novo) mostrar senha | diálogo de senha | `visibility` / `visibility_off` |
| (novo) ícones de estado | erro, permissão, vazio | `error` (outline), `folder` (permissão), `search_off` (busca vazia), `lock` (título do diálogo de senha) |

---

## C. Controle de rotação no header

- **Posição:** `actions` do `TopAppBar` do leitor, **à direita, única ação** (Voltar à esquerda, nome do arquivo no meio). Visível também durante carregamento? Não: só com `ReaderStatus.Ready` (igual hoje).
- **Componente:** `IconToggleButton` padrão do M3 (48dp de alvo, ícone 24dp). **Sem preenchimento azul.**
- **Ícones:** destravado = `screen_rotation`; travado = `screen_lock_rotation`.
- **Cores:** destravado → ícone `onSurfaceVariant`, sem fundo. Travado → ícone `onSurface` sobre um círculo `surfaceContainerHighest` (cinza neutro, 40dp) — o "está ligado" do M3 sem cor de marca.
- **Acessibilidade/testes:** `contentDescription = "Travar rotação da tela"`; `Modifier.semantics { stateDescription = if (locked) "Travada" else "Automática" }`; o `toggleable` já expõe On/Off → teste usa `onNodeWithContentDescription("Travar rotação da tela").assertIsOff()/assertIsOn()`.
- **Tooltip** (`TooltipBox` + `PlainTooltip`) no toque longo: "Travar rotação" / "Destravar rotação".
- **Descoberta do estado sem rótulo — recomendado:** troca de ícone + fundo neutro + **Snackbar curto** no `SnackbarHost` do Scaffold do leitor a cada toque: **"Tela travada nesta posição"** / **"Rotação automática"**. Preferir Snackbar a Toast: segue o tema, não sai do app, some sozinho, fica acima da barra inferior. Nada de Snackbar ao abrir o documento (evita ruído).

---

## D. Sistema visual (jovens + idosos)

### Tipografia (sp, respeita escala do sistema)
| Estilo | Hoje | Proposta | Uso |
|---|---|---|---|
| titleLarge | 24 SemiBold | **22 SemiBold / 28** | nome "johnPDF", títulos de diálogo |
| titleMedium | 22 SemiBold | **18 SemiBold / 24** | nome do PDF em listas, nome no top bar |
| bodyLarge | 20 / 28 | **18 / 26** | textos de estado vazio, mensagens de erro, campo de busca |
| bodyMedium | 20 / 28 | **16 / 24** | linha secundária ("Download · ontem") |
| labelLarge | 20 SemiBold | **16 SemiBold** | botões |
| labelMedium | 20 | **16 Medium** | rótulos da bottom nav, "Página 4 de 200" |
| headlineMedium/bodySmall/labelSmall/titleSmall | 30/20/20/20 | 24 / 16 / 16 / 16 | pouco usados |

**Mínimo absoluto: 16sp** (o `ThemeTest` passa de 20 para 16). Justificativa: 16sp é o padrão de corpo do Android; 18sp no conteúdo principal ainda fica acima da média; a hierarquia (18 SemiBold vs 16 regular + cor secundária) torna a lista mais fácil de escanear do que tudo em 20. Quem precisa de mais usa a escala de fonte do sistema, que já é respeitada (device a 1.3 → 18sp vira ~23sp).

### Alvos de toque
**48dp mínimo** (padrão Android/M3, `minimumInteractiveComponentSize`) para tudo; **56dp** para as ações frequentes/primárias (Anterior/Próxima, "Permitir acesso", FAB "Abrir"); linhas de lista ≥ 72dp inteiras clicáveis. Justificativa: 48dp já supera WCAG 2.5.5 (44px); o ganho real para idosos vem de alvos largos e bem espaçados (≥ 8dp entre eles) e da linha inteira clicável, não de 64dp de altura. `MinTouchTarget` vira `PrimaryTouchTarget = 56.dp`.

### Cor e elevação
- **Esquema completo** (todos os papéis, incluindo `surfaceContainer*`, `secondaryContainer`, `outlineVariant`) gerado no Material Theme Builder a partir de um **seed azul-índigo `#3558D4`**, acabando com o lilás vazado. Referências: primary `#3558D4`, primaryContainer `#DDE1FF`, secondaryContainer (neutro-azulado) `#E0E2EC`, surface `#FBF8FF`, surfaceContainer `#EFEDF4`, onSurface `#1B1B21`, onSurfaceVariant `#45464F`, error `#BA1A1A`. Contraste de texto ≥ 4,5:1 mantido.
- **Tons em vez de sombras**: top bars com `surface` e, ao rolar, `surfaceContainer` (`scrolledContainerColor`); sem `tonalElevation = 3.dp` fixo.
- **Cor de marca só onde importa**: FAB "Abrir", botão principal de cada diálogo, indicador da aba selecionada. Ícone de PDF nos cartões em vermelho suave `#C62828` sobre `#FDECEA` (pista visual "é PDF").
- **Leitor:** fundo entre páginas `surfaceContainerHigh` (#E9E7EF) em vez de #BDBDBD, gap 8dp, páginas com `shadowElevation 1dp`.

### Botões (hierarquia)
Filled = uma ação principal por tela/diálogo; FilledTonal = ações frequentes secundárias (Anterior/Próxima); Text = cancelar/alternativas; Icon = ações universais (voltar, rotação, limpar busca). `BigButton` deixa de existir (ou vira `PrimaryButton` fino sobre `Button` com ícone opcional).

### Por tela — hoje → proposta
**Home / header** — hoje: faixa de 280px com "johnPDF" 30sp + pílula azul "📂 Abrir" (01). → `TopAppBar` normal (64dp) com "johnPDF" titleLarge; **"Abrir" vira `ExtendedFloatingActionButton` (ícone `folder_open` + "Abrir PDF")** no canto inferior direito, ao alcance do polegar; lista ganha `contentPadding` inferior de 88dp.
**Bottom nav** — hoje: emojis 28sp, rótulos 20sp, fundo lilás (01). → `NavigationBar` M3 com `history`/`library_books` (contorno ↔ preenchido), rótulos 16sp sempre visíveis, fundo `surfaceContainer`, indicador `secondaryContainer`.
**Lista (Recentes/Todos)** — hoje: cartões 80dp com fundo tonal, 📄 32sp, tudo 20sp (02, 11a). → `ListItem` de 72dp sem cartão (lista plana, divisores `outlineVariant` recuados), leading = quadrado 40dp arredondado com `picture_as_pdf`, headline 18 SemiBold (2 linhas máx.), supporting 16 `onSurfaceVariant`. Toque longo continua abrindo "Remover da lista?".
**Busca** — hoje: `OutlinedTextField` retangular 64dp com "🔍" no texto (02, device/02). → `TextField` pílula (28dp de raio, 56dp de altura, sem borda, `surfaceContainerHigh`), `leadingIcon = search`, `trailingIcon = close` quando há texto, placeholder "Buscar PDFs".
**Permissão** — hoje: título bold 22sp, lista, pílula azul (02a). → ícone `folder` 48dp em círculo `primaryContainer`, título 22 SemiBold, passos 18sp, botão Filled 56dp "Permitir acesso" com largura máx. 360dp.
**Estados vazios** — hoje: frase solta no meio (01). → ícone 48dp `onSurfaceVariant` + título 18 SemiBold ("Nenhum PDF aberto ainda") + texto 16 ("Os PDFs que você abrir vão aparecer aqui.") + TextButton "Abrir PDF" (redundante com o FAB, ajuda o novato). Busca sem resultado: `search_off` + "Nenhum PDF com esse nome."
**Leitor / top bar** — hoje: "← Voltar" em texto azul + nome, 265px (04b). → `TopAppBar` 64dp: `IconButton(arrow_back, "Voltar")`, título titleMedium com reticências, ação de rotação (seção C).
**Leitor / barra inferior** — hoje: 2 linhas, 3 botões cheios, ~315px (04b); em paisagem sobra ~30% da tela (07a). → **uma linha de 72dp**: `FilledTonalButton` [↑ Anterior] · "4 de 200" (labelMedium, centro) · `FilledTonalButton` [Próxima ↓]; desabilitado = tonal esmaecido (não cinza sólido como device/05). Em paisagem mesma linha única (~64dp), ganhando ~40% de área de leitura.
**Diálogos** — hoje: pílulas azuis 64dp em tudo, fundo lilás (10a, 11b, 09b). → `AlertDialog` M3 com `surfaceContainerHigh`; ações em `TextButton`s alinhados à direita, exceto a principal (Filled). Remover: ícone `delete`, título "Remover da lista?", texto "O arquivo continua no celular.", ações "Cancelar" / **"Remover"**. Erro: ícone `error`, mensagem 18sp, "OK". Senha: ícone `lock`, título "PDF protegido", campo com `visibility`, TextButton com ícone `keyboard`/`dialpad` "Usar letras"/"Usar números", ações "Cancelar" / **"Abrir"**.

---

## E. Ideias opcionais
| Ideia | Custo | Nota |
|---|---|---|
| **Modo escuro da interface** (barras/listas; páginas do PDF continuam brancas) | Baixo (~2h: `darkColorScheme` do mesmo seed + testar contraste) | Não conflita com "modo noturno" fora de escopo, que era sobre inverter o PDF |
| **Cor dinâmica (Android 12+)** | Muito baixo (~30 min) | Perde identidade própria; sugestão: desligado por padrão |
| **Fonte própria: Atkinson Hyperlegible Next** (OFL, feita para baixa visão, visual moderno) | Baixo (~1h, +~150 KB, sem INTERNET — TTF no `res/font`) | Dá identidade e legibilidade; também evita a fonte do fabricante vista em device/* |
| **Toque na página esconde/mostra as barras** (leitura em tela cheia) | Médio (~3h + e2e) | Pode confundir quem não sabe o gesto; sempre mostrar ao abrir |
| **Arrastar-para-remover nos Recentes** (com desfazer no Snackbar) | Médio (~2h) | Mantém o toque longo como alternativa |
| **Respeitar escala de fonte** | Já acontece (sp) | Apenas validar layouts a 1.3 e 2.0 (título do top bar, bottom nav) |

---

## F. Impacto
**Arquivos:** `ui/theme/Theme.kt` (esquema completo, tipografia, alvos, gap), **novo** `ui/icons/JohnIcons.kt` (+ `NOTICE`/licença), `ui/common/BigButton.kt` (remover ou `PrimaryButton`), `ui/common/Dialogs.kt`, `ui/home/HomeScreen.kt` (TopAppBar, FAB, NavigationBar, ListItem, busca, vazios), `ui/reader/ReaderScreen.kt` (TopAppBar com rotação, SnackbarHost, barra única, cor do gap). Spec §4 ("mínimo 20sp/64dp", "ícone sempre com texto") e `docs/e2e/2026-09-28-checklist.md` precisam de atualização.

**Testes a ajustar (Robolectric/Compose):**
- `ReaderContentTest`: `"⬆ Anterior"`/`"⬇ Próxima"` → `"Anterior"`/`"Próxima"`; `"← Voltar"` → `onNodeWithContentDescription("Voltar")`; `rotation_button_toggles_label` → por contentDescription + `assertIsOff/On` (e opcional: Snackbar "Tela travada nesta posição"); `"Este PDF tem senha"` → novo título; `"Página 1 de 3"` → se o rótulo virar "1 de 3", mudar também `pageLabel` e `PageMathTest`.
- `HomeContentTest`: `"📂 Abrir"` → `"Abrir PDF"`; `"Sim"` → `"Remover"`.
- `DialogsTest`: `"abc  Usar letras"`/`"123  Usar números"` → `"Usar letras"`/`"Usar números"`; `"Sim"` → `"Remover"`.
- `ThemeTest`: mínimo 20sp → 16sp.
- `MainActivitySmokeTest`: `"Todos os PDFs"` continua. e2e no aparelho: refazer capturas.

**Esforço:** ~1,5 a 2 dias (tema + ícones 0,5; Home 0,5; Leitor + rotação 0,5; diálogos, testes e recapturas 0,5). Opcionais à parte.

**Riscos:** (1) **regressão de legibilidade** para o público idoso ao descer de 20 para 16–18sp — mitigar testando no aparelho de um familiar e a 1.3/2.0 de escala; (2) **rotação só com ícone** é menos óbvia — mitigado por Snackbar + tooltip, mas vale observar; (3) mudança de layout do leitor mexe na área do `LazyColumn` → revalidar "Página X" dominante e a preservação de posição ao girar; (4) Snackbar sobre a barra inferior pode cobrir Anterior/Próxima por 4s → usar `SnackbarDuration.Short` e posicionar acima da barra (padrão do Scaffold); (5) conversão manual dos SVGs pode errar paths — conferir visualmente cada ícone.

---

## G. Perguntas em aberto (decisão de gosto)
1. **Barra do leitor:** manter texto nos botões ("↑ Anterior" / "Próxima ↓") ou só setas com "4 de 200" no meio? (recomendo manter texto)
2. **Voltar:** só a seta (padrão Android) ou seta + "Voltar"? (recomendo só seta)
3. **"Abrir PDF"** como botão flutuante (FAB) no canto inferior direito, ou manter no header como botão tonal menor?
4. **Modo escuro** da interface: sim (seguindo o sistema) ou não por enquanto?
5. **Cor:** cor fixa johnPDF (índigo `#3558D4`) ou cor dinâmica do papel de parede (Android 12+)?
6. **Fonte:** manter a fonte do sistema ou adotar Atkinson Hyperlegible Next?
