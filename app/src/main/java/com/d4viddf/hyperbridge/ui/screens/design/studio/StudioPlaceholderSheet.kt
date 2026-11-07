package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.PreviewPlaceholderRepository
import com.d4viddf.hyperbridge.data.widget.ResolvedPlaceholderBitmaps
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.PreviewPlaceholderConfig
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor
import kotlin.math.roundToInt

internal enum class PlaceholderEditorTab(val labelRes: Int, val icon: ImageVector) {
    TEXTS(R.string.studio_placeholder_tab_texts, Icons.Rounded.TextFields),
    IMAGES(R.string.studio_placeholder_tab_images, Icons.Rounded.Image),
    MEDIA(R.string.studio_placeholder_tab_media, Icons.Rounded.MusicNote),
    STATE(R.string.studio_placeholder_tab_state, Icons.Rounded.SmartButton),
    DEVICE(R.string.studio_placeholder_tab_device, Icons.Rounded.PhoneAndroid)
}

enum class PlaceholderImageSlot(val fileName: String) {
    PICTURE(PreviewPlaceholderRepository.SLOT_PICTURE),
    AVATAR(PreviewPlaceholderRepository.SLOT_AVATAR),
    SMALL_ICON(PreviewPlaceholderRepository.SLOT_SMALL_ICON)
}

/**
 * Big-Screen (`>= 600.dp`) Floating Side Panel for configuring preview & share placeholders.
 * Keeps `Reset` and `Done` pinned in a sticky bottom bar while the live canvas stays visible alongside it.
 */
@Composable
fun StudioPlaceholderSidePanel(
    modifier: Modifier = Modifier,
    config: PreviewPlaceholderConfig,
    resolvedBitmaps: ResolvedPlaceholderBitmaps,
    document: CustomWidgetDocument? = null,
    initialTabIndex: Int = 0,
    onConfigChange: (PreviewPlaceholderConfig) -> Unit,
    onPickGalleryImage: (slotFileName: String, uri: Uri, onSaved: (String) -> Unit) -> Unit,
    onResetToPreset: (StudioPreviewScenario) -> Unit,
    onClose: () -> Unit
) {
    var selectedTabIndex by rememberSaveable(initialTabIndex) { mutableIntStateOf(initialTabIndex) }
    var showScenarioSheet by remember { mutableStateOf(false) }
    val activeScenario = config.matchingScenarioOrNull() ?: StudioPreviewScenario.STANDARD

    Column(modifier = modifier.fillMaxSize()) {
        // Sticky Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.studio_placeholder_sheet_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (config.isCustomized) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.studio_placeholder_modified_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.studio_placeholder_sheet_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Quick Scenario Preset Selector Card (opens ModalBottomSheet)
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            ScenarioPresetSelectorCard(
                scenario = activeScenario,
                isCustomized = config.isCustomized,
                onClick = { showScenarioSheet = true }
            )
        }

        // Scrollable Button Tabs (matching Studio's Big-Screen Right Inspector Panel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaceholderEditorTab.entries.forEachIndexed { idx, tab ->
                val selected = idx == selectedTabIndex
                FilterChip(
                    selected = selected,
                    onClick = { selectedTabIndex = idx },
                    leadingIcon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Scrollable Grouped Cards Body (`weight(1f)` keeps bottom bar visible)
        PlaceholderEditorFormBody(
            selectedTab = PlaceholderEditorTab.entries[selectedTabIndex.coerceIn(0, PlaceholderEditorTab.entries.lastIndex)],
            config = config,
            resolvedBitmaps = resolvedBitmaps,
            document = document,
            onConfigChange = onConfigChange,
            onPickGalleryImage = onPickGalleryImage,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        )

        // Sticky Bottom Bar (Reset + Done)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onResetToPreset(activeScenario) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.studio_placeholder_reset_preset), maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.studio_placeholder_done))
            }
        }
    }

    if (showScenarioSheet) {
        ScenarioPresetBottomSheet(
            selectedScenario = activeScenario,
            isCustomized = config.isCustomized,
            onSelectScenario = { chosen ->
                onResetToPreset(chosen)
                showScenarioSheet = false
            },
            onDismiss = { showScenarioSheet = false }
        )
    }
}

