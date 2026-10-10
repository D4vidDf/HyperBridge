package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.widget.ButtonAction
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.CanvasSize
import com.d4viddf.hyperbridge.models.widget.ContainerLayout
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.CustomWidgetMetadata
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.ImageSource
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.NodeBounds
import com.d4viddf.hyperbridge.models.widget.NodeCondition
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.ShapeNode
import com.d4viddf.hyperbridge.models.widget.TextNode

/**
 * Creates a sample KWGT-style micro-widget document for Compose Previews.
 */
fun previewSampleWidgetDocument(): CustomWidgetDocument {
    return CustomWidgetDocument(
        id = "preview_island_widget",
        meta = CustomWidgetMetadata(
            name = "HyperOS Music & Battery Island",
            author = "HyperBridge"
        ),
        canvas = CanvasSize.MEDIUM,
        root = LayoutContainer(
            id = "root",
            layout = ContainerLayout.ABSOLUTE,
            children = listOf(
                ImageNode(
                    id = "music_art",
                    name = "Album Art",
                    bounds = NodeBounds(x = 12, y = 10, widthDp = 28, heightDp = 28),
                    source = ImageSource.SystemGlyph("music"),
                    tintHex = "#FF4081"
                ),
                TextNode(
                    id = "track_title",
                    name = "Track Title",
                    bounds = NodeBounds(x = 48, y = 6, widthDp = 180, heightDp = 18),
                    template = "{notif.title}",
                    fontSizeSp = 13,
                    bold = true,
                    colorHex = "#FFFFFF"
                ),
                TextNode(
                    id = "track_artist",
                    name = "Artist Subtitle",
                    bounds = NodeBounds(x = 48, y = 24, widthDp = 180, heightDp = 14),
                    template = "{notif.text}",
                    fontSizeSp = 11,
                    bold = false,
                    colorHex = "#B0BEC5"
                ),
                ProgressNode(
                    id = "track_progress",
                    name = "Playback Progress",
                    bounds = NodeBounds(x = 48, y = 40, widthDp = 140, heightDp = 4),
                    valueTemplate = "{notif.progress}",
                    progressColorHex = "#FF4081",
                    trackColorHex = "#37474F",
                    maxValue = 100,
                    showIf = NodeCondition.HasProgress
                ),
                ButtonNode(
                    id = "action_btn",
                    name = "Play Action",
                    bounds = NodeBounds(x = 240, y = 8, widthDp = 50, heightDp = 30),
                    label = "Play",
                    action = ButtonAction.NotificationAction(1),
                    backgroundHex = "#FF4081",
                    textColorHex = "#FFFFFF"
                ),
                ButtonNode(
                    id = "otp_copy_btn",
                    name = "Copy OTP",
                    bounds = NodeBounds(x = 185, y = 8, widthDp = 50, heightDp = 30),
                    label = "Copy",
                    action = ButtonAction.SmartAction("OTP"),
                    backgroundHex = "#00E676",
                    textColorHex = "#000000",
                    showIf = NodeCondition.HasSmartAction("OTP")
                ),
                ShapeNode(
                    id = "status_badge",
                    name = "Status Pill",
                    bounds = NodeBounds(x = 12, y = 40, widthDp = 28, heightDp = 6),
                    shapeId = "rounded_rect",
                    cornerRadiusDp = 3,
                    fillColorHex = "#FF4081",
                    opacity = 0.85f
                )
            )
        )
    )
}

// =============================================================================
// FULL STUDIO SCREEN PREVIEWS
// =============================================================================

