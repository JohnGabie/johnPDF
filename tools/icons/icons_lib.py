#!/usr/bin/env python3
"""Lógica pura do gerador de ícones do johnPDF.

Sem rede e sem dependências fora da stdlib — tudo aqui é testável por
tools/icons/test_icons_lib.py.
"""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
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

