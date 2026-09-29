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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.data.StorageAccess
import com.johngabie.johnpdf.ui.common.BigButton
import com.johngabie.johnpdf.ui.common.ConfirmDialog
import com.johngabie.johnpdf.ui.common.ErrorDialog
import com.johngabie.johnpdf.ui.common.PrimaryButton
import com.johngabie.johnpdf.ui.common.SecondaryButton
import com.johngabie.johnpdf.ui.icons.JohnIcons
import com.johngabie.johnpdf.ui.theme.MaxActionWidth
import com.johngabie.johnpdf.ui.theme.SpaceL
import com.johngabie.johnpdf.ui.theme.SpaceS
import com.johngabie.johnpdf.ui.theme.SpaceXl
import com.johngabie.johnpdf.ui.theme.SpaceXxl
import com.johngabie.johnpdf.util.friendlyDate

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
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
    nowMillis: Long = System.currentTimeMillis(),
) {
    var pendingRemoval by remember { mutableStateOf<RecentItem?>(null) }

    Scaffold(
        topBar = { HomeTopBar(onOpenPicker) },
        bottomBar = { HomeBottomBar(state.tab, onSelectTab) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.tab) {
                HomeTab.RECENTS -> RecentsTab(state.recents, nowMillis, onOpenRecent, onLongPress = { pendingRemoval = it })
                HomeTab.ALL ->
                    if (!state.hasFilesAccess) PermissionContent(onRequestPermission)
                    else AllPdfsTab(state.filteredPdfs, state.query, state.loadingAll, nowMillis, onQueryChange, onOpenPdf)
            }
            if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }

    pendingRemoval?.let { item ->
        ConfirmDialog(
            "Remover da lista?",
            onYes = { onRemoveRecent(item); pendingRemoval = null },
            onNo = { pendingRemoval = null },
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
private fun HomeTopBar(onOpenPicker: () -> Unit) {
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
 * `contentDescription = null` é intencional: o rótulo textual ao lado já nomeia o item e
 * descrever o ícone duplicaria o anúncio do TalkBack (spec §8.1).
 */
@Composable
private fun HomeBottomBar(tab: HomeTab, onSelectTab: (HomeTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
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
        )
    }
}

@Composable
private fun RecentsTab(items: List<RecentItem>, nowMillis: Long, onOpen: (RecentItem) -> Unit, onLongPress: (RecentItem) -> Unit) {
    if (items.isEmpty()) {
        CenteredMessage("Os PDFs que você abrir vão aparecer aqui.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.path }) { item ->
            PdfCard(
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
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("🔍 Buscar pelo nome…", style = MaterialTheme.typography.bodyLarge) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 64.dp),
        )
        when {
            loading && pdfs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            pdfs.isEmpty() -> CenteredMessage(if (query.isBlank()) "Nenhum PDF encontrado no celular." else "Nenhum PDF com esse nome.")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(pdfs, key = { it.path }) { pdf ->
                    PdfCard(
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

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfCard(name: String, subtitle: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📄", fontSize = 32.sp, modifier = Modifier.padding(end = 16.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
