package com.d4viddf.hyperbridge.ui.screens.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.ui.components.EngineOptionCard
import com.d4viddf.hyperbridge.ui.components.EnginePreview
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme

/**
 * Navigation nodes for the cascading Split-Pane Theme Creator mockup.
 */
enum class MockCreatorNode {
    MAIN_MENU,
    BEHAVIOR_MENU,
    BEHAVIOR_ENGINE,
    BEHAVIOR_ISLAND,
    BEHAVIOR_TYPES,
    COLORS,
    ICONS,
    CALLS,
    NAVIGATION,
    REPLY,
    ACTIONS,
    APPS,
    APP_EDITOR_MENU,
    APP_COLORS
}

@Composable
private fun CreatorRailShellMockup(
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
 * Adaptive Split-Pane & Cascading Theme Creator Mockup.
 *
 * - Always shows 2 panes side-by-side on Tablets & Foldables:
 *   - Depth 1 (`[MAIN_MENU]`):
 *     - Left Pane: Main Creator List (Header + Save, Live Island Preview, Edit Theme Info, 8 Category Cards).
 *     - Right Pane: "Nothing is selected — Please select an option to edit" placeholder state.
 *   - Depth 2 (`[MAIN_MENU, COLORS]` or `[MAIN_MENU, BEHAVIOR_MENU]`):
 *     - Left Pane: Main Creator List with the active option highlighted.
 *     - Right Pane: Selected option editor (`COLORS` or `BEHAVIOR_MENU`).
 *   - Depth 3+ (`[MAIN_MENU, BEHAVIOR_MENU, BEHAVIOR_ENGINE]` or `[MAIN_MENU, APPS, APP_EDITOR_MENU]`):
 *     - Left Pane: Cascades to `stack[stack.size - 2]` (`BEHAVIOR_MENU` or `APPS`) with the active child highlighted.
 *     - Right Pane: Cascades to `stack.last()` (`BEHAVIOR_ENGINE` or `APP_EDITOR_MENU`).
 */
@Composable
fun AdaptiveThemeCreatorSplitMockup(
    stack: List<MockCreatorNode> = listOf(MockCreatorNode.MAIN_MENU)
) {
    val leftNode = if (stack.size >= 2) stack[stack.size - 2] else MockCreatorNode.MAIN_MENU
    val rightNode = if (stack.size >= 2) stack.last() else null

    CreatorRailShellMockup {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT PANE (Always visible: Default Creator List at depth 1-2, cascades at depth 3+)
            Box(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight()
            ) {
                if (leftNode == MockCreatorNode.MAIN_MENU) {
                    MockCreatorMainListPane(selectedChild = rightNode)
                } else {
                    CreatorDetailPaneSurface {
                        MockRenderCreatorNode(
                            node = leftNode,
                            selectedChild = rightNode,
                            isLeftCascadedPane = true
                        )
                    }
                }
            }

            // RIGHT PANE (Always visible: Empty placeholder when nothing is selected, or active detail pane)
            Box(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight()
            ) {
                CreatorDetailPaneSurface {
                    if (rightNode == null) {
                        MockCreatorNothingSelectedPane()
                    } else {
                        MockRenderCreatorNode(
                            node = rightNode,
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
private fun CreatorDetailPaneSurface(
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
private fun MockCreatorMainListPane(
    selectedChild: MockCreatorNode?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Theme",
                        fontWeight = FontWeight.Bold
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
            // Live Island Preview Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    SharedThemePreview(
                        highlightColorHex = "#6750A4",
                        useAppColors = false,
                        shapeId = "cookie",
                        paddingPercent = 15,
                        answerColorHex = "#34C759",
                        declineColorHex = "#FF3B30",
                        answerShapeId = "circle",
                        declineShapeId = "circle"
                    )
                }
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
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Theme Info", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                val items = listOf(
                    Triple(MockCreatorNode.BEHAVIOR_MENU, "Engine & Behavior", "Timeouts, island style & triggers") to Icons.Outlined.DisplaySettings,
                    Triple(MockCreatorNode.COLORS, "Colors", "Highlight & background palette") to Icons.Outlined.ColorLens,
                    Triple(MockCreatorNode.ICONS, "Icons & Shapes", "Corner shapes & icon padding") to Icons.Outlined.Widgets,
                    Triple(MockCreatorNode.CALLS, "Call Style", "Answer & decline buttons") to Icons.Outlined.Call,
                    Triple(MockCreatorNode.NAVIGATION, "Navigation Layout", "Turn-by-turn island layout") to Icons.Outlined.Map,
                    Triple(MockCreatorNode.REPLY, "Inline Reply", "Quick reply bar appearance") to Icons.Outlined.Edit,
                    Triple(MockCreatorNode.ACTIONS, "Smart Actions", "Keyword-based action buttons") to Icons.Outlined.TouchApp,
                    Triple(MockCreatorNode.APPS, "App Overrides", "Per-app custom theme rules") to Icons.Outlined.Apps
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

                        val trailing: (@Composable () -> Unit)? = when (node) {
                            MockCreatorNode.COLORS -> {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF6750A4))
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    )
                                }
                            }
                            MockCreatorNode.ICONS -> {
                                {
                                    Icon(
                                        Icons.Outlined.Image,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            else -> null
                        }

                        MockSelectableCreatorOptionCard(
                            title = title,
                            subtitle = subtitle,
                            icon = icon,
                            shape = shape,
                            selected = isSelected,
                            trailingContent = trailing
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MockSelectableCreatorOptionCard(
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
private fun MockCreatorNothingSelectedPane() {
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
                text = "Please select an option from the list on the left to start customizing your theme.",
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
private fun MockRenderCreatorNode(
    node: MockCreatorNode,
    selectedChild: MockCreatorNode?,
    isLeftCascadedPane: Boolean
) {
    val title = when (node) {
        MockCreatorNode.BEHAVIOR_MENU -> "Behavior & Triggers"
        MockCreatorNode.BEHAVIOR_ENGINE -> "Rendering Engine"
        MockCreatorNode.COLORS -> "Colors"
        MockCreatorNode.APPS -> "App Overrides"
        MockCreatorNode.APP_EDITOR_MENU -> "Edit App"
        MockCreatorNode.APP_COLORS -> "Spotify · Colors"
        else -> "Customize"
    }

    val subtitle = when (node) {
        MockCreatorNode.APP_EDITOR_MENU -> "Spotify (com.spotify.music)"
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold)
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
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
                actions = {
                    if (node == MockCreatorNode.APP_EDITOR_MENU) {
                        Button(
                            onClick = {},
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
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
                MockCreatorNode.COLORS, MockCreatorNode.APP_COLORS -> MockColorsDetailPane()
                MockCreatorNode.BEHAVIOR_MENU -> MockBehaviorMenuPane(selectedChild = selectedChild)
                MockCreatorNode.BEHAVIOR_ENGINE -> MockBehaviorEnginePane()
                MockCreatorNode.APPS -> MockAppsListPane(selectedChild = selectedChild)
                MockCreatorNode.APP_EDITOR_MENU -> MockAppEditorMenuPane(selectedChild = selectedChild)
                else -> {}
            }
        }
    }
}

@Composable
private fun MockColorsDetailPane() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Color Mode Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dynamic App Icon Colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Automatically extract accent colors from each app's icon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = false, onCheckedChange = {})
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("Material Presets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        val colors = listOf(
            Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006E1C),
            Color(0xFFBA1A1A), Color(0xFF984061), Color(0xFF7D5260)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (index == 0) 3.dp else 1.dp,
                            color = if (index == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index == 0) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Bottom Floating Toolbar simulation
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Presets", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Colorize, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Custom", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun MockBehaviorMenuPane(selectedChild: MockCreatorNode?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val items = listOf(
            Triple(MockCreatorNode.BEHAVIOR_ENGINE, "Rendering Engine", "Choose Xiaomi HyperOS or Android 16 Live Updates") to Icons.Outlined.Memory,
            Triple(MockCreatorNode.BEHAVIOR_ISLAND, "Island Behavior", "Floating behavior, shade visibility & timeout") to Icons.Outlined.DisplaySettings,
            Triple(MockCreatorNode.BEHAVIOR_TYPES, "Active Notification Types", "Select which notification categories trigger islands") to Icons.Outlined.NotificationsActive
        )

        items.forEachIndexed { index, (triple, icon) ->
            val (node, title, sub) = triple
            val isSelected = selectedChild == node
            val shape = if (isSelected) RoundedCornerShape(20.dp) else getExpressiveShape(items.size, index, ShapeStyle.Large)
            MockSelectableCreatorOptionCard(
                title = title,
                subtitle = sub,
                icon = icon,
                shape = shape,
                selected = isSelected
            )
        }
    }
}

@Composable
private fun MockBehaviorEnginePane() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        EnginePreview(isNative = false)
        Spacer(Modifier.height(24.dp))
        Text("Engine Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        EngineOptionCard(
            title = "Xiaomi HyperOS Island",
            description = "Full custom capsule and expanded templates",
            isSelected = true,
            onClick = {}
        )
        Spacer(Modifier.height(10.dp))
        EngineOptionCard(
            title = "Android Live Updates",
            description = "Native status bar chip and progress notifications",
            isSelected = false,
            onClick = {}
        )
    }
}

@Composable
private fun MockAppsListPane(selectedChild: MockCreatorNode?) {
    val isAppSelected = selectedChild == MockCreatorNode.APP_EDITOR_MENU
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add App Override")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text("Search configured apps...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            val apps = listOf(
                "Spotify" to "com.spotify.music",
                "WhatsApp" to "com.whatsapp"
            )
            apps.forEachIndexed { index, (name, pkg) ->
                val selected = isAppSelected && index == 0
                Card(
                    onClick = {},
                    shape = if (selected) RoundedCornerShape(20.dp) else getExpressiveShape(apps.size, index, ShapeStyle.Large),
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilledTonalIconButton(
                            onClick = {},
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun MockAppEditorMenuPane(selectedChild: MockCreatorNode?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                SharedThemePreview(
                    highlightColorHex = "#1DB954",
                    useAppColors = false,
                    shapeId = "cookie",
                    paddingPercent = 15,
                    answerColorHex = "#34C759",
                    declineColorHex = "#FF3B30",
                    answerShapeId = "circle",
                    declineShapeId = "circle"
                )
            }
        }

        val items = listOf(
            Triple(MockCreatorNode.BEHAVIOR_MENU, "Engine & Behavior", "Custom per-app triggers") to Icons.Outlined.DisplaySettings,
            Triple(MockCreatorNode.APP_COLORS, "Colors", "Custom (#1DB954)") to Icons.Outlined.ColorLens,
            Triple(MockCreatorNode.ICONS, "Icons & Shapes", "Using theme default") to Icons.Outlined.Widgets,
            Triple(MockCreatorNode.CALLS, "Call Style", "Using theme default") to Icons.Outlined.Call,
            Triple(MockCreatorNode.NAVIGATION, "Navigation Layout", "Using theme default") to Icons.Outlined.Map,
            Triple(MockCreatorNode.ACTIONS, "Smart Actions", "2 custom action rules") to Icons.Outlined.TouchApp
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { index, (triple, icon) ->
                val (node, title, sub) = triple
                val isSelected = selectedChild == node
                val shape = if (isSelected) RoundedCornerShape(20.dp) else getExpressiveShape(items.size, index, ShapeStyle.Large)
                MockSelectableCreatorOptionCard(
                    title = title,
                    subtitle = sub,
                    icon = icon,
                    shape = shape,
                    selected = isSelected
                )
            }
        }
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
fun PreviewCreatorTabletNothingSelected() {
    HyperBridgeTheme {
        AdaptiveThemeCreatorSplitMockup(
            stack = listOf(MockCreatorNode.MAIN_MENU)
        )
    }
}

@Preview(
    name = "1B. Foldable — Nothing Selected (Default Split State)",
    device = Devices.FOLDABLE,
    showBackground = true
)
@Composable
fun PreviewCreatorFoldableNothingSelected() {
    HyperBridgeTheme {
        AdaptiveThemeCreatorSplitMockup(
            stack = listOf(MockCreatorNode.MAIN_MENU)
        )
    }
}

@Preview(
    name = "2A. Tablet — Option Selected (Main List Left + Colors Right)",
    device = Devices.TABLET,
    showBackground = true
)
@Composable
fun PreviewCreatorTabletOptionSelected() {
    HyperBridgeTheme {
        AdaptiveThemeCreatorSplitMockup(
            stack = listOf(MockCreatorNode.MAIN_MENU, MockCreatorNode.COLORS)
        )
    }
}

@Preview(
    name = "3A. Tablet — Cascaded Sub-Menu (Behavior Menu Left -> Engine Right)",
    device = Devices.TABLET,
    showBackground = true
)
@Composable
fun PreviewCreatorTabletCascadedBehavior() {
    HyperBridgeTheme {
        AdaptiveThemeCreatorSplitMockup(
            stack = listOf(
                MockCreatorNode.MAIN_MENU,
                MockCreatorNode.BEHAVIOR_MENU,
                MockCreatorNode.BEHAVIOR_ENGINE
            )
        )
    }
}

@Preview(
    name = "3B. Foldable — Cascaded App Override (App Editor Left -> App Colors Right)",
    device = Devices.FOLDABLE,
    showBackground = true
)
@Composable
fun PreviewCreatorFoldableCascadedAppOverride() {
    HyperBridgeTheme {
        AdaptiveThemeCreatorSplitMockup(
            stack = listOf(
                MockCreatorNode.MAIN_MENU,
                MockCreatorNode.APPS,
                MockCreatorNode.APP_EDITOR_MENU,
                MockCreatorNode.APP_COLORS
            )
        )
    }
}
