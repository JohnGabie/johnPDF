package com.johngabie.johnpdf.data

import android.net.Uri
import java.io.File

sealed interface OpenOutcome {
    data class Ready(val path: String, val name: String) : OpenOutcome
    data class Failed(val error: AppError) : OpenOutcome
}

/** Resolve de onde vem o PDF, registra nos recentes e devolve o caminho que o leitor deve abrir. */
class OpenPdfUseCase(
    private val importer: Importer,
    private val recents: RecentsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun openUri(uri: Uri): OpenOutcome = when (val r = importer.import(uri)) {
        is ImportResult.Success -> register(RecentItem(r.displayName, r.origin, r.file.absolutePath, clock(), imported = true))
        ImportResult.NoSpace -> OpenOutcome.Failed(AppError.NO_SPACE)
        ImportResult.Failed -> OpenOutcome.Failed(AppError.CORRUPTED)
    }

    suspend fun openFile(pdf: PdfFile): OpenOutcome =
        if (!File(pdf.path).exists()) OpenOutcome.Failed(AppError.GONE)
        else register(RecentItem(pdf.name, pdf.origin, pdf.path, clock(), imported = false))

    suspend fun openRecent(item: RecentItem): OpenOutcome =
        if (!File(item.path).exists()) {
            recents.remove(item.path)
            OpenOutcome.Failed(AppError.GONE)
        } else {
            register(item.copy(openedAt = clock()))
        }

    private suspend fun register(item: RecentItem): OpenOutcome {
        recents.add(item)
        return OpenOutcome.Ready(item.path, item.name)
    }
}
