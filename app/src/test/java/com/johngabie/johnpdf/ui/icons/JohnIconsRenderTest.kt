package com.johngabie.johnpdf.ui.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
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
 * Review Focus 1 e 2. Um SVG baixado errado (404 salvo como HTML, `d`
 * truncado) produz um ImageVector que compila e não desenha nada; e se as
 * duas variantes da bottom nav vierem do mesmo arquivo, selecionado e não
 * selecionado ficam idênticos. Só medir pixel pega os dois casos.
 *
 * **Por que não `Icon(...)` + `captureToImage()`:** a captura do Compose passa
 * por `forceRedraw`, que espera um ciclo de desenho real da janela; sob
 * Robolectric o looper pausado nunca entrega esse ciclo e a chamada estoura
 * `ComposeTimeoutException: Condition still not satisfied after 2000 ms`.
 * Em vez disso, o mesmo `ImageVector` que a tela consumiria é rasterizado
 * aqui, honrando os transforms de [VectorGroup] — inclusive o
 * `translationY = 960f` que traz o viewBox `0 -960 960 960` para o quadrante
 * positivo. Um `d` truncado, um SVG vazio ou uma translação errada jogam o
 * desenho para fora do bitmap e o teste conta zero.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JohnIconsRenderTest {

    private val lado = 96

    private fun desenhar(no: VectorNode, canvas: Canvas, paint: Paint) {
        when (no) {
            is VectorPath -> {
                val caminho = PathParser().addPathNodes(no.pathData).toPath().asAndroidPath()
                caminho.fillType = if (no.pathFillType == PathFillType.EvenOdd) {
                    android.graphics.Path.FillType.EVEN_ODD
                } else {
                    android.graphics.Path.FillType.WINDING
                }
                canvas.drawPath(caminho, paint)
            }

            is VectorGroup -> {
                canvas.save()
                canvas.translate(no.translationX, no.translationY)
                canvas.rotate(no.rotation, no.pivotX, no.pivotY)
                canvas.scale(no.scaleX, no.scaleY, no.pivotX, no.pivotY)
                no.forEach { desenhar(it, canvas, paint) }
                canvas.restore()
            }
        }
    }

    /** Rasteriza o ícone inteiro no quadro do viewport e conta os pixels opacos. */
    private fun pixelsOpacos(icone: ImageVector): Int {
        val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = false
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.scale(lado / icone.viewportWidth, lado / icone.viewportHeight)
        desenhar(icone.root, canvas, paint)

        val pixels = IntArray(lado * lado)
        bitmap.getPixels(pixels, 0, lado, 0, 0, lado, lado)
        return pixels.count { android.graphics.Color.alpha(it) > 127 }
    }

    @Test fun todo_icone_pinta_pixels() {
        JohnIcons.All.forEach { icone ->
            val opacos = pixelsOpacos(icone)
            assertTrue(
                "${icone.name} não desenhou nada em ${lado}x$lado — SVG vazio, 404, " +
                    "`d` truncado ou translação do viewBox errada",
                opacos > 0,
            )
        }
    }

    /**
     * `schedule` no lugar de `history`: upstream publica
     * `history_fill1_24px.svg` byte a byte idêntico ao contorno, então esse
     * par nunca poderia passar aqui. Ver tools/icons/icons.txt.
     */
    @Test fun variante_filled_pinta_mais_que_o_contorno() {
        val paresParaComparar = listOf(
            Triple("schedule", JohnIcons.Schedule, JohnIcons.ScheduleFilled),
            Triple("library_books", JohnIcons.LibraryBooks, JohnIcons.LibraryBooksFilled),
        )
        paresParaComparar.forEach { (nome, contorno, preenchido) ->
            val a = pixelsOpacos(contorno)
            val b = pixelsOpacos(preenchido)
            assertTrue(
                "$nome: fill1 pintou $b pixels e o contorno $a — as duas variantes " +
                    "provavelmente vieram do mesmo SVG",
                b > a,
            )
        }
    }
}
