package com.johngabie.johnpdf.ui.home

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johngabie.johnpdf.AppContainer
import com.johngabie.johnpdf.BuildConfig
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.data.StorageAccess
import com.johngabie.johnpdf.ui.common.ErrorDialog
import com.johngabie.johnpdf.ui.common.PrimaryButton
import com.johngabie.johnpdf.ui.common.RemoveDialog
import com.johngabie.johnpdf.ui.common.SecondaryButton
import com.johngabie.johnpdf.ui.common.UpdateAvailableDialog
import com.johngabie.johnpdf.ui.common.UpdateSettingsDialog
import com.johngabie.johnpdf.ui.icons.JohnIcons
import com.johngabie.johnpdf.ui.theme.JohnTheme
import com.johngabie.johnpdf.ui.theme.ListItemMinHeight
import com.johngabie.johnpdf.ui.theme.LocalThemeController
import com.johngabie.johnpdf.ui.theme.MaxActionWidth
import com.johngabie.johnpdf.ui.theme.ThemeMode
import com.johngabie.johnpdf.ui.theme.PrimaryTouchTarget
import com.johngabie.johnpdf.ui.theme.SpaceL
import com.johngabie.johnpdf.ui.theme.SpaceS
import com.johngabie.johnpdf.ui.theme.SpaceXl
import com.johngabie.johnpdf.ui.theme.SpaceXxl
import com.johngabie.johnpdf.ui.theme.johnNavigationBarItemColors
import com.johngabie.johnpdf.util.friendlyDate

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun HomeScreen(viewModel: HomeViewModel, container: AppContainer) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remoteVersion by container.updates.remoteVersion.collectAsStateWithLifecycle(initialValue = null)
    val autoCheckUpdates by container.settings.autoCheckUpdates.collectAsStateWithLifecycle(initialValue = false)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dismissedVersion by remember { mutableStateOf<Int?>(null) }
    var showUpdateSettings by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::openUri)
    }
    val legacyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Android 7–10, depois de "Não perguntar de novo": o sistema não mostra mais o
        // diálogo (granted=false sem rationale) e "Permitir" ficaria sem efeito. Nesse
        // caso, manda o usuário direto para a tela de permissões do app.
        if (!granted) {
            val activity = context.findActivity()
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                try {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                } catch (e: ActivityNotFoundException) {
                    // Sem tela de configurações do app: nada a fazer além do refresh abaixo.
                }
            }
        }
        viewModel.refresh()
    }
    LaunchedEffect(autoCheckUpdates) {
        if (autoCheckUpdates) {
            container.updates.checkForUpdate()
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    if (remoteVersion != null &&
        remoteVersion!!.versionCode > container.updates.currentVersionCode &&
        dismissedVersion != remoteVersion!!.versionCode
    ) {
        UpdateAvailableDialog(
            current = remoteVersion!!,
            currentVersionName = BuildConfig.VERSION_NAME,
            onDismiss = { dismissedVersion = remoteVersion!!.versionCode },
            onOpenLink = { url ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    // Silently fail if browser not available
                }
            },
        )
    }

    if (showUpdateSettings) {
        UpdateSettingsDialog(
            autoCheckUpdates = autoCheckUpdates,
            onToggleAutoCheck = { enabled ->
                scope.launch { container.settings.setAutoCheckUpdates(enabled) }
            },
            onCheckNow = { onResult ->
                dismissedVersion = null
                scope.launch {
                    // Use the returned value: the collected State has not recomposed yet.
                    val remote = container.updates.checkForUpdate(force = true)
                    onResult(remote != null && remote.versionCode > container.updates.currentVersionCode)
                }
            },
            onDismiss = { showUpdateSettings = false },
        )
    }

    HomeContent(
        state = state,
        onOpenPicker = { picker.launch(arrayOf("application/pdf")) },
        onSelectTab = viewModel::selectTab,
        onQueryChange = viewModel::setQuery,
        onOpenRecent = viewModel::openRecent,
        onRemoveRecent = viewModel::removeRecent,
        onOpenPdf = viewModel::openFile,
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                StorageAccess.settingsIntents(context).firstOrNull { intent ->
                    try { context.startActivity(intent); true } catch (e: ActivityNotFoundException) { false }
                }
            } else {
                legacyPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        },
        onDismissError = viewModel::dismissError,
        onOpenUpdateSettings = { showUpdateSettings = true },
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onOpenPicker: () -> Unit,
    onSelectTab: (HomeTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenRecent: (RecentItem) -> Unit,
    onRemoveRecent: (RecentItem) -> Unit,
    onOpenPdf: (PdfFile) -> Unit,
    onRequestPermission: () -> Unit,
    onDismissError: () -> Unit,
    onOpenUpdateSettings: () -> Unit = {},
    nowMillis: Long = System.currentTimeMillis(),
) {
    var pendingRemoval by remember { mutableStateOf<RecentItem?>(null) }

    Scaffold(
        topBar = { HomeTopBar(onOpenPicker, onOpenUpdateSettings) },
        bottomBar = { HomeBottomBar(state.tab, onSelectTab) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.tab) {
                HomeTab.RECENTS -> RecentsTab(
                    items = state.recents,
                    nowMillis = nowMillis,
                    onOpen = onOpenRecent,
                    onLongPress = { pendingRemoval = it },
                    onOpenPicker = onOpenPicker,
                )
                HomeTab.ALL ->
                    if (!state.hasFilesAccess) PermissionContent(onRequestPermission)
                    else AllPdfsTab(state.filteredPdfs, state.query, state.loadingAll, nowMillis, onQueryChange, onOpenPdf, onOpenPicker)
            }
            if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }

    pendingRemoval?.let { item ->
        RemoveDialog(
            onConfirm = { onRemoveRecent(item); pendingRemoval = null },
            onCancel = { pendingRemoval = null },
        )
    }
    state.error?.let { ErrorDialog(it, onDismissError) }
}

/**
 * Ação única "Abrir PDF" nas `actions` do `TopAppBar` — sem FAB (decisão 3 do usuário, spec §1.1a).
 * 48dp é o piso de toque do M3: o header é uma ação de apoio, não a ação principal da tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onOpenPicker: () -> Unit, onOpenUpdateSettings: () -> Unit = {}) {
    TopAppBar(
        title = {
            Text(
                "johnPDF",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            ThemeSwitchAction()
            IconButton(onClick = onOpenUpdateSettings, modifier = Modifier.testTag("update_settings")) {
                Icon(JohnIcons.Schedule, contentDescription = "Configurações de atualização", modifier = Modifier.size(24.dp))
            }
            SecondaryButton(
                text = "Abrir PDF",
                onClick = onOpenPicker,
                icon = JohnIcons.FolderOpen,
                height = 48.dp,
                modifier = Modifier.padding(end = SpaceS).testTag("open_pdf_header"),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

/**
 * Claro / escuro / seguir o sistema, num botão só.
 *
 * Um botão em vez de um menu porque são três opções e a Home é tela de leitura, não de
 * ajustes — e porque o ciclo fecha, então dá para voltar ao automático sem sair daqui.
 *
 * O ícone mostra o modo **atual**, não o próximo: é o que o usuário procura quando quer
 * conferir em que modo está. Mesma divisão do travar-rotação do leitor (spec §6.2): o
 * `contentDescription` nomeia a ação, o `stateDescription` é o que o TalkBack anuncia
 * depois do toque.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSwitchAction() {
    val controller = LocalThemeController.current
    val mode = controller.mode
    val (icon, estado) = when (mode) {
        ThemeMode.SYSTEM -> JohnIcons.BrightnessAuto to "Seguindo o sistema"
        ThemeMode.LIGHT -> JohnIcons.LightMode to "Tema claro"
        ThemeMode.DARK -> JohnIcons.DarkMode to "Tema escuro"
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(estado) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = controller::cycle,
            modifier = Modifier
                .size(48.dp)
                .semantics { stateDescription = estado }
                .testTag("theme_switch"),
        ) {
            Icon(icon, contentDescription = "Trocar o tema", modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * `contentDescription = null` é intencional: o rótulo textual ao lado já nomeia o item e
 * descrever o ícone duplicaria o anúncio do TalkBack (spec §8.1).
 */
@Composable
private fun HomeBottomBar(tab: HomeTab, onSelectTab: (HomeTab) -> Unit) {
    NavigationBar(
        containerColor = JohnTheme.barColor,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        NavigationBarItem(
            selected = tab == HomeTab.RECENTS,
            onClick = { onSelectTab(HomeTab.RECENTS) },
            icon = {
                Icon(
                    if (tab == HomeTab.RECENTS) JohnIcons.ScheduleFilled else JohnIcons.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).testTag("nav_icon_recents"),
                )
            },
            label = { Text("Recentes", style = MaterialTheme.typography.labelMedium) },
            alwaysShowLabel = true,
            colors = johnNavigationBarItemColors(),
        )
        NavigationBarItem(
            selected = tab == HomeTab.ALL,
            onClick = { onSelectTab(HomeTab.ALL) },
            icon = {
                Icon(
                    if (tab == HomeTab.ALL) JohnIcons.LibraryBooksFilled else JohnIcons.LibraryBooks,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).testTag("nav_icon_all"),
                )
            },
            label = { Text("Todos os PDFs", style = MaterialTheme.typography.labelMedium) },
            alwaysShowLabel = true,
            colors = johnNavigationBarItemColors(),
        )
    }
}

