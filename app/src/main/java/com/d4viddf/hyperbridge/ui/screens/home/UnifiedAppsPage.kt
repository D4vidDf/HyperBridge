package com.d4viddf.hyperbridge.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.AppCategory
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.SortOption
import com.d4viddf.hyperbridge.ui.SystemIntegrationId
import com.d4viddf.hyperbridge.ui.SystemIntegrationInfo
import com.d4viddf.hyperbridge.ui.components.AllAppsConfigBottomSheet
import com.d4viddf.hyperbridge.ui.components.EmptyState
import com.d4viddf.hyperbridge.ui.components.FeaturedNotificationWarningBanner
import com.d4viddf.hyperbridge.ui.screens.settings.AppConfigScreen
import com.d4viddf.hyperbridge.ui.screens.settings.ChargingSettingsScreen
import com.d4viddf.hyperbridge.ui.screens.settings.ScreenRecordingSettingsScreen
import com.d4viddf.hyperbridge.ui.screens.settings.SystemUpdateSettingsScreen
import com.d4viddf.hyperbridge.ui.screens.settings.VpnSettingsScreen

private val SIDE_BY_SIDE_DETAIL_MIN_WIDTH = 720.dp

/**
 * Adaptive Tablet & Foldable layout for Active Apps and Library pages.
 *
 * Uses a single persistent composition tree and a single synchronized `detailProgress` spring
 * so opening/closing the detail panel never snaps in two stages, and optimizes `LazyVerticalGrid`
 * with pre-cached `ImageBitmap`s and `contentType` recycling for butter-smooth scrolling.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdaptiveAppsPage(
    isLibraryPage: Boolean,
    apps: List<AppInfo>,
    isLoading: Boolean,
    systemIntegrations: List<SystemIntegrationInfo>,
    viewModel: AppListViewModel,
    showWarning: Boolean = false,
    onWarningClick: () -> Unit = {},
    onDismissWarning: () -> Unit = {},
    onNavConfigClick: (String) -> Unit = {},
    onCreateTranslator: (String) -> Unit = {},
    onEditTranslator: (String) -> Unit = {}
) {
    val searchQuery = if (isLibraryPage) {
        viewModel.librarySearch.collectAsState().value
    } else {
        viewModel.activeSearch.collectAsState().value
    }
    val selectedCategory = if (isLibraryPage) {
        viewModel.libraryCategory.collectAsState().value
    } else {
        viewModel.activeCategory.collectAsState().value
    }
    val sortOption = if (isLibraryPage) {
        viewModel.librarySort.collectAsState().value
    } else {
        viewModel.activeSort.collectAsState().value
    }
    val systemSelected = if (isLibraryPage) {
        viewModel.librarySystemSelected.collectAsState().value
    } else {
        viewModel.activeSystemSelected.collectAsState().value
    }

    val areAllAppsBridged by viewModel.areAllLibraryAppsBridged.collectAsState()
    var showAllAppsConfigSheet by remember { mutableStateOf(false) }

    // Detail selection state preserved per page
    var selectedAppPackage by rememberSaveable(isLibraryPage) { mutableStateOf<String?>(null) }
    var selectedSystemIdName by rememberSaveable(isLibraryPage) { mutableStateOf<String?>(null) }

    // Remember last non-null target so exit animations continue rendering content smoothly
    var lastRenderedAppPackage by remember(isLibraryPage) { mutableStateOf<String?>(null) }
    var lastRenderedSystemIdName by remember(isLibraryPage) { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedAppPackage, selectedSystemIdName) {
        if (selectedAppPackage != null || selectedSystemIdName != null) {
            lastRenderedAppPackage = selectedAppPackage
            lastRenderedSystemIdName = selectedSystemIdName
        }
    }

    val hasDetailSelection = selectedAppPackage != null || selectedSystemIdName != null

    if (hasDetailSelection) {
        BackHandler {
            selectedAppPackage = null
            selectedSystemIdName = null
        }
    }

    val displayedSystemIntegrations = remember(systemIntegrations, isLibraryPage) {
        if (isLibraryPage) systemIntegrations else systemIntegrations.filter { it.enabled }
    }

    val onSearchQueryChange: (String) -> Unit = { query ->
        if (isLibraryPage) {
            viewModel.librarySearch.value = query
        } else {
            viewModel.activeSearch.value = query
        }
    }
    val onCategoryChange: (AppCategory) -> Unit = { category ->
        if (isLibraryPage) {
            viewModel.selectLibraryAppCategory(category)
        } else {
            viewModel.selectActiveAppCategory(category)
        }
    }
    val onSortChange: (SortOption) -> Unit = { sort ->
        if (isLibraryPage) {
            viewModel.librarySort.value = sort
        } else {
            viewModel.activeSort.value = sort
        }
    }
    val onSystemSelectedChange: (Boolean) -> Unit = { selected ->
        if (isLibraryPage) {
            viewModel.selectLibrarySystem(selected)
        } else {
            viewModel.selectActiveSystem(selected)
        }
    }

    val pageTitle = stringResource(if (isLibraryPage) R.string.tab_library else R.string.tab_active)
    val isRefreshing = isLoading && apps.isNotEmpty()
    val pullState = rememberPullToRefreshState()
    val gridState = rememberLazyGridState()

    val detailProgress by animateFloatAsState(
        targetValue = if (hasDetailSelection) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "AppsDetailProgress"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        val canShowSideBySideDetail = maxWidth >= SIDE_BY_SIDE_DETAIL_MIN_WIDTH
        val showInlineSystem = (systemSelected || (selectedCategory == AppCategory.ALL && searchQuery.isBlank())) &&
            displayedSystemIntegrations.isNotEmpty()

        val innerAvailableWidth = (maxWidth - 40.dp).coerceAtLeast(320.dp)
        val splitGap = 20.dp
        val targetLeftSplitWidth = (innerAvailableWidth - splitGap) * 0.45f
        val targetRightSplitWidth = (innerAvailableWidth - splitGap) * 0.55f

        val currentLeftWidth = if (canShowSideBySideDetail) {
            lerp(innerAvailableWidth, targetLeftSplitWidth, detailProgress)
        } else {
            innerAvailableWidth
        }
        val currentGapWidth = if (canShowSideBySideDetail) {
            lerp(0.dp, splitGap, detailProgress)
        } else {
            0.dp
        }
        val currentRightBoxWidth = if (canShowSideBySideDetail) {
            (innerAvailableWidth - currentLeftWidth - currentGapWidth).coerceAtLeast(0.dp)
        } else {
            innerAvailableWidth
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // PERSISTENT LEFT / MAIN PANE (stays mounted so LazyVerticalGrid & ScrollState never reset!)
                if (canShowSideBySideDetail || detailProgress < 0.999f) {
                    Column(
                        modifier = Modifier
                            .width(currentLeftWidth)
                            .fillMaxHeight()
                            .graphicsLayer {
                                if (!canShowSideBySideDetail) {
                                    alpha = 1f - detailProgress
                                    translationX = -detailProgress * size.width * 0.15f
                                }
                            },
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Adaptive Header: Inline SearchBar when wide overview, Stacked when Detail Panel is open or narrow
                        val useInlineHeader = canShowSideBySideDetail && !hasDetailSelection
                        if (useInlineHeader) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pageTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                CompactInlineSearchBar(
                                    searchQuery = searchQuery,
                                    onSearchChange = onSearchQueryChange,
                                    modifier = Modifier.width(360.dp)
                                )
                            }
                        } else {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            CompactInlineSearchBar(
                                searchQuery = searchQuery,
                                onSearchChange = onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Filter & Category Pills + Bridge All Apps pill (on Library page)
                        UnifiedAppsFilterRow(
                            selectedCategory = selectedCategory,
                            onCategoryChange = onCategoryChange,
                            sortOption = sortOption,
                            onSortChange = onSortChange,
                            showSystemCategory = true,
                            systemSelected = systemSelected,
                            onSystemSelected = onSystemSelectedChange,
                            isAllAppsMode = isLibraryPage,
                            areAllAppsBridged = areAllAppsBridged,
                            onToggleAllApps = { viewModel.toggleAllLibraryApps(it) },
                            onOpenAllAppsConfig = { showAllAppsConfigSheet = true }
                        )

                        if (showWarning && !isLibraryPage) {
                            FeaturedNotificationWarningBanner(
                                onOpenSettings = onWarningClick,
                                onDismiss = onDismissWarning
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel.refreshApps() },
                                state = pullState,
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter,
                                indicator = {
                                    PullToRefreshDefaults.LoadingIndicator(
                                        state = pullState,
                                        isRefreshing = isRefreshing,
                                        modifier = Modifier.align(Alignment.TopCenter)
                                    )
                                }
                            ) {
                                if (apps.isEmpty() && !showInlineSystem && isLoading) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        LoadingIndicator()
                                    }
                                } else if (apps.isEmpty() && !showInlineSystem) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        EmptyState(
                                            title = stringResource(
                                                if (!isLibraryPage) R.string.no_active_bridges else R.string.no_apps_found
                                            ),
                                            description = if (!isLibraryPage) {
                                                stringResource(R.string.no_active_bridges_desc)
                                            } else {
                                                ""
                                            },
                                            icon = if (!isLibraryPage) {
                                                Icons.Outlined.NotificationsOff
                                            } else {
                                                Icons.Default.SearchOff
                                            }
                                        )
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = if (hasDetailSelection) {
                                            GridCells.Fixed(1)
                                        } else {
                                            GridCells.Adaptive(minSize = 260.dp)
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 32.dp)
                                    ) {
                                        if (showInlineSystem) {
                                            item(
                                                key = "inline_system_header",
                                                span = { GridItemSpan(maxLineSpan) },
                                                contentType = "section_header"
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.system_integrations),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                                )
                                            }
                                            items(
                                                items = displayedSystemIntegrations,
                                                key = { "inline_sys_${it.id.name}" },
                                                contentType = { "system_card" }
                                            ) { integration ->
                                                ExpressiveSystemIntegrationCard(
                                                    integration = integration,
                                                    isSelected = selectedSystemIdName == integration.id.name,
                                                    onToggle = { viewModel.toggleSystemIntegration(integration.id, it) },
                                                    onSelect = if (integration.available && integration.id.hasSettings) {
                                                        {
                                                            if (selectedSystemIdName == integration.id.name) {
                                                                selectedSystemIdName = null
                                                            } else {
                                                                selectedAppPackage = null
                                                                selectedSystemIdName = integration.id.name
                                                            }
                                                        }
                                                    } else null
                                                )
                                            }
                                        }

                                        if (showInlineSystem && apps.isNotEmpty()) {
                                            item(
                                                key = "inline_apps_header",
                                                span = { GridItemSpan(maxLineSpan) },
                                                contentType = "section_header"
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.apps_section_header),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        items(
                                            items = apps,
                                            key = { it.packageName },
                                            contentType = { "app_card" }
                                        ) { app ->
                                            ExpressiveAppGridCard(
                                                app = app,
                                                isSelected = selectedAppPackage == app.packageName,
                                                onToggle = { enabled ->
                                                    viewModel.toggleApp(app.packageName, enabled)
                                                },
                                                onSelect = {
                                                    if (app.isInstalled) {
                                                        if (selectedAppPackage == app.packageName) {
                                                            selectedAppPackage = null
                                                        } else {
                                                            selectedSystemIdName = null
                                                            selectedAppPackage = app.packageName
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (canShowSideBySideDetail && detailProgress > 0.001f) {
                    Spacer(modifier = Modifier.width(currentGapWidth))

                    // SYNCHRONIZED SINGLE-STAGE DETAIL PANEL (Right side on >= 720.dp)
                    Box(
                        modifier = Modifier
                            .width(currentRightBoxWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val baseColorScheme = MaterialTheme.colorScheme
                        val detailColorScheme = remember(baseColorScheme) {
                            baseColorScheme.copy(
                                background = baseColorScheme.surfaceContainerLow,
                                surface = baseColorScheme.surfaceContainerLow,
                                surfaceContainerLow = baseColorScheme.surfaceContainer
                            )
                        }
                        MaterialTheme(colorScheme = detailColorScheme) {
                            Surface(
                                modifier = Modifier
                                    .requiredWidth(targetRightSplitWidth)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        alpha = detailProgress
                                        translationX = (1f - detailProgress) * 48.dp.toPx()
                                    },
                                shape = RoundedCornerShape(28.dp),
                                color = baseColorScheme.surfaceContainerLow
                            ) {
                                val activePkg = selectedAppPackage ?: lastRenderedAppPackage
                                val activeSys = selectedSystemIdName ?: lastRenderedSystemIdName

                                AnimatedContent(
                                    targetState = Pair(activePkg, activeSys),
                                    transitionSpec = {
                                        (fadeIn(tween(140)) + slideInHorizontally(tween(160)) { it / 10 })
                                            .togetherWith(fadeOut(tween(100)))
                                    },
                                    label = "AppDetailSwitchAnimation"
                                ) { (pkg, sysIdName) ->
                                    UnifiedAppsDetailContent(
                                        selectedAppPackage = pkg,
                                        selectedSystemIdName = sysIdName,
                                        viewModel = viewModel,
                                        onClose = {
                                            selectedAppPackage = null
                                            selectedSystemIdName = null
                                        },
                                        onNavConfigClick = onNavConfigClick,
                                        onCreateTranslator = onCreateTranslator,
                                        onEditTranslator = onEditTranslator
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FULL-WIDTH OVERLAY DETAIL PANEL FOR NARROWER SCREENS (< 720.dp)
            if (!canShowSideBySideDetail && detailProgress > 0.001f) {
                val baseColorScheme = MaterialTheme.colorScheme
                val detailColorScheme = remember(baseColorScheme) {
                    baseColorScheme.copy(
                        background = baseColorScheme.surfaceContainerLow,
                        surface = baseColorScheme.surfaceContainerLow,
                        surfaceContainerLow = baseColorScheme.surfaceContainer
                    )
                }
                MaterialTheme(colorScheme = detailColorScheme) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = detailProgress
                                translationX = (1f - detailProgress) * size.width * 0.2f
                            }
                            .clip(RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        color = baseColorScheme.surfaceContainerLow
                    ) {
                        val activePkg = selectedAppPackage ?: lastRenderedAppPackage
                        val activeSys = selectedSystemIdName ?: lastRenderedSystemIdName

                        UnifiedAppsDetailContent(
                            selectedAppPackage = activePkg,
                            selectedSystemIdName = activeSys,
                            viewModel = viewModel,
                            onClose = {
                                selectedAppPackage = null
                                selectedSystemIdName = null
                            },
                            onNavConfigClick = onNavConfigClick,
                            onCreateTranslator = onCreateTranslator,
                            onEditTranslator = onEditTranslator
                        )
                    }
                }
            }
        }
    }

    if (showAllAppsConfigSheet) {
        AllAppsConfigBottomSheet(
            viewModel = viewModel,
            onDismiss = { showAllAppsConfigSheet = false }
        )
    }
}

@Composable
private fun UnifiedAppsDetailContent(
    selectedAppPackage: String?,
    selectedSystemIdName: String?,
    viewModel: AppListViewModel,
    onClose: () -> Unit,
    onNavConfigClick: (String) -> Unit,
    onCreateTranslator: (String) -> Unit,
    onEditTranslator: (String) -> Unit
) {
    if (selectedAppPackage != null) {
        key(selectedAppPackage) {
            AppConfigScreen(
                packageName = selectedAppPackage,
                viewModel = viewModel,
                onBack = onClose,
                onNavConfigClick = onNavConfigClick,
                onCreateTranslator = onCreateTranslator,
                onEditTranslator = onEditTranslator,
                isDetailPane = true
            )
        }
    } else if (selectedSystemIdName != null) {
        val sysId = remember(selectedSystemIdName) {
            runCatching { SystemIntegrationId.valueOf(selectedSystemIdName) }.getOrNull()
        }
        when (sysId) {
            SystemIntegrationId.SCREEN_RECORDER -> ScreenRecordingSettingsScreen(onBack = onClose)
            SystemIntegrationId.SYSTEM_UPDATER -> SystemUpdateSettingsScreen(onBack = onClose)
            SystemIntegrationId.CHARGING -> ChargingSettingsScreen(onBack = onClose)
            SystemIntegrationId.VPN -> VpnSettingsScreen(onBack = onClose)
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactInlineSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    SearchBar(
        query = searchQuery,
        onQueryChange = onSearchChange,
        onSearch = { focusManager.clearFocus() },
        active = false,
        onActiveChange = {},
        placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(Icons.Default.Clear, stringResource(R.string.clear))
                }
            }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
        content = {}
    )
}

@Composable
private fun UnifiedAppsFilterRow(
    selectedCategory: AppCategory,
    onCategoryChange: (AppCategory) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    showSystemCategory: Boolean,
    systemSelected: Boolean,
    onSystemSelected: (Boolean) -> Unit,
    isAllAppsMode: Boolean,
    areAllAppsBridged: Boolean,
    onToggleAllApps: (Boolean) -> Unit,
    onOpenAllAppsConfig: () -> Unit
) {
    val scrollState = rememberScrollState()
    val categories = AppCategory.entries.toTypedArray()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconToggleButton(
            checked = sortOption == SortOption.NAME_ZA,
            onCheckedChange = { isChecked ->
                onSortChange(if (isChecked) SortOption.NAME_ZA else SortOption.NAME_AZ)
            },
            colors = IconButtonDefaults.iconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(
                imageVector = if (sortOption == SortOption.NAME_AZ) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = stringResource(R.string.sort_order)
            )
        }

        VerticalDivider(
            modifier = Modifier.height(28.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        if (showSystemCategory) {
            ToggleButton(
                checked = systemSelected,
                onCheckedChange = { onSystemSelected(!systemSelected) },
                modifier = Modifier.semantics { role = Role.RadioButton },
                colors = ToggleButtonDefaults.colors(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = if (systemSelected) Icons.Filled.Android else Icons.Outlined.Android,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.system_integrations))
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            categories.forEachIndexed { index, category ->
                val isSelected = !systemSelected && selectedCategory == category
                val shape = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    categories.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }

                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { onCategoryChange(category) },
                    shapes = shape,
                    modifier = Modifier.semantics { role = Role.RadioButton },
                    colors = ToggleButtonDefaults.colors(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = getUnifiedCategoryIcon(category, isSelected),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(getUnifiedCategoryLabelRes(category)))
                }
            }
        }

        if (isAllAppsMode) {
            Spacer(Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onToggleAllApps(!areAllAppsBridged) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bridge_all_apps),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    IconButton(
                        onClick = onOpenAllAppsConfig,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.auto_add_new_apps_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Switch(
                        checked = areAllAppsBridged,
                        onCheckedChange = onToggleAllApps
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveAppGridCard(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    onSelect: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        color = if (app.isInstalled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )

                    if (!app.isInstalled) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_uninstalled_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (app.isInstalled) {
                IconButton(onClick = onSelect) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Switch(
                checked = app.isBridged,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun ExpressiveSystemIntegrationCard(
    integration: SystemIntegrationInfo,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    onSelect: (() -> Unit)?
) {
    val settingsAction = if (integration.id.hasSettings) onSelect else null
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        onClick = { settingsAction?.invoke() ?: onToggle(!integration.enabled) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (integration.iconBitmap != null) {
                        Image(
                            bitmap = integration.iconBitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else if (
                        integration.id == SystemIntegrationId.VPN ||
                        integration.id == SystemIntegrationId.CHARGING ||
                        integration.id == SystemIntegrationId.DEBUGGING
                    ) {
                        Icon(
                            painter = painterResource(
                                when (integration.id) {
                                    SystemIntegrationId.VPN -> R.drawable.ic_vpn
                                    SystemIntegrationId.DEBUGGING -> R.drawable.ic_debugging_adb
                                    else -> R.drawable.ic_charging_bolt
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = when (integration.id) {
                                SystemIntegrationId.SCREEN_RECORDER -> Icons.Outlined.Videocam
                                SystemIntegrationId.SYSTEM_UPDATER -> Icons.Outlined.SystemUpdate
                            },
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        when (integration.id) {
                            SystemIntegrationId.SCREEN_RECORDER -> R.string.screen_recording_title
                            SystemIntegrationId.SYSTEM_UPDATER -> R.string.system_updater_title
                            SystemIntegrationId.VPN -> R.string.vpn_title
                            SystemIntegrationId.CHARGING -> R.string.charging_island_title
                            SystemIntegrationId.DEBUGGING -> R.string.debugging_island_title
                        }
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        !integration.available -> stringResource(R.string.system_integration_unavailable)
                        integration.enabled -> stringResource(R.string.system_integration_enabled)
                        else -> stringResource(R.string.system_integration_disabled)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (settingsAction != null) {
                IconButton(
                    onClick = settingsAction,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Switch(
                checked = integration.enabled,
                onCheckedChange = onToggle,
                enabled = integration.available
            )
        }
    }
}

@StringRes
private fun getUnifiedCategoryLabelRes(category: AppCategory): Int {
    return when (category) {
        AppCategory.ALL -> R.string.all
        AppCategory.MUSIC -> R.string.cat_music
        AppCategory.MAPS -> R.string.cat_nav
        AppCategory.TIMER -> R.string.cat_timer
        AppCategory.OTHER -> R.string.cat_other
    }
}

private fun getUnifiedCategoryIcon(category: AppCategory, isSelected: Boolean): ImageVector {
    return if (isSelected) {
        when (category) {
            AppCategory.ALL -> Icons.Filled.Apps
            AppCategory.MUSIC -> Icons.Filled.MusicNote
            AppCategory.MAPS -> Icons.Filled.Place
            AppCategory.TIMER -> Icons.Filled.Timer
            AppCategory.OTHER -> Icons.Filled.Category
        }
    } else {
        when (category) {
            AppCategory.ALL -> Icons.Outlined.Apps
            AppCategory.MUSIC -> Icons.Outlined.MusicNote
            AppCategory.MAPS -> Icons.Outlined.Place
            AppCategory.TIMER -> Icons.Outlined.Timer
            AppCategory.OTHER -> Icons.Outlined.Category
        }
    }
}