/**
 * Phone (`< 600.dp`) Full-Screen Placeholder Editor.
 * - Live preview strip at the top automatically collapses when the IME keyboard opens while typing.
 * - Uses Design Studio's `PrimaryScrollableTabRow` + grouped `Card` sections.
 * - Keeps `Reset` + `Done` pinned in `Scaffold.bottomBar` above the navigation bar at all times.
 */
@Composable
fun StudioPlaceholderFullScreen(
    config: PreviewPlaceholderConfig,
    resolvedBitmaps: ResolvedPlaceholderBitmaps,
    document: CustomWidgetDocument? = null,
    initialTabIndex: Int = 0,
    forceCollapsePreview: Boolean = false,
    onConfigChange: (PreviewPlaceholderConfig) -> Unit,
    onPickGalleryImage: (slotFileName: String, uri: Uri, onSaved: (String) -> Unit) -> Unit,
    onResetToPreset: (StudioPreviewScenario) -> Unit,
    onDismiss: () -> Unit,
    livePreviewContent: (@Composable () -> Unit)? = null
) {
    BackHandler(onBack = onDismiss)

    var selectedTabIndex by rememberSaveable(initialTabIndex) { mutableIntStateOf(initialTabIndex) }
    var showScenarioSheet by remember { mutableStateOf(false) }
    var isPreviewManuallyExpanded by rememberSaveable { mutableStateOf(true) }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0 || forceCollapsePreview
    val shouldShowLivePreview = livePreviewContent != null && isPreviewManuallyExpanded && !isKeyboardOpen

    val activeScenario = config.matchingScenarioOrNull() ?: StudioPreviewScenario.STANDARD

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    FilledTonalIconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.studio_placeholder_sheet_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (config.isCustomized) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.studio_placeholder_modified_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (livePreviewContent != null) {
                        IconButton(onClick = { isPreviewManuallyExpanded = !isPreviewManuallyExpanded }) {
                            Icon(
                                imageVector = if (isPreviewManuallyExpanded) {
                                    Icons.Rounded.Visibility
                                } else {
                                    Icons.Rounded.VisibilityOff
                                },
                                contentDescription = stringResource(R.string.studio_placeholder_toggle_preview_cd)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onResetToPreset(activeScenario) },
                        modifier = Modifier.weight(0.42f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.studio_placeholder_reset_preset), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.58f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.studio_placeholder_done))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Auto-collapsing Live Preview Card (hides automatically when keyboard opens for typing)
            AnimatedVisibility(
                visible = shouldShowLivePreview,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141414),
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        livePreviewContent?.invoke()
                    }
                }
            }

            // Quick Scenario Preset Selector Card (opens ModalBottomSheet)
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                ScenarioPresetSelectorCard(
                    scenario = activeScenario,
                    isCustomized = config.isCustomized,
                    onClick = { showScenarioSheet = true }
                )
            }

            // Design Studio Phone Tabs (`PrimaryScrollableTabRow` matching `StudioTabsRow`)
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                PlaceholderEditorTab.entries.forEachIndexed { idx, tab ->
                    Tab(
                        selected = idx == selectedTabIndex,
                        onClick = { selectedTabIndex = idx },
                        text = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }

            // Design Studio Grouped Cards Scrollable Body
            PlaceholderEditorFormBody(
                selectedTab = PlaceholderEditorTab.entries[selectedTabIndex.coerceIn(0, PlaceholderEditorTab.entries.lastIndex)],
                config = config,
                resolvedBitmaps = resolvedBitmaps,
                document = document,
                onConfigChange = onConfigChange,
                onPickGalleryImage = onPickGalleryImage,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
        }
    }

    if (showScenarioSheet) {
        ScenarioPresetBottomSheet(
            selectedScenario = activeScenario,
            isCustomized = config.isCustomized,
            onSelectScenario = { chosen ->
                onResetToPreset(chosen)
                showScenarioSheet = false
            },
            onDismiss = { showScenarioSheet = false }
        )
    }
}

/**
 * Adaptive Dialog wrapper used from Share / Export (`DesignShareBottomSheet`):
 * - Big Screens (`>= 600.dp`): Split view with Live Preview Stage on the left + Floating Side Panel on the right.
 * - Phones (`< 600.dp`): Full-screen editor (`StudioPlaceholderFullScreen`) with auto-collapsing preview when typing.
 */
