package com.johngabie.johnpdf.engine

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MuPdfEngineTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val engine = MuPdfEngine()

    private fun asset(name: String): File =
        File(instrumentation.targetContext.cacheDir, name).also { f ->
            instrumentation.context.assets.open(name).use { i -> f.outputStream().use { i.copyTo(it) } }
        }

    @After fun tearDown() = engine.close()

    @Test fun opens_normal_pdf() = runBlocking {
        assertEquals(OpenResult.Success(3), engine.open(asset("normal.pdf")))
    }

    @Test fun page_sizes_are_a4_portrait() = runBlocking {
        engine.open(asset("normal.pdf"))
        val sizes = engine.pageSizes()
        assertEquals(3, sizes.size)
        assertEquals(595.3f, sizes[0].width, 1f)
        assertEquals(841.9f, sizes[0].height, 1f)
    }

    @Test fun landscape_page_is_wider_than_tall() = runBlocking {
        engine.open(asset("landscape.pdf"))
        val size = engine.pageSizes().single()
        assertTrue(size.width > size.height)
    }

    @Test fun long_pdf_has_200_pages() = runBlocking {
        assertEquals(OpenResult.Success(200), engine.open(asset("long.pdf")))
    }

    @Test fun render_has_requested_width_and_page_aspect() = runBlocking {
        engine.open(asset("normal.pdf"))
        val bmp = engine.render(0, 600)
        assertEquals(600, bmp.width)
        assertEquals(849f, bmp.height.toFloat(), 2f)
    }

    @Test fun render_draws_content_on_white_background() = runBlocking {
        engine.open(asset("normal.pdf"))
        val bmp = engine.render(0, 600)
        assertEquals(Color.WHITE, bmp.getPixel(bmp.width - 3, bmp.height - 3))
        val inSquare = bmp.getPixel(91, 91)
        assertTrue("esperava preto, veio ${Integer.toHexString(inSquare)}", Color.red(inSquare) < 60)
    }

    @Test fun render_caps_width_at_max() = runBlocking {
        engine.open(asset("normal.pdf"))
        assertEquals(MAX_RENDER_WIDTH_PX, engine.render(0, 5000).width)
    }

    @Test fun password_flow() = runBlocking {
        val file = asset("password.pdf")
        assertEquals(OpenResult.NeedsPassword, engine.open(file))
        assertEquals(OpenResult.WrongPassword, engine.open(file, "0000"))
        assertEquals(OpenResult.Success(3), engine.open(file, "1234"))
        assertEquals(600, engine.render(0, 600).width)
    }

    @Test fun corrupted_file_returns_corrupted() = runBlocking {
        assertEquals(OpenResult.Corrupted, engine.open(asset("corrupted.pdf")))
    }

    @Test fun missing_file_returns_corrupted() = runBlocking {
        assertEquals(OpenResult.Corrupted, engine.open(File("/nao/existe.pdf")))
    }

    @Test fun concurrent_renders_do_not_crash() = runBlocking {
        engine.open(asset("long.pdf"))
        val results = (0 until 30).map { i -> async(Dispatchers.Default) { engine.render(i, 400).width } }.awaitAll()
        assertTrue(results.all { it == 400 })
    }

    @Test fun close_while_rendering_does_not_crash() = runBlocking {
        engine.open(asset("long.pdf"))
        val jobs = (0 until 10).map { i -> launch(Dispatchers.Default) { runCatching { engine.render(i, 800) } } }
        engine.close()
        jobs.forEach { it.join() }
    }

    @Test fun calls_after_close_are_cancelled_and_never_touch_document() = runBlocking {
        val file = asset("normal.pdf")
        engine.open(file)
        engine.close()
        assertThrows(CancellationException::class.java) { runBlocking { engine.render(0, 400) } }
        assertThrows(CancellationException::class.java) { runBlocking { engine.pageSizes() } }
        assertThrows(CancellationException::class.java) { runBlocking { engine.open(file) } }
        Unit
    }
}
