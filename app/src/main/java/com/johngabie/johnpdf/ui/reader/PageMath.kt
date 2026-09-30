package com.johngabie.johnpdf.ui.reader

import androidx.compose.ui.geometry.Offset

data class VisiblePage(val index: Int, val offset: Int, val size: Int)

/** Centro geométrico dos dedos encostados na tela. Lista vazia = [Offset.Zero]. */
fun centroidOf(points: List<Offset>): Offset {
    if (points.isEmpty()) return Offset.Zero
    var sum = Offset.Zero
    points.forEach { sum += it }
    return sum / points.size.toFloat()
}

/**
 * Abertura da pinça: distância média dos dedos ao centro deles. Com dois dedos é exatamente
 * metade da distância entre eles — como o zoom só usa a razão entre duas aberturas, a constante
 * some; com três ou mais dedos a média generaliza sem salto.
 */
fun fingerSpread(points: List<Offset>): Float {
    if (points.size < 2) return 0f
    val centroid = centroidOf(points)
    return points.sumOf { (it - centroid).getDistance().toDouble() }.toFloat() / points.size
}

/** Índice da página com a maior área visível; empate fica com a de cima. */
fun dominantPage(pages: List<VisiblePage>, viewportStart: Int, viewportEnd: Int): Int? =
    pages.maxByOrNull { p ->
        (minOf(p.offset + p.size, viewportEnd) - maxOf(p.offset, viewportStart)).coerceAtLeast(0)
    }?.index

// The page label itself is now a string resource (R.string.page_of_total), formatted at the
// call site: "Página 3 de 12" and "Page 3 of 12" do not share a word order to build by hand.
