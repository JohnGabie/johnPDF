# Ícones Material Symbols vendorizados — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Vendorizar 22 Material Symbols Rounded como `ImageVector` estáticos em `ui/icons/JohnIcons.kt`, gerados a partir dos SVGs oficiais por um script Python, para que as telas possam trocar emoji por ícone de verdade sem nenhuma dependência nova no Gradle e com impacto praticamente nulo no APK.

**Architecture:** Os SVGs do repositório `google/material-design-icons` são baixados uma única vez para `tools/icons/svg/` e versionados no git. Um gerador Python offline (`tools/icons/generate_johnicons.py`, stdlib apenas) lê esses SVGs, valida cada um e emite `app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt`: um `object JohnIcons` com 22 `val ImageVector by lazy`, cada um construído por uma função `symbol()` que parseia o atributo `d` do SVG com `addPathNodes()` — a string `d` é copiada byte a byte do SVG, nunca transcrita à mão. O build nunca toca a rede; o `.kt` é um artefato gerado, e a revisão acontece no gerador e no diff dos SVGs, não no Kotlin.

**Tech Stack:** Python 3 (stdlib: `urllib.request`, `xml.etree.ElementTree`, `unittest`), Kotlin 2.1 + Jetpack Compose (`androidx.compose.ui.graphics.vector`, BOM `2025.05.00`), Material Symbols Rounded (Apache 2.0), JUnit 4 + Robolectric 4.14.1 (`sdk=35`, graphics nativo) para os testes de renderização.

**Spec:** `docs/superpowers/specs/2026-09-29-icones-vendorizados-spec.md`

## Global Constraints

- **Nenhuma dependência nova** em `gradle/libs.versions.toml` nem em `app/build.gradle.kts`. Nem em `implementation`, nem em `debugImplementation`. `ImageVector`, `addPathNodes`, `group` e `Icon` já vêm de `androidx.compose.ui:ui` e `material3` via BOM `2025.05.00`.
- `isMinifyEnabled = false` permanece inalterado. R8 é explicitamente fora de escopo.
- **Zero coordenada digitada à mão.** Toda string `d` em `JohnIcons.kt` sai do gerador, que a lê de `tools/icons/svg/*.svg`. Nunca copiar um `d` de uma spec, de um exemplo ou de memória — incluindo o exemplo ilustrativo da §4.1 da spec, que está explicitamente marcado como não confiável.
- **Não transcrever paths em `moveTo`/`curveTo` à mão.** O caminho obrigatório é `addPathNodes(d)`. Só se `addPathNodes` não resolver no compile é que o gerador passa a emitir nós explícitos — e ainda assim gerados a partir do mesmo `d`, nunca escritos por uma pessoa.
- viewBox dos Material Symbols é `0 -960 960 960` (origem no canto **inferior** esquerdo, Y negativo). `ImageVector` não tem offset de viewBox: o deslocamento é feito por `group(translationY = 960f)` e as coordenadas do `d` **não** são reescritas.
- `viewportWidth = viewportHeight = 960f`; `defaultWidth = defaultHeight = 24.dp`.
- Estilo único: **Material Symbols Rounded, weight 400, grade 0, optical size 24**. Não misturar com `Icons.Rounded.*` do `material-icons-core`.
- **Licença:** cabeçalho Apache 2.0 + atribuição "Copyright 2023 Google LLC" no topo de `JohnIcons.kt`; `NOTICE` na raiz do repositório; cópia íntegra da licença em `third_party/material-design-icons/LICENSE`. O app é AGPL-3.0 e continua sendo.
- `JohnIcons.kt` carrega a marcação **`ARQUIVO GERADO — não editar à mão`** e o comando de regeneração.
- O gerador é **idempetente**: rodar duas vezes produz bytes idênticos. Escrever sempre com `newline="\n"` (estamos no Windows; `\r\n` faria o `git diff --exit-code` falhar).
- Nenhuma tela, diálogo ou `Theme.kt` é alterado neste plano. `JohnIcons.kt` termina o plano sem nenhum consumidor de produção — as trocas de emoji são tarefas seguintes.
- Total: **22 `ImageVector`** (20 nomes upstream distintos + 2 variantes `fill1`).
- APK release arm64 deve ficar **abaixo de 19,0 MB** (baseline 18,9 MB; delta permitido < 100 KB).
- Ambiente: Windows. Python é `py`; Gradle é `.\gradlew.bat`. Em Linux/macOS, `python3` e `./gradlew`. Gradle roda com `-Xmx2g` (já em `gradle.properties`); **não** rodar dois builds ao mesmo tempo.
- Todo texto de UI e toda mensagem de erro do gerador em **pt-BR**.

## Review Focus

1. **Um SVG baixado errado (404 salvo como HTML, `d` truncado) produz um ícone que compila, passa nos testes de metadado e desenha nada na tela.** Esperado: cada um dos 22 ícones, renderizado a 48dp, pinta pelo menos um pixel opaco. *(Teste: Task 8, `todo_icone_pinta_pixels`.)*
2. **A bottom nav troca contorno ↔ preenchido; se `fetch_svgs.py` baixar o mesmo arquivo para as duas variantes, o estado selecionado fica visualmente idêntico ao não selecionado e ninguém percebe.** Esperado: `HistoryFilled` e `LibraryBooksFilled` pintam mais pixels que suas versões de contorno. *(Teste: Task 8, `variante_filled_pinta_mais_que_o_contorno`.)*
3. **O app aberto com o sistema em árabe/hebraico espelha o layout inteiro; uma seta "voltar" que não espelha aponta para o lado errado.** Esperado: só `ArrowBack` tem `autoMirror = true`; os outros 21, incluindo os verticais `KeyboardArrowUp/Down`, têm `false`. *(Teste: Task 6, `apenas_arrow_back_espelha_em_rtl`.)*
4. **Um ícone sem identidade própria não dá para ser selecionado nem nomeado — nem pelo TalkBack, nem pelos testes Compose, nem no dump de semântica.** Esperado: cada `ImageVector` carrega `name == "JohnIcons.<NomeDoVal>"`, não em branco e único entre os 22 — é o que torna o `contentDescription` de §8.7 da spec verificável quando as telas forem trocadas. *(Teste: Task 6, `todo_icone_tem_nome_unico_e_legivel`.)*
5. **Um `d` sintaticamente válido mas com coordenadas fora do viewBox (SVG de outro optical size, ou um `viewBox` que o gerador aceitou por engano) desenha cortado ou minúsculo dentro do 24dp.** Esperado: o bounding box de todo path fica dentro de `x ∈ [0, 960]`, `y ∈ [-960, 0]` (tolerância 2f) e tem largura e altura maiores que zero. *(Teste: Task 7, `geometria_de_todo_icone_cabe_no_viewbox`.)*

---

## Estrutura de arquivos

```
johnPDF/
├── NOTICE                                        ← NOVO (Task 9)
├── third_party/material-design-icons/LICENSE     ← NOVO, Apache 2.0 íntegro (Task 2)
├── tools/icons/                                  ← NOVO
│   ├── icons.txt              lista declarativa: upstream, nome Kotlin, flags   (Task 1)
│   ├── icons_lib.py           lógica pura e testável: parse, validação, emissão (Tasks 1, 3, 4)
│   ├── test_icons_lib.py      unittest da stdlib, sem rede                      (Tasks 1, 3, 4)
│   ├── fetch_svgs.py          baixa SVGs + LICENSE, fixa o SHA upstream         (Task 2)
│   ├── generate_johnicons.py  svg/*.svg → JohnIcons.kt, offline, idempotente    (Task 4)
│   ├── UPSTREAM.txt           SHA do commit de origem + URL usada               (Task 2, gerado)
│   └── svg/*.svg              23 arquivos vendorizados (22 ícones, 2 do mesmo nome) (Task 2)
├── docs/e2e/img/icons-grid.png                   ← NOVO, evidência visual (Task 10)
└── app/src/
    ├── main/java/com/johngabie/johnpdf/ui/icons/
    │   └── JohnIcons.kt                          ← NOVO, GERADO (Task 5)
    └── test/java/com/johngabie/johnpdf/ui/icons/
        ├── JohnIconsTest.kt                      ← NOVO, metadados e inventário (Task 6)
        ├── JohnIconsGeometryTest.kt              ← NOVO, bounds dos paths       (Task 7)
        ├── JohnIconsRenderTest.kt                ← NOVO, pixels                 (Task 8)
        └── JohnIconsGridCaptureTest.kt           ← NOVO, captura o PNG          (Task 10)
```

Por que `icons_lib.py` separado dos dois scripts: as partes que dá para testar sem rede e sem Android (parse da lista, validação do SVG, emissão do Kotlin) ficam num módulo só, com um `unittest` de verdade. `fetch_svgs.py` e `generate_johnicons.py` viram cascas de ~30 linhas — um faz I/O de rede, o outro I/O de disco.

Por que quatro arquivos de teste Kotlin em vez de um: cada um tem um custo e um motivo diferente. `JohnIconsTest` é metadado puro e roda em milissegundos. `JohnIconsGeometryTest` precisa de `android.graphics.Path` (Robolectric). `JohnIconsRenderTest` precisa do Compose inteiro + captura de bitmap. `JohnIconsGridCaptureTest` escreve um arquivo no repositório e só roda sob demanda. Misturar tudo num arquivo faria a suíte rápida pagar o preço da lenta.

