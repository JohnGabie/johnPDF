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
            ["LibraryBooksFilled", "ScheduleFilled"],
            sorted(i.kotlin for i in icons if i.fill),
        )
        self.assertEqual(20, len({i.upstream for i in icons}), "20 nomes upstream distintos")


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
        # `history` foi trocado por `schedule`: upstream publica
        # history_fill1_24px.svg byte a byte igual ao contorno, então o estado
        # selecionado da bottom nav ficaria idêntico ao não selecionado.
        by_name = {i.kotlin: i for i in icons_lib.load_icons()}
        self.assertNotEqual(by_name["Schedule"].d, by_name["ScheduleFilled"].d)
        self.assertNotEqual(by_name["LibraryBooks"].d, by_name["LibraryBooksFilled"].d)


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
        # O bloco emitido é `get() = listOf(...)`; cortar no primeiro ")" pegaria
        # o "()" de `get()`. Recortar a partir de "listOf(" é o que isola a lista.
        bloco = (
            kt.split("val All: List<ImageVector>", 1)[1]
            .split("listOf(", 1)[1]
            .split(")", 1)[0]
        )
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


if __name__ == "__main__":
    unittest.main()
