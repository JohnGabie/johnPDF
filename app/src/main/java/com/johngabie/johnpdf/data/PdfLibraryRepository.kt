package com.johngabie.johnpdf.data

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfFile(val name: String, val path: String, val origin: Origin, val modifiedAt: Long)

fun interface PdfLibrary {
    suspend fun queryAll(): List<PdfFile>
}

/** Lista os PDFs indexados pelo Android. Exige "acesso a todos os arquivos" (11+) ou READ_EXTERNAL_STORAGE (7–10). */
class PdfLibraryRepository(
    private val resolver: ContentResolver,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PdfLibrary {

    override suspend fun queryAll(): List<PdfFile> = withContext(io) {
        resolver.query(
            MediaStore.Files.getContentUri("external"),
            PROJECTION,
            "${MediaColumns.MIME_TYPE} = ? OR ${DATA_COLUMN} LIKE ?",
            arrayOf("application/pdf", "%.pdf"),
            "${MediaColumns.DATE_MODIFIED} DESC",
        )?.use(::parsePdfCursor) ?: emptyList()
    }

    private companion object {
        @Suppress("DEPRECATION")
        const val DATA_COLUMN = MediaColumns.DATA
        val PROJECTION = arrayOf(MediaColumns.DISPLAY_NAME, DATA_COLUMN, MediaColumns.DATE_MODIFIED)
    }
}

@Suppress("DEPRECATION")
internal fun parsePdfCursor(cursor: Cursor): List<PdfFile> {
    val nameCol = cursor.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME)
    val dataCol = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
    val dateCol = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
    val result = mutableListOf<PdfFile>()
    while (cursor.moveToNext()) {
        val path = cursor.getString(dataCol) ?: continue
        val name = cursor.getString(nameCol) ?: File(path).name
        result += PdfFile(name, path, originFromPath(path), cursor.getLong(dateCol) * 1000)
    }
    return result.distinctBy { it.path }.sortedByDescending { it.modifiedAt }
}
