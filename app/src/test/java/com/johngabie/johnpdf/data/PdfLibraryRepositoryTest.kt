package com.johngabie.johnpdf.data

import android.database.MatrixCursor
import android.provider.MediaStore.MediaColumns
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfLibraryRepositoryTest {
    @Suppress("DEPRECATION")
    private fun cursor(vararg rows: Array<Any?>) =
        MatrixCursor(arrayOf(MediaColumns.DISPLAY_NAME, MediaColumns.DATA, MediaColumns.DATE_MODIFIED)).apply {
            rows.forEach { addRow(it) }
        }

    @Test fun maps_rows_and_converts_seconds_to_millis() {
        val result = parsePdfCursor(cursor(arrayOf("Fatura.pdf", "/storage/emulated/0/Download/Fatura.pdf", 1_700_000_000L)))
        assertEquals(listOf(PdfFile("Fatura.pdf", "/storage/emulated/0/Download/Fatura.pdf", Origin.DOWNLOAD, 1_700_000_000_000L)), result)
    }

    @Test fun skips_rows_without_path() {
        assertEquals(emptyList<PdfFile>(), parsePdfCursor(cursor(arrayOf("x.pdf", null, 1L))))
    }

    @Test fun falls_back_to_file_name_when_display_name_missing() {
        assertEquals("b.pdf", parsePdfCursor(cursor(arrayOf(null, "/sdcard/Documents/b.pdf", 1L))).single().name)
    }

    @Test fun sorts_newest_first_and_removes_duplicates() {
        val result = parsePdfCursor(cursor(
            arrayOf("a.pdf", "/sdcard/a.pdf", 10L),
            arrayOf("b.pdf", "/sdcard/b.pdf", 20L),
            arrayOf("a.pdf", "/sdcard/a.pdf", 10L),
        ))
        assertEquals(listOf("/sdcard/b.pdf", "/sdcard/a.pdf"), result.map { it.path })
    }
}