@Preview(name = "1. Full Studio Editor - Standard (Dark)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewStudioScreenDark() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = true) {
        StudioDesignContent(
            doc = sampleDoc,
            selectedNodeId = "track_title",
            selectedTab = StudioTab.ITEM,
            availableTabs = StudioTab.tabsFor(sampleDoc.root.children[1], isRoot = false),
            selectedScenario = StudioPreviewScenario.STANDARD,
            hiddenNodeIds = emptySet(),
            zoom = 1f,
            isGridVisible = false,
            isWireframeMode = false,
            canUndo = true,
            canRedo = false,
            isDirty = true,
            isSaving = false,
            notificationType = NotificationType.STANDARD,
            validationMessage = null,
            onBack = {},
            onUndo = {},
            onRedo = {},
            onExport = {},
            onRestore = {},
            onSave = {},
            onOpenAddElement = {},
            onSelectNode = {},
            onMoveNode = { _, _, _ -> },
            onResizeNode = { _, _, _ -> },
            onZoomIn = {},
            onZoomOut = {},
            onResetZoom = {},
            onToggleGrid = {},
            onToggleWireframe = {},
            onScenarioSelected = {},
            onTabSelected = {},
            onUpdateNode = {},
            onMoveLayer = { _, _ -> },
            onDeleteNode = {},
            onSelectChild = {},
            onAddChild = {},
            onNameChange = {},
            onCanvasChange = {},
            onNotificationTypeChange = {},
            onToggleVisibility = {},
            onMoveToFront = {},
            onMoveToBack = {},
            onDuplicate = {},
            onGroup = {},
            onUngroup = {},
            onRename = { _, _ -> },
            onToggleLock = {}
        )
    }
}

@Preview(name = "2. Full Studio Editor - Light Theme", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewStudioScreenLight() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = false) {
        StudioDesignContent(
            doc = sampleDoc,
            selectedNodeId = "root",
            selectedTab = StudioTab.ITEMS,
            availableTabs = StudioTab.tabsFor(sampleDoc.root, isRoot = true),
            selectedScenario = StudioPreviewScenario.STANDARD,
            hiddenNodeIds = emptySet(),
            zoom = 1f,
            isGridVisible = false,
            isWireframeMode = false,
            canUndo = false,
            canRedo = false,
            isDirty = false,
            isSaving = false,
            notificationType = NotificationType.STANDARD,
            validationMessage = null,
            onBack = {},
            onUndo = {},
            onRedo = {},
            onExport = {},
            onRestore = {},
            onSave = {},
            onOpenAddElement = {},
            onSelectNode = {},
            onMoveNode = { _, _, _ -> },
            onResizeNode = { _, _, _ -> },
            onZoomIn = {},
            onZoomOut = {},
            onResetZoom = {},
            onToggleGrid = {},
            onToggleWireframe = {},
            onScenarioSelected = {},
            onTabSelected = {},
            onUpdateNode = {},
            onMoveLayer = { _, _ -> },
            onDeleteNode = {},
            onSelectChild = {},
            onAddChild = {},
            onNameChange = {},
            onCanvasChange = {},
            onNotificationTypeChange = {},
            onToggleVisibility = {},
            onMoveToFront = {},
            onMoveToBack = {},
            onDuplicate = {},
            onGroup = {},
            onUngroup = {},
            onRename = { _, _ -> },
            onToggleLock = {}
        )
    }
}

@Preview(name = "3. Full Studio Editor - OTP Scenario (Live)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewStudioScreenOtpScenario() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = true) {
        StudioDesignContent(
            doc = sampleDoc,
            selectedNodeId = "otp_copy_btn",
            selectedTab = StudioTab.ACTIONS,
            availableTabs = StudioTab.tabsFor(sampleDoc.root.children[5], isRoot = false),
            selectedScenario = StudioPreviewScenario.OTP,
            hiddenNodeIds = emptySet(),
            zoom = 1f,
            isGridVisible = true,
            isWireframeMode = false,
            canUndo = true,
            canRedo = false,
            isDirty = true,
            isSaving = false,
            notificationType = NotificationType.STANDARD,
            validationMessage = null,
            onBack = {},
            onUndo = {},
            onRedo = {},
            onExport = {},
            onRestore = {},
            onSave = {},
            onOpenAddElement = {},
            onSelectNode = {},
            onMoveNode = { _, _, _ -> },
            onResizeNode = { _, _, _ -> },
            onZoomIn = {},
            onZoomOut = {},
            onResetZoom = {},
            onToggleGrid = {},
            onToggleWireframe = {},
            onScenarioSelected = {},
            onTabSelected = {},
            onUpdateNode = {},
            onMoveLayer = { _, _ -> },
            onDeleteNode = {},
            onSelectChild = {},
            onAddChild = {},
            onNameChange = {},
            onCanvasChange = {},
            onNotificationTypeChange = {},
            onToggleVisibility = {},
            onMoveToFront = {},
            onMoveToBack = {},
            onDuplicate = {},
            onGroup = {},
            onUngroup = {},
            onRename = { _, _ -> },
            onToggleLock = {}
        )
    }
}

