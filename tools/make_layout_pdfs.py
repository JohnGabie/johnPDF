#!/usr/bin/env python3
"""Gera PDFs de layout variados para testar visibilidade, zoom e códigos de barra no leitor.

Saída: docs/e2e/layout/pdfs/
Uso:
  uv venv /tmp/venv-layout && uv pip install --python /tmp/venv-layout/bin/python reportlab pillow
  /tmp/venv-layout/bin/python tools/make_layout_pdfs.py
"""
import io
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from reportlab.graphics import renderPDF
from reportlab.graphics.barcode import code128, common, eanbc, qr
from reportlab.graphics.shapes import Drawing
from reportlab.lib import colors
from reportlab.lib.pagesizes import A3, A4, A6, letter, landscape
from reportlab.lib.units import mm
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen import canvas

OUT = Path(__file__).resolve().parent.parent / "docs/e2e/layout/pdfs"

# Valores fixos para que o teste confira a decodificação.
BOLETO_DIGITS = "23793381286000000011234567890123456789012345"[:44]
EAN13 = "7891000315507"
CODE128 = "JOHNPDF-2026-0929"
PIX_QR = "00020126360014BR.GOV.BCB.PIX0114+5511999999999520400005303986540510.005802BR5913JOHNPDF TESTE6009SAO PAULO62070503***6304ABCD"


def qr_drawing(value: str, size_pt: float) -> Drawing:
    w = qr.QrCodeWidget(value)
    x0, y0, x1, y1 = w.getBounds()
    d = Drawing(size_pt, size_pt, transform=[size_pt / (x1 - x0), 0, 0, size_pt / (y1 - y0), 0, 0])
    d.add(w)
    return d


def boleto(path: Path) -> None:
    c = canvas.Canvas(str(path), pagesize=A4)
    w, h = A4
    c.setFont("Helvetica-Bold", 14)
    c.drawString(15 * mm, h - 20 * mm, "Banco Teste S.A. | 237-2")
    c.setFont("Helvetica", 11)
    c.drawRightString(w - 15 * mm, h - 20 * mm, "23793.38128 60000.000011 23456.789012 3 45678901234567")
    # Tabela de campos com rótulos minúsculos (6pt) e valores (9pt), como num boleto real.
    fields = [
        ("Local de pagamento", "Pagável em qualquer banco até o vencimento"),
        ("Beneficiário", "johnPDF Testes LTDA — CNPJ 00.000.000/0001-00"),
        ("Data do documento", "29/09/2026"),
        ("Nosso número", "09/00000000123-4"),
        ("Vencimento", "10/10/2026"),
        ("Valor do documento", "R$ 1.234,56"),
    ]
    y = h - 30 * mm
    for label, value in fields:
        c.rect(15 * mm, y - 10 * mm, w - 30 * mm, 10 * mm)
        c.setFont("Helvetica", 6)
        c.drawString(17 * mm, y - 3 * mm, label)
        c.setFont("Helvetica", 9)
        c.drawString(17 * mm, y - 8 * mm, value)
        y -= 10 * mm
    c.setFont("Helvetica", 5)
    c.drawString(15 * mm, y - 5 * mm, "Instruções (texto de 5pt): não receber após o vencimento. Multa de 2% e juros de 1% ao mês. "
                 "Em caso de dúvidas, contate o beneficiário.")
    # Código de barras Intercalado 2 de 5 (padrão FEBRABAN: 44 dígitos, barra estreita ~0,33 mm, altura 13 mm).
    bc = common.I2of5(BOLETO_DIGITS, barWidth=0.33 * mm, ratio=3, barHeight=13 * mm, checksum=0, bearers=0, quiet=0)
    bc.drawOn(c, 15 * mm, y - 30 * mm)
    c.setFont("Helvetica", 6)
    c.drawString(15 * mm, y - 34 * mm, "Autenticação mecânica — Ficha de Compensação")
    c.showPage()
    c.save()