@Composable
private fun RecentsTab(
    items: List<RecentItem>,
    nowMillis: Long,
    onOpen: (RecentItem) -> Unit,
    onLongPress: (RecentItem) -> Unit,
    onOpenPicker: () -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(
            icon = JohnIcons.Schedule,
            title = "Nenhum PDF aberto ainda",
            message = "Os PDFs que você abrir vão aparecer aqui.",
            actionLabel = "Abrir PDF",
            onAction = onOpenPicker,
        )
        return
    }
    // Sem contentPadding inferior reservando espaço de FAB — a Home não tem FAB (spec §1.1a).
    LazyColumn(contentPadding = PaddingValues(vertical = SpaceS)) {
        itemsIndexed(items, key = { _, item -> item.path }) { index, item ->
            if (index > 0) ListDivider()
            PdfListItem(
                name = item.name,
                subtitle = "${item.origin.label} · ${friendlyDate(item.openedAt, nowMillis)}",
                onClick = { onOpen(item) },
                onLongClick = { onLongPress(item) },
            )
        }
    }
}

@Composable
private fun AllPdfsTab(
    pdfs: List<PdfFile>,
    query: String,
    loading: Boolean,
    nowMillis: Long,
    onQueryChange: (String) -> Unit,
    onOpen: (PdfFile) -> Unit,
    onOpenPicker: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        SearchField(query, onQueryChange)
        when {
            loading && pdfs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            pdfs.isEmpty() ->
                // Busca sem resultado não oferece ação: o caminho de saída é apagar a busca (spec §5.3).
                if (query.isBlank()) EmptyState(
                    icon = JohnIcons.FolderOpen,
                    title = "Nenhum PDF no celular",
                    message = "Nenhum PDF encontrado no celular.",
                    actionLabel = "Abrir PDF",
                    onAction = onOpenPicker,
                ) else EmptyState(
                    icon = JohnIcons.SearchOff,
                    title = "Nada encontrado",
                    message = "Nenhum PDF com esse nome.",
                )
            else -> LazyColumn(contentPadding = PaddingValues(vertical = SpaceS)) {
                itemsIndexed(pdfs, key = { _, pdf -> pdf.path }) { index, pdf ->
                    if (index > 0) ListDivider()
                    // Sem onLongClick: toque longo só em Recentes (spec §5.2).
                    PdfListItem(
                        name = pdf.name,
                        subtitle = "${pdf.origin.label} · ${friendlyDate(pdf.modifiedAt, nowMillis)}",
                        onClick = { onOpen(pdf) },
                    )
                }
            }
        }
    }
}

