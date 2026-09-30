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
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Gera docs/e2e/img/icons-grid.png para conferência a olho contra
 * https://fonts.google.com/icons (filtro: Rounded).
 *
 * Só roda com JOHNPDF_CAPTURE_ICONS=1 — escreve arquivo no repositório.
 *
 *   PowerShell:  $env:JOHNPDF_CAPTURE_ICONS="1"; .\gradlew.bat :app:testDebugUnitTest --tests "*JohnIconsGridCaptureTest"
 *   bash:        JOHNPDF_CAPTURE_ICONS=1 ./gradlew :app:testDebugUnitTest --tests '*JohnIconsGridCaptureTest'
 *
 * Rasteriza o `ImageVector` direto no `Canvas`, pelo mesmo motivo de
 * [JohnIconsRenderTest]: `captureToImage()` depende de `forceRedraw`, que o
 * looper pausado do Robolectric nunca entrega.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JohnIconsGridCaptureTest {

    private val colunas = 4
    private val iconePx = 72
    private val celulaW = 180
    private val celulaH = 116
    private val margem = 20

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

    private fun desenharIcone(icone: ImageVector, canvas: Canvas, x: Int, y: Int, paint: Paint) {
        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        canvas.scale(iconePx / icone.viewportWidth, iconePx / icone.viewportHeight)
        desenhar(icone.root, canvas, paint)
        canvas.restore()
    }

    @Test fun gera_a_grade_de_conferencia_visual() {
        assumeTrue(
            "pulado: defina JOHNPDF_CAPTURE_ICONS=1 para regerar a grade",
            System.getenv("JOHNPDF_CAPTURE_ICONS") == "1",
        )

        val icones = JohnIcons.All
        val linhas = (icones.size + colunas - 1) / colunas
        val largura = colunas * celulaW + 2 * margem
        val altura = linhas * celulaH + 2 * margem

        val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        val canvas = Canvas(bitmap)

        val tinta = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        val texto = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.DKGRAY
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }

        icones.forEachIndexed { i, icone ->
            val col = i % colunas
            val lin = i / colunas
            val cx = margem + col * celulaW + celulaW / 2
            val topo = margem + lin * celulaH
            desenharIcone(icone, canvas, cx - iconePx / 2, topo + 8, tinta)
            canvas.drawText(
                icone.name.removePrefix("JohnIcons."),
                cx.toFloat(),
                (topo + 8 + iconePx + 24).toFloat(),
                texto,
            )
        }

        // Os testes unitários rodam com working dir = app/.
        val destino = File("../docs/e2e/img/icons-grid.png").canonicalFile
        destino.parentFile.mkdirs()
        destino.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("grade de ícones salva em $destino (${bitmap.width}x${bitmap.height})")
    }
}
