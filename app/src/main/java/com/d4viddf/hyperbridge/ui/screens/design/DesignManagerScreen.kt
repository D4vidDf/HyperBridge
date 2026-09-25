package com.d4viddf.hyperbridge.ui.screens.design

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.IslandTemplateCatalog
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.ui.components.island.HyperOsIslandPreview
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import com.d4viddf.hyperbridge.ui.screens.translators.TranslatorCardItem
import com.d4viddf.hyperbridge.ui.screens.translators.TranslatorViewModel
import com.d4viddf.hyperbridge.ui.screens.translators.getTranslatorOutlinedIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignManagerScreen(
    onBack: () -> Unit,
    onAddDesign: () -> Unit,
    onEditDesign: (id: String) -> Unit,
    viewModel: TranslatorViewModel = viewModel()
) {
    val allTranslators by viewModel.allTranslators.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var showPreviewView by remember { mutableStateOf(true) }
    var pendingExportTranslator by remember { mutableStateOf<CustomTranslator?>(null) }

    // SAF Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importTranslator(uri) { result ->
                if (result.isSuccess) {
                    val imported = result.getOrNull()
                    val msg = context.getString(R.string.translators_import_success, imported?.meta?.name ?: "")
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                } else {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                    val msg = context.getString(R.string.translators_import_failed, errorMsg)
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        }
    }

    // SAF Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val translator = pendingExportTranslator
        if (uri != null && translator != null) {
            viewModel.exportTranslatorToUri(translator, uri) { result ->
                if (result.isSuccess) {
                    val msg = context.getString(R.string.translators_export_success)
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                } else {
                    val msg = context.getString(R.string.translators_export_failed)
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
                pendingExportTranslator = null
            }
        } else {
            pendingExportTranslator = null
        }
    }

    val designs = remember(allTranslators) {
        allTranslators.filter {
            it.presentation.mode == PresentationMode.TEMPLATE ||
                    it.presentation.mode == PresentationMode.WIDGET
        }
    }

    val filteredDesigns = remember(designs, searchQuery) {
        designs.filter { design ->
            searchQuery.isBlank() ||
                    design.meta.name.contains(searchQuery, ignoreCase = true) ||
                    design.meta.description.contains(searchQuery, ignoreCase = true) ||
                    design.targetPackages.any { it.contains(searchQuery, ignoreCase = true) } ||
                    design.targetNotificationTypes.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.design_manager_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.design_manager_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Toggle between Island Preview and Icon view
                    FilledTonalIconButton(
                        onClick = { showPreviewView = !showPreviewView },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (showPreviewView) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = if (showPreviewView) Icons.Outlined.Preview else Icons.Outlined.Widgets,
                            contentDescription = stringResource(R.string.design_toggle_preview_cd),
                            tint = if (showPreviewView) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Import SAF
                    FilledTonalIconButton(
                        onClick = {
                            importLauncher.launch(arrayOf("*/*", "application/zip", "application/octet-stream", "application/json"))
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = stringResource(R.string.translators_import_button)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDesign,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.design_add_design)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                            FilledTonalIconButton(
                                onClick = { searchQuery = "" },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filteredDesigns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.DashboardCustomize,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.design_designs_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = stringResource(R.string.design_designs_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Button(
                            onClick = onAddDesign,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.design_add_design))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredDesigns, key = { _, item -> item.id }) { index, design ->
                        val shape = getExpressiveShape(filteredDesigns.size, index, ShapeStyle.Large)

                        if (showPreviewView) {
                            DesignPreviewCardItem(
                                design = design,
                                shape = shape,
                                onToggle = { isEnabled -> viewModel.toggleTranslator(design.id, isEnabled) },
                                onClick = { onEditDesign(design.id) },
                                onDuplicate = { viewModel.duplicateTranslator(design) },
                                onDelete = { viewModel.deleteTranslator(design.id) },
                                onShare = {
                                    viewModel.shareTranslator(context, design) { result ->
                                        if (result.isFailure) {
                                            val msg = context.getString(R.string.translators_share_failed)
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    }
                                },
                                onExport = {
                                    pendingExportTranslator = design
                                    val filename = "${design.meta.name.ifBlank { design.id }}.htrans"
                                    exportLauncher.launch(filename)
                                }
                            )
                        } else {
                            TranslatorCardItem(
                                translator = design,
                                shape = shape,
                                onToggle = { isEnabled -> viewModel.toggleTranslator(design.id, isEnabled) },
                                onClick = { onEditDesign(design.id) },
                                onDuplicate = { viewModel.duplicateTranslator(design) },
                                onDelete = { viewModel.deleteTranslator(design.id) },
                                onShare = {
                                    viewModel.shareTranslator(context, design) { result ->
                                        if (result.isFailure) {
                                            val msg = context.getString(R.string.translators_share_failed)
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    }
                                },
                                onExport = {
                                    pendingExportTranslator = design
                                    val filename = "${design.meta.name.ifBlank { design.id }}.htrans"
                                    exportLauncher.launch(filename)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesignPreviewCardItem(
    design: CustomTranslator,
    shape: androidx.compose.ui.graphics.Shape,
    isChecked: Boolean = design.isEnabled,
    showPreview: Boolean = true,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onDelete: () -> Unit = {},
    onShare: () -> Unit = {},
    onExport: () -> Unit = {}
) {
    val template = IslandTemplateCatalog.find(design.presentation.templateId)

    Surface(
        onClick = onClick,
        shape = shape,
        color = if (isChecked) MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Title, template name, switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isChecked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getTranslatorOutlinedIcon(design.meta.iconName),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = design.meta.name.ifBlank { stringResource(R.string.design_section_designs) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isChecked) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = template?.let { stringResource(it.nameRes) }
                            ?: stringResource(R.string.translator_pres_mode_widget),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Switch(
                    checked = isChecked,
                    onCheckedChange = onToggle
                )
            }

            // Live Island Preview (conditional)
            if (showPreview) {
                HyperOsIslandPreview(
                    translator = design,
                    showChrome = false
                )
            }

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Scope badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (design.targetScope) {
                        TargetScope.GLOBAL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                        TargetScope.SYSTEM_APPS -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = when (design.targetScope) {
                            TargetScope.GLOBAL -> stringResource(R.string.translators_scope_global)
                            TargetScope.SPECIFIC_APPS -> stringResource(R.string.translators_scope_apps, design.targetPackages.size)
                            TargetScope.SYSTEM_APPS -> stringResource(R.string.translators_scope_system_apps, design.targetPackages.size)
                            TargetScope.NOTIFICATION_TYPE -> stringResource(R.string.translators_scope_types, design.targetNotificationTypes.size)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (design.targetScope) {
                            TargetScope.GLOBAL -> MaterialTheme.colorScheme.primary
                            TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.secondary
                            TargetScope.SYSTEM_APPS -> MaterialTheme.colorScheme.error
                            TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.translators_share_button),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onExport,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.translators_export_button),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.translators_action_duplicate),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onClick,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.translators_action_edit),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.translators_action_delete),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