/** `verticalScroll` é necessário: com fontScale 2.0 em paisagem o bloco estoura a altura (spec §5.5). */
@Composable
private fun PermissionContent(onRequestPermission: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(SpaceXl).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                JohnIcons.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(SpaceXl))
        Text(
            "Para mostrar os PDFs do celular, o johnPDF precisa de permissão.",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = MaxActionWidth),
        )
        Spacer(Modifier.height(SpaceXl))
        Text(
            "1. Toque em Permitir acesso\n2. Ative a opção do johnPDF\n3. Volte para o app",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.widthIn(max = MaxActionWidth),
        )
        Spacer(Modifier.height(SpaceXl))
        PrimaryButton("Permitir acesso", onRequestPermission, Modifier.fillMaxWidth())
    }
}

/** Busca em pílula: `TextField` sem sublinhado, com lupa à esquerda e "x" só quando há texto. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar PDFs", style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(JohnIcons.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(48.dp)) {
                    Icon(JohnIcons.Close, contentDescription = "Limpar busca")
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = JohnTheme.dialogColor,
            unfocusedContainerColor = JohnTheme.dialogColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpaceL, vertical = SpaceS)
            .heightIn(min = PrimaryTouchTarget),
    )
}

/** `actionLabel`/`onAction` são opcionais: busca sem resultado não oferece saída falsa (spec §5.3). */
@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(SpaceXl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(SpaceL))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(SpaceS))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = MaxActionWidth),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(SpaceL))
            TextButton(onClick = onAction, modifier = Modifier.testTag("empty_state_action")) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Divisor recuado até onde o texto começa — alinha com o ícone de 40dp + folga do `ListItem`. */
@Composable
private fun ListDivider() = HorizontalDivider(
    Modifier.padding(start = ListItemMinHeight),
    color = MaterialTheme.colorScheme.outlineVariant,
)

/**
 * Linha de lista de 72dp inteira clicável. O "vermelho suave de PDF" da spec §5.2 tem papel
 * próprio (`JohnColors.pdfIcon`/`pdfIconContainer`, 4,92:1 no claro e 6,26:1 no escuro) em vez
 * de tomar emprestado o `errorContainer`: o ícone do arquivo não é uma mensagem de erro.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfListItem(name: String, subtitle: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(JohnTheme.colors.pdfIconContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    JohnIcons.PictureAsPdf,
                    contentDescription = null,
                    tint = JohnTheme.colors.pdfIcon,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ListItemMinHeight)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}
