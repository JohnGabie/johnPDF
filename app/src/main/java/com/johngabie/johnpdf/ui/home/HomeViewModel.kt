package com.johngabie.johnpdf.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.OpenOutcome
import com.johngabie.johnpdf.data.OpenPdfUseCase
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.PdfLibrary
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.data.RecentsRepository
import com.johngabie.johnpdf.data.isNoSpace
import com.johngabie.johnpdf.ui.ReaderRoute
import com.johngabie.johnpdf.util.filterByName
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeTab { RECENTS, ALL }

data class HomeUiState(
    val tab: HomeTab = HomeTab.RECENTS,
    val recents: List<RecentItem> = emptyList(),
    val allPdfs: List<PdfFile> = emptyList(),
    val query: String = "",
    val hasFilesAccess: Boolean = false,
    val loadingAll: Boolean = false,
    val busy: Boolean = false,
    val error: AppError? = null,
) {
    val filteredPdfs: List<PdfFile> get() = filterByName(allPdfs, query) { it.name }
}

class HomeViewModel(
    private val recents: RecentsRepository,
    private val library: PdfLibrary,
    private val openPdf: OpenPdfUseCase,
    private val hasFilesAccess: () -> Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _navigation = Channel<ReaderRoute>(Channel.BUFFERED)
    val navigation: Flow<ReaderRoute> = _navigation.receiveAsFlow()

    init {
        viewModelScope.launch { recents.load() }
        viewModelScope.launch { recents.items.collect { items -> _state.update { it.copy(recents = items) } } }
    }

    /** Chamado a cada ON_RESUME: a permissão pode ter mudado nas configurações. */
    fun refresh() {
        val access = hasFilesAccess()
        _state.update { it.copy(hasFilesAccess = access) }
        if (!access) return
        viewModelScope.launch {
            _state.update { it.copy(loadingAll = true) }
            val pdfs = try {
                library.queryAll()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            _state.update { it.copy(allPdfs = pdfs, loadingAll = false) }
        }
    }

    fun selectTab(tab: HomeTab) = _state.update { it.copy(tab = tab) }
    fun setQuery(query: String) = _state.update { it.copy(query = query) }
    fun dismissError() = _state.update { it.copy(error = null) }

    fun openUri(uri: Uri) = launchOpen { openPdf.openUri(uri) }
    fun openFile(pdf: PdfFile) = launchOpen { openPdf.openFile(pdf) }
    fun openRecent(item: RecentItem) = launchOpen { openPdf.openRecent(item) }

    fun removeRecent(item: RecentItem) {
        viewModelScope.launch {
            try {
                recents.remove(item.path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Swallow: mantém a lista como está.
            }
        }
    }

    private fun launchOpen(block: suspend () -> OpenOutcome) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val outcome = try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    OpenOutcome.Failed(if (isNoSpace(e)) AppError.NO_SPACE else AppError.CORRUPTED)
                }
                when (outcome) {
                    is OpenOutcome.Ready -> _navigation.send(ReaderRoute(outcome.path, outcome.name))
                    is OpenOutcome.Failed -> _state.update { it.copy(error = outcome.error) }
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
