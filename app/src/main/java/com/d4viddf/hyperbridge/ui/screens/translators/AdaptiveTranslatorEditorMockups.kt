package com.d4viddf.hyperbridge.ui.screens.translators

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.models.translator.CompactPillConfig
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PillLeftDesign
import com.d4viddf.hyperbridge.models.translator.PillRightDesign
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.translator.TextSlotConfig
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.ui.components.EngineOptionCard
import com.d4viddf.hyperbridge.ui.components.EnginePreview
import com.d4viddf.hyperbridge.ui.components.island.HyperOsIslandPreview
import com.d4viddf.hyperbridge.ui.components.island.IslandPreviewMode
import com.d4viddf.hyperbridge.ui.components.island.IslandTemplateGallery
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme

/**
 * Navigation nodes for the Adaptive Split-Pane & Cascading Translator Editor mockup.
 */
enum class MockTranslatorNode {
    MAIN_MENU,
    APPS,
    CONDITIONS,
    PRESENTATION,
    PRESENTATION_TEMPLATES,
    PILL,
    PROGRESS,
    ACTIONS,
    BEHAVIOR,
    BEHAVIOR_ENGINE
}

@Composable
private fun TranslatorRailShellMockup(
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            NavigationRailItem(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Filled.Brush, null) },
                label = { Text("Design") }
            )
            NavigationRailItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Outlined.NotificationsActive, null) },
                label = { Text("Active") }
            )
            NavigationRailItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Outlined.Apps, null) },
                label = { Text("Library") }
            )
            NavigationRailItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Outlined.Settings, null) },
                label = { Text("Settings") }
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

/**
 * Adaptive Split-Pane & Cascading Translator Editor Mockup.
 *
 * Follows the exact same design principles as [com.d4viddf.hyperbridge.ui.screens.theme.AdaptiveThemeCreatorSplitMockup]:
 * - Always shows 2 panes side-by-side on Tablets & Foldables (`0.44f` Left / `0.56f` Right):
 *   - Depth 1 (`[MAIN_MENU]`):
 *     - Left Pane: Main Translator List (TopAppBar + Save, Live HyperOS Island Preview, Edit Info & Priority, 7 Category Cards).
 *     - Right Pane: "Nothing is selected — Please select a section from the list on the left..." placeholder state.
 *   - Depth 2 (`[MAIN_MENU, PRESENTATION]`, `[MAIN_MENU, PILL]`, `[MAIN_MENU, CONDITIONS]`, etc.):
 *     - Left Pane: Main Translator List with the active section card highlighted and Live Island Preview visible alongside the editor.
 *     - Right Pane: Selected section editor inside [TranslatorDetailPaneSurface] (no duplicate full Island Preview needed since Left Pane already displays it).
 *   - Depth 3+ (`[MAIN_MENU, PRESENTATION, PRESENTATION_TEMPLATES]` or `[MAIN_MENU, BEHAVIOR, BEHAVIOR_ENGINE]`):
 *     - Left Pane: Cascades to `stack[stack.size - 2]` (`PRESENTATION` or `BEHAVIOR`) inside [TranslatorDetailPaneSurface].
 *     - Right Pane: Cascades to `stack.last()` (`PRESENTATION_TEMPLATES` or `BEHAVIOR_ENGINE`).
 */
