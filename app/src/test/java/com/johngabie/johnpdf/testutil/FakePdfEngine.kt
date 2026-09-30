package com.johngabie.johnpdf.testutil

import android.graphics.Bitmap
import com.johngabie.johnpdf.engine.OpenResult
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.engine.PdfEngine
import java.io.File

/** Requer Robolectric (usa Bitmap). */
class FakePdfEngine(
    var sizes: List<PageSize> = List(3) { PageSize(595f, 842f) },
    var password: String? = null,
    var corrupted: Boolean = false,
) : PdfEngine {
    var closed = false
    val renderFailures = ArrayDeque<Throwable>()
    val renderRequests = mutableListOf<Pair<Int, Int>>()

    override suspend fun open(file: File, password: String?): OpenResult = when {
        corrupted -> OpenResult.Corrupted
        this.password == null -> OpenResult.Success(sizes.size)
        password == null -> OpenResult.NeedsPassword
        password != this.password -> OpenResult.WrongPassword
        else -> OpenResult.Success(sizes.size)
    }

    override suspend fun pageSizes(): List<PageSize> = sizes

    override suspend fun render(index: Int, widthPx: Int): Bitmap {
        renderRequests += index to widthPx
        renderFailures.removeFirstOrNull()?.let { throw it }
        return Bitmap.createBitmap(widthPx, widthPx, Bitmap.Config.ARGB_8888)
    }

    override fun close() { closed = true }
}
