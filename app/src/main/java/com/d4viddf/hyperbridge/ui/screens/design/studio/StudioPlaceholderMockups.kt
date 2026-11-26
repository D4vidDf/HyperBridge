package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.data.widget.PreviewPlaceholderRepository
import com.d4viddf.hyperbridge.data.widget.ResolvedPlaceholderBitmaps
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.PreviewPlaceholderConfig

/**
 * Interactive preview harness for the Adaptive Placeholder Editor:
 * - Big Screens (`>= 600.dp`): Auto-hides the Left Element Tree panel to maximize canvas room and swaps
 *   the Right column to `StudioPlaceholderSidePanel` with a sticky `Reset` + `Done` footer.
 * - Phones (`< 600.dp`): Renders `StudioPlaceholderFullScreen` with Design Studio's `PrimaryScrollableTabRow`,
 *   grouped `PlaceholderGroupCard` sections, auto-collapsing live preview when typing, and a pinned bottom bar.
 */
@Composable
fun StudioPlaceholderWorkspaceMockup(
    doc: CustomWidgetDocument = previewSampleWidgetDocument(),
    initialScenario: StudioPreviewScenario = StudioPreviewScenario.MEDIA,
    isCustomized: Boolean = true
) {
    var config by remember(initialScenario, isCustomized) {
        mutableStateOf(
            PreviewPlaceholderConfig.fromScenario(initialScenario).copy(
                isCustomized = isCustomized
            )
        )
    }
    var selectedScenario by remember(initialScenario) { mutableStateOf(initialScenario) }

    val sampleArtBitmap = remember {
        PreviewPlaceholderRepository.createPresetSampleBitmap(
            PreviewPlaceholderConfig.PRESET_ART_NEON,
            192
        )
    }
    val sampleAvatarBitmap = remember {
        PreviewPlaceholderRepository.createPresetSampleBitmap(
            PreviewPlaceholderConfig.PRESET_AVATAR_CORAL,
            128
        )
    }
    val resolvedBitmaps = remember(config) {
        ResolvedPlaceholderBitmaps(
            pictureBitmap = if (config.pictureRef != null) sampleArtBitmap else null,
            avatarBitmap = if (config.avatarRef != null) sampleAvatarBitmap else null
        )
    }
    val liveContext = remember(config, resolvedBitmaps) {
        config.toVariableContext(
            pictureBitmap = resolvedBitmaps.pictureBitmap,
            avatarBitmap = resolvedBitmaps.avatarBitmap
        )
    }

    StudioDesignContent(
        doc = doc,
        selectedNodeId = "track_title",
        selectedTab = StudioTab.INFO,
        availableTabs = StudioTab.tabsFor(null, isRoot = true),
        selectedScenario = selectedScenario,
        hiddenNodeIds = emptySet(),
        zoom = 1f,
        isGridVisible = true,
        isWireframeMode = false,
        canUndo = true,
        canRedo = false,
        isDirty = false,
        isSaving = false,
        notificationType = NotificationType.MEDIA,
        validationMessage = null,
        onBack = {},
        onUndo = {},
        onRedo = {},
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
        onToggleLock = {},
        previewContext = liveContext,
        widgetId = doc.id,
        isCustomPlaceholdersActive = config.isCustomized,
        onOpenPlaceholderSheet = {},
        isPlaceholderEditorOpen = true,
        onClosePlaceholderEditor = {},
        placeholderConfig = config,
        resolvedPlaceholderBitmaps = resolvedBitmaps,
        onPlaceholderConfigChange = { updated ->
            config = updated
            updated.matchingScenarioOrNull()?.let { selectedScenario = it }
        },
        onResetPlaceholderToPreset = { scenario ->
            selectedScenario = scenario
            config = PreviewPlaceholderConfig.fromScenario(scenario)
        }
    )
}

