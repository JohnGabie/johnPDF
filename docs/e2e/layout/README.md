# Teste de layouts de PDF — INCOMPLETO (WIP)

Gerado por `tools/make_layout_pdfs.py` (reportlab + pillow) em `pdfs/`: boleto (código de barras + linha digitável), duas colunas, escaneado (imagem rasterizada), fatura, planilha em paisagem, tamanhos de página mistos, tipografia (tamanhos pequenos).

Prints em `img/` (emulador API 36): cada layout a 1× e com zoom, exceto `layout-tamanhos-mistos.pdf` (sem prints).

**Status:** o agente que rodava este teste foi interrompido antes de escrever o relatório. Ainda falta:
1. Julgar cada print (texto legível? código de barras nítido e decodificável com zoom?) e registrar PASS/FAIL aqui.
2. Rodar `layout-tamanhos-mistos.pdf` (A6/A4/A3/Letter no mesmo arquivo).
3. Opcional: decodificar os códigos de barras a partir dos prints com zoom (valores esperados estão fixos no topo do script).
