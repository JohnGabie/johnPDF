package com.johngabie.johnpdf.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.util.LruCache
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/** Único ponto de contato com o MuPDF. Todas as chamadas rodam numa só thread (MuPDF não é thread-safe). */
class MuPdfEngine(private val maxRenderWidthPx: Int = MAX_RENDER_WIDTH_PX) : PdfEngine {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "mupdf") }
    private val dispatcher = executor.asCoroutineDispatcher()
    private var document: Document? = null
    private var authenticated = false

    // Se executor.execute() lançar RejectedExecutionException (fila fechada por close()), o
    // kotlinx-coroutines reenvia o bloco para Dispatchers.IO em vez de propagar o erro — o que
    // rodaria chamadas nativas do MuPDF fora da thread única, concorrendo com document.destroy().
    // Essa flag é checada antes de despachar (evita o despacho) e de novo dentro do withContext
    // (pega o caso em que o bloco acabou rodando em outra thread mesmo assim).
    @Volatile private var closed = false

    private val cache = object : LruCache<Long, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: Long, value: Bitmap) = value.byteCount / 1024
    }

    private fun checkNotClosed() {
        if (closed) throw CancellationException("MuPdfEngine closed")
    }

    override suspend fun open(file: File, password: String?): OpenResult {
        checkNotClosed()
        return withContext(dispatcher) {
            checkNotClosed()
            val doc = document ?: try {
                Document.openDocument(file.absolutePath).also { document = it }
            } catch (e: Exception) {
                return@withContext OpenResult.Corrupted
            }
            if (!authenticated && doc.needsPassword()) {
                if (password == null) return@withContext OpenResult.NeedsPassword
                if (!doc.authenticatePassword(password)) return@withContext OpenResult.WrongPassword
                authenticated = true
            }
            val count = try { doc.countPages() } catch (e: Exception) { 0 }
            if (count <= 0) OpenResult.Corrupted else OpenResult.Success(count)
        }
    }

    override suspend fun pageSizes(): List<PageSize> {
        checkNotClosed()
        return withContext(dispatcher) {
            checkNotClosed()
            val doc = checkNotNull(document) { "open() primeiro" }
            List(doc.countPages()) { i ->
                val page = doc.loadPage(i)
                try {
                    val b = page.bounds
                    PageSize(b.x1 - b.x0, b.y1 - b.y0)
                } finally {
                    page.destroy()
                }
            }
        }
    }

    override suspend fun render(index: Int, widthPx: Int): Bitmap {
        checkNotClosed()
        return withContext(dispatcher) {
            checkNotClosed()
            val width = widthPx.coerceIn(1, maxRenderWidthPx)
            val key = (index.toLong() shl 32) or width.toLong()
            cache.get(key)?.let { return@withContext it }
            val doc = checkNotNull(document) { "open() primeiro" }
            val page = doc.loadPage(index)
            try {
                val b = page.bounds
                val scale = width / (b.x1 - b.x0)
                val height = ((b.y1 - b.y0) * scale).roundToInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                val device = AndroidDrawDevice(bitmap, 0, 0)
                try {
                    page.run(device, Matrix(scale, 0f, 0f, scale, -b.x0 * scale, -b.y0 * scale), null)
                    device.close()
                } finally {
                    device.destroy()
                }
                cache.put(key, bitmap)
                bitmap
            } finally {
                page.destroy()
            }
        }
    }

    /** Destrói o documento depois das tarefas já enfileiradas; chamadas novas são canceladas. */
    override fun close() {
        if (closed) return
        closed = true
        executor.execute {
            cache.evictAll()
            document?.destroy()
            document = null
        }
        executor.shutdown()
    }
}
