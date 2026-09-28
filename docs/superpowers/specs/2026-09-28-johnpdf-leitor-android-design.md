# johnPDF — Leitor de PDF para Android (design)

- **Data:** 2026-09-28
- **Status:** aguardando revisão
- **Caminho:** arquitetural (projeto novo)

## 1. Objetivo

Leitor de PDF nativo para Android, **para uso pessoal e familiar**, distribuído
como **APK** (sem Play Store). Público amplo, **incluindo pessoas idosas**: a
prioridade é ser fácil de enxergar e de tocar. **Sem anúncios**, sem login,
100% offline.

### Critérios de sucesso

1. Um familiar recebe um PDF no WhatsApp, toca em "Abrir com → johnPDF" e lê o
   documento sem ajuda.
2. Encontra na aba "Todos os PDFs" um arquivo baixado dias atrás.
3. Abre extratos e faturas protegidos por senha (ex.: CPF).
4. Girar o celular não perde a posição da leitura.
5. Nenhum crash nos PDFs de teste (corrompido, com senha, grande, paisagem).

### Fora de escopo (v1)

Busca de texto dentro do PDF, lembrar a última página, modo noturno,
anotações/edição, TalkBack, Play Store, EPUB, impressão e compartilhamento.

## 2. Decisões tomadas

| Tema | Decisão | Motivo |
|---|---|---|
| Linguagem/UI | Kotlin + Jetpack Compose (Material 3) | Nativo, UI própria |
| Motor | **MuPDF** (`com.artifex.mupdf:fitz`, última versão estável, repo `https://maven.ghostscript.com`) | Maduro, rápido, suporta senha, tolera PDFs quebrados |
| Referência | `mupdf-android-viewer` (Artifex) | Usado como consulta para renderização, zoom e reúso de bitmaps; **não é um fork** |
| Licença | App sob **AGPL-3.0** (exigência do MuPDF) | Uso familiar: basta o repositório ser acessível a quem recebe o APK |
| Distribuição | APK assinado com keystore própria, **um APK por ABI** (`arm64-v8a`, `armeabi-v7a`) + um universal | Reduz tamanho |
| Android | `minSdk 24` (Android 7), `targetSdk`/`compileSdk` = maior API estável disponível | Cobre praticamente todos os aparelhos da família |
| Leitura | **Rolagem vertical contínua** | Escolha do usuário |
| Acesso a arquivos | `MANAGE_EXTERNAL_STORAGE` (Android 11+), `READ_EXTERNAL_STORAGE` (7–10) | Necessário para listar todos os PDFs; sem restrição fora da Play Store |
| Persistência | DataStore (preferências) + arquivo JSON (recentes) | Sem banco de dados; volume pequeno |

## 3. Arquitetura

```
app (Compose, Activity única)
├── ui/
│   ├── home/     HomeScreen: Header + BottomBar [Recentes | Todos os PDFs]
│   ├── reader/   ReaderScreen: LazyColumn de páginas + barra inferior
│   ├── permission/ PermissionScreen: explica e abre a configuração do sistema
│   └── common/   PasswordDialog, ErrorDialog, tema (tamanhos/cores)
├── viewmodel/
│   ├── HomeViewModel     (abas, lista de recentes, lista de todos, busca por nome)
│   └── ReaderViewModel   (documento aberto, página atual, zoom, trava de rotação)
├── data/
│   ├── RecentsRepository   (JSON; máximo de 20 itens; cópias internas)
│   ├── PdfLibraryRepository (consulta ao MediaStore de application/pdf)
│   ├── ImportRepository    (copia content:// para filesDir/imports)
│   └── SettingsRepository  (DataStore: trava de rotação)
└── engine/
    ├── PdfEngine        (interface)
    └── MuPdfEngine      (a única classe que importa com.artifex.mupdf)
```

### 3.1 PdfEngine (interface)

```kotlin
interface PdfEngine {
    suspend fun open(file: File, password: String? = null): OpenResult
    // OpenResult = Success(pageCount) | NeedsPassword | WrongPassword | Corrupted
    suspend fun pageSize(index: Int): SizeF          // em pontos PDF
    suspend fun render(index: Int, widthPx: Int): Bitmap
    fun close()
}
```

- `MuPdfEngine` executa **todas** as chamadas em uma única thread dedicada
  (`newSingleThreadContext`), porque o MuPDF não é thread-safe.
- Um cache LRU de bitmaps fica limitado a ~1/8 da memória do app. A chave é
  `(página, larguraPx)`.
- Os tamanhos das páginas são lidos todos na abertura. A `LazyColumn` reserva a
  altura correta antes de renderizar, e a rolagem não "pula".

### 3.2 Fluxos de dados

- **Abrir com… / seletor do Header:** a URI `content://` é copiada por
  `ImportRepository` para `filesDir/imports/<hash>.pdf`, registrada nos
  recentes e aberta no leitor.
- **Aba Todos os PDFs:** `PdfLibraryRepository` consulta
  `MediaStore.Files` filtrando `MIME_TYPE = application/pdf`, ordena por
  `DATE_MODIFIED desc` e abre o arquivo **direto do caminho, sem cópia**. O
  arquivo é registrado nos recentes pelo caminho.
- **Recentes:** item = `{id, nome, origem, caminho, abertoEm}`. Ao passar de 20
  itens, o mais antigo é removido, junto com sua cópia interna se houver.
- **Rotação:** o documento vive no `ReaderViewModel`, então a recriação da
  Activity não reabre o arquivo. A trava de rotação aplica
  `requestedOrientation = LOCKED` ou `UNSPECIFIED` e fica salva no DataStore.

