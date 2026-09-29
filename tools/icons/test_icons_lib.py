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


if __name__ == "__main__":
    unittest.main()
