package com.johngabie.johnpdf.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Review Focus 5: um `d` sintaticamente válido mas com coordenadas erradas
 * compila, passa nos testes de metadado e desenha cortado ou minúsculo.
 * Aqui os paths são reconstruídos e medidos.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JohnIconsGeometryTest {

    private fun paths(no: VectorNode): List<VectorPath> = when (no) {
        is VectorPath -> listOf(no)
        is VectorGroup -> no.flatMap { paths(it) }
    }

    private fun paths(icone: ImageVector): List<VectorPath> = paths(icone.root)

    @Test fun nenhum_icone_esta_vazio() {
        JohnIcons.All.forEach { icone ->
            val encontrados = paths(icone)
            assertTrue("${icone.name}: nenhum VectorPath na árvore", encontrados.isNotEmpty())
            encontrados.forEach { p ->
                assertTrue("${icone.name}: pathData vazio", p.pathData.isNotEmpty())
            }
        }
    }

    /**
     * O viewBox dos Material Symbols é `0 -960 960 960`: X de 0 a 960,
     * Y de -960 a 0. Tolerância de 2f para arredondamento do parser.
     */
    @Test fun geometria_de_todo_icone_cabe_no_viewbox() {
        val tol = 2f
        JohnIcons.All.forEach { icone ->
            paths(icone).forEach { p ->
                val caminho = PathParser().addPathNodes(p.pathData).toPath()
                val b = caminho.getBounds()
                val onde = "${icone.name} bounds=$b"
                assertTrue("$onde: largura zero", b.width > 0f)
                assertTrue("$onde: altura zero", b.height > 0f)
                assertTrue("$onde: x mínimo fora do viewBox", b.left >= -tol)
                assertTrue("$onde: x máximo fora do viewBox", b.right <= 960f + tol)
                assertTrue("$onde: y mínimo fora do viewBox", b.top >= -960f - tol)
                assertTrue("$onde: y máximo fora do viewBox", b.bottom <= tol)
            }
        }
    }

    /** Um símbolo de 24px ocupa a maior parte do quadro; 1/4 do lado é o piso. */
    @Test fun todo_icone_ocupa_uma_area_plausivel() {
        JohnIcons.All.forEach { icone ->
            val b = PathParser().addPathNodes(paths(icone).first().pathData).toPath().getBounds()
            assertTrue(
                "${icone.name}: desenho minúsculo (${b.width}x${b.height} em 960x960) — " +
                    "suspeita de `d` truncado",
                b.width >= 240f || b.height >= 240f,
            )
        }
    }
}
