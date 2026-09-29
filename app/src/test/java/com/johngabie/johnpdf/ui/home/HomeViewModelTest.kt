package com.johngabie.johnpdf.ui.home

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.ImportResult
import com.johngabie.johnpdf.data.OpenPdfUseCase
import com.johngabie.johnpdf.data.Origin
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.PdfLibrary
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.data.RecentsRepository
import com.johngabie.johnpdf.testutil.MainDispatcherRule
import com.johngabie.johnpdf.ui.ReaderRoute
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private val recents by lazy { RecentsRepository(File(tmp.root, "r.json"), Dispatchers.Unconfined) }
    private var access = false
    private var libraryCalls = 0
    private var library: PdfLibrary = PdfLibrary { libraryCalls++; pdfs }
    private var pdfs = listOf(
        PdfFile("Fatura Setembro.pdf", "/sdcard/Download/f.pdf", Origin.DOWNLOAD, 2L),
        PdfFile("Receita médica.pdf", "/sdcard/Documents/r.pdf", Origin.DOCUMENTS, 1L),
    )
    private var importResult: ImportResult = ImportResult.Failed
    private fun vm() = HomeViewModel(recents, library, OpenPdfUseCase({ importResult }, recents, { 100L }), { access })

    @Test fun refresh_without_access_does_not_query() {
        val vm = vm()
        vm.refresh()
        assertFalse(vm.state.value.hasFilesAccess)
        assertEquals(0, libraryCalls)
    }

    @Test fun refresh_with_access_loads_pdfs() {
        access = true
        val vm = vm()
        vm.refresh()
        assertTrue(vm.state.value.hasFilesAccess)
        assertEquals(pdfs, vm.state.value.allPdfs)
        assertFalse(vm.state.value.loadingAll)
    }

    @Test fun library_failure_results_in_empty_list() {
        access = true
        library = PdfLibrary { throw SecurityException("revogada") }
        val vm = vm()
        vm.refresh()
        assertEquals(emptyList<PdfFile>(), vm.state.value.allPdfs)
    }

    @Test fun query_filters_ignoring_accents() {
        access = true
        val vm = vm()
        vm.refresh()
        vm.setQuery("medica")
        assertEquals(listOf("Receita médica.pdf"), vm.state.value.filteredPdfs.map { it.name })
    }

    @Test fun open_existing_file_navigates_and_adds_recent() = runTest {
        val f = tmp.newFile("a.pdf")
        val vm = vm()
        vm.navigation.test {
            vm.openFile(PdfFile("a.pdf", f.path, Origin.OTHER, 1L))
            assertEquals(ReaderRoute(f.path, "a.pdf"), awaitItem())
        }
        assertEquals(listOf(f.path), vm.state.value.recents.map { it.path })
    }

    @Test fun open_missing_recent_shows_gone_and_removes() = runTest {
        recents.add(RecentItem("x.pdf", Origin.OTHER, "/nao/existe.pdf", 1L, imported = false))
        val vm = vm()
        vm.openRecent(vm.state.value.recents.single())
        assertEquals(AppError.GONE, vm.state.value.error)
        assertEquals(emptyList<RecentItem>(), vm.state.value.recents)
        vm.dismissError()
        assertEquals(null, vm.state.value.error)
    }

    @Test fun open_uri_without_space_shows_error() {
        importResult = ImportResult.NoSpace
        val vm = vm()
        vm.openUri(Uri.parse("content://x/1"))
        assertEquals(AppError.NO_SPACE, vm.state.value.error)
        assertFalse(vm.state.value.busy)
    }

    @Test fun remove_recent_updates_list() = runTest {
        recents.add(RecentItem("a.pdf", Origin.OTHER, "/a.pdf", 1L, imported = false))
        val vm = vm()
        vm.removeRecent(vm.state.value.recents.single())
        assertEquals(emptyList<RecentItem>(), vm.state.value.recents)
    }

    @Test fun select_tab() {
        val vm = vm()
        vm.selectTab(HomeTab.ALL)
        assertEquals(HomeTab.ALL, vm.state.value.tab)
    }
}