// =============================================================================
// COMPONENT-LEVEL PREVIEWS
// =============================================================================

@Preview(name = "4. Canvas Island Preview - Live Mode", showBackground = true)
@Composable
fun PreviewStudioCanvasLive() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioCanvas(
                root = sampleDoc.root,
                canvasHeightDp = sampleDoc.canvas.heightDp,
                selectedId = "track_title",
                onSelect = {},
                onMove = { _, _, _ -> },
                onResize = { _, _, _ -> },
                zoom = 1f,
                isGridVisible = true,
                isWireframeMode = false,
                scenario = StudioPreviewScenario.MEDIA
            )
        }
    }
}

@Preview(name = "5. Canvas Island Preview - Wireframe Mode", showBackground = true)
@Composable
fun PreviewStudioCanvasWireframe() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioCanvas(
                root = sampleDoc.root,
                canvasHeightDp = sampleDoc.canvas.heightDp,
                selectedId = "track_progress",
                onSelect = {},
                onMove = { _, _, _ -> },
                onResize = { _, _, _ -> },
                zoom = 1f,
                isGridVisible = true,
                isWireframeMode = true,
                scenario = StudioPreviewScenario.PROGRESS
            )
        }
    }
}

@Preview(name = "6. Layers Hierarchy Panel", showBackground = true, widthDp = 360)
@Composable
fun PreviewStudioLayersPanel() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioLayersPanel(
                rootContainer = sampleDoc.root,
                selectedNodeId = "track_title",
                hiddenNodeIds = setOf("otp_copy_btn"),
                onSelectNode = {},
                onToggleLock = {},
                onToggleVisibility = {},
                onMoveLayer = { _, _ -> },
                onMoveToFront = {},
                onMoveToBack = {},
                onDuplicate = {},
                onGroup = {},
                onUngroup = {},
                onRename = { _, _ -> },
                onDelete = {},
                onAddElement = {}
            )
        }
    }
}

@Preview(name = "7. Inspector - Item Properties Tab", showBackground = true, widthDp = 360)
@Composable
fun PreviewStudioInspectorItemTab() {
    val sampleDoc = previewSampleWidgetDocument()
    val textNode = sampleDoc.root.children[1]
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioInspector(
                node = textNode,
                isRoot = false,
                selectedTab = StudioTab.ITEM,
                canMoveUp = true,
                canMoveDown = true,
                onChange = {},
                onMoveLayer = {},
                onDelete = {},
                selectedNodeId = textNode.id
            )
        }
    }
}

@Preview(name = "8. Inspector - Layer Position & Order Tab", showBackground = true, widthDp = 360)
@Composable
fun PreviewStudioInspectorLayerTab() {
    val sampleDoc = previewSampleWidgetDocument()
    val buttonNode = sampleDoc.root.children[4]
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioInspector(
                node = buttonNode,
                isRoot = false,
                selectedTab = StudioTab.LAYER,
                canMoveUp = true,
                canMoveDown = true,
                onChange = {},
                onMoveLayer = {},
                onDelete = {},
                selectedNodeId = buttonNode.id
            )
        }
    }
}

@Preview(name = "9. Scenario Switcher Controls", showBackground = true)
@Composable
fun PreviewStudioScenarioSwitcher() {
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioScenarioSwitcher(
                selectedScenario = StudioPreviewScenario.OTP,
                onScenarioSelected = {}
            )
        }
    }
}

@Preview(name = "10. Canvas Floating Toolbar & Breadcrumb", showBackground = true, widthDp = 360)
@Composable
fun PreviewStudioToolbarAndBreadcrumb() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column {
                StudioBreadcrumb(
                    document = sampleDoc,
                    selectedNodeId = "track_title",
                    onSelectNode = {}
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(vertical = 12.dp)
                        .background(StudioCanvasBackground)
                        .padding(12.dp)
                ) {
                    StudioFloatingToolbar(
                        zoom = 1.15f,
                        isGridVisible = true,
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
                        onOpenAddElement = {},
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

@Preview(name = "11. Formula Editor Dialog (\$fx\$)", showBackground = true)
@Composable
fun PreviewStudioFormulaDialog() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioFormulaDialog(
            propertyName = "textColorHex",
            initialFormula = "{theme.accent}",
            scenario = StudioPreviewScenario.STANDARD,
            onDismiss = {},
            onApply = {}
        )
    }
}

@Preview(name = "12. Installed App Chooser Dialog", showBackground = true)
@Composable
fun PreviewStudioAppChooserDialog() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioAppChooserDialog(
            onDismiss = {},
            onAppSelected = {}
        )
    }
}

