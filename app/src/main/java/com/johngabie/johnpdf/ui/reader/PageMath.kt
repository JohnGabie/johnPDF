package com.johngabie.johnpdf.ui.reader

data class VisiblePage(val index: Int, val offset: Int, val size: Int)

/** Índice da página com a maior área visível; empate fica com a de cima. */
fun dominantPage(pages: List<VisiblePage>, viewportStart: Int, viewportEnd: Int): Int? =
    pages.maxByOrNull { p ->
        (minOf(p.offset + p.size, viewportEnd) - maxOf(p.offset, viewportStart)).coerceAtLeast(0)
    }?.index

fun pageLabel(current: Int, total: Int): String = "Página ${current + 1} de $total"
