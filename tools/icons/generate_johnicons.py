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