## 4. Telas e comportamento

**Padrões visuais:** texto com no mínimo 20sp, alvos de toque com no mínimo
64dp, alto contraste (fundo claro, texto quase preto), ícone sempre acompanhado
de texto e nada escondido atrás de menus ⋮.

### 4.1 Home

- **Header:** "johnPDF" + botão **"📂 Abrir"** (seletor do sistema,
  `OpenDocument` com `application/pdf`).
- **Bottom menu:** duas abas grandes, **🕘 Recentes** e **📚 Todos os PDFs**.
- **Recentes:** cartões com nome e data amigável ("ontem", "12 de set."). Um
  toque abre. Um toque longo mostra "Remover da lista?" com Sim/Não. Lista
  vazia: "Os PDFs que você abrir vão aparecer aqui."
- **Todos os PDFs:**
  - Sem permissão: mostra o `PermissionScreen` com o texto "Para mostrar os
    PDFs do celular, o johnPDF precisa de permissão" e o botão
    **"Permitir"**, que abre `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`
    (11+) ou pede a permissão comum (7–10).
  - Com permissão: campo **"🔍 Buscar pelo nome…"** e cartões com nome, origem
    amigável (WhatsApp, Download, Documentos, Outros) e data. A lista é
    recarregada ao voltar para o app (`ON_RESUME`).

### 4.2 Leitor

- **Topo:** "← Voltar" + nome do arquivo (uma linha, com "…").
- **Centro:** páginas empilhadas com espaço cinza entre elas.
  - **Zoom** vale para o documento inteiro, de 1× a 4×. Com zoom ativo, dá
    para arrastar na horizontal.
  - **Pinça:** durante o gesto a imagem só é escalada
    (`graphicsLayer`). Ao soltar, as páginas visíveis são renderizadas de novo
    na nova largura.
  - **Duplo toque:** alterna entre 1× e 2,5×.
- **Barra inferior fixa:** `[⬆ Anterior]  Página 3 de 12  [⬇ Próxima]  [🔒 Girar]`.
  - Anterior/Próxima rolam com animação até o topo da página vizinha.
  - "Página X" = página que ocupa a maior parte da tela.
- **Senha:** diálogo com um campo grande, teclado numérico por padrão e um
  botão "abc" para trocar para o teclado de texto. Senha errada mostra
  "Senha incorreta, tente de novo" e o campo continua aberto.

### 4.3 Erros (diálogo com um único botão "OK", sem termos técnicos)

| Situação | Mensagem |
|---|---|
| Arquivo corrompido ou que não é PDF | "Não foi possível abrir este arquivo." |
| Recente cujo arquivo sumiu | "Este arquivo não está mais disponível." (sai da lista) |
| Sem espaço para copiar | "Sem espaço no celular para abrir este arquivo." |
| Falha de memória ao renderizar | Tenta de novo com metade da resolução; persistindo, mostra a página em branco com "Não foi possível mostrar esta página." |

## 5. Estratégia de testes

O desenvolvimento segue **TDD**: primeiro o teste falhando, depois a
implementação.

| Camada | Ferramenta | O que cobre |
|---|---|---|
| Unitários JVM | JUnit 5, kotlinx-coroutines-test, Turbine, `FakePdfEngine` | ViewModels (página atual, zoom, senha, erros), `RecentsRepository` (limite de 20, remoção da cópia), mapeamento da origem amigável, data amigável, busca por nome |
| Robolectric | JUnit 4 + Robolectric | `ImportRepository` (cópia de `content://`), `SettingsRepository`, parsing do cursor do MediaStore |
| Instrumentados | AndroidX Test no emulador | `MuPdfEngine` contra os PDFs de `androidTest/assets`: normal, com senha (`1234`), corrompido, 200 páginas, paisagem |
| UI Compose | `createAndroidComposeRule` | Home (abas, lista vazia, remoção), leitor (botões Anterior/Próxima, indicador, diálogo de senha) |
| E2E exploratório | **mobile-mcp** (MCP de automação Android) no emulador | Fluxos dos critérios de sucesso: abrir via intent, permissão, rotação, senha e screenshots |

### Ambiente (levantado em 2026-09-28)

- A máquina tem KVM, 8 núcleos, ~7 GB de RAM e 90 GB livres.
- **Faltam:** JDK 17+, Android SDK (cmdline-tools, platform-tools, emulator,
  system image x86_64) e mobile-mcp. Instalar é o primeiro passo do plano.
- **Risco:** 7 GB de RAM é pouco para Gradle e emulador ao mesmo tempo. Por
  isso: emulador sem janela (`-no-window`) com 2 GB e Gradle com
  `-Xmx2g`. Se não bastar, os testes E2E rodam num celular físico via USB.

## 6. Estimativa

| Etapa | Horas humanas equivalentes |
|---|---|
| Ambiente + projeto + dependência do MuPDF + splits por ABI | 2–3 |
| Motor (`PdfEngine`/`MuPdfEngine`) + testes | 3–4 |
| Leitor (rolagem, zoom, barra, senha) + testes | 6–8 |
| Home (Header, bottom menu, recentes) + testes | 3–4 |
| Todos os PDFs (permissão, MediaStore, busca) + testes | 5–7 |
| "Abrir com…", rotação, erros | 2–3 |
| E2E com mobile-mcp + correções | 4–5 |
| **Total** | **~25–34 h** |

Com implementação feita pelo Claude em TDD, a meta de **1 dia de calendário**
é viável se o ambiente Android (seção 5) subir sem problemas. O custo em
dinheiro é **US$ 0**.
