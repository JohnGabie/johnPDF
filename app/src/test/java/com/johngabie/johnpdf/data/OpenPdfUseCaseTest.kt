package com.johngabie.johnpdf.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenPdfUseCaseTest {
    @get:Rule val tmp = TemporaryFolder()
    private val recents by lazy { RecentsRepository(File(tmp.root, "r.json"), Dispatchers.Unconfined) }
    private var importResult: ImportResult = ImportResult.Failed
    private val useCase by lazy { OpenPdfUseCase({ importResult }, recents, clock = { 1000L }) }

    @Test fun open_uri_success_registers_imported_recent() = runTest {
        val copy = tmp.newFile("copy.pdf")
        importResult = ImportResult.Success(copy, "Fatura.pdf", Origin.WHATSAPP)

        assertEquals(OpenOutcome.Ready(copy.path, "Fatura.pdf"), useCase.openUri(Uri.parse("content://x/1")))
        assertEquals(listOf(RecentItem("Fatura.pdf", Origin.WHATSAPP, copy.path, 1000L, imported = true)), recents.items.value)
    }

    @Test fun open_uri_no_space_fails() = runTest {
        importResult = ImportResult.NoSpace
        assertEquals(OpenOutcome.Failed(AppError.NO_SPACE), useCase.openUri(Uri.parse("content://x/1")))
    }

    @Test fun open_uri_generic_failure_is_corrupted() = runTest {
        importResult = ImportResult.Failed
        assertEquals(OpenOutcome.Failed(AppError.CORRUPTED), useCase.openUri(Uri.parse("content://x/1")))
    }

    @Test fun open_file_registers_non_imported_recent() = runTest {
        val f = tmp.newFile("a.pdf")
        assertEquals(OpenOutcome.Ready(f.path, "a.pdf"), useCase.openFile(PdfFile("a.pdf", f.path, Origin.DOWNLOAD, 5L)))
        assertEquals(false, recents.items.value.single().imported)
    }

    @Test fun open_missing_file_is_gone() = runTest {
        assertEquals(OpenOutcome.Failed(AppError.GONE), useCase.openFile(PdfFile("x.pdf", "/nao/existe.pdf", Origin.OTHER, 1L)))
    }

    @Test fun open_missing_recent_removes_it() = runTest {
        recents.add(RecentItem("x.pdf", Origin.OTHER, "/nao/existe.pdf", 1L, imported = false))
        assertEquals(OpenOutcome.Failed(AppError.GONE), useCase.openRecent(recents.items.value.single()))
        assertEquals(emptyList<RecentItem>(), recents.items.value)
    }

    @Test fun open_recent_bumps_timestamp() = runTest {
        val f = tmp.newFile("a.pdf")
        recents.add(RecentItem("a.pdf", Origin.OTHER, f.path, 1L, imported = false))
        useCase.openRecent(recents.items.value.single())
        assertEquals(1000L, recents.items.value.single().openedAt)
    }
}
