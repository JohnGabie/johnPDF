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
