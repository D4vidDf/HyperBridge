package com.d4viddf.hyperbridge.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.ui.AppCategory
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.SystemIntegrationId
import com.d4viddf.hyperbridge.ui.SystemIntegrationInfo
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme

// ============================================================================
// SHARED MOCK DATA & UNIFIED EXPRESSIVE RAIL SHELL
// ============================================================================

private val sampleApps = listOf(
    AppInfo("Spotify", "com.spotify.music", null, isBridged = true, category = AppCategory.MUSIC),
    AppInfo("Google Maps", "com.google.android.apps.maps", null, isBridged = true, category = AppCategory.MAPS),
    AppInfo("YouTube Music", "com.google.android.apps.youtube.music", null, isBridged = true, category = AppCategory.MUSIC),
    AppInfo("Clock & Timer", "com.google.android.deskclock", null, isBridged = true, category = AppCategory.TIMER),
    AppInfo("Telegram", "org.telegram.messenger", null, isBridged = false, category = AppCategory.OTHER),
    AppInfo("WhatsApp", "com.whatsapp", null, isBridged = true, category = AppCategory.OTHER),
    AppInfo("Waze Navigation", "com.waze", null, isBridged = false, category = AppCategory.MAPS),
    AppInfo("Pocket Casts", "au.com.shiftyjelly.pocketcasts", null, isBridged = true, category = AppCategory.MUSIC),
)

private val sampleIntegrations = listOf(
    SystemIntegrationInfo(SystemIntegrationId.SCREEN_RECORDER, enabled = true, available = true),
    SystemIntegrationInfo(SystemIntegrationId.CHARGING, enabled = true, available = true),
    SystemIntegrationInfo(SystemIntegrationId.SYSTEM_UPDATER, enabled = true, available = true),
    SystemIntegrationInfo(SystemIntegrationId.VPN, enabled = false, available = true),
)

enum class DesignModuleSelection {
    NONE,
    THEMES,
    WIDGETS,
    DESIGNS,
    TRANSLATORS
}

enum class AppsScopeFilter {
    ALL_LIBRARY,
    ACTIVE_ONLY
}

/**
 * Minimum content width (excluding the Rail) required to show Master + Detail side-by-side.
 * - On Tablets (1280dp wide -> ~1100dp content width): >= 720.dp -> Side-by-Side Full-Height Detail Panel.
 * - On Unfolded Foldables (673dp wide -> ~577dp content width): < 720.dp -> Detail Panel expands to Full Width
 *   of the content area so it never looks cramped!
 */
private val SIDE_BY_SIDE_DETAIL_MIN_WIDTH = 720.dp

