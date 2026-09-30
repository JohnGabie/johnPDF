package com.johngabie.johnpdf.ui.reader

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A pinça tem que ser proporcional: afastar os dedos até o dobro da distância inicial dá 2x,
 * até o triplo dá 3x. Antes a escala era o produto das razões quadro a quadro de
 * `calculateZoom()`, que perde proporção assim que o conjunto de dedos muda no meio do gesto.
 */
@RunWith(AndroidJUnit4::class)
class ReaderPinchTest {
    @get:Rule val rule = createComposeRule()

    private val applied = mutableListOf<Float>()

    private fun show(initialZoom: Float = 1f) {
        rule.setContent {
            var state by remember {
                mutableStateOf(
                    ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { PageSize(595f, 842f) }), zoom = initialZoom),
                )
            }
            JohnPdfTheme {
                ReaderContent(
                    state = state,
                    onBack = {},
                    onPageVisible = {},
                    onZoomChange = {
                        applied += it
                        state = state.copy(zoom = it.coerceIn(ReaderViewModel.MIN_ZOOM, ReaderViewModel.MAX_ZOOM))
                    },
                    onDoubleTap = {},
                    onToggleRotation = {},
                    onSubmitPassword = {},
                    renderPage = { _, w -> Bitmap.createBitmap(maxOf(1, w), maxOf(1, w), Bitmap.Config.ARGB_8888) },
                )
            }
        }
    }

    private fun center(): Offset =
        rule.onRoot().fetchSemanticsNode().size.let { Offset(it.width / 2f, it.height / 2f) }

    /** Pinça horizontal do meio-afastamento [from] até [to] (em px), em passos de ~5px. */
    private fun pinch(from: Float, to: Float, extra: (TouchInjectionScope.() -> Unit)? = null) {
        val c = center()
        rule.onRoot().performTouchInput {
            down(0, Offset(c.x - from, c.y))
            down(1, Offset(c.x + from, c.y))
            val steps = 20
            for (i in 1..steps) {
                val d = from + (to - from) * i / steps
                updatePointerTo(0, Offset(c.x - d, c.y))
                updatePointerTo(1, Offset(c.x + d, c.y))
                move()
            }
            extra?.invoke(this)
            up(0)
            up(1)
        }
        rule.waitForIdle()
    }

    @Test fun afastar_ao_dobro_aplica_zoom_2x() {
        show()
        pinch(from = 25f, to = 50f)
        assertEquals(2f, applied.last(), 0.02f)
    }

    @Test fun afastar_ao_triplo_aplica_zoom_3x() {
        show()
        pinch(from = 25f, to = 75f)
        assertEquals(3f, applied.last(), 0.02f)
    }

    /** Aproximar os dedos à metade tem que reduzir o zoom na mesma proporção. */
    @Test fun aproximar_a_metade_reduz_o_zoom_pela_metade() {
        show(initialZoom = 4f)
        pinch(from = 80f, to = 40f)
        assertEquals(2f, applied.last(), 0.02f)
    }

    /** Dois gestos seguidos compõem: 2x depois 2x = 4x. */
    @Test fun pincas_seguidas_compoem_o_zoom() {
        show()
        pinch(from = 25f, to = 50f)
        pinch(from = 25f, to = 50f)
        assertEquals(4f, applied.last(), 0.05f)
    }

    /**
     * Regressão: o 2º dedo raramente encosta junto com o 1º — normalmente a lista já rolou um
     * pouco antes. A âncora só pode ser medida quando os dois dedos já estão na tela.
     */
    @Test fun pinca_depois_de_arrastar_um_dedo_continua_proporcional() {
        show()
        val c = center()
        rule.onRoot().performTouchInput {
            down(0, Offset(c.x - 30f, c.y))
            repeat(4) { updatePointerTo(0, Offset(c.x - 30f, c.y)); move() }
            down(1, Offset(c.x + 30f, c.y))
            for (i in 1..20) {
                val d = 30f + 30f * i / 20f
                updatePointerTo(0, Offset(c.x - d, c.y))
                updatePointerTo(1, Offset(c.x + d, c.y))
                move()
            }
            up(0)
            up(1)
        }
        rule.waitForIdle()
        assertEquals(2f, applied.last(), 0.05f)
    }

    /**
     * Regressão do acúmulo multiplicativo: abrir muito além do teto e voltar tem que terminar
     * na razão real entre a distância final e a inicial dos dedos, não num valor represado.
     */
    @Test fun abrir_alem_do_maximo_e_voltar_termina_na_proporcao_real() {
        show()
        val c = center()
        rule.onRoot().performTouchInput {
            down(0, Offset(c.x - 20f, c.y))
            down(1, Offset(c.x + 20f, c.y))
            for (d in 25..120 step 5) { updatePointerTo(0, Offset(c.x - d, c.y)); updatePointerTo(1, Offset(c.x + d, c.y)); move() }
            for (d in 115 downTo 40 step 5) { updatePointerTo(0, Offset(c.x - d, c.y)); updatePointerTo(1, Offset(c.x + d, c.y)); move() }
            up(0)
            up(1)
        }
        rule.waitForIdle()
        assertEquals(2f, applied.last(), 0.02f)
    }

    /** Um toque simples nunca pode virar zoom. */
    @Test fun toque_de_um_dedo_nao_aplica_zoom() {
        show()
        val c = center()
        rule.onRoot().performTouchInput {
            down(0, c)
            repeat(5) { i -> updatePointerTo(0, Offset(c.x, c.y - i * 6f)); move() }
            up(0)
        }
        rule.waitForIdle()
        assertTrue("um dedo não pode aplicar zoom, veio $applied", applied.isEmpty())
    }

    /** O zoom aplicado tem que virar largura de página de verdade, não só escala de preview. */
    @Test fun o_zoom_aplicado_alarga_a_pagina_na_mesma_proporcao() {
        show()
        val antes = rule.onNodeWithContentDescription("Página 1").getUnclippedBoundsInRoot().width
        pinch(from = 25f, to = 50f)
        val depois = rule.onNodeWithContentDescription("Página 1").getUnclippedBoundsInRoot().width
        assertEquals(2f, depois / antes, 0.02f)
    }
}
