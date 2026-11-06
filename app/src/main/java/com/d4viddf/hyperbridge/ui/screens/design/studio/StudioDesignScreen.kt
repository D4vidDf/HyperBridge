package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.widget.CanvasSize
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.CustomWidgetGlobals
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.TextFontFamily
import com.d4viddf.hyperbridge.models.widget.adaptedContentHeight
import com.d4viddf.hyperbridge.models.widget.adaptedContentWidth
import com.d4viddf.hyperbridge.models.widget.findNode
import com.d4viddf.hyperbridge.models.widget.parentOf
import com.d4viddf.hyperbridge.models.widget.replaceNode
import com.d4viddf.hyperbridge.models.widget.withBounds
import com.d4viddf.hyperbridge.ui.screens.translators.getTranslatorOutlinedIcon
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private const val CANVAS_WIDTH_DP = 350

/**
 * Studio Design (#328): KWGT-style element-by-element micro-widget builder for HyperOS islands.
 *
 * Implements:
 * - Material Expressive Theme scope
 * - Adaptive 3-Column Floating Layout for tablets and foldables (`maxWidth >= 600.dp`) with:
 *   1. Resizable Collapsible Left Element Tree Panel (default 1/4 width)
 *   2. Center Proportionally Scaled & Pannable Canvas (`350×heightDp`, default 2/4 width) rendered
 *      behind the floating side panels so zoomed-in canvas content slides smoothly underneath
 *   3. Resizable Right Floating Inspector Panel (default 1/4 width) with scrollable `StudioTabsRow`
 * - KWGT-style breadcrumb hierarchy navigation (Root › Group › Element) on compact screens
 * - Dynamic category tabs (Items, Layer, Item, Container, Actions, Formulas)
 * - KWGT numeric steppers with fast +/- buttons and formula bindings
 * - Process-death resilient SavedState draft persistence with undo/redo stack
 */
@Composable
fun StudioDesignScreen(
    widgetId: String?,
    onBack: () -> Unit,
    studioViewModel: StudioViewModel = viewModel()
) {
    StudioExpressiveTheme {
        val context = LocalContext.current
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
                if (widgetId == null) {
                    studioViewModel.createNewDocument()
                }
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
                            if (widgetId == null) {
                                studioViewModel.createNewDocument()
                            }
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
                    val movedDoc = current.replaceNode(id) { node ->
                        node.withBounds(node.bounds.movedBy(dx, dy, CANVAS_WIDTH_DP, current.canvas.heightDp))
                    }
                    val parent = current.parentOf(id)
                    if (parent != null && parent.id != current.root.id) {
                        movedDoc.replaceNode(parent.id) { p ->
                            if (p is LayoutContainer) {
                                p.copy(
                                    bounds = p.bounds.copy(
                                        widthDp = p.adaptedContentWidth() ?: p.bounds.widthDp,
                                        heightDp = p.adaptedContentHeight() ?: p.bounds.heightDp
                                    )
                                )
                            } else p
                        }
                    } else movedDoc
                }
            },
            onResizeNode = { id, width, height ->
                studioViewModel.updateDocument { current ->
                    val clampedW = width.coerceIn(12, CANVAS_WIDTH_DP)
                    val clampedH = height.coerceIn(12, current.canvas.heightDp)
                    val resizedDoc = current.replaceNode(id) { node ->
                        node.withBounds(node.bounds.copy(widthDp = clampedW, heightDp = clampedH))
                    }
                    val parent = current.parentOf(id)
                    if (parent != null && parent.id != current.root.id) {
                        resizedDoc.replaceNode(parent.id) { p ->
                            if (p is LayoutContainer) {
                                p.copy(
                                    bounds = p.bounds.copy(
                                        widthDp = p.adaptedContentWidth() ?: p.bounds.widthDp,
                                        heightDp = p.adaptedContentHeight() ?: p.bounds.heightDp
                                    )
                                )
                            } else p
                        }
                    } else resizedDoc
                }
            },
            onZoomIn = { zoom = (zoom + 0.15f).coerceAtMost(2.5f) },
            onZoomOut = { zoom = (zoom - 0.15f).coerceAtLeast(0.5f) },
            onResetZoom = { zoom = 1f },
            onToggleGrid = { isGridVisible = !isGridVisible },
            onToggleWireframe = { isWireframeMode = !isWireframeMode },
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
            onToggleLock = { studioViewModel.toggleLock(it) },
            onMoveInto = { fromId, targetId -> studioViewModel.moveInto(fromId, targetId) },
            onCreateTranslator = { studioViewModel.createTranslatorForDesign() },
            onUpdateGlobals = { studioViewModel.updateGlobals(it) },
            onApplyFontToAll = { studioViewModel.applyFontToAllTextNodes(it) }
        )
    }
}