@Composable
private fun UnifiedExpressiveRailShell(
    selectedTab: Int,
    expanded: Boolean = false,
    showHeaderFab: Boolean = false,
    content: @Composable () -> Unit
) {
    val railState = rememberWideNavigationRailState(
        initialValue = if (expanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        WideNavigationRail(
            state = railState,
            colors = WideNavigationRailDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            arrangement = Arrangement.Top,
            header = {
                Column(
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = if (expanded) Icons.AutoMirrored.Rounded.MenuOpen else Icons.Default.Menu,
                            contentDescription = "Toggle Rail"
                        )
                    }
                    if (showHeaderFab) {
                        if (expanded) {
                            ExtendedFloatingActionButton(
                                onClick = {},
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                icon = { Icon(Icons.Rounded.Add, null) },
                                text = { Text("Create New") }
                            )
                        } else {
                            FloatingActionButton(
                                onClick = {},
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add")
                            }
                        }
                    }
                }
            }
        ) {
            WideNavigationRailItem(
                railExpanded = expanded,
                selected = selectedTab == 0,
                onClick = {},
                icon = { Icon(if (selectedTab == 0) Icons.Filled.Brush else Icons.Outlined.Brush, null) },
                label = { Text("Design") }
            )
            WideNavigationRailItem(
                railExpanded = expanded,
                selected = selectedTab == 1,
                onClick = {},
                icon = { Icon(if (selectedTab == 1) Icons.Filled.Apps else Icons.Outlined.Apps, null) },
                label = { Text("Apps") }
            )
            WideNavigationRailItem(
                railExpanded = expanded,
                selected = selectedTab == 2,
                onClick = {},
                icon = { Icon(if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings, null) },
                label = { Text("Settings") }
            )
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

// ============================================================================
// 1. COMBINED DESIGN SCREEN MOCKUP
//    - Overview: Mockup 2 (Hero + 1x4 Bento Strip + 2-Col Featured/Guides)
//    - Option Selected (Wide Tablet): Side-by-Side Master + Full-Height Detail Panel
//    - Option Selected (Narrower Foldable): Full-Width Detail Panel (never cramped!)
// ============================================================================

@Composable
fun CombinedDesignScreenMockup(
    selectedModule: DesignModuleSelection = DesignModuleSelection.NONE,
    expandedRail: Boolean = false
) {
    UnifiedExpressiveRailShell(
        selectedTab = 0,
        expanded = expandedRail,
        showHeaderFab = true
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            val canShowSideBySide = maxWidth >= SIDE_BY_SIDE_DETAIL_MIN_WIDTH
            val hasSelection = selectedModule != DesignModuleSelection.NONE

            // CASE A: Narrower screen (e.g. Foldable) with an option selected -> Full-width Detail Panel
            if (hasSelection && !canShowSideBySide) {
                DesignModuleDetailPanel(
                    modifier = Modifier.fillMaxSize(),
                    isFullWidthMode = true
                )
            } else {
                // CASE B: Overview OR Wide Tablet Side-by-Side Split
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // LEFT / MAIN PANE
                    Column(
                        modifier = Modifier
                            .weight(if (hasSelection) 0.95f else 1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column {
                            Text(
                                text = "HyperBridge Studio",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Themes, app widgets, custom islands & smart translators",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 1. Hero Section
                        if (!hasSelection) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(Modifier.weight(1.6f)) { MockHeroBanner(height = 156) }
                                Card(
                                    modifier = Modifier.weight(1f).height(156.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.14f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(20.dp),
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        Text("Theme Creator Pro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text("Build dynamic island themes", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } else {
                            MockHeroBanner(height = 136)
                        }

                        // 2. Bento Modules: 1x4 Strip when unselected vs 2x2 Grid when side-by-side detail is open
                        if (!hasSelection) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                MockBentoCard(
                                    modifier = Modifier.weight(1f).height(144.dp),
                                    title = "Themes",
                                    status = "Neon City Active",
                                    icon = Icons.Rounded.Palette,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    iconColor = MaterialTheme.colorScheme.primary
                                )
                                MockBentoCard(
                                    modifier = Modifier.weight(1f).height(144.dp),
                                    title = "Widgets",
                                    status = "4 Saved",
                                    icon = Icons.Rounded.Widgets,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    iconColor = MaterialTheme.colorScheme.secondary
                                )
                                MockBentoCard(
                                    modifier = Modifier.weight(1f).height(144.dp),
                                    title = "Designs",
                                    status = "3 Active",
                                    icon = Icons.Outlined.DashboardCustomize,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                    iconColor = MaterialTheme.colorScheme.tertiary
                                )
                                MockBentoCard(
                                    modifier = Modifier.weight(1f).height(144.dp),
                                    title = "Translators",
                                    status = "6 Enabled",
                                    icon = Icons.Default.Extension,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    iconColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    MockBentoCard(
                                        modifier = Modifier.weight(1f).height(124.dp),
                                        title = "Themes",
                                        status = "Selected • 4 Themes",
                                        icon = Icons.Rounded.Palette,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        isSelected = selectedModule == DesignModuleSelection.THEMES
                                    )
                                    MockBentoCard(
                                        modifier = Modifier.weight(1f).height(124.dp),
                                        title = "Widgets",
                                        status = "4 Saved",
                                        icon = Icons.Rounded.Widgets,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                        iconColor = MaterialTheme.colorScheme.secondary,
                                        isSelected = selectedModule == DesignModuleSelection.WIDGETS
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    MockBentoCard(
                                        modifier = Modifier.weight(1f).height(124.dp),
                                        title = "Designs",
                                        status = "3 Active",
                                        icon = Icons.Outlined.DashboardCustomize,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                        iconColor = MaterialTheme.colorScheme.tertiary,
                                        isSelected = selectedModule == DesignModuleSelection.DESIGNS
                                    )
                                    MockBentoCard(
                                        modifier = Modifier.weight(1f).height(124.dp),
                                        title = "Translators",
                                        status = "6 Enabled",
                                        icon = Icons.Default.Extension,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        iconColor = MaterialTheme.colorScheme.onSurface,
                                        isSelected = selectedModule == DesignModuleSelection.TRANSLATORS
                                    )
                                }
                            }
                        }

                        // 3. Featured & Guides Section
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!hasSelection) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) { MockFeaturedRow() }
                                    Column(modifier = Modifier.weight(1f)) { MockGuidesList() }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    MockFeaturedRow()
                                    MockGuidesList()
                                }
                            }
                        }
                    }

                    // FULL-HEIGHT RIGHT DETAIL PANEL (on Wide Tablet)
                    if (hasSelection) {
                        DesignModuleDetailPanel(
                            modifier = Modifier
                                .weight(1.05f)
                                .fillMaxHeight(),
                            isFullWidthMode = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignModuleDetailPanel(
    modifier: Modifier = Modifier,
    isFullWidthMode: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isFullWidthMode) {
                        FilledTonalIconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Design Hub")
                        }
                    }
                    Column {
                        Text(
                            text = "Installed Themes",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select, import, or customize an island theme",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExtendedFloatingActionButton(
                        onClick = {},
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = { Icon(Icons.Rounded.Add, null) },
                        text = { Text("New Theme") },
                        modifier = Modifier.height(40.dp)
                    )
                    if (!isFullWidthMode) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Close, contentDescription = "Close Detail Panel")
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isFullWidthMode) 260.dp else 320.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val themes = listOf(
                    Triple("Neon City", "by David • Active Theme", true),
                    Triple("Sunset Glow", "by Alice • Material Expressive", false),
                    Triple("HyperOS Stock", "by System • Minimalist", false),
                    Triple("AMOLED Pitch", "by Community • High Contrast", false)
                )
                items(themes) { (name, author, active) ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (active) Icons.Rounded.Check else Icons.Outlined.Palette,
                                            contentDescription = null,
                                            tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column {
                                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Rounded.Tune, contentDescription = "Edit")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. UNIFIED APPS SCREEN MOCKUP (Active + Library Merged)
//    - Overview (No app selected): Unified Top Bar + System Integrations Left Sidebar + Multi-Column App Grid
//    - App Selected on Wide Tablet:
//        * Left Pane: Phone-style vertically stacked header (Title + Active/All Toggle + SearchBar + Category Pills)
//          above the scrollable App list
//        * Right Pane: FULL-HEIGHT App Detail Panel from top to bottom!
//    - App Selected on Narrower Screen (Foldable):
//        * FULL-WIDTH App Detail Panel so it never looks cramped!
// ============================================================================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CombinedUnifiedAppsScreenMockup(
    scopeFilter: AppsScopeFilter = AppsScopeFilter.ALL_LIBRARY,
    selectedAppPackage: String? = null,
    expandedRail: Boolean = false
) {
    val displayedApps = if (scopeFilter == AppsScopeFilter.ACTIVE_ONLY) {
        sampleApps.filter { it.isBridged }
    } else {
        sampleApps
    }

    UnifiedExpressiveRailShell(
        selectedTab = 1,
        expanded = expandedRail
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            val canShowSideBySide = maxWidth >= SIDE_BY_SIDE_DETAIL_MIN_WIDTH
            val hasSelection = selectedAppPackage != null
            val selectedApp = sampleApps.find { it.packageName == selectedAppPackage } ?: sampleApps.first()

            when {
                // CASE 1: Narrower screen (e.g. Foldable) with an App selected -> FULL-WIDTH Detail Panel
                hasSelection && !canShowSideBySide -> {
                    AppDetailFullHeightPanel(
                        app = selectedApp,
                        isFullWidthMode = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // CASE 2: Wide screen (Tablet) with an App selected ->
                // Left Pane has vertically stacked controls (like on phone) + App list;
                // Right Pane is a FULL-HEIGHT Detail Panel!
                hasSelection && canShowSideBySide -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // LEFT PANE: Stacked Phone-Style Controls + Scrollable List
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Title + Segmented [Active | All Apps] Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Apps",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                AppsScopeSegmentedToggle(scopeFilter = scopeFilter)
                            }

                            // 2. Full-width Search Bar (stacked like on phone)
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    Text("Search apps or packages...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            // 3. Connected Category Filter Row (stacked like on phone)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val categories = listOf("All", "System", "Music", "Maps", "Timers")
                                categories.forEachIndexed { index, label ->
                                    val shape = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        categories.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                    ToggleButton(
                                        checked = index == 0,
                                        onCheckedChange = {},
                                        shapes = shape,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(label)
                                    }
                                }
                            }

                            // 4. Single-column scrollable list of Expressive App Cards
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                items(displayedApps) { app ->
                                    MockExpressiveAppCard(
                                        app = app,
                                        highlighted = app.packageName == selectedAppPackage
                                    )
                                }
                            }
                        }

                        // RIGHT PANE: FULL-HEIGHT Detail Panel from top to bottom!
                        AppDetailFullHeightPanel(
                            app = selectedApp,
                            isFullWidthMode = false,
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // CASE 3: Overview State (No App Selected) -> Unified Header + System Integrations Sidebar + Multi-Column Grid
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top Row: Title + Segmented [Active | All Apps] + Inline Search
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Apps & Bridges",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                AppsScopeSegmentedToggle(scopeFilter = scopeFilter)
                            }

                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.width(260.dp).height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    Text("Search apps...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        // Second Row: Connected Category Filter Pills + Bridge All Apps Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                                val categories = listOf("All", "Music", "Maps", "Timers", "Other")
                                categories.forEachIndexed { index, label ->
                                    val shape = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        categories.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                    ToggleButton(
                                        checked = index == 0,
                                        onCheckedChange = {},
                                        shapes = shape,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(label)
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Apps, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("Bridge All Apps", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    Switch(checked = false, onCheckedChange = {})
                                }
                            }
                        }

                        // Main Body: System Integrations Left Sidebar + Adaptive Multi-Column App Grid
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .width(275.dp)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "System Integrations",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "3/4 Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(sampleIntegrations) { integration ->
                                            MockSystemIntegrationCard(integration)
                                        }
                                    }
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 250.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                items(displayedApps) { app ->
                                    MockExpressiveAppCard(app = app)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppsScopeSegmentedToggle(scopeFilter: AppsScopeFilter) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        ToggleButton(
            checked = scopeFilter == AppsScopeFilter.ACTIVE_ONLY,
            onCheckedChange = {},
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            colors = ToggleButtonDefaults.colors(
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (scopeFilter == AppsScopeFilter.ACTIVE_ONLY) Icons.Filled.ToggleOn else Icons.Outlined.ToggleOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Active (6)")
        }
        ToggleButton(
            checked = scopeFilter == AppsScopeFilter.ALL_LIBRARY,
            onCheckedChange = {},
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
            colors = ToggleButtonDefaults.colors(
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (scopeFilter == AppsScopeFilter.ALL_LIBRARY) Icons.Filled.Apps else Icons.Outlined.Apps,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("All Apps (84)")
        }
    }
}

@Composable
private fun AppDetailFullHeightPanel(
    app: AppInfo,
    isFullWidthMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header inside Full-Height Detail Panel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isFullWidthMode) {
                    FilledTonalIconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Apps")
                    }
                }
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${app.packageName} • Music Category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!isFullWidthMode) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Close, contentDescription = "Close Detail Panel")
                    }
                }
            }

            // Expressive Master Bridge Toggle Banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "HyperIsland Bridge Enabled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Intercept notifications and render live islands for ${app.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Switch(checked = app.isBridged, onCheckedChange = {})
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Configuration Cards (Uses 2-column grid if in Full-Width mode on Foldable, or stacked list in Side-by-Side)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isFullWidthMode) 250.dp else 340.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val configSections = listOf(
                    Triple("Island Behavior & Priority", "Standard priority • Dynamic media visualizer", Icons.Default.NotificationsActive),
                    Triple("Color & Theme Override", "Extract album art palette dynamically", Icons.Rounded.ColorLens),
                    Triple("Smart Actions & Controls", "Play/Pause, Next Track, Like Song", Icons.Rounded.Tune),
                    Triple("Custom Translator Rules", "1 active media metadata rule", Icons.Default.Extension)
                )
                items(configSections) { (title, subtitle, icon) ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// HELPER BUILDING BLOCKS
// ============================================================================

@Composable
private fun MockHeroBanner(height: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF4CAF50).copy(alpha = 0.14f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.22f))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                Text("Customization Guide", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Personalize HyperOS Dynamic Islands for every app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MockBentoCard(
    modifier: Modifier = Modifier,
    title: String,
    status: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    isSelected: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, null, tint = iconColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MockFeaturedRow() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Featured Showcase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                modifier = Modifier.weight(1f).height(104.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column {
                        Text("Community Hub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Explore top presets", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                modifier = Modifier.weight(1f).height(104.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Rounded.ColorLens, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Column {
                        Text("Color Schemes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Dynamic palettes", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun MockGuidesList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Documentation & Guides", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        listOf(
            "Island Studio Basics" to "Build multi-node widgets and expressions",
            "Smart Translators" to "Map notification extras into HyperOS islands"
        ).forEach { (title, desc) ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MockSystemIntegrationCard(integration: SystemIntegrationInfo) {
    val (title, icon) = when (integration.id) {
        SystemIntegrationId.SCREEN_RECORDER -> "Screen Recorder" to Icons.Outlined.Videocam
        SystemIntegrationId.CHARGING -> "Charging Island" to Icons.Rounded.Bolt
        SystemIntegrationId.SYSTEM_UPDATER -> "System Updater" to Icons.Outlined.SystemUpdate
        SystemIntegrationId.VPN -> "VPN Status" to Icons.Rounded.VpnKey
        SystemIntegrationId.DEBUGGING -> "ADB Debugging" to Icons.Rounded.Android
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (integration.enabled) "Active" else "Disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = integration.enabled, onCheckedChange = {})
        }
    }
}

@Composable
private fun MockExpressiveAppCard(app: AppInfo, highlighted: Boolean = false) {
    val catIcon = when (app.category) {
        AppCategory.MUSIC -> Icons.Default.MusicNote
        AppCategory.MAPS -> Icons.Default.Place
        AppCategory.TIMER -> Icons.Default.Timer
        else -> Icons.Default.Android
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        border = if (highlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(catIcon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Switch(checked = app.isBridged, onCheckedChange = {})
        }
    }
}

// ============================================================================
// COMPOSE PREVIEWS
// ============================================================================

@Preview(name = "1A. Design Combined — Unselected Overview (Tablet)", device = Devices.TABLET, showBackground = true)
@Composable
fun PreviewDesignCombinedOverviewTablet() {
    HyperBridgeTheme {
        CombinedDesignScreenMockup(
            selectedModule = DesignModuleSelection.NONE,
            expandedRail = true
        )
    }
}

@Preview(name = "1B. Design Combined — Side-by-Side Detail Panel (Tablet)", device = Devices.TABLET, showBackground = true)
@Composable
fun PreviewDesignCombinedDetailTablet() {
    HyperBridgeTheme {
        CombinedDesignScreenMockup(
            selectedModule = DesignModuleSelection.THEMES,
            expandedRail = false
        )
    }
}

@Preview(name = "1C. Design Combined — Full-Width Detail Panel (Foldable)", device = Devices.FOLDABLE, showBackground = true)
@Composable
fun PreviewDesignCombinedDetailFoldable() {
    HyperBridgeTheme {
        CombinedDesignScreenMockup(
            selectedModule = DesignModuleSelection.THEMES,
            expandedRail = false
        )
    }
}

@Preview(name = "2A. Unified Apps — Overview (Tablet)", device = Devices.TABLET, showBackground = true)
@Composable
fun PreviewUnifiedAppsOverviewTablet() {
    HyperBridgeTheme {
        CombinedUnifiedAppsScreenMockup(
            scopeFilter = AppsScopeFilter.ALL_LIBRARY,
            selectedAppPackage = null,
            expandedRail = true
        )
    }
}

@Preview(name = "2B. Unified Apps — Full-Height Side Detail Panel + Stacked Left Controls (Tablet)", device = Devices.TABLET, showBackground = true)
@Composable
fun PreviewUnifiedAppsDetailOpenTablet() {
    HyperBridgeTheme {
        CombinedUnifiedAppsScreenMockup(
            scopeFilter = AppsScopeFilter.ALL_LIBRARY,
            selectedAppPackage = "com.spotify.music",
            expandedRail = false
        )
    }
}

@Preview(name = "2C. Unified Apps — Full-Width Detail Panel when Narrow (Foldable)", device = Devices.FOLDABLE, showBackground = true)
@Composable
fun PreviewUnifiedAppsDetailOpenFoldable() {
    HyperBridgeTheme {
        CombinedUnifiedAppsScreenMockup(
            scopeFilter = AppsScopeFilter.ALL_LIBRARY,
            selectedAppPackage = "com.spotify.music",
            expandedRail = false
        )
    }
}