---

### Task 1: Lista declarativa dos 22 ícones e seu parser

**Files:**
- Create: `tools/icons/icons.txt`
- Create: `tools/icons/icons_lib.py`
- Test: `tools/icons/test_icons_lib.py`

**Interfaces:**
- Consumes: nada.
- Produces:
  - `@dataclass class Icon` com campos `upstream: str`, `kotlin: str`, `fill: bool`, `automirror: bool`, `d: str = ""`, `even_odd: bool = False`.
  - `parse_icons_txt(text: str) -> list[Icon]` — ordenada alfabeticamente por `kotlin`.
  - `svg_filename(icon: Icon) -> str` — `"history_fill1_24px.svg"` ou `"history_24px.svg"`.
  - `const_name(kotlin: str) -> str` — `"HistoryFilled"` → `"D_HISTORY_FILLED"`.
  - `ICONS_TXT: pathlib.Path`, `SVG_DIR: pathlib.Path`, `REPO_ROOT: pathlib.Path`, `KT_OUT: pathlib.Path`.

- [ ] **Step 1: Escrever a lista de ícones**

Criar `tools/icons/icons.txt` exatamente com este conteúdo (colunas separadas por espaços, alinhamento livre):

```
# Ícones vendorizados de Material Symbols Rounded (weight 400, grade 0, optical size 24).
# Formato: <nome_upstream> <NomeKotlin> [fill] [automirror]
#   fill       → baixa a variante *_fill1_24px.svg
#   automirror → ImageVector com autoMirror = true (espelha em RTL)
# Ver docs/superpowers/specs/2026-09-29-icones-vendorizados-spec.md §2

arrow_back            ArrowBack            automirror
close                 Close
delete                Delete
dialpad               Dialpad
error                 Error
folder                Folder
folder_open           FolderOpen
history               History
history               HistoryFilled        fill
keyboard              Keyboard
keyboard_arrow_down   KeyboardArrowDown
keyboard_arrow_up     KeyboardArrowUp
library_books         LibraryBooks
library_books         LibraryBooksFilled   fill
lock                  Lock
picture_as_pdf        PictureAsPdf
screen_lock_rotation  ScreenLockRotation
screen_rotation       ScreenRotation
search                Search
search_off            SearchOff
visibility            Visibility
visibility_off        VisibilityOff
```

- [ ] **Step 2: Escrever o teste que falha**

Criar `tools/icons/test_icons_lib.py`:

```python
"""Testes do gerador de ícones. Stdlib apenas, sem rede.

Uso:
  py -m unittest discover -s tools/icons -t tools/icons -v
"""
import unittest

import icons_lib


class ParseIconsTxtTest(unittest.TestCase):
    def test_ignora_comentarios_e_linhas_vazias(self):
        icons = icons_lib.parse_icons_txt("# comentário\n\nclose Close\n")
        self.assertEqual(1, len(icons))
        self.assertEqual("close", icons[0].upstream)
        self.assertEqual("Close", icons[0].kotlin)
        self.assertFalse(icons[0].fill)
        self.assertFalse(icons[0].automirror)

    def test_le_as_flags(self):
        icons = icons_lib.parse_icons_txt(
            "history HistoryFilled fill\narrow_back ArrowBack automirror\n"
        )
        by_name = {i.kotlin: i for i in icons}
        self.assertTrue(by_name["HistoryFilled"].fill)
        self.assertFalse(by_name["HistoryFilled"].automirror)
        self.assertTrue(by_name["ArrowBack"].automirror)
        self.assertFalse(by_name["ArrowBack"].fill)

    def test_ordena_alfabeticamente_pelo_nome_kotlin(self):
        icons = icons_lib.parse_icons_txt("search Search\nclose Close\ndelete Delete\n")
        self.assertEqual(["Close", "Delete", "Search"], [i.kotlin for i in icons])

    def test_recusa_flag_desconhecida(self):
        with self.assertRaises(ValueError) as cm:
            icons_lib.parse_icons_txt("close Close outline\n")
        self.assertIn("outline", str(cm.exception))

    def test_recusa_nome_kotlin_duplicado(self):
        with self.assertRaises(ValueError) as cm:
            icons_lib.parse_icons_txt("close Close\nclose_fullscreen Close\n")
        self.assertIn("Close", str(cm.exception))

    def test_recusa_nome_kotlin_fora_do_pascal_case(self):
        with self.assertRaises(ValueError):
            icons_lib.parse_icons_txt("close closeIcon\n")


class NomesDerivadosTest(unittest.TestCase):
    def test_svg_filename_contorno_e_preenchido(self):
        contorno = icons_lib.Icon("history", "History", False, False)
        preenchido = icons_lib.Icon("history", "HistoryFilled", True, False)
        self.assertEqual("history_24px.svg", icons_lib.svg_filename(contorno))
        self.assertEqual("history_fill1_24px.svg", icons_lib.svg_filename(preenchido))

    def test_const_name_converte_pascal_para_snake_maiusculo(self):
        self.assertEqual("D_HISTORY", icons_lib.const_name("History"))
        self.assertEqual("D_HISTORY_FILLED", icons_lib.const_name("HistoryFilled"))
        self.assertEqual("D_PICTURE_AS_PDF", icons_lib.const_name("PictureAsPdf"))
        self.assertEqual("D_KEYBOARD_ARROW_UP", icons_lib.const_name("KeyboardArrowUp"))


class IconsTxtRealTest(unittest.TestCase):
    def test_o_arquivo_versionado_tem_22_icones_validos(self):
        icons = icons_lib.parse_icons_txt(icons_lib.ICONS_TXT.read_text(encoding="utf-8"))
        self.assertEqual(22, len(icons))
        self.assertEqual(
            ["ArrowBack"],
            [i.kotlin for i in icons if i.automirror],
            "só ArrowBack espelha em RTL",
        )
        self.assertEqual(
            ["HistoryFilled", "LibraryBooksFilled"],
            sorted(i.kotlin for i in icons if i.fill),
        )
        self.assertEqual(20, len({i.upstream for i in icons}), "20 nomes upstream distintos")


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 3: Rodar o teste e confirmar que falha**

```powershell
py -m unittest discover -s tools/icons -t tools/icons -v
```

Esperado: `ModuleNotFoundError: No module named 'icons_lib'`.

- [ ] **Step 4: Escrever a implementação mínima**

Criar `tools/icons/icons_lib.py`:

```python
#!/usr/bin/env python3
"""Lógica pura do gerador de ícones do johnPDF.

Sem rede e sem dependências fora da stdlib — tudo aqui é testável por
tools/icons/test_icons_lib.py.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO_ROOT = HERE.parent.parent
ICONS_TXT = HERE / "icons.txt"
SVG_DIR = HERE / "svg"
UPSTREAM_TXT = HERE / "UPSTREAM.txt"
LICENSE_OUT = REPO_ROOT / "third_party" / "material-design-icons" / "LICENSE"
KT_OUT = (
    REPO_ROOT
    / "app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt"
)

VALID_FLAGS = {"fill", "automirror"}
PASCAL_CASE = re.compile(r"^[A-Z][A-Za-z0-9]*$")


@dataclass
class Icon:
    upstream: str
    kotlin: str
    fill: bool
    automirror: bool
    d: str = ""
    even_odd: bool = False


def parse_icons_txt(text: str) -> list[Icon]:
    """Lê icons.txt. Devolve os ícones ordenados por nome Kotlin."""
    icons: list[Icon] = []
    vistos: set[str] = set()
    for numero, linha_bruta in enumerate(text.splitlines(), start=1):
        linha = linha_bruta.split("#", 1)[0].strip()
        if not linha:
            continue
        partes = linha.split()
        if len(partes) < 2:
            raise ValueError(f"icons.txt linha {numero}: faltam colunas em {linha_bruta!r}")
        upstream, kotlin, *flags = partes
        if not PASCAL_CASE.match(kotlin):
            raise ValueError(
                f"icons.txt linha {numero}: {kotlin!r} não está em PascalCase"
            )
        if kotlin in vistos:
            raise ValueError(f"icons.txt linha {numero}: nome Kotlin duplicado {kotlin!r}")
        vistos.add(kotlin)
        desconhecidas = set(flags) - VALID_FLAGS
        if desconhecidas:
            raise ValueError(
                f"icons.txt linha {numero}: flag desconhecida "
                f"{sorted(desconhecidas)}; válidas: {sorted(VALID_FLAGS)}"
            )
        icons.append(
            Icon(
                upstream=upstream,
                kotlin=kotlin,
                fill="fill" in flags,
                automirror="automirror" in flags,
            )
        )
    return sorted(icons, key=lambda i: i.kotlin)


def svg_filename(icon: Icon) -> str:
    """Nome do arquivo SVG em tools/icons/svg/."""
    sufixo = "_fill1_24px.svg" if icon.fill else "_24px.svg"
    return f"{icon.upstream}{sufixo}"


def const_name(kotlin: str) -> str:
    """'HistoryFilled' -> 'D_HISTORY_FILLED'."""
    snake = re.sub(r"(?<!^)(?=[A-Z])", "_", kotlin).upper()
    return f"D_{snake}"
```

- [ ] **Step 5: Rodar o teste e confirmar que passa**

```powershell
py -m unittest discover -s tools/icons -t tools/icons -v
```

Esperado: `OK`, 9 testes.

- [ ] **Step 6: Commit**

```bash
git add tools/icons/icons.txt tools/icons/icons_lib.py tools/icons/test_icons_lib.py
git commit -m "feat(icons): lista declarativa dos 22 Material Symbols e seu parser"
```

---

### Task 2: Baixar os SVGs, a licença e fixar o commit upstream

**Files:**
- Create: `tools/icons/fetch_svgs.py`
- Create (gerado, versionado): `tools/icons/svg/*.svg` (23 arquivos), `tools/icons/UPSTREAM.txt`, `third_party/material-design-icons/LICENSE`

**Interfaces:**
- Consumes: `icons_lib.parse_icons_txt`, `icons_lib.svg_filename`, `icons_lib.ICONS_TXT`, `icons_lib.SVG_DIR`, `icons_lib.UPSTREAM_TXT`, `icons_lib.LICENSE_OUT`.
- Produces: os arquivos acima. Nenhuma API Python consumida por tarefas seguintes — `fetch_svgs.py` roda uma vez e some do fluxo.

> **Por que não há teste automatizado aqui:** o script só faz I/O de rede; um teste dele testaria o `urllib`. A verificação de que o download deu certo é o passo 4 (contagem e inspeção dos arquivos) e, depois, `todo_icone_pinta_pixels` na Task 8 — que é o que realmente pega um 404 salvo como HTML.

- [ ] **Step 1: Escrever o downloader**

Criar `tools/icons/fetch_svgs.py`:

```python
#!/usr/bin/env python3
"""Baixa os SVGs de Material Symbols Rounded para tools/icons/svg/.