@Composable
fun StudioDesignContent(
    modifier: Modifier = Modifier,
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
    onMoveInto: (String, String) -> Unit = { _, _ -> },
    onCreateTranslator: (() -> Unit)? = null,
    onUpdateGlobals: ((CustomWidgetGlobals) -> Unit)? = null,
    onApplyFontToAll: ((TextFontFamily) -> Unit)? = null
) {
    var isTreeVisible by rememberSaveable { mutableStateOf(true) }
    // Default 1/4 width for Left Tree (0.25f), 2/4 for Center Canvas (0.50f), 1/4 for Right Inspector (0.25f)
    var leftFraction by rememberSaveable { mutableFloatStateOf(0.25f) }
    var rightFraction by rememberSaveable { mutableFloatStateOf(0.25f) }

    // Canvas pan offset (in px) so users can drag the canvas around when zoomed in
    var panOffsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var panOffsetY by rememberSaveable { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = if (isExpandedScreen) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.background
            },
            topBar = {
                TopAppBar(
                    colors = if (isExpandedScreen) {
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    } else {
                        TopAppBarDefaults.topAppBarColors()
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isExpandedScreen) {
                                FilledTonalIconButton(
                                    onClick = { isTreeVisible = !isTreeVisible },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (isTreeVisible) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Layers,
                                        contentDescription = stringResource(R.string.studio_element_tree_toggle_cd),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
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
                            if (isExpandedScreen) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "${doc.canvas.name} ($CANVAS_WIDTH_DP×${doc.canvas.heightDp} dp)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        FilledTonalIconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
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
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                if (!isExpandedScreen) {
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
                        onResetZoom = {
                            panOffsetX = 0f
                            panOffsetY = 0f
                            onResetZoom()
                        },
                        onToggleGrid = onToggleGrid,
                        onToggleWireframe = onToggleWireframe,
                        onOpenAddElement = onOpenAddElement
                    )
                }
            }
        ) { padding ->
            if (isExpandedScreen) {
                val targetNode = selectedNodeId?.let { doc.findNode(it) } ?: doc.root
                val isRoot = targetNode.id == doc.root.id
                val siblings = doc.parentOf(targetNode.id)?.children.orEmpty()
                val siblingIndex = siblings.indexOfFirst { it.id == targetNode.id }

                // In 3-column mode, Element Tree has its own dedicated left panel, so exclude ITEMS from the right inspector
                val inspectorTabs = remember(availableTabs) {
                    availableTabs.filter { it != StudioTab.ITEMS }.ifEmpty { availableTabs }
                }
                val activeInspectorTab = if (selectedTab in inspectorTabs) selectedTab else inspectorTabs.first()

                BoxWithConstraints(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    val density = LocalDensity.current
                    val totalWidthDp = maxWidth.value
                    val totalHeightDp = maxHeight.value

                    val effectiveLeftDp = if (isTreeVisible) {
                        (totalWidthDp * leftFraction).coerceIn(180f, totalWidthDp * 0.42f)
                    } else {
                        0f
                    }
                    val effectiveRightDp = (totalWidthDp * rightFraction).coerceIn(220f, totalWidthDp * 0.45f)
                    val centerStageWidthDp = (totalWidthDp - effectiveLeftDp - effectiveRightDp).coerceAtLeast(200f)

                    val usableWidthDp = (centerStageWidthDp - 48f).coerceAtLeast(160f)
                    val usableHeightDp = (totalHeightDp - 160f).coerceAtLeast(120f)

                    val logicalW = CANVAS_WIDTH_DP.toFloat()
                    val logicalH = doc.canvas.heightDp.toFloat()

                    // Uniform proportional scale: fits 350 x canvas.heightDp inside the middle column without distortion
                    val fitScale = min(usableWidthDp / logicalW, usableHeightDp / logicalH).coerceIn(0.5f, 1.85f)
                    val effectiveScale = (fitScale * zoom).coerceIn(0.4f, 3.0f)
                    val scalePercent = (effectiveScale * 100).roundToInt()

                    // Center the canvas inside the middle stage even when Left and Right columns have different widths
                    val stageCenterOffsetXDp = (effectiveLeftDp - effectiveRightDp) / 2f

                    // Continuous Pro-Studio Dot-Grid Backdrop across the entire workspace
                    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.13f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val spacing = 20.dp.toPx()
                        var x = spacing
                        while (x < size.width) {
                            var y = spacing
                            while (y < size.height) {
                                drawCircle(color = dotColor, radius = 1.5.dp.toPx(), center = Offset(x, y))
                                y += spacing
                            }
                            x += spacing
                        }
                    }

                    // LAYER 1 (BEHIND THE TWO FLOATING PANELS): Full-bleed Pannable & Zoomable Canvas Stage
                    // Because this Box is rendered before the Row of floating panels, zooming in or panning
                    // allows the canvas to slide cleanly behind the Left Tree and Right Inspector panels!
                    val isElementSelectedForDrag = selectedNodeId != null && selectedNodeId != doc.root.id && !targetNode.locked

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isElementSelectedForDrag) {
                                if (!isElementSelectedForDrag) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        panOffsetX += dragAmount.x
                                        panOffsetY += dragAmount.y
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(bottom = 28.dp)
                                .graphicsLayer {
                                    translationX = with(density) { stageCenterOffsetXDp.dp.toPx() } + panOffsetX
                                    translationY = panOffsetY
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "◄──────────── $CANVAS_WIDTH_DP dp ────────────►",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )

                            Box(
                                modifier = Modifier
                                    .width((logicalW * effectiveScale).dp)
                                    .height((logicalH * effectiveScale).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(logicalW.dp)
                                        .height(logicalH.dp)
                                        .graphicsLayer {
                                            scaleX = effectiveScale
                                            scaleY = effectiveScale
                                        }
                                ) {
                                    StudioCanvas(
                                        root = doc.root,
                                        canvasWidthDp = logicalW,
                                        canvasHeightDp = doc.canvas.heightDp,
                                        selectedId = selectedNodeId,
                                        onSelect = { onSelectNode(it) },
                                        onMove = onMoveNode,
                                        onResize = onResizeNode,
                                        zoom = 1f,
                                        isGridVisible = isGridVisible,
                                        isWireframeMode = isWireframeMode,
                                        hiddenNodeIds = hiddenNodeIds,
                                        scenario = selectedScenario,
                                        globals = doc.globals
                                    )
                                }
                            }

                            Text(
                                text = stringResource(
                                    R.string.studio_ruler_height_fmt,
                                    doc.canvas.heightDp,
                                    doc.canvas.name,
                                    effectiveScale
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // LAYER 2 (ON TOP OF CANVAS): Resizable Left Tree (1/4), Center Controls (2/4), Right Inspector (1/4)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. LEFT FLOATING PANEL: Collapsible & Resizable Element Tree Hierarchy (Default 1/4 width)
                        AnimatedVisibility(visible = isTreeVisible) {
                            Row(
                                modifier = Modifier.fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .width(effectiveLeftDp.dp)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f),
                                    tonalElevation = 4.dp,
                                    shadowElevation = 6.dp,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    StudioLayersPanel(
                                        rootContainer = doc.root,
                                        selectedNodeId = selectedNodeId,
                                        hiddenNodeIds = hiddenNodeIds,
                                        onSelectNode = { onSelectNode(it) },
                                        onToggleLock = onToggleLock,
                                        onToggleVisibility = onToggleVisibility,
                                        onMoveLayer = onMoveLayer,
                                        onMoveToFront = onMoveToFront,
                                        onMoveToBack = onMoveToBack,
                                        onDuplicate = onDuplicate,
                                        onGroup = onGroup,
                                        onUngroup = onUngroup,
                                        onRename = onRename,
                                        onDelete = onDeleteNode,
                                        onAddElement = onAddChild,
                                        onMoveInto = onMoveInto,
                                        isStandalonePanel = true,
                                        onCollapsePanel = { isTreeVisible = false }
                                    )
                                }

                                // Draggable Column Resize Handle (Left Tree <-> Center Stage)
                                StudioColumnResizeHandle(
                                    onDragDeltaDp = { deltaDp ->
                                        leftFraction = (leftFraction + deltaDp / totalWidthDp).coerceIn(0.16f, 0.40f)
                                    },
                                    onDoubleTapReset = { leftFraction = 0.25f }
                                )
                            }
                        }

                        // 2. MIDDLE STAGE OVERLAY (Default 2/4 width): Top Status Pills + Bottom Floating Toolbar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            val toolbarScale = (centerStageWidthDp / 400f).coerceIn(0.84f, 1f)
                            val isPanned = abs(panOffsetX) > 2f || abs(panOffsetY) > 2f

                            // Top Stage Header: Show Tree Pill (when collapsed) + Aspect Ratio Indicator + Recenter Pill + Validation
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!isTreeVisible) {
                                            Surface(
                                                onClick = { isTreeVisible = true },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Layers,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = stringResource(
                                                            R.string.studio_element_tree_show_fmt,
                                                            doc.root.children.size
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            tonalElevation = 2.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AspectRatio,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "${CANVAS_WIDTH_DP}×${doc.canvas.heightDp} dp · $scalePercent%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        if (isPanned || zoom != 1f) {
                                            Surface(
                                                onClick = {
                                                    panOffsetX = 0f
                                                    panOffsetY = 0f
                                                    onResetZoom()
                                                },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.CenterFocusStrong,
                                                        contentDescription = stringResource(R.string.studio_toolbar_zoom_reset),
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.studio_toolbar_zoom_reset),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                validationMessage?.let { message ->
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }

                            // Bottom Floating Horizontal Toolbar kept in the middle column at the bottom
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                                    .graphicsLayer {
                                        scaleX = toolbarScale
                                        scaleY = toolbarScale
                                    }
                            ) {
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
                                    onResetZoom = {
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                        onResetZoom()
                                    },
                                    onToggleGrid = onToggleGrid,
                                    onToggleWireframe = onToggleWireframe,
                                    onOpenAddElement = onOpenAddElement
                                )
                            }
                        }

                        // Draggable Column Resize Handle (Center Stage <-> Right Inspector)
                        StudioColumnResizeHandle(
                            onDragDeltaDp = { deltaDp ->
                                rightFraction = (rightFraction - deltaDp / totalWidthDp).coerceIn(0.18f, 0.44f)
                            },
                            onDoubleTapReset = { rightFraction = 0.25f }
                        )

                        // 3. RIGHT FLOATING PANEL: Resizable Inspector Tabs & Options (Default 1/4 width)
                        Surface(
                            modifier = Modifier
                                .width(effectiveRightDp.dp)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.96f),
                            tonalElevation = 4.dp,
                            shadowElevation = 6.dp,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Sticky Inspector Node Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isRoot) {
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isRoot) Icons.Rounded.Widgets else Icons.Rounded.Tune,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (isRoot) {
                                                        MaterialTheme.colorScheme.onTertiaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    }
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = if (isRoot) {
                                                    stringResource(R.string.studio_root_canvas_title)
                                                } else {
                                                    targetNode.name ?: targetNode.id
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isRoot) {
                                                    stringResource(R.string.studio_root_canvas_subtitle)
                                                } else {
                                                    targetNode::class.simpleName ?: ""
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Text(
                                            text = if (isRoot) "ROOT" else "#${targetNode.id}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Horizontally Scrollable Row of Button/FilterChip Tabs
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    inspectorTabs.forEach { tab ->
                                        FilterChip(
                                            selected = tab == activeInspectorTab,
                                            onClick = { onTabSelected(tab) },
                                            label = {
                                                Text(
                                                    text = stringResource(tab.labelRes),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Scrollable Inspector Options
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Spacer(Modifier.height(4.dp))

                                    StudioInspector(
                                        node = targetNode,
                                        isRoot = isRoot,
                                        selectedTab = activeInspectorTab,
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
                                        scenario = selectedScenario,
                                        onMoveInto = onMoveInto,
                                        onCreateTranslator = onCreateTranslator,
                                        onUpdateGlobals = onUpdateGlobals,
                                        onApplyFontToAll = onApplyFontToAll
                                    )

                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                // Compact / Phone single-column layout
                val targetNode = selectedNodeId?.let { doc.findNode(it) } ?: doc.root
                val isElementSelectedForDrag = selectedNodeId != null && selectedNodeId != doc.root.id && !targetNode.locked

                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Canvas preview (proportionally scaled to fit available width and pannable when zoomed)
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .pointerInput(isElementSelectedForDrag) {
                                    if (!isElementSelectedForDrag) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            panOffsetX += dragAmount.x
                                            panOffsetY += dragAmount.y
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val availableWidthDp = maxWidth.value
                            val logicalW = CANVAS_WIDTH_DP.toFloat()
                            val logicalH = doc.canvas.heightDp.toFloat()
                            val fitScale = (availableWidthDp / logicalW).coerceIn(0.5f, 1.5f)
                            val effectiveScale = (fitScale * zoom).coerceIn(0.4f, 2.5f)

                            Box(
                                modifier = Modifier
                                    .width((logicalW * effectiveScale).dp)
                                    .height((logicalH * effectiveScale).dp)
                                    .graphicsLayer {
                                        translationX = panOffsetX
                                        translationY = panOffsetY
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(logicalW.dp)
                                        .height(logicalH.dp)
                                        .graphicsLayer {
                                            scaleX = effectiveScale
                                            scaleY = effectiveScale
                                        }
                                ) {
                                    StudioCanvas(
                                        root = doc.root,
                                        canvasWidthDp = logicalW,
                                        canvasHeightDp = doc.canvas.heightDp,
                                        selectedId = selectedNodeId,
                                        onSelect = { onSelectNode(it) },
                                        onMove = onMoveNode,
                                        onResize = onResizeNode,
                                        zoom = 1f,
                                        isGridVisible = isGridVisible,
                                        isWireframeMode = isWireframeMode,
                                        hiddenNodeIds = hiddenNodeIds,
                                        scenario = selectedScenario,
                                        globals = doc.globals
                                    )
                                }
                            }
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
                                scenario = selectedScenario,
                                onMoveInto = onMoveInto,
                                onCreateTranslator = onCreateTranslator,
                                onUpdateGlobals = onUpdateGlobals,
                                onApplyFontToAll = onApplyFontToAll
                            )

                            Spacer(Modifier.height(112.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draggable vertical divider handle between floating columns so the user can adjust the widths
 * of the Left Element Tree and Right Inspector panels.
 */
@Composable
private fun StudioColumnResizeHandle(
    modifier: Modifier = Modifier,
    onDragDeltaDp: (Float) -> Unit,
    onDoubleTapReset: () -> Unit = {}
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .width(14.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onDoubleTapReset() })
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaDp = with(density) { dragAmount.x.toDp().value }
                    onDragDeltaDp(deltaDp)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        )
    }
}