def fatura(path: Path) -> None:
    c = canvas.Canvas(str(path), pagesize=A4)
    w, h = A4
    c.setFont("Helvetica-Bold", 16)
    c.drawString(15 * mm, h - 20 * mm, "Fatura de Energia — Setembro/2026")
    c.setFont("Helvetica", 8)
    header = ["Item", "Descrição", "Qtd", "Unit. (R$)", "Total (R$)"]
    xs = [15, 30, 120, 140, 170]
    y = h - 35 * mm
    c.setFillColor(colors.HexColor("#DDE1FF"))
    c.rect(15 * mm, y - 2 * mm, w - 30 * mm, 7 * mm, fill=1, stroke=0)
    c.setFillColor(colors.black)
    for x, t in zip(xs, header):
        c.drawString(x * mm, y, t)
    for i in range(1, 26):
        y -= 6 * mm
        if i % 2 == 0:
            c.setFillColor(colors.HexColor("#F2F2F2"))
            c.rect(15 * mm, y - 2 * mm, w - 30 * mm, 6 * mm, fill=1, stroke=0)
            c.setFillColor(colors.black)
        row = [f"{i:02d}", f"Consumo faixa {i} (kWh) — tarifa TUSD/TE", f"{i * 3}", "0,8123", f"{i * 3 * 0.8123:.2f}".replace(".", ",")]
        for x, t in zip(xs, row):
            c.drawString(x * mm, y, t)
    c.setFont("Helvetica-Bold", 11)
    c.drawRightString(w - 15 * mm, y - 10 * mm, "Total a pagar: R$ 263,19")
    # PIX (QR 30 mm) + QR pequeno (12 mm) + Code128 + EAN-13.
    renderPDF.draw(qr_drawing(PIX_QR, 30 * mm), c, 15 * mm, 25 * mm)
    c.setFont("Helvetica", 7)
    c.drawString(15 * mm, 21 * mm, "Pague com PIX (QR 30 mm)")
    renderPDF.draw(qr_drawing(CODE128, 12 * mm), c, 55 * mm, 25 * mm)
    c.drawString(55 * mm, 21 * mm, "QR 12 mm")
    code128.Code128(CODE128, barWidth=0.25 * mm, barHeight=10 * mm).drawOn(c, 80 * mm, 30 * mm)
    c.drawString(80 * mm, 26 * mm, "Code128 (barra 0,25 mm)")
    d = Drawing(40 * mm, 25 * mm)
    d.add(eanbc.Ean13BarcodeWidget(EAN13, barHeight=15 * mm))
    renderPDF.draw(d, c, 150 * mm, 22 * mm)
    c.setFont("Helvetica", 4)
    c.drawString(15 * mm, 12 * mm, "Rodapé em 4pt: ANEEL — Resolução Normativa nº 1000/2021. Tributos: ICMS 18%, PIS 1,65%, COFINS 7,6%.")
    c.showPage()
    c.save()


def tipografia(path: Path) -> None:
    c = canvas.Canvas(str(path), pagesize=A4)
    w, h = A4
    y = h - 20 * mm
    c.setFont("Helvetica-Bold", 14)
    c.drawString(15 * mm, y, "Escada de tamanhos de fonte")
    y -= 12 * mm
    for size in (4, 5, 6, 7, 8, 9, 10, 12, 14, 18, 24):
        c.setFont("Helvetica", size)
        c.drawString(15 * mm, y, f"{size}pt — Ação às 09:45: R$ 1.234,56 — 0O 1lI 5S 8B")
        y -= size * 1.6 + 3 * mm
    c.setFont("Times-Roman", 6)
    c.drawString(15 * mm, y, "Serifada 6pt: Contrato nº 123/2026, cláusula 4ª, parágrafo único.")
    y -= 10 * mm
    for lw in (0.1, 0.25, 0.5, 1.0):
        c.setLineWidth(lw)
        c.line(15 * mm, y, 120 * mm, y)
        c.setFont("Helvetica", 7)
        c.drawString(125 * mm, y - 1, f"linha {lw}pt")
        y -= 6 * mm
    # Cinza claro sobre branco (contraste baixo).
    c.setFillColor(colors.HexColor("#BBBBBB"))
    c.setFont("Helvetica", 8)
    c.drawString(15 * mm, y - 4 * mm, "Texto cinza-claro #BBBBBB em 8pt (contraste baixo)")
    c.showPage()
    c.save()