@Composable
fun AdaptiveTranslatorEditorSplitMockup(
    stack: List<MockTranslatorNode> = listOf(MockTranslatorNode.MAIN_MENU)
) {
    val sampleTranslator = remember {
        CustomTranslator(
            id = "mock_delivery",
            meta = TranslatorMetadata(
                name = "Delivery Tracker",
                author = "HyperBridge",
                description = "Live courier & order progress updates",
                iconName = "LocalShipping"
            ),
            targetScope = TargetScope.SPECIFIC_APPS,
            targetPackages = listOf("com.ubercab.eats", "com.dd.doordash"),
            presentation = PresentationConfig(
                mode = PresentationMode.TEMPLATE,
                templateId = "tpl_standard_progress",
                textSlot = TextSlotConfig(
                    titleTemplate = "{notif.title}",
                    subtitleTemplate = "{notif.text}",
                    highlightTextTemplate = "Arriving in 8m"
                ),
                progressSlot = ProgressSlotConfig(
                    type = ProgressSlotType.PROGRESS_BAR,
                    showPercentage = true
                ),
                pill = CompactPillConfig(
                    leftDesign = PillLeftDesign.ICON_AND_TEXT,
                    rightDesign = PillRightDesign.PROGRESS_PERCENT
                )
            )
        )
    }

    val leftNode = if (stack.size >= 2) stack[stack.size - 2] else MockTranslatorNode.MAIN_MENU
    val rightNode = if (stack.size >= 2) stack.last() else null

    TranslatorRailShellMockup {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT PANE (Default Translator List at depth 1-2, cascades at depth 3+)
            Box(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight()
            ) {
                if (leftNode == MockTranslatorNode.MAIN_MENU) {
                    MockTranslatorMainListPane(
                        translator = sampleTranslator,
                        selectedChild = rightNode
                    )
                } else {
                    TranslatorDetailPaneSurface {
                        MockRenderTranslatorNode(
                            node = leftNode,
                            translator = sampleTranslator,
                            selectedChild = rightNode,
                            isLeftCascadedPane = true
                        )
                    }
                }
            }

            // RIGHT PANE (Empty placeholder when nothing is selected, or active detail pane)
            Box(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight()
            ) {
                TranslatorDetailPaneSurface {
                    if (rightNode == null) {
                        MockTranslatorNothingSelectedPane()
                    } else {
                        MockRenderTranslatorNode(
                            node = rightNode,
                            translator = sampleTranslator,
                            selectedChild = null,
                            isLeftCascadedPane = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslatorDetailPaneSurface(
    content: @Composable () -> Unit
) {
    val baseScheme = MaterialTheme.colorScheme
    val detailScheme = remember(baseScheme) {
        baseScheme.copy(
            background = baseScheme.surfaceContainerLow,
            surface = baseScheme.surfaceContainerLow,
            surfaceContainerLow = baseScheme.surfaceContainer
        )
    }
    MaterialTheme(colorScheme = detailScheme) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = baseScheme.surfaceContainerLow
        ) {
            content()
        }
    }
}

// ============================================================================
// LEFT PANE (DEFAULT MAIN LIST)
// ============================================================================

@Composable
private fun MockTranslatorMainListPane(
    translator: CustomTranslator,
    selectedChild: MockTranslatorNode?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = translator.meta.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {},
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Live HyperOS Island Preview at top of Left Pane
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                HyperOsIslandPreview(
                    translator = translator,
                    initialMode = if (selectedChild == MockTranslatorNode.PILL) {
                        IslandPreviewMode.COMPACT_PILL
                    } else {
                        IslandPreviewMode.EXPANDED
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Outlined.LocalShipping, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Info & Priority", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                val items = listOf(
                    Triple(MockTranslatorNode.APPS, "Target Applications", "2 selected apps") to Icons.Outlined.Apps,
                    Triple(MockTranslatorNode.CONDITIONS, "Match Conditions", "Filter by title, text regex, category") to Icons.Outlined.FilterList,
                    Triple(MockTranslatorNode.PRESENTATION, "Presentation & Layout", "Title, subtitle templates, tags") to Icons.AutoMirrored.Outlined.ViewQuilt,
                    Triple(MockTranslatorNode.PILL, "Pill Customization", "Compact island left & right slot design") to Icons.Outlined.DashboardCustomize,
                    Triple(MockTranslatorNode.PROGRESS, "Progress & Timing", "Bar, timer, waypoints, text regex") to Icons.Outlined.Speed,
                    Triple(MockTranslatorNode.ACTIONS, "Buttons", "Smart actions, intent triggers, action buttons") to Icons.Outlined.TouchApp,
                    Triple(MockTranslatorNode.BEHAVIOR, "Behavior & Engine", "Island timeouts, popup floats, engine mode") to Icons.Outlined.DisplaySettings
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items.forEachIndexed { index, (triple, icon) ->
                        val (node, title, subtitle) = triple
                        val isSelected = selectedChild == node
                        val shape = if (isSelected) {
                            RoundedCornerShape(20.dp)
                        } else {
                            getExpressiveShape(items.size, index, ShapeStyle.Large)
                        }

                        MockSelectableTranslatorOptionCard(
                            title = title,
                            subtitle = subtitle,
                            icon = icon,
                            shape = shape,
                            selected = isSelected
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MockSelectableTranslatorOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    shape: Shape,
    selected: Boolean,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = {},
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 76.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 76.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ============================================================================
// RIGHT PANE: "NOTHING SELECTED" EMPTY PLACEHOLDER STATE
// ============================================================================

@Composable
private fun MockTranslatorNothingSelectedPane() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(340.dp)
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Nothing is selected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Please select a section from the list on the left to start configuring your translator.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// DETAIL & CASCADED PANES
// ============================================================================

@Composable
private fun MockRenderTranslatorNode(
    node: MockTranslatorNode,
    translator: CustomTranslator,
    selectedChild: MockTranslatorNode?,
    isLeftCascadedPane: Boolean
) {
    val title = when (node) {
        MockTranslatorNode.APPS -> "Target Applications"
        MockTranslatorNode.CONDITIONS -> "Match Conditions"
        MockTranslatorNode.PRESENTATION -> "Presentation & Layout"
        MockTranslatorNode.PRESENTATION_TEMPLATES -> "Template Gallery"
        MockTranslatorNode.PILL -> "Pill Customization"
        MockTranslatorNode.PROGRESS -> "Progress & Timing"
        MockTranslatorNode.ACTIONS -> "Buttons"
        MockTranslatorNode.BEHAVIOR -> "Behavior & Engine"
        MockTranslatorNode.BEHAVIOR_ENGINE -> "Rendering Engine"
        else -> "Configure"
    }

    val subtitle = when (node) {
        MockTranslatorNode.PRESENTATION_TEMPLATES -> "Presentation & Layout"
        MockTranslatorNode.BEHAVIOR_ENGINE -> "Behavior & Engine"
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {},
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = if (isLeftCascadedPane) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            when (node) {
                MockTranslatorNode.PRESENTATION -> MockPresentationDetailPane(
                    translator = translator,
                    showStickyPreview = isLeftCascadedPane,
                    isTemplateGallerySelected = selectedChild == MockTranslatorNode.PRESENTATION_TEMPLATES
                )
                MockTranslatorNode.PRESENTATION_TEMPLATES -> MockTemplateGalleryPane(
                    currentTemplateId = translator.presentation.templateId
                )
                MockTranslatorNode.PILL -> MockPillDetailPane()
                MockTranslatorNode.CONDITIONS -> MockConditionsDetailPane()
                MockTranslatorNode.BEHAVIOR -> MockBehaviorDetailPane(
                    isEngineSelected = selectedChild == MockTranslatorNode.BEHAVIOR_ENGINE
                )
                MockTranslatorNode.BEHAVIOR_ENGINE -> MockBehaviorEngineDetailPane()
                else -> {}
            }
        }
    }
}

@Composable
private fun MockPresentationDetailPane(
    translator: CustomTranslator,
    showStickyPreview: Boolean,
    isTemplateGallerySelected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showStickyPreview) {
            HyperOsIslandPreview(
                translator = translator,
                showChrome = false
            )
        }

        // 1. Presentation Mode Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.DashboardCustomize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Presentation Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose how the notification island is structured",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.DashboardCustomize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Xiaomi Preset Template",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Prebuilt HyperOS 3 island cards with slot bindings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = {},
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 2. Template Selector Card (highlights when cascaded to Template Gallery!)
        Card(
            shape = RoundedCornerShape(20.dp),
            border = if (isTemplateGallerySelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            colors = CardDefaults.cardColors(
                containerColor = if (isTemplateGallerySelected) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.DashboardCustomize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Island Template",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Pick a built-in Xiaomi HyperOS template and bind notification fields to its slots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTemplateGallerySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        contentColor = if (isTemplateGallerySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.DashboardCustomize, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Standard Progress Card")
                }
            }
        }

        // 3. Linked Theme Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Linked Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Brush,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Use Active Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = {},
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Select", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 4. Text Slot Bindings Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Text Slot Bindings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = "{notif.title}",
                    onValueChange = {},
                    label = { Text("Title Template") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = "{notif.text}",
                    onValueChange = {},
                    label = { Text("Subtitle Template") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MockTemplateGalleryPane(currentTemplateId: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Choose a preset HyperOS 3 island layout. Slot bindings stay editable on the left.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IslandTemplateGallery(
            currentTemplateId = currentTemplateId,
            onTemplateSelected = {},
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MockPillDetailPane() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Learn how Compact Pill works",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Slot Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {}
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Left Content",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Icon + Text",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {}
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Right Content",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Progress Percentage (72%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MockConditionsDetailPane() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Only activate this translator when incoming notifications match all configured rules.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {},
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Condition Rule", fontWeight = FontWeight.SemiBold)
        }

        // Title Regex Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Title, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Title Regex", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
                OutlinedTextField(
                    value = "^(Order|Courier|Arriving).*",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Text Regex Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Subject, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Body Text Regex", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
                OutlinedTextField(
                    value = "\\d+\\s*(min|mins|minutes)",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
private fun MockBehaviorDetailPane(isEngineSelected: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Engine Configuration",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            border = if (isEngineSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            colors = CardDefaults.cardColors(
                containerColor = if (isEngineSelected) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rendering Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Xiaomi HyperOS Island",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Change Engine")
                }
            }
        }

        Text(
            text = "Global Behavior",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Float on Update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Pop up expanded island when notification updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = true, onCheckedChange = {})
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Remove Original Notification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Hide standard shade card while island is active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = false, onCheckedChange = {})
                }
            }
        }
    }
}

@Composable
private fun MockBehaviorEngineDetailPane() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        EnginePreview(isNative = false)
        Spacer(Modifier.height(8.dp))
        Text("Engine Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        EngineOptionCard(
            title = "Inherit from Active Theme",
            description = "Follows the engine configured in your active global theme",
            isSelected = false,
            onClick = {}
        )
        EngineOptionCard(
            title = "Xiaomi HyperOS Island",
            description = "Full custom capsule, templates, and micro-widget support",
            isSelected = true,
            onClick = {}
        )
        EngineOptionCard(
            title = "Android Live Updates",
            description = "Native status bar chip and progress notifications",
            isSelected = false,
            onClick = {}
        )
    }
}

// ============================================================================
// COMPOSE PREVIEWS (TABLET & FOLDABLE)
// ============================================================================

@Preview(
    name = "1A. Tablet — Nothing Selected (Default Split State)",
    device = Devices.TABLET,
    showBackground = true
)
@Composable
fun PreviewTranslatorTabletNothingSelected() {
    HyperBridgeTheme {
        AdaptiveTranslatorEditorSplitMockup(
            stack = listOf(MockTranslatorNode.MAIN_MENU)
        )
    }
}

@Preview(
    name = "1B. Foldable — Nothing Selected (Default Split State)",
    device = Devices.FOLDABLE,
    showBackground = true
)
@Composable
fun PreviewTranslatorFoldableNothingSelected() {
    HyperBridgeTheme {
        AdaptiveTranslatorEditorSplitMockup(
            stack = listOf(MockTranslatorNode.MAIN_MENU)
        )
    }
}

@Preview(
    name = "2A. Tablet — Presentation Selected (Main List Left + Presentation Right)",
    device = Devices.TABLET,
    showBackground = true
)
@Composable
fun PreviewTranslatorTabletPresentationSelected() {
    HyperBridgeTheme {
        AdaptiveTranslatorEditorSplitMockup(
            stack = listOf(MockTranslatorNode.MAIN_MENU, MockTranslatorNode.PRESENTATION)
        )
    }
}

@Preview(
    name = "2B. Foldable — Pill Customization Selected (Main List Left + Pill Right)",
    device = Devices.FOLDABLE,
    showBackground = true
)
@Composable
fun PreviewTranslatorFoldablePillSelected() {
    HyperBridgeTheme {
        AdaptiveTranslatorEditorSplitMockup(
            stack = listOf(MockTranslatorNode.MAIN_MENU, MockTranslatorNode.PILL)
        )
    }
}

@Preview(
    name = "2C. Tablet — Match Conditions Selected (Main List Left + Conditions Right)",
    device = Devices.TABLET,
    showBackground = true
)
@Composable
fun PreviewTranslatorTabletConditionsSelected() {
    HyperBridgeTheme {
        AdaptiveTranslatorEditorSplitMockup(
            stack = listOf(MockTranslatorNode.MAIN_MENU, MockTranslatorNode.CONDITIONS)
        )
    }
}

@Preview(
    name = "3A. Tablet — Cascaded Template Gallery (Presentation Left -> Gallery Right)",
    device = Devices.TABLET,
    showBackground = true
)
@Composable
fun PreviewTranslatorTabletCascadedTemplates() {
    HyperBridgeTheme {
        AdaptiveTranslatorEditorSplitMockup(
            stack = listOf(
                MockTranslatorNode.MAIN_MENU,
                MockTranslatorNode.PRESENTATION,
                MockTranslatorNode.PRESENTATION_TEMPLATES
            )
        )
    }
}

@Preview(
    name = "3B. Foldable — Cascaded Engine Picker (Behavior Left -> Engine Right)",
    device = Devices.FOLDABLE,
    showBackground = true
)
@Composable
fun PreviewTranslatorFoldableCascadedEngine() {
    HyperBridgeTheme {
        AdaptiveTranslatorEditorSplitMockup(
            stack = listOf(
                MockTranslatorNode.MAIN_MENU,
                MockTranslatorNode.BEHAVIOR,
                MockTranslatorNode.BEHAVIOR_ENGINE
            )
        )
    }
}
