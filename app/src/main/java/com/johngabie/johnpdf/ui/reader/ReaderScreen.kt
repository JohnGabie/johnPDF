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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
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
                    PageList(
                        pageSizes = status.pageSizes,
                        zoom = state.zoom,
                        listState = listState,
                        onZoomChange = onZoomChange,
                        onDoubleTap = onDoubleTap,
                        onSingleTap = { barsVisible = !barsVisible },
                        renderPage = renderPage,
                        currentPage = state.currentPage,
                        totalPages = state.pageCount,
                        barsVisible = showBars,
                        onPrevious = { scope.launch { listState.animateScrollToItem((state.currentPage - 1).coerceAtLeast(0)) } },
                        onNext = { scope.launch { listState.animateScrollToItem((state.currentPage + 1).coerceAtMost(state.pageCount - 1)) } },
                    )
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


@Composable
private fun PageList(
    pageSizes: List<PageSize>,
    zoom: Float,
    listState: LazyListState,
    onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    onSingleTap: () -> Unit,
    renderPage: suspend (Int, Int) -> Bitmap?,
    currentPage: Int,
    totalPages: Int,
    barsVisible: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val currentSingleTap by rememberUpdatedState(onSingleTap)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    // Estado da pinça em andamento (liveZoom null = sem pinça). Fica FORA do BoxWithConstraints
    // de propósito: o conteúdo dele é subcomposto na medida, e o `remember` de lá não é estável
    // o bastante para uma corrotina de pointerInput de vida longa escrever nele.
    val liveZoom = remember { mutableStateOf<Float?>(null) }
    val pinchOrigin = remember { mutableStateOf(TransformOrigin.Center) }
    LaunchedEffect(zoom) { liveZoom.value = null }
    BoxWithConstraints(Modifier.fillMaxSize().background(JohnTheme.colors.pageGap)) {
        val contentWidth = maxWidth * zoom
        val widthPx = with(LocalDensity.current) { contentWidth.roundToPx() }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(zoom) {
                    detectPinch(
                        onPinch = { s, centroid ->
                            // Amplia em volta dos dedos, não do centro da tela: senão o trecho
                            // que o usuário está segurando foge da mão durante o gesto.
                            pinchOrigin.value = TransformOrigin(
                                if (size.width > 0) (centroid.x / size.width).coerceIn(0f, 1f) else 0.5f,
                                if (size.height > 0) (centroid.y / size.height).coerceIn(0f, 1f) else 0.5f,
                            )
                            liveZoom.value = (zoom * s).coerceIn(ReaderViewModel.MIN_ZOOM, ReaderViewModel.MAX_ZOOM)
                        },
                        onPinchEnd = { s -> onZoomChange(zoom * s) },
                    )
                }
                .pointerInput(Unit) { detectTapGestures(onTap = { currentSingleTap() }, onDoubleTap = { currentDoubleTap() }) }
                // Lido DENTRO do bloco de propósito: assim a escala invalida a camada
                // diretamente, sem depender de recomposição — é o que faz o preview
                // acompanhar os dedos quadro a quadro durante o gesto.
                .graphicsLayer {
                    val preview = (liveZoom.value ?: zoom) / zoom
                    scaleX = preview
                    scaleY = preview
                    transformOrigin = pinchOrigin.value
                }
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

        // Setas flutuantes no canto direito
        AnimatedVisibility(
            barsVisible,
            modifier = Modifier.align(Alignment.BottomEnd).padding(SpaceL),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SpaceS)) {
                FilledTonalIconButton(
                    onClick = onPrevious,
                    enabled = currentPage > 0,
                    modifier = Modifier.size(PrimaryTouchTarget),
                ) {
                    Icon(JohnIcons.KeyboardArrowUp, contentDescription = "Página anterior")
                }
                FilledTonalIconButton(
                    onClick = onNext,
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier.size(PrimaryTouchTarget),
                ) {
                    Icon(JohnIcons.KeyboardArrowDown, contentDescription = "Próxima página")
                }
            }
        }

        // Número de páginas sutil no canto inferior esquerdo
        AnimatedVisibility(
            barsVisible,
            modifier = Modifier.align(Alignment.BottomStart).padding(SpaceL + 8.dp, SpaceL),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                pageLabel(currentPage, totalPages),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * Pinça com dois dedos, interceptada na passagem Initial para não brigar com a rolagem da lista.
 *
 * A escala é sempre `abertura atual / abertura âncora` — medida absoluta, não o produto das
 * razões quadro a quadro de `calculateZoom()`. Com o produto, todo quadro em que o conjunto de
 * dedos muda (segundo dedo descendo, dedo extra encostando, dedo saindo e voltando) entra no
 * acumulado como 1.0 ou como um salto de centroide, e o erro fica preso no resultado: o zoom
 * deixa de acompanhar os dedos. Com a âncora, cada quadro é recalculado do zero e o gesto é
 * proporcional por construção; quando o conjunto de dedos muda, re-ancoramos preservando a
 * escala já alcançada, então o reposicionamento não move a página.
 */
private suspend fun PointerInputScope.detectPinch(
    onPinch: (scale: Float, centroid: Offset) -> Unit,
    onPinchEnd: (scale: Float) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var anchor = 0f // abertura que corresponde a scale = 1; 0 = precisa (re)ancorar
        var scale = 1f
        var pinched = false
        var fingers = emptyList<PointerId>()
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.filter { it.pressed }
            val ids = down.map { it.id }
            if (ids != fingers) {
                fingers = ids
                anchor = 0f
            }
            if (down.size >= 2) {
                val positions = down.map { it.position }
                val spread = fingerSpread(positions)
                if (spread > 0f) {
                    if (anchor <= 0f) {
                        anchor = spread / scale // (re)ancora sem mexer na escala já alcançada
                    } else {
                        scale = spread / anchor
                        pinched = true
                    }
                    // Emite também no quadro da âncora: assim o ponto de ampliação já nasce
                    // debaixo dos dedos e o primeiro quadro com escala nova não salta.
                    onPinch(scale, centroidOf(positions))
                }
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (pinched) onPinchEnd(scale)
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