def duas_colunas(path: Path) -> None:
    c = canvas.Canvas(str(path), pagesize=A4)
    w, h = A4
    text = ("A leitura em duas colunas é comum em artigos, bulas de remédio e contratos. "
            "O leitor precisa permitir zoom e deslocamento horizontal para acompanhar cada coluna. ") * 6
    col_w = (w - 40 * mm) / 2
    c.setFont("Times-Bold", 16)
    c.drawString(15 * mm, h - 20 * mm, "Bula — Paracetamol 750 mg")
    for col in range(2):
        t = c.beginText(15 * mm + col * (col_w + 10 * mm), h - 32 * mm)
        t.setFont("Times-Roman", 7.5)
        words, line = text.split(), ""
        for word in words:
            if c.stringWidth(line + " " + word, "Times-Roman", 7.5) > col_w:
                t.textLine(line)
                line = word
            else:
                line = (line + " " + word).strip()
        t.textLine(line)
        c.drawText(t)
    c.showPage()
    c.save()


def planilha_paisagem(path: Path) -> None:
    size = landscape(A4)
    c = canvas.Canvas(str(path), pagesize=size)
    w, h = size
    c.setFont("Helvetica-Bold", 12)
    c.drawString(10 * mm, h - 15 * mm, "Extrato anual — planilha larga (paisagem, 7pt)")
    cols = ["Mês"] + [f"Conta {i}" for i in range(1, 12)]
    colw = (w - 20 * mm) / len(cols)
    c.setFont("Helvetica", 7)
    y = h - 25 * mm
    for i, name in enumerate(cols):
        c.drawString(10 * mm + i * colw, y, name)
    for m, mes in enumerate(["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"]):
        y -= 5 * mm
        c.drawString(10 * mm, y, mes)
        for i in range(1, 12):
            c.drawRightString(10 * mm + (i + 1) * colw - 2 * mm, y, f"{(m + 1) * i * 37.5:,.2f}".replace(",", "X").replace(".", ",").replace("X", "."))
    c.setLineWidth(0.25)
    for i in range(len(cols) + 1):
        c.line(10 * mm + i * colw, h - 27 * mm, 10 * mm + i * colw, y - 2 * mm)
    c.showPage()
    c.save()


def tamanhos_mistos(path: Path) -> None:
    c = canvas.Canvas(str(path))
    for name, size in [("A4 retrato", A4), ("A6 (etiqueta)", A6), ("A3 paisagem", landscape(A3)), ("Carta (US Letter)", letter)]:
        c.setPageSize(size)
        w, h = size
        c.setStrokeColor(colors.red)
        c.setLineWidth(2)
        c.rect(5, 5, w - 10, h - 10)
        c.setFont("Helvetica-Bold", 18)
        c.drawCentredString(w / 2, h / 2, name)
        c.setFont("Helvetica", 7)
        c.drawString(12, 12, f"canto inferior esquerdo — {w:.0f}x{h:.0f}pt")
        c.drawRightString(w - 12, h - 20, "canto superior direito")
        c.showPage()
    c.save()


def escaneado(path: Path) -> None:
    """Documento 'escaneado': imagem raster a 150 dpi com texto pequeno e um QR."""
    dpi = 150
    wpx, hpx = int(210 / 25.4 * dpi), int(297 / 25.4 * dpi)
    img = Image.new("L", (wpx, hpx), 245)
    dr = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("DejaVuSans.ttf", 18)
        small = ImageFont.truetype("DejaVuSans.ttf", 12)
    except OSError:
        font = small = ImageFont.load_default()
    dr.text((80, 80), "RECIBO DIGITALIZADO (150 dpi)", fill=20, font=font)
    for i in range(30):
        dr.text((80, 140 + i * 22), f"Linha {i + 1:02d}: pagamento referente ao serviço nº {1000 + i}, valor R$ {i * 12.5:.2f}", fill=40, font=small)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    c = canvas.Canvas(str(path), pagesize=A4)
    w, h = A4
    c.drawImage(ImageReader(io.BytesIO(buf.getvalue())), 0, 0, w, h)
    renderPDF.draw(qr_drawing(PIX_QR, 25 * mm), c, w - 40 * mm, 15 * mm)
    c.showPage()
    c.save()


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for fn, name in [(boleto, "boleto"), (fatura, "fatura"), (tipografia, "tipografia"), (duas_colunas, "duas-colunas"),
                     (planilha_paisagem, "planilha-paisagem"), (tamanhos_mistos, "tamanhos-mistos"), (escaneado, "escaneado")]:
        fn(OUT / f"layout-{name}.pdf")
    print("\n".join(sorted(p.name for p in OUT.glob("*.pdf"))))


if __name__ == "__main__":
    main()
