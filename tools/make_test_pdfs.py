#!/usr/bin/env python3
"""Gera os PDFs de teste em app/src/androidTest/assets/.

Uso:
  python3 -m venv .venv-tools
  .venv-tools/bin/pip install reportlab pypdf cryptography
  .venv-tools/bin/python tools/make_test_pdfs.py
"""
from pathlib import Path

from pypdf import PdfReader, PdfWriter
from reportlab.lib.pagesizes import A4, landscape
from reportlab.pdfgen import canvas

OUT = Path(__file__).resolve().parent.parent / "app/src/androidTest/assets"


def make(path: Path, pages: int, size=A4) -> None:
    c = canvas.Canvas(str(path), pagesize=size)
    w, h = size
    for n in range(1, pages + 1):
        c.setFillColorRGB(0, 0, 0)
        # Quadrado preto de 100pt a 40pt do canto superior esquerdo (usado nos testes de pixel).
        c.rect(40, h - 140, 100, 100, fill=1, stroke=0)
        c.setFont("Helvetica", 48)
        c.drawString(40, h / 2, f"Pagina {n}")
        c.showPage()
    c.save()


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    make(OUT / "normal.pdf", 3)
    make(OUT / "long.pdf", 200)
    make(OUT / "landscape.pdf", 1, landscape(A4))
    writer = PdfWriter(clone_from=PdfReader(OUT / "normal.pdf"))
    writer.encrypt(user_password="1234", owner_password="dono-1234", algorithm="AES-128")
    with open(OUT / "password.pdf", "wb") as f:
        writer.write(f)
    (OUT / "corrupted.pdf").write_bytes(b"%PDF-1.7\n" + bytes(range(256)) * 8 + b"\n%%EOF\n")
    print("\n".join(sorted(p.name for p in OUT.glob("*.pdf"))))


if __name__ == "__main__":
    main()
