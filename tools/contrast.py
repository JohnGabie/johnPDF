"""Confere o contraste WCAG 2.1 dos esquemas de cor do johnPDF.

Usado para validar uma paleta ANTES de escrevê-la em ui/theme/Theme.kt.
A mesma regra está travada em ContrastTest.kt; este script é para iterar rápido
(por exemplo, quando o seed de marca mudar).

Regras do projeto:
  - texto                     >= 4.5:1  (WCAG 1.4.3 AA)
  - texto principal            >= 7.0:1  (WCAG 1.4.6 AAA)
  - divisores / bordas        >= 3.0:1  (WCAG 1.4.11)
  - rótulo desabilitado       >= 3.0:1  (mais rígido que a WCAG, que isenta)

Uso:  .venv-tools/Scripts/python tools/contrast.py
"""

# --- esquemas: espelham ui/theme/Theme.kt -----------------------------------

LIGHT = {
    "primary": "#006494", "onPrimary": "#FFFFFF",
    "primaryContainer": "#CBE6FF", "onPrimaryContainer": "#001E30",
    "secondary": "#4E616D", "onSecondary": "#FFFFFF",
    "secondaryContainer": "#D2E5F5", "onSecondaryContainer": "#0B1D29",
    "tertiary": "#00677C", "onTertiary": "#FFFFFF",
    "tertiaryContainer": "#B2EBFF", "onTertiaryContainer": "#001F27",
    "error": "#BA1A1A", "onError": "#FFFFFF",
    "errorContainer": "#FFDAD6", "onErrorContainer": "#410002",
    "background": "#F7FAFC", "onBackground": "#171C1F",
    "surface": "#F7FAFC", "onSurface": "#171C1F",
    "surfaceVariant": "#DCE3E9", "onSurfaceVariant": "#40484D",
    "surfaceContainerLowest": "#FFFFFF", "surfaceContainerLow": "#F1F4F7",
    "surfaceContainer": "#EBEFF2", "surfaceContainerHigh": "#E5EAEE",
    "surfaceContainerHighest": "#DFE4E8",
    "outline": "#70787D", "outlineVariant": "#767E83",
    "inverseSurface": "#2C3134", "inverseOnSurface": "#EDF1F4",
    "inversePrimary": "#96C4E5",
    "pageGap": "#E5EAEE",
    "pdfIcon": "#C62828", "pdfIconContainer": "#FDECEA",
    "tabIndicator": "#365A6C", "onTabIndicator": "#FFFFFF",
}

DARK = {
    "primary": "#96C4E5", "onPrimary": "#003450",
    "primaryContainer": "#004B70", "onPrimaryContainer": "#CBE6FF",
    "secondary": "#B6C9D8", "onSecondary": "#21333E",
    "secondaryContainer": "#384955", "onSecondaryContainer": "#D2E5F5",
    "tertiary": "#83D3EB", "onTertiary": "#003641",
    "tertiaryContainer": "#004E5D", "onTertiaryContainer": "#B2EBFF",
    "error": "#FFB4AB", "onError": "#690005",
    "errorContainer": "#93000A", "onErrorContainer": "#FFDAD6",
    "background": "#0F1417", "onBackground": "#DFE3E7",
    "surface": "#0F1417", "onSurface": "#DFE3E7",
    "surfaceVariant": "#40484D", "onSurfaceVariant": "#C0C8CD",
    "surfaceContainerLowest": "#0A0F12", "surfaceContainerLow": "#171C1F",
    "surfaceContainer": "#1B2124", "surfaceContainerHigh": "#262B2F",
    "surfaceContainerHighest": "#31363A",
    "outline": "#8A9297", "outlineVariant": "#848C91",
    "inverseSurface": "#DFE3E7", "inverseOnSurface": "#2C3134",
    "inversePrimary": "#006494",
    "pageGap": "#0A0F12",
    "pdfIcon": "#FF8A80", "pdfIconContainer": "#262B2F",
    "tabIndicator": "#5A7385", "onTabIndicator": "#FFFFFF",
}

PDF_PAGE = "#FFFFFF"  # nunca muda com o tema

ON_PAIRS = [
    ("onPrimary", "primary"), ("onSecondary", "secondary"),
    ("onTertiary", "tertiary"), ("onError", "error"),
    ("onBackground", "background"), ("onSurface", "surface"),
    ("onSurfaceVariant", "surfaceVariant"),
    ("onPrimaryContainer", "primaryContainer"),
    ("onSecondaryContainer", "secondaryContainer"),
    ("onTertiaryContainer", "tertiaryContainer"),
    ("onErrorContainer", "errorContainer"),
    ("inverseOnSurface", "inverseSurface"),
]
CONTAINERS = ["surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer",
              "surfaceContainerHigh", "surfaceContainerHighest"]