@Preview(name = "13. Inspector Image Item Tab (Sources & Shapes)", showBackground = true)
@Composable
fun PreviewStudioInspectorImageItemTab() {
    val sampleDoc = previewSampleWidgetDocument()
    val imageNode = sampleDoc.root.children[0]
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioInspector(
                node = imageNode,
                isRoot = false,
                selectedTab = StudioTab.ITEM,
                canMoveUp = false,
                canMoveDown = true,
                onChange = {},
                onMoveLayer = {},
                onDelete = {},
                selectedNodeId = imageNode.id
            )
        }
    }
}

@Preview(name = "14. Inspector Formulas & Bindings Tab", showBackground = true)
@Composable
fun PreviewStudioInspectorBindingsTab() {
    val sampleDoc = previewSampleWidgetDocument()
    val textNode = sampleDoc.root.children[1]
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioInspector(
                node = textNode,
                isRoot = false,
                selectedTab = StudioTab.BINDINGS,
                canMoveUp = true,
                canMoveDown = true,
                onChange = {},
                onMoveLayer = {},
                onDelete = {},
                selectedNodeId = textNode.id
            )
        }
    }
}

@Preview(name = "15. Inspector Compound Conditions Builder", showBackground = true)
@Composable
fun PreviewStudioInspectorConditions() {
    val sampleDoc = previewSampleWidgetDocument()
    val conditionalNode = (sampleDoc.root.children[1] as TextNode).copy(
        showIf = NodeCondition.All(
            listOf(
                NodeCondition.HasInlineReply,
                NodeCondition.Not(NodeCondition.HasSmartAction("OTP")),
                NodeCondition.NotBlank("{notif.title}")
            )
        )
    )
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioInspector(
                node = conditionalNode,
                isRoot = false,
                selectedTab = StudioTab.LAYER,
                canMoveUp = true,
                canMoveDown = true,
                scenario = StudioPreviewScenario.MESSAGE,
                onChange = {},
                onMoveLayer = {},
                onDelete = {},
                selectedNodeId = conditionalNode.id
            )
        }
    }
}

@Preview(name = "16. Inspector Root Container Tab", showBackground = true)
@Composable
fun PreviewStudioInspectorRootContainerTab() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioInspector(
                node = sampleDoc.root,
                isRoot = true,
                selectedTab = StudioTab.CONTAINER,
                canMoveUp = false,
                canMoveDown = false,
                onChange = {},
                onMoveLayer = {},
                onDelete = {},
                document = sampleDoc,
                selectedNodeId = sampleDoc.root.id
            )
        }
    }
}

@Preview(name = "17. Inspector Root Design Tab", showBackground = true)
@Composable
fun PreviewStudioInspectorRootDesignTab() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioInspector(
                node = sampleDoc.root,
                isRoot = true,
                selectedTab = StudioTab.DESIGN,
                canMoveUp = false,
                canMoveDown = false,
                onChange = {},
                onMoveLayer = {},
                onDelete = {},
                document = sampleDoc,
                onNameChange = {},
                onIconChange = {},
                onCanvasChange = {},
                selectedNodeId = sampleDoc.root.id
            )
        }
    }
}

@Preview(name = "18. Inspector Root Where It Applies Tab", showBackground = true)
@Composable
fun PreviewStudioInspectorRootScopeTab() {
    val sampleDoc = previewSampleWidgetDocument()
    StudioExpressiveTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            StudioInspector(
                node = sampleDoc.root,
                isRoot = true,
                selectedTab = StudioTab.SCOPE,
                canMoveUp = false,
                canMoveDown = false,
                onChange = {},
                onMoveLayer = {},
                onDelete = {},
                document = sampleDoc,
                notificationType = NotificationType.MEDIA,
                targetScope = TargetScope.SPECIFIC_APPS,
                targetPackages = listOf("com.spotify.music", "org.telegram.messenger"),
                onNotificationTypeChange = {},
                onTargetScopeChange = {},
                selectedNodeId = sampleDoc.root.id
            )
        }
    }
}


