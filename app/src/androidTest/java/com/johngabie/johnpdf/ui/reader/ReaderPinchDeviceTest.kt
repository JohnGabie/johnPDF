package com.johngabie.johnpdf.ui.reader

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pinça no aparelho de verdade. Ao contrário do teste JVM, aqui os eventos são `MotionEvent`
 * injetados por `sendPointerSync`: atravessam o `InputDispatcher` do sistema e chegam na janela
 * real, com densidade real, gráficos reais e o batching de `ACTION_MOVE` do Android — que é
 * exatamente o que o Robolectric não reproduz.
 */
@RunWith(AndroidJUnit4::class)
class ReaderPinchDeviceTest {
    @get:Rule val rule = createComposeRule()

    private val applied = mutableListOf<Float>()
    private var origin = intArrayOf(0, 0)
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    /** Página com textura: escalar um bitmap liso não muda pixel nenhum e mascararia o defeito. */
    private fun grid(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        c.drawColor(android.graphics.Color.WHITE)
        val p = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = maxOf(1f, w / 200f)
        }
        for (i in 0..20) {
            c.drawLine(w * i / 20f, 0f, w * i / 20f, h.toFloat(), p)
            c.drawLine(0f, h * i / 20f, w.toFloat(), h * i / 20f, p)
        }
        return bmp
    }

    private fun show(initialZoom: Float = 1f) {
        rule.setContent {
            var state by remember {
                mutableStateOf(
                    ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { PageSize(595f, 842f) }), zoom = initialZoom),
                )
            }
            val view = LocalView.current
            remember(view) { view.getLocationOnScreen(origin); 0 }
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
                    renderPage = { _, w ->
                        val px = maxOf(1, minOf(w, 2048))
                        grid(px, px * 842 / 595)
                    },
                )
            }
        }
        rule.waitForIdle()
    }

    private fun event(down: Long, action: Int, pts: List<Pair<Float, Float>>): MotionEvent {
        val props = Array(pts.size) { i ->
            MotionEvent.PointerProperties().apply { id = i; toolType = MotionEvent.TOOL_TYPE_FINGER }
        }
        val coords = Array(pts.size) { i ->
            MotionEvent.PointerCoords().apply {
                x = pts[i].first + origin[0]
                y = pts[i].second + origin[1]
                pressure = 1f
                size = 1f
            }
        }
        return MotionEvent.obtain(
            down, SystemClock.uptimeMillis(), action, pts.size, props, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
    }

    private fun inject(e: MotionEvent) {
        instrumentation.sendPointerSync(e)
        e.recycle()
    }

    /** Pinça horizontal em torno de ([cx], [cy]) do meio-afastamento [from] até [to], em pixels. */
    private fun pinch(cx: Float, cy: Float, from: Float, to: Float, steps: Int = 24) {
        val t0 = SystemClock.uptimeMillis()
        fun pair(d: Float) = listOf(cx - d to cy, cx + d to cy)
        inject(event(t0, MotionEvent.ACTION_DOWN, listOf(cx - from to cy)))
        inject(
            event(
                t0,
                MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                pair(from),
            ),
        )
        for (i in 1..steps) {
            val d = from + (to - from) * i / steps
            inject(event(t0, MotionEvent.ACTION_MOVE, pair(d)))
            SystemClock.sleep(8)
        }
        inject(
            event(
                t0,
                MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                pair(to),
            ),
        )
        inject(event(t0, MotionEvent.ACTION_UP, listOf(cx - to to cy)))
        rule.waitForIdle()
    }

    private fun centerOfContent(): Pair<Float, Float> {
        val n = rule.onRoot().fetchSemanticsNode().size
        return n.width / 2f to n.height / 2f
    }


    @Test fun pinca_real_ao_dobro_aplica_2x() {
        show()
        val (cx, cy) = centerOfContent()
        pinch(cx, cy, from = 120f, to = 240f)
        assertTrue("nenhum zoom aplicado — a pinça não chegou no detector", applied.isNotEmpty())
        assertEquals(2f, applied.last(), 0.08f)
    }

    @Test fun pinca_real_ao_triplo_aplica_3x() {
        show()
        val (cx, cy) = centerOfContent()
        pinch(cx, cy, from = 100f, to = 300f)
        assertEquals(3f, applied.last(), 0.12f)
    }

    @Test fun pinca_real_de_fechar_reduz_o_zoom() {
        show(initialZoom = 4f)
        val (cx, cy) = centerOfContent()
        pinch(cx, cy, from = 240f, to = 120f)
        assertEquals(2f, applied.last(), 0.08f)
    }

    /**
     * Gesto em poucos saltos grandes, com uma pausa em cada um: o sistema coalesce os
     * `ACTION_MOVE` de outro jeito, e a âncora tem que continuar dando a razão real no fim.
     */
    @Test fun pinca_real_em_passos_grandes_mantem_a_proporcao() {
        show()
        val (cx, cy) = centerOfContent()
        val t0 = SystemClock.uptimeMillis()
        fun pair(d: Float) = listOf(cx - d to cy, cx + d to cy)
        inject(event(t0, MotionEvent.ACTION_DOWN, listOf(cx - 100f to cy)))
        inject(event(t0, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), pair(100f)))
        for (d in listOf(150f, 200f, 250f, 300f)) {
            inject(event(t0, MotionEvent.ACTION_MOVE, pair(d)))
            SystemClock.sleep(16)
            rule.waitForIdle()
        }
        inject(event(t0, MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), pair(300f)))
        inject(event(t0, MotionEvent.ACTION_UP, listOf(cx - 300f to cy)))
        rule.waitForIdle()
        assertEquals(3f, applied.last(), 0.12f)
    }
}
