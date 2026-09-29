package com.johngabie.johnpdf.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ImportResult {
    data class Success(val file: File, val displayName: String, val origin: Origin) : ImportResult
    data object NoSpace : ImportResult
    data object Failed : ImportResult
}

fun interface Importer {
    suspend fun import(uri: Uri): ImportResult
}

/** Copia um PDF recebido por URI (acesso temporário) para filesDir/imports. */
class ImportRepository(
    private val resolver: ContentResolver,
    private val importsDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : Importer {

    override suspend fun import(uri: Uri): ImportResult = withContext(io) {
        val name = displayName(uri)
        val target = File(importsDir, "%08x.pdf".format(uri.toString().hashCode()))
        val partial = File(importsDir, target.name + ".part")
        try {
            importsDir.mkdirs()
            val input = resolver.openInputStream(uri) ?: return@withContext ImportResult.Failed
            input.use { i -> partial.outputStream().use { o -> i.copyTo(o) } }
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) return@withContext ImportResult.Failed
            ImportResult.Success(target, name, originFromAuthority(uri.authority))
        } catch (e: IOException) {
            if (isNoSpace(e)) ImportResult.NoSpace else ImportResult.Failed
        } catch (e: SecurityException) {
            ImportResult.Failed
        } finally {
            partial.delete()
        }
    }

    private fun displayName(uri: Uri): String =
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "documento.pdf"
}

internal fun isNoSpace(e: Throwable): Boolean =
    generateSequence(e) { it.cause }.any { t ->
        val msg = t.message.orEmpty()
        "ENOSPC" in msg || msg.contains("No space left", ignoreCase = true)
    }