Roda UMA VEZ, quando icons.txt muda. Os SVGs entram no git; o build
nunca acessa a rede.

Uso:
  py tools/icons/fetch_svgs.py            # fixa no HEAD de master e anota o SHA
  py tools/icons/fetch_svgs.py --ref <sha>

Fonte primária (pinável por commit):
  https://raw.githubusercontent.com/google/material-design-icons/<sha>/
    symbols/web/<nome>/materialsymbolsrounded/<nome>[_fill1]_24px.svg

Fallback documentado (NÃO pinável — registrar em UPSTREAM.txt se usado):
  https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/
    <nome>/{default,fill1}/24px.svg
"""
from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request

import icons_lib

REPO = "google/material-design-icons"
API_COMMIT = f"https://api.github.com/repos/{REPO}/commits/{{ref}}"
RAW = f"https://raw.githubusercontent.com/{REPO}/{{sha}}/{{path}}"
UA = {"User-Agent": "johnPDF-icon-fetcher"}


def baixar(url: str) -> bytes:
    """Baixa url. Falha ALTO: um 404 silencioso viraria um ícone vazio."""
    req = urllib.request.Request(url, headers=UA)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            if r.status != 200:
                raise SystemExit(f"ERRO: HTTP {r.status} em {url}")
            return r.read()
    except urllib.error.HTTPError as e:
        raise SystemExit(f"ERRO: HTTP {e.code} em {url}") from e
    except urllib.error.URLError as e:
        raise SystemExit(f"ERRO: rede indisponível para {url}: {e.reason}") from e


def resolver_sha(ref: str) -> str:
    dados = json.loads(baixar(API_COMMIT.format(ref=ref)).decode("utf-8"))
    return dados["sha"]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--ref", default="master", help="branch, tag ou SHA upstream")
    args = ap.parse_args()

    icons = icons_lib.parse_icons_txt(icons_lib.ICONS_TXT.read_text(encoding="utf-8"))
    sha = resolver_sha(args.ref)
    print(f"upstream {REPO} @ {sha}")

    icons_lib.SVG_DIR.mkdir(parents=True, exist_ok=True)
    baixados: set[str] = set()
    for icon in icons:
        nome = icons_lib.svg_filename(icon)
        if nome in baixados:
            continue
        caminho = f"symbols/web/{icon.upstream}/materialsymbolsrounded/{nome}"
        conteudo = baixar(RAW.format(sha=sha, path=caminho))
        if b"<svg" not in conteudo[:200]:
            raise SystemExit(f"ERRO: {nome} não parece um SVG (início: {conteudo[:80]!r})")
        (icons_lib.SVG_DIR / nome).write_bytes(conteudo)
        baixados.add(nome)
        print(f"  ok  {nome}  ({len(conteudo)} bytes)")

    icons_lib.LICENSE_OUT.parent.mkdir(parents=True, exist_ok=True)
    licenca = baixar(RAW.format(sha=sha, path="LICENSE"))
    if b"Apache License" not in licenca:
        raise SystemExit("ERRO: o LICENSE baixado não é a Apache License")
    icons_lib.LICENSE_OUT.write_bytes(licenca)
    print(f"  ok  {icons_lib.LICENSE_OUT.relative_to(icons_lib.REPO_ROOT)}")

    icons_lib.UPSTREAM_TXT.write_text(
        "\n".join(
            [
                f"repo:   https://github.com/{REPO}",
                f"commit: {sha}",
                "estilo: materialsymbolsrounded, weight 400, grade 0, optical size 24",
                "fonte:  raw.githubusercontent.com (primária)",
                f"arquivos: {len(baixados)} SVG + LICENSE",
                "",
            ]
        ),
        encoding="utf-8",
        newline="\n",
    )
    print(f"\n{len(baixados)} SVGs em {icons_lib.SVG_DIR}")


if __name__ == "__main__":
    main()
```

> `import icons_lib` funciona porque o Python põe o diretório do script no início do `sys.path` — vale tanto para `py tools/icons/fetch_svgs.py` a partir da raiz quanto para rodar de dentro de `tools/icons`. Remover o `import sys` se o linter reclamar de não usado (ele só é usado em `generate_johnicons.py`).

- [ ] **Step 2: Rodar o download**

```powershell
py tools/icons/fetch_svgs.py
```

Esperado: a linha `upstream google/material-design-icons @ <sha>`, depois **22** linhas `ok  <nome>_24px.svg  (NNN bytes)` — 20 nomes upstream distintos, sendo que `history` e `library_books` aparecem duas vezes cada (contorno e `_fill1_`) —, mais uma linha `ok third_party/material-design-icons/LICENSE`, e por fim `22 SVGs em .../tools/icons/svg`.

Se alguma URL der 404, o script aborta com `ERRO: HTTP 404 em <url>`. Nesse caso, usar o fallback do docstring (`fonts.gstatic.com`, com `/default/24px.svg` e `/fill1/24px.svg`), baixar à mão para `tools/icons/svg/` com os mesmos nomes de arquivo, e **registrar em `UPSTREAM.txt` que a fonte foi o gstatic e que não há SHA fixado**.

- [ ] **Step 3: Conferir o que chegou**

```powershell
Get-ChildItem tools/icons/svg | Measure-Object | Select-Object Count
Get-Content tools/icons/svg/history_24px.svg
Get-Content tools/icons/svg/history_fill1_24px.svg
Get-Content tools/icons/UPSTREAM.txt
```

Esperado: 22 arquivos; cada SVG uma linha só, com `viewBox="0 -960 960 960"`, `height="24px"`, `width="24px"` e **um** `<path d="…"/>`; `history_24px.svg` e `history_fill1_24px.svg` com `d` **diferentes** (se forem iguais, o download da variante preenchida falhou silenciosamente — pare e investigue).

- [ ] **Step 4: Commit**

```bash
git add tools/icons/fetch_svgs.py tools/icons/svg tools/icons/UPSTREAM.txt third_party/material-design-icons/LICENSE
git commit -m "feat(icons): vendoriza os SVGs de Material Symbols Rounded e a licenca Apache 2.0"
```

---

### Task 3: Validação e extração do `d` de cada SVG

**Files:**
- Modify: `tools/icons/icons_lib.py` (acrescenta `extract_path`)
- Test: `tools/icons/test_icons_lib.py` (acrescenta `ExtractPathTest`)

**Interfaces:**
- Consumes: `icons_lib.Icon`, `icons_lib.SVG_DIR`, `icons_lib.svg_filename`.
- Produces: `extract_path(svg_text: str, origem: str) -> tuple[str, bool]` devolvendo `(d, even_odd)`; `load_icons() -> list[Icon]` que lê `icons.txt`, lê cada SVG de `SVG_DIR` e devolve os `Icon` com `d` e `even_odd` preenchidos.

- [ ] **Step 1: Escrever os testes que falham**

Acrescentar a `tools/icons/test_icons_lib.py`, antes do bloco `if __name__`:

```python
SVG_OK = (
    '<svg xmlns="http://www.w3.org/2000/svg" height="24px" '
    'viewBox="0 -960 960 960" width="24px" fill="#000000">'
    '<path d="M480-480 120-120Z"/></svg>'
)


class ExtractPathTest(unittest.TestCase):
    def test_extrai_o_d_sem_normalizar(self):
        d, even_odd = icons_lib.extract_path(SVG_OK, "ok.svg")
        self.assertEqual("M480-480 120-120Z", d)
        self.assertFalse(even_odd)

    def test_detecta_fill_rule_evenodd(self):
        svg = SVG_OK.replace('<path d=', '<path fill-rule="evenodd" d=')
        _, even_odd = icons_lib.extract_path(svg, "ok.svg")
        self.assertTrue(even_odd)

    def test_recusa_viewbox_diferente(self):
        svg = SVG_OK.replace('viewBox="0 -960 960 960"', 'viewBox="0 0 24 24"')
        with self.assertRaises(ValueError) as cm:
            icons_lib.extract_path(svg, "ruim.svg")
        self.assertIn("ruim.svg", str(cm.exception))
        self.assertIn("viewBox", str(cm.exception))

    def test_recusa_mais_de_um_path(self):
        svg = SVG_OK.replace("</svg>", '<path d="M0 0Z"/></svg>')
        with self.assertRaises(ValueError) as cm:
            icons_lib.extract_path(svg, "dois.svg")
        self.assertIn("dois.svg", str(cm.exception))

    def test_recusa_zero_paths(self):
        svg = '<svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px"></svg>'
        with self.assertRaises(ValueError):
            icons_lib.extract_path(svg, "vazio.svg")

    def test_recusa_d_vazio(self):
        svg = SVG_OK.replace('d="M480-480 120-120Z"', 'd="  "')
        with self.assertRaises(ValueError) as cm:
            icons_lib.extract_path(svg, "sem-d.svg")
        self.assertIn("sem-d.svg", str(cm.exception))

    def test_recusa_tamanho_diferente_de_24(self):
        svg = SVG_OK.replace('height="24px"', 'height="48px"')
        with self.assertRaises(ValueError) as cm:
            icons_lib.extract_path(svg, "grande.svg")
        self.assertIn("grande.svg", str(cm.exception))

    def test_recusa_html_salvo_como_svg(self):
        with self.assertRaises(ValueError):
            icons_lib.extract_path("<html><body>404</body></html>", "404.svg")


class LoadIconsTest(unittest.TestCase):
    def test_carrega_os_22_icones_versionados_com_d_nao_vazio(self):
        icons = icons_lib.load_icons()
        self.assertEqual(22, len(icons))
        for icon in icons:
            with self.subTest(icon.kotlin):
                self.assertTrue(icon.d.strip(), f"{icon.kotlin} ficou com d vazio")
                self.assertGreater(len(icon.d), 20, f"{icon.kotlin} tem d suspeito de truncado")

    def test_variantes_preenchidas_diferem_do_contorno(self):
        by_name = {i.kotlin: i for i in icons_lib.load_icons()}
        self.assertNotEqual(by_name["History"].d, by_name["HistoryFilled"].d)
        self.assertNotEqual(by_name["LibraryBooks"].d, by_name["LibraryBooksFilled"].d)
```

- [ ] **Step 2: Rodar e confirmar que falha**

```powershell
py -m unittest discover -s tools/icons -t tools/icons -v
```

Esperado: `AttributeError: module 'icons_lib' has no attribute 'extract_path'` nos testes novos; os 9 da Task 1 seguem verdes.

- [ ] **Step 3: Implementar**

Acrescentar a `tools/icons/icons_lib.py` (e o `import xml.etree.ElementTree as ET` no topo):

```python
SVG_NS = "{http://www.w3.org/2000/svg}"
EXPECTED_VIEWBOX = "0 -960 960 960"
EXPECTED_SIZE = {"24px", "24"}


def extract_path(svg_text: str, origem: str) -> tuple[str, bool]:
    """Extrai o atributo `d` do único <path> do SVG, SEM normalizar nada.

    Devolve (d, even_odd). Levanta ValueError com o nome do arquivo em
    qualquer desvio — um SVG errado que passasse daqui viraria um ícone
    que compila e não desenha nada.
    """
    try:
        raiz = ET.fromstring(svg_text)
    except ET.ParseError as e:
        raise ValueError(f"{origem}: XML inválido ({e})") from e
    if raiz.tag != f"{SVG_NS}svg":
        raise ValueError(f"{origem}: raiz é {raiz.tag!r}, esperava <svg>")

    viewbox = raiz.get("viewBox", "")
    if viewbox != EXPECTED_VIEWBOX:
        raise ValueError(
            f"{origem}: viewBox {viewbox!r}, esperava {EXPECTED_VIEWBOX!r} "
            "(o símbolo não é Material Symbols optical size 24)"
        )
    for atributo in ("width", "height"):
        valor = raiz.get(atributo, "")
        if valor not in EXPECTED_SIZE:
            raise ValueError(
                f"{origem}: {atributo}={valor!r}, esperava um de {sorted(EXPECTED_SIZE)}"
            )

    paths = raiz.findall(f".//{SVG_NS}path")
    if len(paths) != 1:
        raise ValueError(
            f"{origem}: {len(paths)} <path>, esperava exatamente 1 "
            "(o gerador só monta ícones de path único)"
        )
    d = paths[0].get("d", "")
    if not d.strip():
        raise ValueError(f"{origem}: atributo d vazio ou ausente")
    even_odd = paths[0].get("fill-rule", "").strip().lower() == "evenodd"
    return d, even_odd


def load_icons() -> list[Icon]:
    """icons.txt + svg/*.svg -> Icon completos, ordenados por nome Kotlin."""
    icons = parse_icons_txt(ICONS_TXT.read_text(encoding="utf-8"))
    for icon in icons:
        arquivo = SVG_DIR / svg_filename(icon)
        if not arquivo.exists():
            raise ValueError(
                f"{arquivo} não existe — rode `py tools/icons/fetch_svgs.py`"
            )
        icon.d, icon.even_odd = extract_path(
            arquivo.read_text(encoding="utf-8"), arquivo.name
        )
    return icons
```

- [ ] **Step 4: Rodar e confirmar que passa**

```powershell
py -m unittest discover -s tools/icons -t tools/icons -v
```

Esperado: `OK`, 19 testes. Se `test_recusa_tamanho_diferente_de_24` ou `test_carrega_os_22_icones_versionados_com_d_nao_vazio` falhar contra os SVGs reais, o problema está no download da Task 2 — volte, não relaxe a validação.

- [ ] **Step 5: Commit**

```bash
git add tools/icons/icons_lib.py tools/icons/test_icons_lib.py
git commit -m "feat(icons): valida e extrai o path de cada SVG vendorizado"
```

---

### Task 4: Emissor do Kotlin

**Files:**
- Modify: `tools/icons/icons_lib.py` (acrescenta `render_kotlin`)
- Create: `tools/icons/generate_johnicons.py`
- Test: `tools/icons/test_icons_lib.py` (acrescenta `RenderKotlinTest`)

**Interfaces:**
- Consumes: `icons_lib.Icon`, `icons_lib.load_icons`, `icons_lib.const_name`, `icons_lib.KT_OUT`.
- Produces: `render_kotlin(icons: list[Icon]) -> str` — o conteúdo completo de `JohnIcons.kt`, terminado em `\n`, sem `\r`.

- [ ] **Step 1: Escrever os testes que falham**

Acrescentar a `tools/icons/test_icons_lib.py`:

```python
class RenderKotlinTest(unittest.TestCase):
    def render(self):
        icons = [
            icons_lib.Icon("arrow_back", "ArrowBack", False, True, d="M1-1Z"),
            icons_lib.Icon("history", "History", False, False, d="M2-2Z"),
            icons_lib.Icon("history", "HistoryFilled", True, False, d="M3-3Z"),
            icons_lib.Icon("error", "Error", False, False, d="M4-4Z", even_odd=True),
        ]
        return icons_lib.render_kotlin(icons)

    def test_tem_cabecalho_apache_e_marca_de_arquivo_gerado(self):
        kt = self.render()
        self.assertIn("Apache License, Version 2.0", kt)
        self.assertIn("Copyright 2023 Google LLC", kt)
        self.assertIn("ARQUIVO GERADO", kt)
        self.assertIn("py tools/icons/generate_johnicons.py", kt)
        self.assertIn("package com.johngabie.johnpdf.ui.icons", kt)

    def test_usa_addpathnodes_e_o_group_do_viewbox_negativo(self):
        kt = self.render()
        self.assertIn("import androidx.compose.ui.graphics.vector.addPathNodes", kt)
        self.assertIn("pathData = addPathNodes(d)", kt)
        self.assertIn("group(translationY = 960f)", kt)
        self.assertIn("viewportWidth = 960f", kt)
        self.assertIn("viewportHeight = 960f", kt)
        self.assertIn("defaultWidth = 24.dp", kt)
        self.assertNotIn("moveTo(", kt)
        self.assertNotIn("curveToRelative(", kt)

    def test_emite_um_val_por_icone_com_nome_proprio(self):
        kt = self.render()
        self.assertIn(
            'val ArrowBack: ImageVector by lazy { symbol("ArrowBack", D_ARROW_BACK, autoMirror = true) }',
            kt,
        )
        self.assertIn(
            'val History: ImageVector by lazy { symbol("History", D_HISTORY) }', kt
        )
        self.assertIn(
            'val HistoryFilled: ImageVector by lazy { symbol("HistoryFilled", D_HISTORY_FILLED) }',
            kt,
        )

    def test_evenodd_vira_pathfilltype(self):
        kt = self.render()
        self.assertIn(
            'val Error: ImageVector by lazy { symbol("Error", D_ERROR, fillType = PathFillType.EvenOdd) }',
            kt,
        )

    def test_copia_o_d_byte_a_byte(self):
        kt = self.render()
        self.assertIn('private const val D_ARROW_BACK =\n    "M1-1Z"', kt)
        self.assertIn('private const val D_HISTORY_FILLED =\n    "M3-3Z"', kt)

    def test_lista_all_em_ordem_alfabetica(self):
        kt = self.render()
        self.assertIn("val All: List<ImageVector>", kt)
        bloco = kt.split("val All: List<ImageVector>", 1)[1].split(")", 1)[0]
        for nome in ("ArrowBack", "Error", "History", "HistoryFilled"):
            self.assertIn(nome, bloco)

    def test_saida_e_deterministica_e_so_tem_lf(self):
        self.assertEqual(self.render(), self.render())
        self.assertNotIn("\r", self.render())
        self.assertTrue(self.render().endswith("\n"))


class GeneratorEndToEndTest(unittest.TestCase):
    def test_gera_os_22_vals_a_partir_dos_svgs_versionados(self):
        kt = icons_lib.render_kotlin(icons_lib.load_icons())
        self.assertEqual(22, kt.count(": ImageVector by lazy {"))
        self.assertEqual(22, kt.count("private const val D_"))
        self.assertEqual(1, kt.count("autoMirror = true"))
```

- [ ] **Step 2: Rodar e confirmar que falha**

```powershell
py -m unittest discover -s tools/icons -t tools/icons -v
```

Esperado: `AttributeError: module 'icons_lib' has no attribute 'render_kotlin'`.

- [ ] **Step 3: Implementar o emissor**

Acrescentar a `tools/icons/icons_lib.py`:

```python
HEADER = '''/*
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
 * Regenerar com: py tools/icons/generate_johnicons.py
 * SVGs de origem: tools/icons/svg/ (commit upstream em tools/icons/UPSTREAM.txt)
 */
