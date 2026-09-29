package com.johngabie.johnpdf.ui.reader

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johngabie.johnpdf.data.PAGE_RENDER_FAILED_MESSAGE
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.ui.common.ErrorDialog
import com.johngabie.johnpdf.ui.common.PasswordDialog
import com.johngabie.johnpdf.ui.icons.JohnIcons
import com.johngabie.johnpdf.ui.theme.JohnTheme
import com.johngabie.johnpdf.ui.theme.MinGap
import com.johngabie.johnpdf.ui.theme.PageElevation
import com.johngabie.johnpdf.ui.theme.PageGap
import com.johngabie.johnpdf.ui.theme.PdfPageBackground
import com.johngabie.johnpdf.ui.theme.PrimaryTouchTarget
import com.johngabie.johnpdf.ui.theme.SpaceL
import com.johngabie.johnpdf.ui.theme.SpaceS
import com.johngabie.johnpdf.ui.theme.readableIconButtonColors
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
    val snackbarHostState = remember { SnackbarHostState() }
    val status = state.status
    // Toque simples no PDF esconde/mostra as barras (mais área de leitura, sobretudo em paisagem).
    var barsVisible by rememberSaveable { mutableStateOf(true) }
    val showBars = barsVisible || status !is ReaderStatus.Ready
    ApplySystemBarsVisibility(showBars)

    Scaffold(
        topBar = {
            AnimatedVisibility(showBars, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                ReaderTopBar(
                    title = state.title,
                    showRotation = status is ReaderStatus.Ready,
                    rotationLocked = state.rotationLocked,
                    onBack = onBack,
                    onToggleRotation = {
                        // O StateFlow do ViewModel ainda não recompôs aqui: o estado "depois do
                        // toque" é o inverso do atual.
                        val willBeLocked = !state.rotationLocked
                        onToggleRotation()
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss() // toques rápidos não enfileiram
                            snackbarHostState.showSnackbar(
                                message = if (willBeLocked) "Tela travada nesta posição" else "Rotação automática",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                showBars && status is ReaderStatus.Ready,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                ReaderBottomBar(
                    current = state.currentPage,
                    total = state.pageCount,
                    onPrevious = { scope.launch { listState.animateScrollToItem((state.currentPage - 1).coerceAtLeast(0)) } },
                    onNext = { scope.launch { listState.animateScrollToItem((state.currentPage + 1).coerceAtMost(state.pageCount - 1)) } },
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
                    PageList(status.pageSizes, state.zoom, listState, onZoomChange, onDoubleTap, onSingleTap = { barsVisible = !barsVisible }, renderPage)
                }
            }
        }
    }
}

/** Esconde status/navigation bar do sistema junto com as barras do leitor; deslizar da borda mostra temporariamente. */
@Composable
private fun ApplySystemBarsVisibility(visible: Boolean) {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window, visible) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (visible) controller.show(WindowInsetsCompat.Type.systemBars())
        else controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
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

/** Voltar é só a seta: é a convenção universal, e o `contentDescription` cobre o TalkBack (spec §3.3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    title: String,
    showRotation: Boolean,
    rotationLocked: Boolean,
    onBack: () -> Unit,
    onToggleRotation: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(JohnIcons.ArrowBack, contentDescription = "Voltar")
            }
        },
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        actions = { if (showRotation) RotationLockAction(rotationLocked, onToggleRotation) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

/**
 * Sem preenchimento azul: destravado = ícone `onSurfaceVariant` sem fundo; travado =
 * `onSurface` sobre `surfaceContainerHigh` (spec §6.2 / D1 §C). O `stateDescription` é o que
 * o TalkBack anuncia depois do toque — o `contentDescription` nomeia a ação, não o estado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RotationLockAction(locked: Boolean, onToggle: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(if (locked) "Destravar rotação" else "Travar rotação") } },
        state = rememberTooltipState(),
    ) {
        IconToggleButton(
            checked = locked,
            onCheckedChange = { onToggle() },
            modifier = Modifier
                .size(48.dp)
                .semantics { stateDescription = if (locked) "Travada" else "Automática" },
            colors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContentColor = MaterialTheme.colorScheme.onSurface,
                checkedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Icon(
                if (locked) JohnIcons.ScreenLockRotation else JohnIcons.ScreenRotation,
                contentDescription = "Travar rotação da tela",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Faixa única: rótulo "Página X de Y" à esquerda e as setas empilhadas à direita, 56dp cada
 * com 8dp de folga. Empilhadas (e não lado a lado) porque ▲/▼ casam com o sentido da rolagem.
 */
@Composable
private fun ReaderBottomBar(current: Int, total: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    // Sem tonalElevation: no escuro ele tinge a superfície com primary e reintroduz
    // cor vazada. O nível de superfície é escolhido explicitamente.
    Surface(color = JohnTheme.barColor, contentColor = MaterialTheme.colorScheme.onSurface) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SpaceL, vertical = SpaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(pageLabel(current, total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(MinGap)) {
                FilledTonalIconButton(
                    onClick = onPrevious,
                    enabled = current > 0,
                    colors = readableIconButtonColors(),
                    modifier = Modifier.size(PrimaryTouchTarget),
                ) { Icon(JohnIcons.KeyboardArrowUp, contentDescription = "Página anterior") }
                FilledTonalIconButton(
                    onClick = onNext,
                    enabled = current < total - 1,
                    colors = readableIconButtonColors(),
                    modifier = Modifier.size(PrimaryTouchTarget),
                ) { Icon(JohnIcons.KeyboardArrowDown, contentDescription = "Próxima página") }
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
    onSingleTap: () -> Unit,
    renderPage: suspend (Int, Int) -> Bitmap?,
) {
    val currentSingleTap by rememberUpdatedState(onSingleTap)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    BoxWithConstraints(Modifier.fillMaxSize().background(JohnTheme.colors.pageGap)) {
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
                .pointerInput(Unit) { detectTapGestures(onTap = { currentSingleTap() }, onDoubleTap = { currentDoubleTap() }) }
                .graphicsLayer { scaleX = pinch; scaleY = pinch }
                .horizontalScroll(rememberScrollState(), enabled = zoom > 1f),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(contentWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(PageGap),
                contentPadding = PaddingValues(vertical = PageGap),
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
    // PdfPageBackground é o papel do PDF — a única cor fixa do app, porque a página
    // não é superfície do tema (vive em Theme.kt, branca nos dois modos).
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
        color = PdfPageBackground,
        shadowElevation = PageElevation,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    modifier = Modifier.padding(SpaceL),
                )
                PageImage.Loading -> Unit
            }
        }
    }
}
