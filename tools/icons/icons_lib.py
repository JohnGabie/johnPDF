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