package com.johngabie.johnpdf.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/**
 * Constrói um Material Symbol 24dp a partir do atributo `d` do SVG.
 *
 * O viewBox dos Symbols é `0 -960 960 960` (origem embaixo, Y negativo). O
 * [group] com `translationY = 960f` traz o desenho para o quadrante positivo
 * sem mexer numa única coordenada do `d` — é o que mantém a string idêntica
 * ao arquivo upstream.
 *
 * `name` vira `"JohnIcons.<Nome>"`: é como o ícone aparece no dump de
 * semântica e nas mensagens de falha dos testes.
 *
 * A cor preta é um marcador. `Icon(...)` sempre aplica tint
 * (`LocalContentColor` por padrão), então nenhum ícone chega à tela em preto fixo.
 */
private fun symbol(
    name: String,
    d: String,
    autoMirror: Boolean = false,
    fillType: PathFillType = PathFillType.NonZero,
): ImageVector =
    ImageVector.Builder(
        name = "JohnIcons.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = autoMirror,
    ).apply {
        group(translationY = 960f) {
            addPath(
                pathData = addPathNodes(d),
                pathFillType = fillType,
                fill = SolidColor(Color.Black),
            )
        }
    }.build()

/** Os ícones do johnPDF. Sempre usar via `Icon(JohnIcons.X, contentDescription = ...)`. */
object JohnIcons {
'''

ALL_DOC = """
    /**
     * Todos os ícones, para os testes de sanidade de `JohnIconsTest`.
     * Não usar em código de produção.
     */
"""


def _val_line(icon: Icon) -> str:
    args = [f'"{icon.kotlin}"', const_name(icon.kotlin)]
    if icon.automirror:
        args.append("autoMirror = true")
    if icon.even_odd:
        args.append("fillType = PathFillType.EvenOdd")
    chamada = f"symbol({', '.join(args)})"
    doc = f"    /** `{icon.upstream}`"
    if icon.fill:
        doc += " (fill 1)"
    doc += f" — {svg_filename(icon)}. */\n"
    return f"{doc}    val {icon.kotlin}: ImageVector by lazy {{ {chamada} }}\n"


def _all_block(icons: list[Icon]) -> str:
    linhas = ["    val All: List<ImageVector>", "        get() = listOf("]
    atual = "           "
    for icon in icons:
        pedaco = f" {icon.kotlin},"
        if len(atual) + len(pedaco) > 96:
            linhas.append(atual)
            atual = "           "
        atual += pedaco
    linhas.append(atual)
    linhas.append("        )")
    return "\n".join(linhas) + "\n"


def render_kotlin(icons: list[Icon]) -> str:
    """Monta o conteúdo inteiro de JohnIcons.kt. Determinístico, só LF."""
    icons = sorted(icons, key=lambda i: i.kotlin)
    partes = [HEADER]
    partes.append("\n".join(_val_line(i).rstrip("\n") for i in icons))
    partes.append("\n")
    partes.append(ALL_DOC)
    partes.append(_all_block(icons))
    partes.append("}\n")
    for icon in icons:
        partes.append(
            f"\n// Copiado byte a byte do atributo `d` de "
            f"tools/icons/svg/{svg_filename(icon)} — não editar.\n"
            f"private const val {const_name(icon.kotlin)} =\n"
            f'    "{icon.d}"\n'
        )
    texto = "".join(partes)
    assert "\r" not in texto
    return texto
```

- [ ] **Step 4: Escrever o script gerador**

Criar `tools/icons/generate_johnicons.py`:

```python
#!/usr/bin/env python3
"""Gera app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt.

Offline e idempotente: lê tools/icons/icons.txt + tools/icons/svg/*.svg.
Nunca acessa a rede. Rodar duas vezes produz bytes idênticos.

Uso:
  py tools/icons/generate_johnicons.py
  py tools/icons/generate_johnicons.py --check   # falha se o .kt estiver defasado
"""
from __future__ import annotations

import argparse
import sys

import icons_lib


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument(
        "--check",
        action="store_true",
        help="não escreve; sai com 1 se o arquivo versionado estiver defasado",
    )
    args = ap.parse_args()

    icons = icons_lib.load_icons()
    novo = icons_lib.render_kotlin(icons)
    destino = icons_lib.KT_OUT

    if args.check:
        atual = destino.read_text(encoding="utf-8") if destino.exists() else ""
        if atual != novo:
            print(
                f"ERRO: {destino.relative_to(icons_lib.REPO_ROOT)} está defasado.\n"
                "Rode: py tools/icons/generate_johnicons.py",
                file=sys.stderr,
            )
            return 1
        print(f"ok: {destino.name} corresponde aos SVGs versionados")
        return 0

    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text(novo, encoding="utf-8", newline="\n")
    print(f"{destino.relative_to(icons_lib.REPO_ROOT)}: {len(icons)} ícones, {len(novo)} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
```

- [ ] **Step 5: Rodar os testes e confirmar que passam**

```powershell
py -m unittest discover -s tools/icons -t tools/icons -v
```

Esperado: `OK`, 27 testes.

- [ ] **Step 6: Commit**

```bash
git add tools/icons/icons_lib.py tools/icons/generate_johnicons.py tools/icons/test_icons_lib.py
git commit -m "feat(icons): emissor de JohnIcons.kt a partir dos SVGs"
```

---

### Task 5: Gerar `JohnIcons.kt`, compilar e provar a idempotência

**Files:**
- Create (gerado): `app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt`

**Interfaces:**
- Consumes: `generate_johnicons.py`, `tools/icons/svg/`.
- Produces: `object JohnIcons` no pacote `com.johngabie.johnpdf.ui.icons`, com 22 `val <Nome>: ImageVector` e `val All: List<ImageVector>`. É a API que todas as tarefas seguintes (e as tarefas de tela, fora deste plano) consomem.

- [ ] **Step 1: Gerar o arquivo**

```powershell
py tools/icons/generate_johnicons.py
```

Esperado: `app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt: 22 ícones, NNNNN bytes`.

- [ ] **Step 2: Compilar — este é o teste que importa aqui**

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Esperado: `BUILD SUCCESSFUL`.

Este passo é a verificação obrigatória da §3.2(a) da spec: se `addPathNodes`, `group` ou o parâmetro `name` do `ImageVector.Builder` não resolverem contra a BOM `2025.05.00`, o erro aparece aqui.

**Se `Unresolved reference: addPathNodes`:** não transcreva nada à mão. O fallback é fazer `render_kotlin` emitir os nós explícitos a partir do mesmo `d` — ou seja, portar o parser de path para o `icons_lib.py` e emitir `moveTo`/`curveToRelative`/`close`. Isso é uma tarefa nova e grande; pare, registre o erro exato e leve para o seu parceiro humano antes de começar. A API pública de `JohnIcons` não muda no fallback.

**Se `Unresolved reference: group`:** trocar `group(translationY = 960f) { addPath(...) }` por `addGroup(translationY = 960f)` + `addPath(...)` + `clearGroup()` em `HEADER`, e ajustar `test_usa_addpathnodes_e_o_group_do_viewbox_negativo` para procurar `addGroup(translationY = 960f)`.

- [ ] **Step 3: Provar que o gerador é idempotente**

```powershell
py tools/icons/generate_johnicons.py
git diff --exit-code -- app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt
py tools/icons/generate_johnicons.py --check
```

Esperado: nenhuma saída do `git diff` (exit 0) e `ok: JohnIcons.kt corresponde aos SVGs versionados`. Se houver diff, quase certamente é `\r\n` — confira que `write_text(..., newline="\n")` está no `generate_johnicons.py`.

- [ ] **Step 4: Conferir o arquivo a olho (só a forma, não as coordenadas)**

```powershell
Get-Content app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt -TotalCount 60
```

Esperado: cabeçalho Apache 2.0, `Copyright 2023 Google LLC`, `ARQUIVO GERADO`, `package com.johngabie.johnpdf.ui.icons`, a função `symbol` e o início dos `val` em ordem alfabética começando por `ArrowBack`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt
git commit -m "feat(ui): gera ui/icons/JohnIcons.kt com 22 Material Symbols Rounded"
```

---

### Task 6: Testes de metadado, inventário, auto-mirror e nome

**Files:**
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsTest.kt`

**Interfaces:**
- Consumes: `com.johngabie.johnpdf.ui.icons.JohnIcons` (todos os 22 `val` + `All`).
- Produces: nada consumido por outras tarefas.

> Roda com `@RunWith(AndroidJUnit4::class)` como o resto da suíte (Robolectric, `sdk=35` via `app/src/test/resources/robolectric.properties`). A spec §7.1 sugeria JVM puro, mas `ImageVector` arrasta `androidx.compose.ui.graphics` e o projeto não configura `returnDefaultValues` — manter um runner só é mais barato que descobrir isso num `NullPointerException` de `android.graphics`.

- [ ] **Step 1: Escrever o teste que falha**

Criar `app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.icons

import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Sanidade dos 22 Material Symbols vendorizados em JohnIcons.kt. */
@RunWith(AndroidJUnit4::class)
class JohnIconsTest {

    /** Ordem alfabética, igual à de JohnIcons.All. Escrita à mão de propósito. */
    private val nomesEsperados = listOf(
        "ArrowBack", "Close", "Delete", "Dialpad", "Error", "Folder", "FolderOpen",
        "History", "HistoryFilled", "Keyboard", "KeyboardArrowDown", "KeyboardArrowUp",
        "LibraryBooks", "LibraryBooksFilled", "Lock", "PictureAsPdf",
        "ScreenLockRotation", "ScreenRotation", "Search", "SearchOff",
        "Visibility", "VisibilityOff",
    )

    @Test fun inventario_completo() {
        assertEquals(22, nomesEsperados.size)
        assertEquals(
            "JohnIcons.All não bate com o inventário; alguém criou um val e esqueceu de All",
            22,
            JohnIcons.All.size,
        )
    }

    @Test fun todos_os_icones_tem_24dp_e_viewport_960() {
        JohnIcons.All.forEach { icone ->
            assertEquals("${icone.name}: defaultWidth", 24.dp, icone.defaultWidth)
            assertEquals("${icone.name}: defaultHeight", 24.dp, icone.defaultHeight)
            assertEquals("${icone.name}: viewportWidth", 960f, icone.viewportWidth, 0f)
            assertEquals("${icone.name}: viewportHeight", 960f, icone.viewportHeight, 0f)
        }
    }

    /** Review Focus 4: sem nome próprio, o ícone não dá para ser identificado em lugar nenhum. */
    @Test fun todo_icone_tem_nome_unico_e_legivel() {
        val nomes = JohnIcons.All.map { it.name }
        assertEquals(
            "cada ImageVector deve se chamar JohnIcons.<NomeDoVal>",
            nomesEsperados.map { "JohnIcons.$it" },
            nomes,
        )
        assertEquals("nomes duplicados em JohnIcons", nomes.size, nomes.toSet().size)
        nomes.forEach { assertTrue("nome em branco", it.isNotBlank()) }
    }

    /** Review Focus 3: só a seta de voltar espelha em RTL. */
    @Test fun apenas_arrow_back_espelha_em_rtl() {
        assertTrue("ArrowBack deve espelhar em RTL", JohnIcons.ArrowBack.autoMirror)
        JohnIcons.All.filter { it.name != "JohnIcons.ArrowBack" }.forEach { icone ->
            assertFalse("${icone.name} não deve espelhar em RTL", icone.autoMirror)
        }
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*JohnIconsTest"
```

Esperado: neste ponto o arquivo `JohnIcons.kt` já existe (Task 5), então os testes devem **passar** direto. Se algum falhar, o defeito está no gerador — conserte `icons_lib.py`, regenere e commite os dois. Registre qual falhou antes de mexer.

- [ ] **Step 3: Corrigir o que falhar no gerador (se houver)**

Se `todo_icone_tem_nome_unico_e_legivel` falhar, `_val_line` em `icons_lib.py` não está passando `"${icon.kotlin}"` como primeiro argumento de `symbol(...)`. Se `apenas_arrow_back_espelha_em_rtl` falhar, a flag `automirror` em `icons.txt` está no lugar errado. Em qualquer caso: editar o gerador, `py tools/icons/generate_johnicons.py`, rodar de novo.

- [ ] **Step 4: Rodar e confirmar que passa**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*JohnIconsTest"
```

Esperado: `BUILD SUCCESSFUL`, 4 testes verdes.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsTest.kt
git commit -m "test(icons): inventario, viewport, nome e auto-mirror dos 22 icones"
```

---

### Task 7: Teste de geometria — todo path cabe no viewBox

**Files:**
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsGeometryTest.kt`

**Interfaces:**
- Consumes: `JohnIcons.All`, `androidx.compose.ui.graphics.vector.VectorGroup`, `VectorPath`, `PathParser`.
- Produces: nada.

Este teste ataca o Review Focus 5 e também cobre o `nenhum_icone_esta_vazio` da spec §7.1: ele desce na árvore do `ImageVector`, exige um `VectorPath` com nós, reconstrói o `Path` e mede o bounding box. Um `d` truncado dá bounds degenerados; um SVG de outro optical size dá bounds fora do quadrante.

- [ ] **Step 1: Escrever o teste que falha**

Criar `app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsGeometryTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Review Focus 5: um `d` sintaticamente válido mas com coordenadas erradas
 * compila, passa nos testes de metadado e desenha cortado ou minúsculo.
 * Aqui os paths são reconstruídos e medidos.
 */
@RunWith(AndroidJUnit4::class)
class JohnIconsGeometryTest {

    private fun paths(no: VectorNode): List<VectorPath> = when (no) {
        is VectorPath -> listOf(no)
        is VectorGroup -> no.flatMap { paths(it) }
    }

    private fun paths(icone: ImageVector): List<VectorPath> = paths(icone.root)

    @Test fun nenhum_icone_esta_vazio() {
        JohnIcons.All.forEach { icone ->
            val encontrados = paths(icone)
            assertTrue("${icone.name}: nenhum VectorPath na árvore", encontrados.isNotEmpty())
            encontrados.forEach { p ->
                assertTrue("${icone.name}: pathData vazio", p.pathData.isNotEmpty())
            }
        }
    }

    /**
     * O viewBox dos Material Symbols é `0 -960 960 960`: X de 0 a 960,
     * Y de -960 a 0. Tolerância de 2f para arredondamento do parser.
     */
    @Test fun geometria_de_todo_icone_cabe_no_viewbox() {
        val tol = 2f
        JohnIcons.All.forEach { icone ->
            paths(icone).forEach { p ->
                val caminho = PathParser().addPathNodes(p.pathData).toPath()
                val b = caminho.getBounds()
                val onde = "${icone.name} bounds=$b"
                assertTrue("$onde: largura zero", b.width > 0f)
                assertTrue("$onde: altura zero", b.height > 0f)
                assertTrue("$onde: x mínimo fora do viewBox", b.left >= -tol)
                assertTrue("$onde: x máximo fora do viewBox", b.right <= 960f + tol)
                assertTrue("$onde: y mínimo fora do viewBox", b.top >= -960f - tol)
                assertTrue("$onde: y máximo fora do viewBox", b.bottom <= tol)
            }
        }
    }

    /** Um símbolo de 24px ocupa a maior parte do quadro; 1/4 do lado é o piso. */
    @Test fun todo_icone_ocupa_uma_area_plausivel() {
        JohnIcons.All.forEach { icone ->
            val b = PathParser().addPathNodes(paths(icone).first().pathData).toPath().getBounds()
            assertTrue(
                "${icone.name}: desenho minúsculo (${b.width}x${b.height} em 960x960) — " +
                    "suspeita de `d` truncado",
                b.width >= 240f || b.height >= 240f,
            )
        }
    }
}
```

- [ ] **Step 2: Rodar e confirmar o resultado**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*JohnIconsGeometryTest"
```

Esperado: 3 testes verdes.

**Se `getBounds()` devolver `Rect(0,0,0,0)` em todos:** o Robolectric está em modo de gráficos legado. Acrescentar ao topo da classe, acima de `class`:

```kotlin
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
```

**Se um ícone específico falhar `geometria_de_todo_icone_cabe_no_viewbox`:** o SVG dele está errado. Abra `tools/icons/svg/<nome>_24px.svg`, compare com https://fonts.google.com/icons (filtro Rounded), rebaixe se necessário — **não** afrouxe a tolerância.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsGeometryTest.kt
git commit -m "test(icons): bounds de todo path dentro do viewBox 0 -960 960 960"
```

---

### Task 8: Teste de renderização — cada ícone pinta pixels, e o preenchido pinta mais

**Files:**
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsRenderTest.kt`

**Interfaces:**
- Consumes: `JohnIcons.All`, `JohnIcons.History`, `JohnIcons.HistoryFilled`, `JohnIcons.LibraryBooks`, `JohnIcons.LibraryBooksFilled`.
- Produces: função de contagem de pixels reaproveitada em espírito (não em código) pela Task 10.

Este é o teste que a spec §7.1 não tem e que fecha o Review Focus 1 e 2: um `ImageVector` que existe não é um ícone que desenha.

- [ ] **Step 1: Escrever o teste que falha**

Criar `app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsRenderTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Review Focus 1 e 2. Um SVG baixado errado (404 salvo como HTML, `d`
 * truncado) produz um ImageVector que compila e não desenha nada; e se as
 * duas variantes da bottom nav vierem do mesmo arquivo, selecionado e não
 * selecionado ficam idênticos. Só medir pixel pega os dois casos.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JohnIconsRenderTest {
    @get:Rule val rule = createComposeRule()

    private val TAG = "icone"

    private fun pixelsOpacos(icone: ImageVector): Int {
        rule.setContent {
            Box(Modifier.size(48.dp).testTag(TAG)) {
                Icon(icone, contentDescription = null, tint = Color.Black)
            }
        }
        val bitmap: ImageBitmap = rule.onNodeWithTag(TAG).captureToImage()
        val pixels = bitmap.toPixelMap()
        var opacos = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (pixels[x, y].alpha > 0.5f) opacos++
            }
        }
        return opacos
    }

    @Test fun todo_icone_pinta_pixels() {
        JohnIcons.All.forEach { icone ->
            val opacos = pixelsOpacos(icone)
            assertTrue(
                "${icone.name} não desenhou nada a 48dp — SVG vazio, 404 ou `d` truncado",
                opacos > 0,
            )
        }
    }

    @Test fun variante_filled_pinta_mais_que_o_contorno() {
        val paresParaComparar = listOf(
            Triple("history", JohnIcons.History, JohnIcons.HistoryFilled),
            Triple("library_books", JohnIcons.LibraryBooks, JohnIcons.LibraryBooksFilled),
        )
        paresParaComparar.forEach { (nome, contorno, preenchido) ->
            val a = pixelsOpacos(contorno)
            val b = pixelsOpacos(preenchido)
            assertTrue(
                "$nome: fill1 pintou $b pixels e o contorno $a — as duas variantes " +
                    "provavelmente vieram do mesmo SVG",
                b > a,
            )
        }
    }
}
```

> `Icon(...)` com `tint = Color.Black` e fundo transparente: contar `alpha > 0.5f` mede exatamente a tinta do vetor.
> Se `rule.setContent` reclamar de ser chamado duas vezes na mesma regra, trocar o laço por um único `setContent` que desenha todos os 22 com `testTag(icone.name)` e capturar cada nó pelo tag.

- [ ] **Step 2: Rodar e confirmar o resultado**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*JohnIconsRenderTest"
```

Esperado: 2 testes verdes.

**Se `captureToImage` lançar `UnsupportedOperationException` ou devolver bitmap preto:** o `@GraphicsMode(NATIVE)` não pegou. Conferir que `app/src/test/resources/robolectric.properties` tem `sdk=35` (gráficos nativos exigem API ≥ 26) e que `testOptions { unitTests.isIncludeAndroidResources = true }` está em `app/build.gradle.kts` — as duas coisas já existem no projeto.

**Se `variante_filled_pinta_mais_que_o_contorno` falhar com contagens iguais:** `fetch_svgs.py` baixou o mesmo arquivo duas vezes. Voltar à Task 2, conferir `history_fill1_24px.svg` contra `history_24px.svg`.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsRenderTest.kt
git commit -m "test(icons): todo icone pinta pixels e fill1 pinta mais que o contorno"
```

---

### Task 9: `NOTICE` e conferência das obrigações da Apache 2.0

**Files:**
- Create: `NOTICE`
- Verify: `third_party/material-design-icons/LICENSE` (criado na Task 2), cabeçalho de `JohnIcons.kt` (Task 5)

**Interfaces:**
- Consumes: os caminhos produzidos nas Tasks 2 e 5.
- Produces: nada em código.

- [ ] **Step 1: Escrever o `NOTICE`**

Criar `NOTICE` na raiz do repositório:

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

- [ ] **Step 2: Conferir as três obrigações**

```powershell
Get-Content NOTICE
Get-Content third_party/material-design-icons/LICENSE -TotalCount 5
(Get-Content third_party/material-design-icons/LICENSE | Measure-Object -Line).Lines
Select-String -Path app/src/main/java/com/johngabie/johnpdf/ui/icons/JohnIcons.kt -Pattern "Apache License|Copyright 2023 Google LLC|ARQUIVO GERADO"
Get-Content tools/icons/UPSTREAM.txt
```

Esperado:
1. `NOTICE` existe e cita Google LLC, Apache 2.0 e o caminho da cópia da licença.
2. `third_party/material-design-icons/LICENSE` começa com `Apache License` / `Version 2.0, January 2004` e tem ~200 linhas (texto íntegro, sem modificação).
3. `JohnIcons.kt` casa os três padrões procurados.
4. `UPSTREAM.txt` registra o SHA do commit de origem.

- [ ] **Step 3: Conferir que nada novo entrou no build**

```powershell
git diff --stat main -- app/build.gradle.kts gradle/libs.versions.toml
Select-String -Path app/build.gradle.kts -Pattern "isMinifyEnabled"
```

Esperado: `git diff --stat` **vazio** (nenhuma linha alterada nos dois arquivos de build) e `isMinifyEnabled = false` intacto. Se houver qualquer diff aí, uma dependência entrou sem querer — reverta.

- [ ] **Step 4: Commit**

```bash
git add NOTICE
git commit -m "docs: NOTICE com a atribuicao Apache 2.0 dos Material Symbols"
```

---

### Task 10: Conferência visual — grade dos 22 ícones em PNG

**Files:**
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsGridCaptureTest.kt`
- Create (evidência): `docs/e2e/img/icons-grid.png`

**Interfaces:**
- Consumes: `JohnIcons.All`.
- Produces: `docs/e2e/img/icons-grid.png` — a evidência exigida pelo critério de aceite da spec §9.2.

Este é o único passo que nenhum teste substitui: prova que o ícone desenha **a coisa certa**, não só que desenha alguma coisa. Roda sob demanda, guardado por variável de ambiente, para não escrever no repositório em toda execução da suíte.

- [ ] **Step 1: Escrever o capturador**

Criar `app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsGridCaptureTest.kt`:

```kotlin
package com.johngabie.johnpdf.ui.icons

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Gera docs/e2e/img/icons-grid.png para conferência a olho contra
 * https://fonts.google.com/icons (filtro: Rounded).
 *
 * Só roda com JOHNPDF_CAPTURE_ICONS=1 — escreve arquivo no repositório.
 *
 *   PowerShell:  $env:JOHNPDF_CAPTURE_ICONS="1"; .\gradlew.bat :app:testDebugUnitTest --tests "*JohnIconsGridCaptureTest"
 *   bash:        JOHNPDF_CAPTURE_ICONS=1 ./gradlew :app:testDebugUnitTest --tests '*JohnIconsGridCaptureTest'
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JohnIconsGridCaptureTest {
    @get:Rule val rule = createComposeRule()

    @Test fun gera_a_grade_de_conferencia_visual() {
        assumeTrue(
            "pulado: defina JOHNPDF_CAPTURE_ICONS=1 para regerar a grade",
            System.getenv("JOHNPDF_CAPTURE_ICONS") == "1",
        )

        val porLinha = 4
        rule.setContent {
            Column(
                Modifier.background(Color.White).padding(16.dp).testTag("grade"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                JohnIcons.All.chunked(porLinha).forEach { linha ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        linha.forEach { icone ->
                            Column(
                                Modifier.size(width = 140.dp, height = 84.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    icone,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(48.dp),
                                )
                                Text(
                                    icone.name.removePrefix("JohnIcons."),
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        val bitmap = rule.onNodeWithTag("grade").captureToImage().asAndroidBitmap()
        // Os testes unitários rodam com working dir = app/.
        val destino = File("../docs/e2e/img/icons-grid.png").canonicalFile
        destino.parentFile.mkdirs()
        destino.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("grade de ícones salva em $destino (${bitmap.width}x${bitmap.height})")
    }
}
```

> O `fontSize = 11.sp` viola a regra global de 20sp do app de propósito: isto não é UI, é uma folha de contato de diagnóstico que nunca chega ao APK. Não copiar esse valor para nenhuma tela.

- [ ] **Step 2: Confirmar que pula por padrão**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*JohnIconsGridCaptureTest"
```

Esperado: `BUILD SUCCESSFUL`, 1 teste ignorado (skipped).

- [ ] **Step 3: Gerar o PNG**

```powershell
$env:JOHNPDF_CAPTURE_ICONS="1"
.\gradlew.bat :app:testDebugUnitTest --tests "*JohnIconsGridCaptureTest"
Remove-Item Env:\JOHNPDF_CAPTURE_ICONS
```

Esperado: `BUILD SUCCESSFUL` e, no log do teste, `grade de ícones salva em ...\docs\e2e\img\icons-grid.png`. Se o caminho sair errado, rodar `Get-ChildItem docs/e2e/img/icons-grid.png` para confirmar e ajustar o `File("../docs/e2e/img/...")` conforme o working dir real impresso.

- [ ] **Step 4: Conferir os 22 ícones a olho**

Abrir `docs/e2e/img/icons-grid.png` e comparar, um a um, com https://fonts.google.com/icons (filtro **Rounded**, weight 400, grade 0, optical size 24):

- [ ] `ArrowBack` — seta apontando para a **esquerda**
- [ ] `Close` — X
- [ ] `Delete` — lixeira
- [ ] `Dialpad` — 9 pontos em grade 3×3
- [ ] `Error` — círculo com "!"
- [ ] `Folder` / `FolderOpen` — pasta fechada / pasta aberta, **visivelmente diferentes**
- [ ] `History` / `HistoryFilled` — relógio com seta de retorno, contorno / preenchido
- [ ] `Keyboard` — teclado; `KeyboardArrowUp` / `KeyboardArrowDown` — chevrons para cima / para baixo (não confundir com `Keyboard`)
- [ ] `LibraryBooks` / `LibraryBooksFilled` — livros empilhados, contorno / preenchido
- [ ] `Lock` — cadeado fechado
- [ ] `PictureAsPdf` — folha com "PDF"
- [ ] `ScreenRotation` / `ScreenLockRotation` — celular com setas de rotação; o segundo tem cadeado
- [ ] `Search` / `SearchOff` — lupa / lupa riscada
- [ ] `Visibility` / `VisibilityOff` — olho / olho riscado
- [ ] Nenhum quadro em branco; nenhum ícone cortado nas bordas; nenhum de cabeça para baixo (se todos estiverem invertidos verticalmente, o `translationY = 960f` do `group` está errado)

Qualquer divergência: trocar a linha em `icons.txt` ou rebaixar o SVG (Task 2), regerar (`py tools/icons/generate_johnicons.py`) e capturar de novo.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/johngabie/johnpdf/ui/icons/JohnIconsGridCaptureTest.kt docs/e2e/img/icons-grid.png
git commit -m "test(icons): grade de conferencia visual dos 22 icones + evidencia PNG"
```

---

### Task 11: Suíte completa, medição do APK e critério de aceite

**Files:**
- Verify only: nenhum arquivo novo.

**Interfaces:**
- Consumes: tudo que as Tasks 1–10 produziram.
- Produces: o veredito do critério de aceite da spec §9.2.

- [ ] **Step 1: Rodar a suíte Python inteira**

```powershell
py -m unittest discover -s tools/icons -t tools/icons -v
```

Esperado: `OK`, 27 testes. Nenhum erro, nenhum skip.

- [ ] **Step 2: Rodar a suíte Android inteira e confirmar que não há regressão**

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Esperado: `BUILD SUCCESSFUL`. Os testes existentes (`HomeContentTest`, `HomeViewModelTest`, `ReaderContentTest`, `ReaderViewModelTest`, `PageMathTest`, `DialogsTest`, `ThemeTest`, os de `data/` e `util/`) devem estar todos verdes e **inalterados** — `JohnIcons.kt` ainda não tem nenhum consumidor de produção, então qualquer regressão aí é sinal de que alguém mexeu numa tela, o que está fora do escopo deste plano.

Relatório em `app/build/reports/tests/testDebugUnitTest/index.html` se algo falhar.

- [ ] **Step 3: Provar de novo a idempotência do gerador (estado final)**

```powershell
py tools/icons/generate_johnicons.py --check
git status --porcelain
```

Esperado: `ok: JohnIcons.kt corresponde aos SVGs versionados` e árvore limpa.

- [ ] **Step 4: Medir o APK release**

```powershell
.\gradlew.bat :app:assembleRelease
Get-ChildItem app/build/outputs/apk/release/ | Select-Object Name, @{n='MB';e={[math]::Round($_.Length/1MB,2)}}
```

Esperado: o APK `arm64-v8a` abaixo de **19,0 MB** (baseline 18,9 MB). Se o `keystore.properties` não existir, o release sai não assinado — o tamanho ainda serve para a medição.

Se passar de 19,0 MB, investigar antes de seguir: 22 strings de path não chegam a 100 KB, então um estouro significa que outra coisa entrou no build.

- [ ] **Step 5: Percorrer o critério de aceite da spec §9.2**

- [ ] `JohnIcons.kt` existe, compila, tem cabeçalho Apache 2.0 e a marcação "ARQUIVO GERADO"
- [ ] `NOTICE` na raiz + `third_party/material-design-icons/LICENSE` íntegro
- [ ] `tools/icons/svg/` versionado, com `UPSTREAM.txt` registrando o commit de origem
- [ ] `.\gradlew.bat :app:testDebugUnitTest` verde, incluindo `JohnIconsTest`, `JohnIconsGeometryTest` e `JohnIconsRenderTest`
- [ ] Gerador idempotente (`--check` limpo, `git diff --exit-code` limpo)
- [ ] Os 22 ícones conferidos visualmente, evidência em `docs/e2e/img/icons-grid.png`
- [ ] APK release arm64 abaixo de 19,0 MB
- [ ] Nenhuma dependência nova em `gradle/libs.versions.toml` nem em `app/build.gradle.kts`; `isMinifyEnabled = false` inalterado
- [ ] Nenhuma tela, diálogo ou `Theme.kt` alterado

- [ ] **Step 6: Commit final**

Se os passos 1–5 não produziram nenhuma mudança de arquivo, não há o que commitar — confirme com `git status` e siga. Se algum ajuste foi necessário:

```bash
git add -A
git commit -m "chore(icons): ajustes finais da vendorizacao dos Material Symbols"
```

O commit que fecha o conjunto (se precisar de um commit de fechamento, por exemplo depois de um squash) usa a mensagem sugerida pela spec §9.3:

```
feat(ui): vendoriza 22 Material Symbols Rounded como ImageVector

Cria ui/icons/JohnIcons.kt a partir dos SVGs de google/material-design-icons
(Apache 2.0), gerado por tools/icons/generate_johnicons.py. Sem dependência
nova; substitui a opção material-icons-extended (+8-12 MB) avaliada no D1 §B.

Nenhuma tela alterada ainda — as trocas de emoji vêm nas tarefas seguintes.
```

---

## Notas para quem executar

**Desvios conscientes em relação à spec, e por quê:**

1. **`symbol()` ganha um parâmetro `name`** (spec §3.3 não tinha). Sem ele, `ImageVector.name` fica `""` e nenhum teste, nenhum dump de semântica e nenhuma mensagem de falha consegue dizer *qual* ícone quebrou. Custa uma string por ícone.
2. **`symbol()` ganha `fillType`** — a spec §6.2 já previa `PathFillType.EvenOdd` para SVGs com `fill-rule="evenodd"`; o parâmetro é onde isso aterrissa.
3. **`JohnIconsTest` roda com Robolectric**, não JVM puro como a spec §7.1 sugeria. Motivo em nota na Task 6.
4. **A spec pedia um arquivo de teste; este plano tem quatro.** Motivo na seção "Estrutura de arquivos".
5. **A conferência visual usa Robolectric + `captureToImage`** (a opção "sem dependência nova" da spec §7.3), não `@Preview`. A opção do `@Preview` exigiria `debugImplementation("androidx.compose.ui:ui-tooling")`, e a restrição global deste plano é zero dependência nova.
6. **`icons_lib.py` separado dos scripts** (a spec §6 listava só `fetch_svgs.py` e `generate_johnicons.py`). Sem essa separação não há como escrever um teste do gerador que não faça I/O de rede.
7. **A validação de tamanho aceita `24px` e `24`.** A spec §6.2 pedia `24px` estrito; alguns SVGs de Material Symbols vêm sem a unidade. Qualquer outro valor (`48px`, `40px`) continua sendo recusado, que é o que a validação existe para pegar.

**O que este plano não faz** (e são as tarefas seguintes, fora daqui): trocar 📂🕘📚🔍📄🔒🔓⬆⬇ e os rótulos "← Voltar" / "abc" / "123" por `Icon(JohnIcons.X, ...)` em `HomeScreen.kt`, `ReaderScreen.kt` e `Dialogs.kt`; aplicar as regras de `contentDescription` da spec §8.7; atualizar `HomeContentTest`, `ReaderContentTest` e `DialogsTest` para selecionar por `contentDescription` em vez de emoji. Ao final deste plano, `JohnIcons.kt` não tem nenhum consumidor de produção — isso é esperado, e é o que mantém as duas frentes revisáveis separadamente.
