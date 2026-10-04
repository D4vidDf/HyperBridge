package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.widget.*
import com.d4viddf.hyperbridge.ui.screens.translators.getTranslatorOutlinedIcon
import kotlinx.coroutines.launch

private const val CANVAS_WIDTH_DP = 350

/**
 * Studio Design (#328): KWGT-style element-by-element micro-widget builder for HyperOS islands.
 *
 * Implements:
 * - Material Expressive Theme scope
 * - KWGT-style breadcrumb hierarchy navigation (Root › Group › Element)
 * - Canvas floating vertical action toolbar (zoom, center, grid, wireframe)
 * - Dynamic category tabs (Items, Layer, Item, Container, Actions, Formulas)
 * - KWGT numeric steppers with fast +/- buttons and formula bindings
 * - Process-death resilient SavedState draft persistence with undo/redo stack
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDesignScreen(
    widgetId: String?,
    onBack: () -> Unit,
    studioViewModel: StudioViewModel = viewModel()
) {
    StudioExpressiveTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val translatorViewModel: com.d4viddf.hyperbridge.ui.screens.translators.TranslatorViewModel =
            viewModel()
        val allTranslators by translatorViewModel.allTranslators.collectAsState()

        val doc by studioViewModel.document.collectAsState()
        val savedDoc by studioViewModel.savedDocument.collectAsState()
        val selectedNodeId by studioViewModel.selectedNodeId.collectAsState()
        val boundTranslator by studioViewModel.boundTranslator.collectAsState()
        val notificationType by studioViewModel.notificationType.collectAsState()
        val targetScope by studioViewModel.targetScope.collectAsState()
        val targetPackages by studioViewModel.targetPackages.collectAsState()
        val validationMessage by studioViewModel.validationMessage.collectAsState()
        val canUndo by studioViewModel.canUndo.collectAsState()
        val canRedo by studioViewModel.canRedo.collectAsState()
        val isSaving by studioViewModel.isSaving.collectAsState()
        val hiddenNodeIds by studioViewModel.hiddenNodeIds.collectAsState()

        var showAddElement by remember { mutableStateOf(false) }
        var targetParentForAdd by remember { mutableStateOf<String?>(null) }
        var showDiscardDialog by remember { mutableStateOf(false) }

        // Canvas interactive states
        var zoom by remember { mutableFloatStateOf(1f) }
        var isGridVisible by remember { mutableStateOf(false) }
        var isWireframeMode by remember { mutableStateOf(false) }
        var selectedScenario by remember { mutableStateOf(StudioPreviewScenario.STANDARD) }

        // Category Tab State
        var selectedTab by remember { mutableStateOf(StudioTab.ITEMS) }

        LaunchedEffect(widgetId, allTranslators) {
            studioViewModel.loadWidget(widgetId, allTranslators)
        }

        val isDirty = doc != savedDoc
        val selectedNode: CustomWidgetNode? = selectedNodeId?.let { doc.findNode(it) }

        val availableTabs = remember(selectedNode) {
            StudioTab.tabsFor(selectedNode, isRoot = selectedNode == null || selectedNode.id == doc.root.id)
        }

        LaunchedEffect(availableTabs) {
            if (selectedTab !in availableTabs) {
                selectedTab = availableTabs.first()
            }
        }

        val handleBack = {
            if (isDirty) {
                showDiscardDialog = true
            } else {
                onBack()
            }
        }

        BackHandler(enabled = true) {
            handleBack()
        }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text(stringResource(R.string.studio_discard_dialog_title)) },
                text = { Text(stringResource(R.string.studio_discard_dialog_desc)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            onBack()
                        }
                    ) {
                        Text(
                            stringResource(R.string.studio_discard_dialog_confirm),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text(stringResource(R.string.studio_discard_dialog_keep_editing))
                    }
                }
            )
        }

        if (showAddElement) {
            AddElementScreen(
                onBack = { showAddElement = false },
                onPick = { element ->
                    val node = element.create()
                    val parentId = targetParentForAdd
                        ?: (selectedNode as? LayoutContainer)?.id
                        ?: (selectedNodeId?.let { doc.parentOf(it)?.id } ?: doc.root.id)
                    studioViewModel.addNode(parentId, node)
                    targetParentForAdd = null
                    showAddElement = false
                }
            )
            return@StudioExpressiveTheme
        }

        StudioDesignContent(
            doc = doc,
            selectedNodeId = selectedNodeId,
            selectedTab = selectedTab,
            availableTabs = availableTabs,
            selectedScenario = selectedScenario,
            hiddenNodeIds = hiddenNodeIds,
            zoom = zoom,
            isGridVisible = isGridVisible,
            isWireframeMode = isWireframeMode,
            canUndo = canUndo,
            canRedo = canRedo,
            isDirty = isDirty,
            isSaving = isSaving,
            notificationType = notificationType,
            targetScope = targetScope,
            targetPackages = targetPackages,
            boundTranslator = boundTranslator,
            validationMessage = validationMessage,
            onBack = handleBack,
            onUndo = { studioViewModel.undo() },
            onRedo = { studioViewModel.redo() },
            onExport = {
                scope.launch {
                    val file = studioViewModel.exportWidget()
                    if (file != null) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(intent, context.getString(R.string.studio_export))
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    }
                }
            },
            onRestore = { studioViewModel.restoreLastSaved() },
            onSave = {
                studioViewModel.save(
                    onSaveTranslator = { translator ->
                        translatorViewModel.saveTranslator(translator)
                    },
                    onSuccess = {
                        Toast.makeText(context, R.string.studio_saved, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onOpenAddElement = {
                targetParentForAdd = (selectedNode as? LayoutContainer)?.id
                    ?: (selectedNodeId?.let { doc.parentOf(it)?.id } ?: doc.root.id)
                showAddElement = true
            },
            onSelectNode = { studioViewModel.selectNode(it) },
            onMoveNode = { id, dx, dy ->
                studioViewModel.updateDocument { current ->
                    current.replaceNode(id) { node ->
                        node.withBounds(node.bounds.movedBy(dx, dy, CANVAS_WIDTH_DP, current.canvas.heightDp))
                    }
                }
            },
            onResizeNode = { id, width, height ->
                studioViewModel.updateDocument { current ->
                    current.replaceNode(id) { node ->
                        val clampedW = width.coerceIn(12, CANVAS_WIDTH_DP)
                        val clampedH = height.coerceIn(12, current.canvas.heightDp)
                        node.withBounds(node.bounds.copy(widthDp = clampedW, heightDp = clampedH))
                    }
                }
            },
            onZoomIn = { zoom = (zoom + 0.15f).coerceAtMost(2.5f) },
            onZoomOut = { zoom = (zoom - 0.15f).coerceAtLeast(0.5f) },
            onResetZoom = { zoom = 1f },
            onToggleGrid = { isGridVisible = !isGridVisible },
            onToggleWireframe = { isWireframeMode = !isWireframeMode },
            onScenarioSelected = { selectedScenario = it },
            onTabSelected = { selectedTab = it },
            onUpdateNode = { studioViewModel.updateNode(it) },
            onMoveLayer = { id, delta -> studioViewModel.moveLayer(id, delta) },
            onDeleteNode = { studioViewModel.removeNode(it) },
            onSelectChild = { studioViewModel.selectNode(it) },
            onAddChild = { parentId ->
                targetParentForAdd = parentId
                showAddElement = true
            },
            onNameChange = { newName ->
                studioViewModel.updateDocument { current ->
                    current.copy(meta = current.meta.copy(name = newName))
                }
            },
            onIconChange = { newIcon ->
                studioViewModel.updateDocument { current ->
                    current.copy(meta = current.meta.copy(icon = newIcon))
                }
            },
            onCanvasChange = { newCanvas ->
                studioViewModel.updateDocument { current ->
                    current.copy(canvas = newCanvas)
                }
            },
            onNotificationTypeChange = { studioViewModel.setNotificationType(it) },
            onTargetScopeChange = { studioViewModel.setTargetScope(it) },
            onAddTargetPackage = { studioViewModel.addTargetPackage(it) },
            onRemoveTargetPackage = { studioViewModel.removeTargetPackage(it) },
            onToggleVisibility = { studioViewModel.toggleEditorVisibility(it) },
            onMoveToFront = { studioViewModel.moveNodeToFront(it) },
            onMoveToBack = { studioViewModel.moveNodeToBack(it) },
            onDuplicate = { studioViewModel.duplicateNode(it) },
            onGroup = { studioViewModel.groupNode(it) },
            onUngroup = { studioViewModel.ungroupNode(it) },
            onRename = { id, newName -> studioViewModel.renameNode(id, newName) },
            onToggleLock = { studioViewModel.toggleLock(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDesignContent(
    doc: CustomWidgetDocument,
    selectedNodeId: String?,
    selectedTab: StudioTab,
    availableTabs: List<StudioTab>,
    selectedScenario: StudioPreviewScenario,
    hiddenNodeIds: Set<String>,
    zoom: Float,
    isGridVisible: Boolean,
    isWireframeMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    isDirty: Boolean,
    isSaving: Boolean,
    notificationType: NotificationType,
    targetScope: TargetScope = TargetScope.NOTIFICATION_TYPE,
    targetPackages: List<String> = emptyList(),
    boundTranslator: CustomTranslator? = null,
    validationMessage: String?,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onSave: () -> Unit,
    onOpenAddElement: () -> Unit,
    onSelectNode: (String?) -> Unit,
    onMoveNode: (String, Int, Int) -> Unit,
    onResizeNode: (String, Int, Int) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleWireframe: () -> Unit,
    onScenarioSelected: (StudioPreviewScenario) -> Unit,
    onTabSelected: (StudioTab) -> Unit,
    onUpdateNode: (CustomWidgetNode) -> Unit,
    onMoveLayer: (String, Int) -> Unit,
    onDeleteNode: (String) -> Unit,
    onSelectChild: (String) -> Unit,
    onAddChild: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit = {},
    onCanvasChange: (CanvasSize) -> Unit,
    onNotificationTypeChange: (NotificationType) -> Unit,
    onTargetScopeChange: (TargetScope) -> Unit = {},
    onAddTargetPackage: (String) -> Unit = {},
    onRemoveTargetPackage: (String) -> Unit = {},
    onToggleVisibility: (String) -> Unit,
    onMoveToFront: (String) -> Unit,
    onMoveToBack: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onGroup: (String) -> Unit,
    onUngroup: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onToggleLock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = getTranslatorOutlinedIcon(doc.meta.icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = doc.meta.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Rounded.IosShare, stringResource(R.string.studio_export))
                    }

                    IconButton(
                        onClick = onRestore,
                        enabled = isDirty
                    ) {
                        Icon(Icons.Rounded.Restore, stringResource(R.string.studio_restore))
                    }

                    IconButton(
                        onClick = onSave,
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Rounded.Save, stringResource(R.string.studio_save))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            StudioFloatingToolbar(
                zoom = zoom,
                isGridVisible = isGridVisible,
                isWireframeMode = isWireframeMode,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onResetZoom = onResetZoom,
                onToggleGrid = onToggleGrid,
                onToggleWireframe = onToggleWireframe,
                onOpenAddElement = onOpenAddElement
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Canvas preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    StudioCanvas(
                        root = doc.root,
                        canvasHeightDp = doc.canvas.heightDp,
                        selectedId = selectedNodeId,
                        onSelect = { onSelectNode(it) },
                        onMove = onMoveNode,
                        onResize = onResizeNode,
                        zoom = zoom,
                        isGridVisible = isGridVisible,
                        isWireframeMode = isWireframeMode,
                        hiddenNodeIds = hiddenNodeIds,
                        scenario = selectedScenario
                    )
                }

            validationMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // KWGT Breadcrumb Navigation
            StudioBreadcrumb(
                document = doc,
                selectedNodeId = selectedNodeId,
                onSelectNode = onSelectNode,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Category Tabs (Items, Layer, Item, Container, Actions, Formulas)
            StudioTabsRow(
                tabs = availableTabs,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Inspector Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                val targetNode = selectedNodeId?.let { doc.findNode(it) } ?: doc.root
                val siblings = doc.parentOf(targetNode.id)?.children.orEmpty()
                val siblingIndex = siblings.indexOfFirst { it.id == targetNode.id }

                StudioInspector(
                    node = targetNode,
                    isRoot = targetNode.id == doc.root.id,
                    selectedTab = selectedTab,
                    canMoveUp = siblingIndex in 0 until siblings.lastIndex,
                    canMoveDown = siblingIndex > 0,
                    onChange = onUpdateNode,
                    onMoveLayer = { delta -> onMoveLayer(targetNode.id, delta) },
                    onDelete = { onDeleteNode(targetNode.id) },
                    onSelectChild = onSelectChild,
                    onAddChild = onAddChild,
                    document = doc,
                    notificationType = notificationType,
                    targetScope = targetScope,
                    targetPackages = targetPackages,
                    boundTranslator = boundTranslator,
                    onNameChange = onNameChange,
                    onIconChange = onIconChange,
                    onCanvasChange = onCanvasChange,
                    onNotificationTypeChange = onNotificationTypeChange,
                    onTargetScopeChange = onTargetScopeChange,
                    onAddTargetPackage = onAddTargetPackage,
                    onRemoveTargetPackage = onRemoveTargetPackage,
                    selectedNodeId = selectedNodeId,
                    hiddenNodeIds = hiddenNodeIds,
                    onToggleVisibility = onToggleVisibility,
                    onMoveNodeLayer = onMoveLayer,
                    onMoveToFront = onMoveToFront,
                    onMoveToBack = onMoveToBack,
                    onDuplicate = onDuplicate,
                    onGroup = onGroup,
                    onUngroup = onUngroup,
                    onRename = onRename,
                    onDeleteNode = onDeleteNode,
                    onToggleNodeLock = onToggleLock,
                    scenario = selectedScenario
                )

                Spacer(Modifier.height(112.dp))
            }
        }
    }
}
}