@Composable
fun StudioPlaceholderAdaptiveDialog(
    config: PreviewPlaceholderConfig,
    resolvedBitmaps: ResolvedPlaceholderBitmaps,
    document: CustomWidgetDocument? = null,
    onConfigChange: (PreviewPlaceholderConfig) -> Unit,
    onPickGalleryImage: (slotFileName: String, uri: Uri, onSaved: (String) -> Unit) -> Unit,
    onResetToPreset: (StudioPreviewScenario) -> Unit,
    onDismiss: () -> Unit,
    livePreviewContent: (@Composable () -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        StudioExpressiveTheme {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                val isExpandedScreen = maxWidth >= 600.dp
                if (isExpandedScreen) {
                    val sidePanelWidth = (maxWidth * 0.38f).coerceIn(340.dp, 430.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left / Center Live Preview Stage
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF141414),
                            tonalElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                livePreviewContent?.invoke()
                            }
                        }

                        // Right Floating Placeholder Side Panel
                        Surface(
                            modifier = Modifier
                                .width(sidePanelWidth)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            StudioPlaceholderSidePanel(
                                config = config,
                                resolvedBitmaps = resolvedBitmaps,
                                document = document,
                                onConfigChange = onConfigChange,
                                onPickGalleryImage = onPickGalleryImage,
                                onResetToPreset = onResetToPreset,
                                onClose = onDismiss
                            )
                        }
                    }
                } else {
                    StudioPlaceholderFullScreen(
                        config = config,
                        resolvedBitmaps = resolvedBitmaps,
                        document = document,
                        onConfigChange = onConfigChange,
                        onPickGalleryImage = onPickGalleryImage,
                        onResetToPreset = onResetToPreset,
                        onDismiss = onDismiss,
                        livePreviewContent = livePreviewContent
                    )
                }
            }
        }
    }
}

/**
 * Clean Material 3 Selector Card that opens [ScenarioPresetBottomSheet].
 */
@Composable
fun ScenarioPresetSelectorCard(
    scenario: StudioPreviewScenario,
    isCustomized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = scenario.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.studio_placeholder_presets_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(scenario.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCustomized && scenario != StudioPreviewScenario.CUSTOM) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.studio_placeholder_modified_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Material 3 ModalBottomSheet for picking a Quick Scenario Preset (including `CUSTOM` with empty options as default).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioPresetBottomSheet(
    selectedScenario: StudioPreviewScenario,
    isCustomized: Boolean,
    onSelectScenario: (StudioPreviewScenario) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        ScenarioPresetBottomSheetContent(
            selectedScenario = selectedScenario,
            isCustomized = isCustomized,
            onSelectScenario = onSelectScenario
        )
    }
}

