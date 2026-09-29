package com.johngabie.johnpdf.ui.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.RotationLockSetting
import com.johngabie.johnpdf.engine.MAX_RENDER_WIDTH_PX
import com.johngabie.johnpdf.engine.OpenResult
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.engine.PdfEngine
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ReaderStatus {
    data object Loading : ReaderStatus
    data class NeedsPassword(val wrongAttempt: Boolean) : ReaderStatus
    data class Ready(val pageSizes: List<PageSize>) : ReaderStatus
    data class Failed(val error: AppError) : ReaderStatus
}

data class ReaderUiState(
    val title: String,
    val status: ReaderStatus = ReaderStatus.Loading,
    val currentPage: Int = 0,
    val zoom: Float = 1f,
    val rotationLocked: Boolean = false,
) {
    val pageCount: Int get() = (status as? ReaderStatus.Ready)?.pageSizes?.size ?: 0
}

class ReaderViewModel(
    private val file: File,
    title: String,
    private val engine: PdfEngine,
    private val settings: RotationLockSetting,
) : ViewModel() {
    private val _state = MutableStateFlow(ReaderUiState(title))
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.rotationLocked.collect { locked -> _state.update { it.copy(rotationLocked = locked) } }
        }
        open(password = null)
    }

    fun submitPassword(password: String) = open(password)

    private fun open(password: String?) {
        viewModelScope.launch {
            val status = try {
                when (engine.open(file, password)) {
                    is OpenResult.Success -> ReaderStatus.Ready(engine.pageSizes())
                    OpenResult.NeedsPassword -> ReaderStatus.NeedsPassword(wrongAttempt = false)
                    OpenResult.WrongPassword -> ReaderStatus.NeedsPassword(wrongAttempt = true)
                    OpenResult.Corrupted -> ReaderStatus.Failed(AppError.CORRUPTED)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReaderStatus.Failed(AppError.CORRUPTED)
            }
            _state.update { it.copy(status = status) }
        }
    }

    fun onPageVisible(index: Int) =
        _state.update { it.copy(currentPage = index.coerceIn(0, maxOf(0, it.pageCount - 1))) }

    fun setZoom(zoom: Float) = _state.update { it.copy(zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)) }

    fun toggleDoubleTapZoom() =
        _state.update { it.copy(zoom = if (it.zoom > MIN_ZOOM) MIN_ZOOM else DOUBLE_TAP_ZOOM) }

    fun toggleRotationLock() {
        viewModelScope.launch { settings.setRotationLocked(!_state.value.rotationLocked) }
    }

    /** null = não foi possível renderizar (mostrar PAGE_RENDER_FAILED_MESSAGE). Só OOM tenta de novo, com metade da largura. */
    suspend fun renderPage(index: Int, widthPx: Int): Bitmap? = try {
        engine.render(index, widthPx)
    } catch (e: OutOfMemoryError) {
        tryRender(index, minOf(widthPx, MAX_RENDER_WIDTH_PX) / 2)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun tryRender(index: Int, widthPx: Int): Bitmap? = try {
        engine.render(index, widthPx)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        null
    }

    override fun onCleared() = engine.close()

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 4f
        const val DOUBLE_TAP_ZOOM = 2.5f
    }
}
