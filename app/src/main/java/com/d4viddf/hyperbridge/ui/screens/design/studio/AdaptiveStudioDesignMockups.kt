package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.FirstPage
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.NodeCondition
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.ShapeNode
import com.d4viddf.hyperbridge.models.widget.TextNode
import com.d4viddf.hyperbridge.models.widget.findNode
import com.d4viddf.hyperbridge.ui.screens.translators.getTranslatorOutlinedIcon
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

private const val LOGICAL_CANVAS_WIDTH_DP = 350f

/**
 * Unified Adaptive 3-Column Studio Mockup:
 * - Default widths: 1/4 Left Element Tree, 2/4 Center Canvas Stage, 1/4 Right Inspector Panel
 * - Draggable vertical resize handles between columns so users can make side columns narrower or wider
 * - Full-bleed pannable/zoomable Canvas stage rendered *behind* the two floating side panels so zoomed-in
 *   canvas content slides smoothly underneath the Left Tree and Right Inspector panels
 * - Scrollable tab row (`StudioTabsRow`) in the Right Inspector Panel
 */
@Composable
fun StudioUnifiedFloatingThreeColumnMockup(
    doc: CustomWidgetDocument = previewSampleWidgetDocument(),
    selectedNodeId: String = "track_title",
    selectedTab: StudioTab = StudioTab.INFO,
    zoom: Float = 1f,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
    isGridVisible: Boolean = true,
    isTreeVisible: Boolean = true
) {
    val targetNode = doc.findNode(selectedNodeId) ?: doc.root
    val isRoot = targetNode.id == doc.root.id
    val availableTabs = remember(targetNode, isRoot) {
        StudioTab.tabsFor(targetNode, isRoot).filter { it != StudioTab.ITEMS }
    }
    val activeTab = if (selectedTab in availableTabs) selectedTab else availableTabs.first()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                navigationIcon = {
                    FilledTonalIconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {},
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
                                contentDescription = "Toggle Element Tree",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Icon(
                            imageVector = getTranslatorOutlinedIcon(doc.meta.icon),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = doc.meta.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${doc.canvas.name} (350×${doc.canvas.heightDp} dp)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Restore, contentDescription = null)
                    }
                    FilledTonalButton(
                        onClick = {},
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val density = LocalDensity.current
            val totalWidthDp = maxWidth.value
            val totalHeightDp = maxHeight.value

            // Default column fractions: 1/4 Left Tree (0.25f), 2/4 Center Canvas (0.50f), 1/4 Right Inspector (0.25f)
            var leftFraction by remember { mutableFloatStateOf(0.25f) }
            var rightFraction by remember { mutableFloatStateOf(0.25f) }

            val effectiveLeftDp = if (isTreeVisible) (totalWidthDp * leftFraction).coerceIn(180f, totalWidthDp * 0.42f) else 0f
            val effectiveRightDp = (totalWidthDp * rightFraction).coerceIn(220f, totalWidthDp * 0.45f)
            val centerStageWidthDp = (totalWidthDp - effectiveLeftDp - effectiveRightDp).coerceAtLeast(200f)

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

            // LAYER 1 (BEHIND SIDE PANELS): Full-Bleed Pannable & Zoomable Proportional Canvas Stage
            // When zoomed in, the canvas extends behind the Left Tree and Right Inspector panels!
            val usableWidthDp = (centerStageWidthDp - 48f).coerceAtLeast(160f)
            val usableHeightDp = (totalHeightDp - 160f).coerceAtLeast(120f)
            val logicalW = LOGICAL_CANVAS_WIDTH_DP
            val logicalH = doc.canvas.heightDp.toFloat()

            val fitScale = min(usableWidthDp / logicalW, usableHeightDp / logicalH).coerceIn(0.5f, 1.85f)
            val effectiveScale = (fitScale * zoom).coerceIn(0.4f, 3.0f)

            // Bias the default center to the middle of the visible center stage
            val stageCenterOffsetXDp = (effectiveLeftDp - effectiveRightDp) / 2f

            Box(
                modifier = Modifier.fillMaxSize(),
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
                        text = "◄──────────── 350 dp ────────────►",
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
                                onSelect = {},
                                onMove = { _, _, _ -> },
                                onResize = { _, _, _ -> },
                                zoom = 1f,
                                isGridVisible = isGridVisible,
                                isWireframeMode = false,
                                scenario = StudioPreviewScenario.STANDARD,
                                globals = doc.globals
                            )
                        }
                    }

                    Text(
                        text = String.format(
                            Locale.US,
                            "Height: %d dp (%s) · Proportional %.2f×",
                            doc.canvas.heightDp,
                            doc.canvas.name,
                            effectiveScale
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            // LAYER 2 (FOREGROUND OVERLAY): Resizable Left Tree (1/4), Center Controls (2/4), Right Inspector (1/4)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. LEFT FLOATING PANEL (Default 1/4 width, resizable)
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
                            CollapsibleElementTreePanel(
                                doc = doc,
                                selectedNodeId = selectedNodeId,
                                onCollapseTree = {}
                            )
                        }

                        // Draggable Column Resize Handle (Left <-> Center)
                        ColumnResizeHandle(
                            onDragDeltaDp = { deltaDp ->
                                leftFraction = (leftFraction + deltaDp / totalWidthDp).coerceIn(0.16f, 0.40f)
                            }
                        )
                    }
                }

                // 2. MIDDLE STAGE OVERLAY (Default 2/4 width - Status Pills + Bottom Floating Toolbar)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val scalePercent = (effectiveScale * 100).roundToInt()
                    val toolbarScale = (centerStageWidthDp / 400f).coerceIn(0.84f, 1f)

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isTreeVisible) {
                                Surface(
                                    onClick = {},
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
                                            text = "Show Tree (${doc.root.children.size})",
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
                                        text = "350×${doc.canvas.heightDp} dp · $scalePercent%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenWith,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Drag to pan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
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
                            isWireframeMode = false,
                            canUndo = true,
                            canRedo = false,
                            onUndo = {},
                            onRedo = {},
                            onZoomIn = {},
                            onZoomOut = {},
                            onResetZoom = {},
                            onToggleGrid = {},
                            onToggleWireframe = {},
                            onOpenAddElement = {}
                        )
                    }
                }

                // Draggable Column Resize Handle (Center <-> Right)
                ColumnResizeHandle(
                    onDragDeltaDp = { deltaDp ->
                        rightFraction = (rightFraction - deltaDp / totalWidthDp).coerceIn(0.18f, 0.44f)
                    }
                )

                // 3. RIGHT FLOATING PANEL (Default 1/4 width, resizable, with Scrollable Tab Row)
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
                    InspectorFloatingPanelContent(
                        doc = doc,
                        targetNode = targetNode,
                        isRoot = isRoot,
                        availableTabs = availableTabs,
                        activeTab = activeTab,
                        selectedNodeId = selectedNodeId
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnResizeHandle(
    onDragDeltaDp: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .width(14.dp)
            .fillMaxHeight()
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

// =============================================================================
// SHARED BUILDING BLOCKS
// =============================================================================

@Composable
private fun InspectorFloatingPanelContent(
    doc: CustomWidgetDocument,
    targetNode: CustomWidgetNode,
    isRoot: Boolean,
    availableTabs: List<StudioTab>,
    activeTab: StudioTab,
    selectedNodeId: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        InspectorColumnHeader(targetNode = targetNode, isRoot = isRoot)

        // Horizontally Scrollable Row of Button/FilterChip Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableTabs.forEach { tab ->
                FilterChip(
                    selected = tab == activeTab,
                    onClick = {},
                    label = {
                        Text(
                            text = androidx.compose.ui.res.stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            StudioInspector(
                node = targetNode,
                isRoot = isRoot,
                selectedTab = activeTab,
                canMoveUp = true,
                canMoveDown = true,
                onChange = {},
                onMoveLayer = {},
                onDelete = {},
                document = doc,
                notificationType = NotificationType.MEDIA,
                selectedNodeId = selectedNodeId
            )
        }
    }
}

@Composable
private fun InspectorColumnHeader(
    targetNode: CustomWidgetNode,
    isRoot: Boolean
) {
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
                    text = if (isRoot) "Root Canvas" else (targetNode.name ?: targetNode.id),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isRoot) "Global & Canvas Properties" else targetNode::class.simpleName ?: "Element",
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
}

/**
 * Left Floating Element Tree Panel with a collapse/hide action button so the user can hide
 * the tree to maximize canvas space.
 */
@Composable
private fun CollapsibleElementTreePanel(
    doc: CustomWidgetDocument,
    selectedNodeId: String,
    onCollapseTree: () -> Unit
) {
    val isRootSelected = selectedNodeId == doc.root.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tree Header with Add + Collapse/Hide buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Element Tree",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${doc.root.children.size} elements",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {},
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Element",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onCollapseTree,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FirstPage,
                        contentDescription = "Hide Element Tree",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Root Canvas Node (Always selectable at the top of the tree!)
            Surface(
                selected = isRootSelected,
                onClick = {},
                shape = RoundedCornerShape(12.dp),
                color = if (isRootSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isRootSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Root (Canvas)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRootSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = "Background, Global, Design, Scope",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isRootSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Child Elements indented under Root
            doc.root.children.forEachIndexed { index, child ->
                val isSelected = child.id == selectedNodeId
                MockTreeElementRow(
                    node = child,
                    index = index,
                    isSelected = isSelected
                )
            }
        }
    }
}

@Composable
private fun MockTreeElementRow(
    node: CustomWidgetNode,
    index: Int,
    isSelected: Boolean
) {
    val icon = when (node) {
        is LayoutContainer -> Icons.Rounded.FolderOpen
        is TextNode -> Icons.Rounded.Title
        is ImageNode -> Icons.Rounded.Image
        is ProgressNode -> Icons.Rounded.LinearScale
        is ButtonNode -> Icons.Rounded.SmartButton
        is ShapeNode -> Icons.Rounded.Category
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            selected = isSelected,
            onClick = {},
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name ?: node.id,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#$index",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                        if (node.showIf !is NodeCondition.Always) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "cond",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                Icon(
                    imageVector = if (node.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 2.dp),
                    tint = contentColor.copy(alpha = 0.55f)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor.copy(alpha = 0.55f)
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// =============================================================================
// COMPOSE PREVIEWS (TABLET 1280x800 & FOLDABLE 840x800)
// =============================================================================

@Preview(
    name = "1. Default 1/4 - 2/4 - 1/4 Resizable Columns + Scrollable Tabs (Tablet 1280dp)",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800
)
@Composable
fun PreviewStudioUnifiedTabletTreeOpen() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioUnifiedFloatingThreeColumnMockup(
            selectedNodeId = "track_title",
            selectedTab = StudioTab.INFO,
            isTreeVisible = true
        )
    }
}

@Preview(
    name = "2. Zoomed In & Panned Canvas Behind Floating Panels (Tablet 1280dp)",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800
)
@Composable
fun PreviewStudioUnifiedTabletZoomedBehindPanels() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioUnifiedFloatingThreeColumnMockup(
            selectedNodeId = "action_btn",
            selectedTab = StudioTab.BUTTON,
            zoom = 1.65f,
            panOffsetX = -140f,
            panOffsetY = 30f,
            isTreeVisible = true
        )
    }
}

@Preview(
    name = "3. Default 1/4 - 2/4 - 1/4 Resizable Columns + Scrollable Tabs (Foldable 840dp)",
    showBackground = true,
    widthDp = 840,
    heightDp = 800
)
@Composable
fun PreviewStudioUnifiedFoldableTreeOpen() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioUnifiedFloatingThreeColumnMockup(
            selectedNodeId = "action_btn",
            selectedTab = StudioTab.BUTTON,
            isTreeVisible = true
        )
    }
}

@Preview(
    name = "4. Left Tree Hidden -> Expanded Canvas Stage (Foldable 840dp)",
    showBackground = true,
    widthDp = 840,
    heightDp = 800
)
@Composable
fun PreviewStudioUnifiedFoldableTreeHidden() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioUnifiedFloatingThreeColumnMockup(
            selectedNodeId = "action_btn",
            selectedTab = StudioTab.BUTTON,
            isTreeVisible = false
        )
    }
}