@Composable
internal fun ScenarioPresetBottomSheetContent(
    selectedScenario: StudioPreviewScenario,
    isCustomized: Boolean,
    onSelectScenario: (StudioPreviewScenario) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.studio_placeholder_presets_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        StudioPreviewScenario.entries.forEach { scenario ->
            val isSelected = scenario == selectedScenario
            Surface(
                onClick = { onSelectScenario(scenario) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = scenario.icon,
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(scenario.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (isSelected && isCustomized && scenario != StudioPreviewScenario.CUSTOM) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiary
                                ) {
                                    Text(
                                        text = stringResource(R.string.studio_placeholder_modified_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(scenario.descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )
                }
            }
        }
    }
}

/**
 * Shared scrollable form body organized into Design Studio's Grouped Section Cards (`PlaceholderGroupCard`).
 */
@Composable
private fun PlaceholderEditorFormBody(
    selectedTab: PlaceholderEditorTab,
    config: PreviewPlaceholderConfig,
    resolvedBitmaps: ResolvedPlaceholderBitmaps,
    document: CustomWidgetDocument?,
    onConfigChange: (PreviewPlaceholderConfig) -> Unit,
    onPickGalleryImage: (slotFileName: String, uri: Uri, onSaved: (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAppChooserOpen by remember { mutableStateOf(false) }
    var appChooserForIconOnly by remember { mutableStateOf(false) }

    var pendingGallerySlot by remember { mutableStateOf<Pair<String, (String) -> Unit>?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val pending = pendingGallerySlot
        pendingGallerySlot = null
        if (uri != null && pending != null) {
            onPickGalleryImage(pending.first, uri, pending.second)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        when (selectedTab) {
            PlaceholderEditorTab.TEXTS -> {
                PlaceholderGroupCard(title = stringResource(R.string.studio_placeholder_group_notif)) {
                    PlaceholderTextField(
                        value = config.notifTitle,
                        label = stringResource(R.string.studio_placeholder_field_title),
                        onValueChange = { onConfigChange(config.copy(notifTitle = it, isCustomized = true)) }
                    )
                    PlaceholderTextField(
                        value = config.notifText,
                        label = stringResource(R.string.studio_placeholder_field_text),
                        singleLine = false,
                        onValueChange = { onConfigChange(config.copy(notifText = it, isCustomized = true)) }
                    )
                    PlaceholderTextField(
                        value = config.subtext,
                        label = stringResource(R.string.studio_placeholder_field_subtext),
                        onValueChange = { onConfigChange(config.copy(subtext = it, isCustomized = true)) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlaceholderTextField(
                            value = config.sender,
                            label = stringResource(R.string.studio_placeholder_field_sender),
                            onValueChange = { onConfigChange(config.copy(sender = it, isCustomized = true)) },
                            modifier = Modifier.weight(1f)
                        )
                        PlaceholderTextField(
                            value = config.conversation,
                            label = stringResource(R.string.studio_placeholder_field_conversation),
                            onValueChange = { onConfigChange(config.copy(conversation = it, isCustomized = true)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                PlaceholderGroupCard(title = stringResource(R.string.studio_placeholder_group_app)) {
                    FilledTonalButton(
                        onClick = {
                            appChooserForIconOnly = false
                            isAppChooserOpen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.studio_placeholder_pick_app))
                    }

                    PlaceholderTextField(
                        value = config.appName,
                        label = stringResource(R.string.studio_placeholder_field_app_name),
                        onValueChange = { onConfigChange(config.copy(appName = it, isCustomized = true)) }
                    )
                    PlaceholderTextField(
                        value = config.notifPackage,
                        label = stringResource(R.string.studio_placeholder_field_package),
                        monospace = true,
                        onValueChange = { onConfigChange(config.copy(notifPackage = it, isCustomized = true)) }
                    )
                }
            }

            PlaceholderEditorTab.IMAGES -> {
                // 1. Album Art / Picture
                PlaceholderImageSlotCard(
                    title = stringResource(R.string.studio_placeholder_img_picture_title),
                    description = stringResource(R.string.studio_placeholder_img_picture_desc),
                    currentBitmap = resolvedBitmaps.pictureBitmap,
                    currentRef = config.pictureRef,
                    fallbackIcon = Icons.Rounded.Album,
                    isCirclePreview = false,
                    presetIds = PreviewPlaceholderConfig.BUILT_IN_ART_PRESETS,
                    onPickGallery = {
                        pendingGallerySlot = PlaceholderImageSlot.PICTURE.fileName to { savedName ->
                            onConfigChange(config.copy(pictureRef = savedName, isCustomized = true))
                        }
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSelectPreset = { presetId ->
                        onConfigChange(config.copy(pictureRef = presetId, isCustomized = true))
                    },
                    onUseAppIcon = null,
                    onClear = {
                        onConfigChange(config.copy(pictureRef = null, isCustomized = true))
                    }
                )

                // 2. Avatar / Contact Photo
                PlaceholderImageSlotCard(
                    title = stringResource(R.string.studio_placeholder_img_avatar_title),
                    description = stringResource(R.string.studio_placeholder_img_avatar_desc),
                    currentBitmap = resolvedBitmaps.avatarBitmap,
                    currentRef = config.avatarRef,
                    fallbackIcon = Icons.Rounded.Person,
                    isCirclePreview = true,
                    presetIds = PreviewPlaceholderConfig.BUILT_IN_AVATAR_PRESETS,
                    onPickGallery = {
                        pendingGallerySlot = PlaceholderImageSlot.AVATAR.fileName to { savedName ->
                            onConfigChange(config.copy(avatarRef = savedName, isCustomized = true))
                        }
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSelectPreset = { presetId ->
                        onConfigChange(config.copy(avatarRef = presetId, isCustomized = true))
                    },
                    onUseAppIcon = null,
                    onClear = {
                        onConfigChange(config.copy(avatarRef = null, isCustomized = true))
                    }
                )

                // 3. App / Small Icon
                PlaceholderImageSlotCard(
                    title = stringResource(R.string.studio_placeholder_img_icon_title),
                    description = stringResource(R.string.studio_placeholder_img_icon_desc),
                    currentBitmap = resolvedBitmaps.smallIconBitmap,
                    currentRef = config.smallIconRef,
                    fallbackIcon = Icons.Rounded.Apps,
                    isCirclePreview = true,
                    presetIds = emptyList(),
                    onPickGallery = {
                        pendingGallerySlot = PlaceholderImageSlot.SMALL_ICON.fileName to { savedName ->
                            onConfigChange(config.copy(smallIconRef = savedName, isCustomized = true))
                        }
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSelectPreset = {},
                    onUseAppIcon = {
                        if (config.notifPackage.isBlank()) {
                            appChooserForIconOnly = true
                            isAppChooserOpen = true
                        } else {
                            onConfigChange(
                                config.copy(
                                    smallIconRef = PreviewPlaceholderConfig.REF_APP_ICON,
                                    isCustomized = true
                                )
                            )
                        }
                    },
                    onClear = {
                        onConfigChange(config.copy(smallIconRef = null, isCustomized = true))
                    }
                )
            }

            PlaceholderEditorTab.MEDIA -> {
                PlaceholderGroupCard(title = stringResource(R.string.studio_placeholder_group_playback)) {
                    PlaceholderInlineSwitch(
                        title = stringResource(R.string.studio_placeholder_media_playing),
                        checked = config.isMediaPlaying,
                        onCheckedChange = { playing ->
                            onConfigChange(
                                config.copy(
                                    isMediaPlaying = playing,
                                    mediaState = if (playing) "PLAYING" else "PAUSED",
                                    isCustomized = true
                                )
                            )
                        }
                    )

                    PlaceholderTextField(
                        value = config.mediaTrack,
                        label = stringResource(R.string.studio_placeholder_media_track),
                        onValueChange = { onConfigChange(config.copy(mediaTrack = it, isCustomized = true)) }
                    )

                    PlaceholderTextField(
                        value = config.mediaArtist,
                        label = stringResource(R.string.studio_placeholder_media_artist),
                        onValueChange = { onConfigChange(config.copy(mediaArtist = it, isCustomized = true)) }
                    )
                }

                PlaceholderGroupCard(title = stringResource(R.string.studio_placeholder_group_timeline)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlaceholderTextField(
                            value = config.mediaPosition,
                            label = stringResource(R.string.studio_placeholder_media_position),
                            monospace = true,
                            onValueChange = { onConfigChange(config.copy(mediaPosition = it, isCustomized = true)) },
                            modifier = Modifier.weight(1f)
                        )
                        PlaceholderTextField(
                            value = config.mediaDuration,
                            label = stringResource(R.string.studio_placeholder_media_duration),
                            monospace = true,
                            onValueChange = { onConfigChange(config.copy(mediaDuration = it, isCustomized = true)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PlaceholderInlineSlider(
                        label = stringResource(R.string.studio_placeholder_media_progress_fmt, config.mediaProgress),
                        value = config.mediaProgress,
                        onValueChange = { pct ->
                            onConfigChange(config.copy(mediaProgress = pct, isCustomized = true))
                        }
                    )
                }
            }

            PlaceholderEditorTab.STATE -> {
                PlaceholderGroupCard(title = stringResource(R.string.studio_placeholder_group_progress)) {
                    PlaceholderInlineSwitch(
                        title = stringResource(R.string.studio_placeholder_notif_progress_toggle),
                        checked = config.hasNotifProgress,
                        onCheckedChange = { enabled ->
                            onConfigChange(config.copy(hasNotifProgress = enabled, isCustomized = true))
                        }
                    )

                    if (config.hasNotifProgress) {
                        PlaceholderInlineSlider(
                            label = stringResource(R.string.studio_placeholder_notif_progress_fmt, config.notifProgress),
                            value = config.notifProgress,
                            onValueChange = { pct ->
                                onConfigChange(config.copy(notifProgress = pct, isCustomized = true))
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlaceholderTextField(
                            value = config.timerText,
                            label = stringResource(R.string.studio_placeholder_pill_timer),
                            monospace = true,
                            onValueChange = { onConfigChange(config.copy(timerText = it, isCustomized = true)) },
                            modifier = Modifier.weight(1f)
                        )
                        PlaceholderTextField(
                            value = config.countdownText,
                            label = stringResource(R.string.studio_placeholder_pill_countdown),
                            monospace = true,
                            onValueChange = { onConfigChange(config.copy(countdownText = it, isCustomized = true)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                PlaceholderGroupCard(title = stringResource(R.string.studio_placeholder_group_actions)) {
                    PlaceholderInlineSwitch(
                        title = stringResource(R.string.studio_placeholder_inline_reply),
                        checked = config.hasInlineReply,
                        onCheckedChange = { reply ->
                            onConfigChange(config.copy(hasInlineReply = reply, isCustomized = true))
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.studio_placeholder_smart_actions),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PreviewPlaceholderConfig.ALL_SMART_ACTION_TYPES.forEach { actionType ->
                                val selected = actionType in config.smartActionTypes
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val updated = if (selected) {
                                            config.smartActionTypes - actionType
                                        } else {
                                            config.smartActionTypes + actionType
                                        }
                                        onConfigChange(config.copy(smartActionTypes = updated, isCustomized = true))
                                    },
                                    label = { Text(actionType) }
                                )
                            }
                        }
                    }

                    var rawActionsText by remember(config.presetName, config.notificationActionTitles) {
                        mutableStateOf(config.notificationActionTitles.joinToString(", "))
                    }
                    PlaceholderTextField(
                        value = rawActionsText,
                        label = stringResource(R.string.studio_placeholder_action_buttons),
                        onValueChange = { raw ->
                            rawActionsText = raw
                            val parsed = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            onConfigChange(config.copy(notificationActionTitles = parsed, isCustomized = true))
                        }
                    )
                }
            }

            PlaceholderEditorTab.DEVICE -> {
                PlaceholderGroupCard(title = stringResource(R.string.studio_placeholder_group_device)) {
                    PlaceholderTextField(
                        value = config.timeNowFormatted,
                        label = stringResource(R.string.studio_placeholder_time_now),
                        monospace = true,
                        onValueChange = { onConfigChange(config.copy(timeNowFormatted = it, isCustomized = true)) }
                    )

                    PlaceholderInlineSlider(
                        label = stringResource(R.string.studio_placeholder_battery_fmt, config.deviceBatteryPercent),
                        value = config.deviceBatteryPercent,
                        onValueChange = { pct ->
                            onConfigChange(config.copy(deviceBatteryPercent = pct, isCustomized = true))
                        }
                    )
                }

                PlaceholderGroupCard(title = stringResource(R.string.studio_placeholder_group_theme)) {
                    PlaceholderHexColorField(
                        value = config.themePrimary,
                        fallbackHex = "#3DDA82",
                        label = stringResource(R.string.studio_placeholder_theme_primary),
                        onValueChange = { onConfigChange(config.copy(themePrimary = it, isCustomized = true)) }
                    )
                    PlaceholderHexColorField(
                        value = config.themeAccent,
                        fallbackHex = "#00E5FF",
                        label = stringResource(R.string.studio_placeholder_theme_accent),
                        onValueChange = { onConfigChange(config.copy(themeAccent = it, isCustomized = true)) }
                    )
                    PlaceholderHexColorField(
                        value = config.themeSurface,
                        fallbackHex = "#1E1E1E",
                        label = stringResource(R.string.studio_placeholder_theme_surface),
                        onValueChange = { onConfigChange(config.copy(themeSurface = it, isCustomized = true)) }
                    )
                }

                PlaceholderGroupCard(title = stringResource(R.string.studio_placeholder_sources_title)) {
                    val detectedKeys = remember(document) {
                        document?.let { doc ->
                            PreviewPlaceholderConfig.extractDocumentSourceKeys(doc).map { "${it.first}.${it.second}" }
                        }.orEmpty()
                    }
                    val allSourceKeys = remember(detectedKeys, config.sourceValues.keys) {
                        (detectedKeys + config.sourceValues.keys).distinct()
                    }

                    allSourceKeys.forEach { key ->
                        val isIconKey = key.endsWith(".icon")
                        val currentValue = config.sourceValues[key].orEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = currentValue,
                                onValueChange = { newVal ->
                                    val updated = config.sourceValues.toMutableMap().apply { put(key, newVal) }
                                    onConfigChange(config.copy(sourceValues = updated, isCustomized = true))
                                },
                                label = { Text("{source.$key}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (isIconKey) {
                                FilledTonalButton(
                                    onClick = {
                                        val safeSlot = "source_${key.replace('.', '_')}.png"
                                        pendingGallerySlot = safeSlot to { savedName ->
                                            val updated = config.sourceValues.toMutableMap().apply { put(key, savedName) }
                                            onConfigChange(config.copy(sourceValues = updated, isCustomized = true))
                                        }
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (key !in detectedKeys) {
                                IconButton(
                                    onClick = {
                                        val updated = config.sourceValues.toMutableMap().apply { remove(key) }
                                        onConfigChange(config.copy(sourceValues = updated, isCustomized = true))
                                    }
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                                }
                            }
                        }
                    }

                    var newSourceKey by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSourceKey,
                            onValueChange = { newSourceKey = it },
                            label = { Text(stringResource(R.string.studio_placeholder_sources_add_key)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            enabled = newSourceKey.contains('.') && newSourceKey.length >= 3,
                            onClick = {
                                val cleanKey = newSourceKey.trim().removePrefix("source.").removeSurrounding("{", "}")
                                if (cleanKey.contains('.')) {
                                    val updated = config.sourceValues.toMutableMap().apply {
                                        putIfAbsent(cleanKey, "")
                                    }
                                    onConfigChange(config.copy(sourceValues = updated, isCustomized = true))
                                    newSourceKey = ""
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.studio_placeholder_sources_add_btn))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (isAppChooserOpen) {
        StudioAppChooserDialog(
            onDismiss = { isAppChooserOpen = false },
            onAppSelected = { _ -> isAppChooserOpen = false },
            onAppItemSelected = { item ->
                if (appChooserForIconOnly) {
                    onConfigChange(
                        config.copy(
                            notifPackage = item.packageName,
                            smallIconRef = PreviewPlaceholderConfig.REF_APP_ICON,
                            isCustomized = true
                        )
                    )
                } else {
                    onConfigChange(
                        config.copy(
                            appName = item.name,
                            notifPackage = item.packageName,
                            smallIconRef = PreviewPlaceholderConfig.REF_APP_ICON,
                            isCustomized = true
                        )
                    )
                }
                isAppChooserOpen = false
            }
        )
    }
}

/**
 * Grouped Section Card matching Design Studio's `StudioSection` (`RoundedCornerShape(18.dp)`, `surfaceContainer`).
 */
@Composable
internal fun PlaceholderGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun PlaceholderTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    monospace: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        textStyle = if (monospace) {
            MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        } else {
            MaterialTheme.typography.bodyMedium
        },
        trailingIcon = {
            if (value.isNotEmpty() && singleLine) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PlaceholderHexColorField(
    value: String,
    fallbackHex: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    val swatchColor = remember(value, fallbackHex) {
        safeParseColor(value.ifBlank { fallbackHex })
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(fallbackHex, fontFamily = FontFamily.Monospace) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlaceholderInlineSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PlaceholderInlineSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 100)) },
            valueRange = 0f..100f
        )
    }
}

@Composable
private fun PlaceholderImageSlotCard(
    title: String,
    description: String,
    currentBitmap: Bitmap?,
    currentRef: String?,
    fallbackIcon: ImageVector,
    isCirclePreview: Boolean,
    presetIds: List<String>,
    onPickGallery: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onUseAppIcon: (() -> Unit)?,
    onClear: () -> Unit
) {
    val previewShape = if (isCirclePreview) CircleShape else RoundedCornerShape(14.dp)
    val imageBitmap = remember(currentBitmap) { currentBitmap?.asImageBitmap() }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(previewShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, previewShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onPickGallery,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.studio_placeholder_img_pick_gallery))
                }

                if (onUseAppIcon != null) {
                    OutlinedButton(
                        onClick = onUseAppIcon,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.studio_placeholder_img_use_app_icon))
                    }
                }

                if (!currentRef.isNullOrBlank()) {
                    TextButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.studio_placeholder_img_clear))
                    }
                }
            }

            if (presetIds.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.studio_placeholder_img_samples),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        presetIds.forEach { presetId ->
                            val swatchBmp = remember(presetId) {
                                PreviewPlaceholderRepository.createPresetSampleBitmap(presetId, 96).asImageBitmap()
                            }
                            val isSelected = currentRef == presetId
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(previewShape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = previewShape
                                    )
                                    .clickable { onSelectPreset(presetId) },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = swatchBmp,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
