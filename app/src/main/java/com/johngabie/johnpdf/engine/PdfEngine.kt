package com.johngabie.johnpdf.engine

import android.graphics.Bitmap
import java.io.File

const val MAX_RENDER_WIDTH_PX = 2048

data class PageSize(val width: Float, val height: Float)

sealed interface OpenResult {
    data class Success(val pageCount: Int) : OpenResult
    data object NeedsPassword : OpenResult
    data object WrongPassword : OpenResult
    data object Corrupted : OpenResult
}

interface PdfEngine {
    /** Pode ser chamado de novo no mesmo arquivo com a senha. */
    suspend fun open(file: File, password: String? = null): OpenResult
    suspend fun pageSizes(): List<PageSize>
    /** Largura limitada a MAX_RENDER_WIDTH_PX. Pode lançar OutOfMemoryError. */
    suspend fun render(index: Int, widthPx: Int): Bitmap
    fun close()
}
