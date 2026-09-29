package com.johngabie.johnpdf.ui.reader

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johngabie.johnpdf.data.PAGE_RENDER_FAILED_MESSAGE
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.ui.common.BigButton
import com.johngabie.johnpdf.ui.common.ErrorDialog
import com.johngabie.johnpdf.ui.common.PasswordDialog
import com.johngabie.johnpdf.ui.theme.MinTouchTarget
import com.johngabie.johnpdf.ui.theme.PageGapColor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val A4_ASPECT = 595f / 842f

@Composable
fun ReaderScreen(viewModel: ReaderViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ApplyRotationLock(state.rotationLocked)
    ReaderContent(
        state = state,
        onBack = onBack,
        onPageVisible = viewModel::onPageVisible,
        onZoomChange = viewModel::setZoom,
        onDoubleTap = viewModel::toggleDoubleTapZoom,
        onToggleRotation = viewModel::toggleRotationLock,
        onSubmitPassword = viewModel::submitPassword,
        renderPage = viewModel::renderPage,
    )
}

@Composable
private fun ApplyRotationLock(locked: Boolean) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(locked) {
        activity.requestedOrientation =
            if (locked) ActivityInfo.SCREEN_ORIENTATION_LOCKED else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}

@Composable
fun ReaderContent(
    state: ReaderUiState,
    onBack: () -> Unit,
    onPageVisible: (Int) -> Unit,
    onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    onToggleRotation: () -> Unit,
    onSubmitPassword: (String) -> Unit,
    renderPage: suspend (Int, Int) -> Bitmap?,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val status = state.status

    Scaffold(
        topBar = { ReaderTopBar(state.title, onBack) },
        bottomBar = {
            if (status is ReaderStatus.Ready) {
                ReaderBottomBar(
                    current = state.currentPage,
                    total = state.pageCount,
                    rotationLocked = state.rotationLocked,
                    onPrevious = { scope.launch { listState.animateScrollToItem((state.currentPage - 1).coerceAtLeast(0)) } },
                    onNext = { scope.launch { listState.animateScrollToItem((state.currentPage + 1).coerceAtMost(state.pageCount - 1)) } },
                    onToggleRotation = onToggleRotation,
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (status) {
                ReaderStatus.Loading -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Abrindo…", style = MaterialTheme.typography.bodyLarge)
                }
                is ReaderStatus.NeedsPassword -> PasswordDialog(status.wrongAttempt, onSubmitPassword, onCancel = onBack)
                is ReaderStatus.Failed -> ErrorDialog(status.error, onDismiss = onBack)
                is ReaderStatus.Ready -> {
                    TrackVisiblePage(listState, onPageVisible)
                    PageList(status.pageSizes, state.zoom, listState, onZoomChange, onDoubleTap, renderPage)
                }
            }
        }
    }
}

@Composable
private fun TrackVisiblePage(listState: LazyListState, onPageVisible: (Int) -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            dominantPage(
                info.visibleItemsInfo.map { VisiblePage(it.index, it.offset, it.size) },
                info.viewportStartOffset,
                info.viewportEndOffset,
            )
        }.filterNotNull().distinctUntilChanged().collect { onPageVisible(it) }
    }
}

@Composable
private fun ReaderTopBar(title: String, onBack: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text("← Voltar", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ReaderBottomBar(
    current: Int,
    total: Int,
    rotationLocked: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRotation: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pageLabel(current, total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                BigButton(if (rotationLocked) "🔒 Travada" else "🔓 Gira sozinha", onToggleRotation)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("⬆ Anterior", onPrevious, Modifier.weight(1f), enabled = current > 0)
                BigButton("⬇ Próxima", onNext, Modifier.weight(1f), enabled = current < total - 1)
            }
        }
    }
}

@Composable
private fun PageList(
    pageSizes: List<PageSize>,
    zoom: Float,
    listState: LazyListState,
    onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    renderPage: suspend (Int, Int) -> Bitmap?,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(PageGapColor)) {
        val contentWidth = maxWidth * zoom
        val widthPx = with(LocalDensity.current) { contentWidth.roundToPx() }
        var pinch by remember { mutableFloatStateOf(1f) }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(zoom) {
                    detectPinch(
                        onPinch = { s -> pinch = (zoom * s).coerceIn(ReaderViewModel.MIN_ZOOM, ReaderViewModel.MAX_ZOOM) / zoom },
                        onPinchEnd = { s -> pinch = 1f; onZoomChange(zoom * s) },
                    )
                }
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }
                .graphicsLayer { scaleX = pinch; scaleY = pinch }
                .horizontalScroll(rememberScrollState(), enabled = zoom > 1f),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(contentWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                itemsIndexed(pageSizes) { index, size -> PdfPage(index, size, widthPx, renderPage) }
            }
        }
    }
}

/** Pinça com dois dedos, interceptada na passagem Initial para não brigar com a rolagem da lista. */
private suspend fun PointerInputScope.detectPinch(onPinch: (Float) -> Unit, onPinchEnd: (Float) -> Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var scale = 1f
        var pinching = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                pinching = true
                scale *= event.calculateZoom()
                onPinch(scale)
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (pinching) onPinchEnd(scale)
    }
}

private sealed interface PageImage {
    data object Loading : PageImage
    data class Loaded(val image: ImageBitmap) : PageImage
    data object Failed : PageImage
}

@Composable
private fun PdfPage(index: Int, size: PageSize, widthPx: Int, renderPage: suspend (Int, Int) -> Bitmap?) {
    // Mantém a imagem anterior enquanto re-renderiza após zoom (evita piscar em branco).
    var image by remember(index) { mutableStateOf<PageImage>(PageImage.Loading) }
    LaunchedEffect(index, widthPx) {
        image = renderPage(index, widthPx)?.let { PageImage.Loaded(it.asImageBitmap()) } ?: PageImage.Failed
    }
    val aspect = (size.width / size.height).takeIf { it.isFinite() && it > 0f } ?: A4_ASPECT
    Box(
        Modifier.fillMaxWidth().aspectRatio(aspect).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        when (val img = image) {
            is PageImage.Loaded -> Image(
                img.image,
                contentDescription = "Página ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            PageImage.Failed -> Text(
                PAGE_RENDER_FAILED_MESSAGE,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
            PageImage.Loading -> Unit
        }
    }
}
