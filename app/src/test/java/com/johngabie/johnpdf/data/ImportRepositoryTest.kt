package com.johngabie.johnpdf.data

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ImportRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val importsDir = File(context.filesDir, "imports")
    private val repo = ImportRepository(context.contentResolver, importsDir, Dispatchers.Unconfined)

    @Suppress("DEPRECATION")
    private fun register(uri: Uri, stream: InputStream) = shadowOf(context.contentResolver).registerInputStream(uri, stream)

    @Test fun copies_content_into_imports_dir() = runTest {
        val uri = Uri.parse("content://com.whatsapp.provider.media/item/42/Fatura.pdf")
        val bytes = "%PDF-1.7 conteudo".toByteArray()
        register(uri, ByteArrayInputStream(bytes))

        val result = repo.import(uri) as ImportResult.Success

        assertArrayEquals(bytes, result.file.readBytes())
        assertEquals(importsDir, result.file.parentFile)
        assertTrue(result.file.name.endsWith(".pdf"))
        assertEquals("Fatura.pdf", result.displayName)
        assertEquals(Origin.WHATSAPP, result.origin)
    }

    @Test fun same_uri_reuses_same_file_name() = runTest {
        val uri = Uri.parse("content://x/doc.pdf")
        register(uri, ByteArrayInputStream(byteArrayOf(1)))
        val first = repo.import(uri) as ImportResult.Success
        register(uri, ByteArrayInputStream(byteArrayOf(2)))
        val second = repo.import(uri) as ImportResult.Success
        assertEquals(first.file, second.file)
        assertEquals(1, importsDir.listFiles()!!.size)
    }

    @Test fun no_space_is_reported_and_partial_file_removed() = runTest {
        val uri = Uri.parse("content://x/grande.pdf")
        register(uri, object : InputStream() {
            override fun read(): Int = throw IOException("write failed: ENOSPC (No space left on device)")
        })
        assertEquals(ImportResult.NoSpace, repo.import(uri))
        assertTrue(importsDir.listFiles().orEmpty().none { it.name.endsWith(".part") })
    }

    @Test fun other_io_error_is_failed() = runTest {
        val uri = Uri.parse("content://x/quebrado.pdf")
        register(uri, object : InputStream() { override fun read(): Int = throw IOException("boom") })
        assertEquals(ImportResult.Failed, repo.import(uri))
    }

    @Test fun no_space_detection_looks_at_causes() {
        assertTrue(isNoSpace(IOException("wrapper", IOException("No space left on device"))))
    }
}