@Composable
fun StudioPlaceholderPhoneMockup(
    doc: CustomWidgetDocument = previewSampleWidgetDocument(),
    initialScenario: StudioPreviewScenario = StudioPreviewScenario.MEDIA,
    initialTabIndex: Int = 0,
    simulateTypingKeyboardOpen: Boolean = false
) {
    var config by remember(initialScenario) {
        mutableStateOf(
            PreviewPlaceholderConfig.fromScenario(initialScenario).copy(
                isCustomized = initialScenario != StudioPreviewScenario.CUSTOM
            )
        )
    }
    val sampleArtBitmap = remember {
        PreviewPlaceholderRepository.createPresetSampleBitmap(
            PreviewPlaceholderConfig.PRESET_ART_NEON,
            192
        )
    }
    val sampleAvatarBitmap = remember {
        PreviewPlaceholderRepository.createPresetSampleBitmap(
            PreviewPlaceholderConfig.PRESET_AVATAR_CORAL,
            128
        )
    }
    val resolvedBitmaps = remember(config) {
        ResolvedPlaceholderBitmaps(
            pictureBitmap = if (config.pictureRef != null) sampleArtBitmap else null,
            avatarBitmap = if (config.avatarRef != null) sampleAvatarBitmap else null
        )
    }
    val liveContext = remember(config, resolvedBitmaps) {
        config.toVariableContext(
            pictureBitmap = resolvedBitmaps.pictureBitmap,
            avatarBitmap = resolvedBitmaps.avatarBitmap
        )
    }

    StudioPlaceholderFullScreen(
        config = config,
        resolvedBitmaps = resolvedBitmaps,
        document = doc,
        initialTabIndex = initialTabIndex,
        forceCollapsePreview = simulateTypingKeyboardOpen,
        onConfigChange = { config = it },
        onPickGalleryImage = { _, _, _ -> },
        onResetToPreset = { config = PreviewPlaceholderConfig.fromScenario(it) },
        onDismiss = {},
        livePreviewContent = {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val scale = ((maxWidth.value - 8f) / 350f).coerceIn(0.65f, 1.0f)
                Box(
                    modifier = Modifier
                        .width((350f * scale).dp)
                        .height((doc.canvas.heightDp * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(350.dp)
                            .height(doc.canvas.heightDp.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {
                        StudioCanvas(
                            root = doc.root,
                            canvasWidthDp = 350f,
                            canvasHeightDp = doc.canvas.heightDp,
                            selectedId = null,
                            onSelect = {},
                            onMove = { _, _, _ -> },
                            onResize = { _, _, _ -> },
                            scenario = initialScenario,
                            context = liveContext,
                            globals = doc.globals
                        )
                    }
                }
            }
        }
    )
}

// =============================================================================
// COMPOSE PREVIEWS
// =============================================================================

@Preview(
    name = "1. Big Screen (1280x800) - Left Panel Auto-Hidden + Right Side Panel",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800
)
@Composable
fun PreviewPlaceholderBigScreenSidePanelTexts() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioPlaceholderWorkspaceMockup(initialScenario = StudioPreviewScenario.MEDIA)
    }
}

@Preview(
    name = "2. Foldable (840x700) - Left Panel Hidden + Side Panel",
    showBackground = true,
    widthDp = 840,
    heightDp = 700
)
@Composable
fun PreviewPlaceholderFoldableSidePanelImages() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioPlaceholderWorkspaceMockup(initialScenario = StudioPreviewScenario.CUSTOM, isCustomized = false)
    }
}

@Preview(
    name = "3. Phone (392x820) - Studio Tabs + Grouped Cards + Live Preview",
    showBackground = true,
    widthDp = 392,
    heightDp = 820
)
@Composable
fun PreviewPlaceholderPhoneFullScreenTexts() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioPlaceholderPhoneMockup(
            initialScenario = StudioPreviewScenario.MEDIA,
            initialTabIndex = 0,
            simulateTypingKeyboardOpen = false
        )
    }
}

@Preview(
    name = "4. Phone (392x820) - Typing Mode (Preview Collapsed + Custom Empty Preset)",
    showBackground = true,
    widthDp = 392,
    heightDp = 820
)
@Composable
fun PreviewPlaceholderPhoneTypingCustomEmpty() {
    StudioExpressiveTheme(darkTheme = true) {
        StudioPlaceholderPhoneMockup(
            initialScenario = StudioPreviewScenario.CUSTOM,
            initialTabIndex = 0,
            simulateTypingKeyboardOpen = true
        )
    }
}

@Preview(
    name = "5. Quick Scenario Preset BottomSheet (Includes Custom Empty Default)",
    showBackground = true,
    widthDp = 412,
    heightDp = 640
)
@Composable
fun PreviewScenarioPresetBottomSheet() {
    StudioExpressiveTheme(darkTheme = true) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            ScenarioPresetBottomSheetContent(
                selectedScenario = StudioPreviewScenario.CUSTOM,
                isCustomized = false,
                onSelectScenario = {}
            )
        }
    }
}
