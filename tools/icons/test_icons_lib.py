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
            ["HistoryFilled", "LibraryBooksFilled"],
            sorted(i.kotlin for i in icons if i.fill),
        )
        self.assertEqual(20, len({i.upstream for i in icons}), "20 nomes upstream distintos")


if __name__ == "__main__":
    unittest.main()