ON_CONTAINER = ["onSurface", "onSurfaceVariant", "primary", "error"]

# Texto principal: AAA (WCAG 1.4.6). onSurface sobre tudo que o app usa como fundo.
AAA_FOREGROUNDS = ["onSurface"]


# --- WCAG 2.1 ---------------------------------------------------------------

def _rgb(hex_color):
    h = hex_color.lstrip("#")
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def _channel(value):
    c = value / 255.0
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4


def luminance(hex_color):
    r, g, b = _rgb(hex_color)
    return 0.2126 * _channel(r) + 0.7152 * _channel(g) + 0.0722 * _channel(b)


def contrast(a, b):
    la, lb = luminance(a), luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)


def composite(fg, bg, alpha):
    """Sobrepõe fg com alpha sobre bg (equivale a Color.copy(alpha).compositeOver(bg))."""
    f, b = _rgb(fg), _rgb(bg)
    return "#%02X%02X%02X" % tuple(round(f[i] * alpha + b[i] * (1 - alpha)) for i in range(3))


# --- verificação ------------------------------------------------------------

def check(name, scheme):
    failures = []

    def assert_min(label, fg, bg, minimum):
        ratio = contrast(fg, bg)
        ok = ratio >= minimum
        if not ok:
            failures.append(f"{label}: {ratio:.2f}:1 (mínimo {minimum}:1)")
        print(f"  {ratio:6.2f}:1  {'ok  ' if ok else 'FALHA'}  {label}")

    print(f"===== {name} =====")
    print("  -- texto sobre o par 'on' correspondente (>= 4.5) --")
    for fg, bg in ON_PAIRS:
        assert_min(f"{fg}/{bg}", scheme[fg], scheme[bg], 4.5)

    print("  -- texto sobre surfaceContainer* (>= 4.5) --")
    for bg in CONTAINERS:
        for fg in ON_CONTAINER:
            assert_min(f"{fg}/{bg}", scheme[fg], scheme[bg], 4.5)

    print("  -- AAA: texto principal (>= 7.0) --")
    for bg in ["surface", "background"] + CONTAINERS:
        for fg in AAA_FOREGROUNDS:
            assert_min(f"AAA {fg}/{bg}", scheme[fg], scheme[bg], 7.0)
    assert_min("AAA onSurfaceVariant/surface",
               scheme["onSurfaceVariant"], scheme["surface"], 7.0)

    print("  -- indicador da aba selecionada (>= 3.0) e seu rótulo (>= 4.5) --")
    assert_min("tabIndicator/surfaceContainer",
               scheme["tabIndicator"], scheme["surfaceContainer"], 3.0)
    assert_min("onTabIndicator/tabIndicator",
               scheme["onTabIndicator"], scheme["tabIndicator"], 4.5)

    print("  -- ícone de PDF (>= 3.0) --")
    assert_min("pdfIcon/pdfIconContainer",
               scheme["pdfIcon"], scheme["pdfIconContainer"], 3.0)

    print("  -- divisores (>= 3.0) --")
    for bg in ["surface"] + CONTAINERS:
        assert_min(f"outlineVariant/{bg}", scheme["outlineVariant"], scheme[bg], 3.0)

    print("  -- desabilitado, alpha 0.60 sobre container 12% (>= 3.0) --")
    container = composite(scheme["onSurface"], scheme["surfaceContainer"], 0.12)
    label = composite(scheme["onSurface"], container, 0.60)
    assert_min("rótulo desabilitado/container", label, container, 3.0)

    print("  -- página do PDF (sempre branca) emoldurada pelo vão --")
    ratio = contrast(PDF_PAGE, scheme["pageGap"])
    print(f"  {ratio:6.2f}:1  info   página {PDF_PAGE} / vão {scheme['pageGap']}")

    if failures:
        print(f"\n  >>> {len(failures)} FALHA(S):")
        for f in failures:
            print(f"      - {f}")
    else:
        print("\n  >>> tudo dentro da regra")
    print()
    return failures


if __name__ == "__main__":
    import sys
    problems = check("CLARO", LIGHT) + check("ESCURO", DARK)
    sys.exit(1 if problems else 0)
