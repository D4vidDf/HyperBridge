package com.d4viddf.hyperbridge.ui.screens.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.screens.design.DesignManagerScreen
import com.d4viddf.hyperbridge.ui.screens.design.DesignScreen
import com.d4viddf.hyperbridge.ui.screens.design.DesignSection
import com.d4viddf.hyperbridge.ui.screens.design.SavedAppWidgetsScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetConfigScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetPickerScreen
import com.d4viddf.hyperbridge.ui.screens.design.studio.StudioDesignScreen
import com.d4viddf.hyperbridge.ui.screens.theme.ThemeCreatorScreen
import com.d4viddf.hyperbridge.ui.screens.theme.ThemeManagerScreen
import kotlinx.coroutines.launch

private enum class DesignRoute {
    DASHBOARD,
    WIDGET_LIST,
    THEME_MANAGER,
    THEME_CREATOR,
    DESIGN_MANAGER,
    TRANSLATOR_MANAGER,
    TRANSLATOR_EDITOR,
    STUDIO_EDITOR
}

@Composable
fun HomeScreen(
    viewModel: AppListViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onScreenRecordingConfigClick: () -> Unit = {},
    onSystemUpdateConfigClick: () -> Unit = {},
    onChargingConfigClick: () -> Unit = {},
    onVpnConfigClick: () -> Unit = {},
    onAppConfigClick: (String) -> Unit = {},
    onNavConfigClick: (String) -> Unit = {},
    onCreateTranslatorForApp: (String) -> Unit = {},
    onEditTranslatorFromApp: (String) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(1) }

    var designRoute by rememberSaveable { mutableStateOf(DesignRoute.DASHBOARD) }
    var editingThemeId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTranslatorId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingCustomWidgetId by rememberSaveable { mutableStateOf<String?>(null) }
    var newTranslatorPackageName by rememberSaveable { mutableStateOf<String?>(null) }
    var previousTranslatorManagerRoute by rememberSaveable { mutableStateOf(DesignRoute.TRANSLATOR_MANAGER) }
    var previousStudioRoute by rememberSaveable { mutableStateOf(DesignRoute.DESIGN_MANAGER) }

    var showWidgetPicker by rememberSaveable { mutableStateOf(false) }
    var editingWidgetId by rememberSaveable { mutableStateOf<Int?>(null) }

    val activeApps by viewModel.activeAppsState.collectAsState()
    val libraryApps by viewModel.libraryAppsState.collectAsState()
    val systemIntegrations by viewModel.systemIntegrationsState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val prefs = remember { com.d4viddf.hyperbridge.data.AppPreferences(context) }
    val showWarning by prefs.featuredPermissionWarningFlow.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    if (editingWidgetId != null) BackHandler { editingWidgetId = null }
    if (showWidgetPicker) BackHandler { showWidgetPicker = false }

    if (selectedTab == 0 && designRoute != DesignRoute.DASHBOARD) {
        BackHandler {
            designRoute = when (designRoute) {
                DesignRoute.THEME_CREATOR -> {
                    editingThemeId = null
                    DesignRoute.THEME_MANAGER
                }
                DesignRoute.STUDIO_EDITOR -> {
                    editingCustomWidgetId = null
                    previousStudioRoute
                }
                DesignRoute.TRANSLATOR_EDITOR -> {
                    editingTranslatorId = null
                    newTranslatorPackageName = null
                    previousTranslatorManagerRoute
                }
                else -> DesignRoute.DASHBOARD
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        val openPlayStoreThemes: () -> Unit = {
            val query = "Hyper Bridge Theme"
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "market://search?q=$query&c=apps".toUri()
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/search?q=$query&c=apps".toUri()
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }

        if (useRail) {
            // TABLET & FOLDABLE LAYOUT: Material 3 NavigationRail + Adaptive Content
            Row(modifier = Modifier.fillMaxSize()) {
                val isFullScreenStudioRoute = selectedTab == 0 && (
                    designRoute == DesignRoute.STUDIO_EDITOR ||
                        designRoute == DesignRoute.TRANSLATOR_EDITOR
                    )

                AnimatedVisibility(
                    visible = !isFullScreenStudioRoute && !showWidgetPicker && editingWidgetId == null,
                    enter = slideInHorizontally { -it } + fadeIn(),
                    exit = slideOutHorizontally { -it } + fadeOut()
                ) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(top = 12.dp, bottom = 48.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            NavigationRailItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = {
                                    Icon(
                                        if (selectedTab == 0) Icons.Filled.Brush else Icons.Outlined.Brush,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(R.string.design)) }
                            )

                            NavigationRailItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = {
                                    Icon(
                                        if (selectedTab == 1) Icons.Filled.ToggleOn else Icons.Outlined.ToggleOff,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(R.string.tab_active)) }
                            )

                            NavigationRailItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = {
                                    Icon(
                                        if (selectedTab == 2) Icons.Filled.Apps else Icons.Outlined.Apps,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(R.string.tab_library)) }
                            )

                            // Settings is permanently visible in the rail on bigger screens
                            NavigationRailItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = {
                                    Icon(
                                        if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = stringResource(R.string.settings)
                                    )
                                },
                                label = { Text(stringResource(R.string.settings)) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedTab) {
                        0 -> {
                            when (designRoute) {
                                DesignRoute.STUDIO_EDITOR -> {
                                    StudioDesignScreen(
                                        widgetId = editingCustomWidgetId,
                                        onBack = {
                                            editingCustomWidgetId = null
                                            designRoute = previousStudioRoute
                                        }
                                    )
                                }

                                DesignRoute.THEME_CREATOR -> {
                                    ThemeCreatorScreen(
                                        editThemeId = editingThemeId,
                                        onBack = {
                                            designRoute = DesignRoute.THEME_MANAGER
                                            editingThemeId = null
                                        },
                                        onThemeCreated = {
                                            designRoute = DesignRoute.THEME_MANAGER
                                            editingThemeId = null
                                        }
                                    )
                                }

                                DesignRoute.TRANSLATOR_EDITOR -> {
                                    com.d4viddf.hyperbridge.ui.screens.translators.TranslatorEditorScreen(
                                        translatorId = editingTranslatorId,
                                        initialPackageName = newTranslatorPackageName,
                                        onBack = {
                                            designRoute = previousTranslatorManagerRoute
                                            editingTranslatorId = null
                                            newTranslatorPackageName = null
                                        }
                                    )
                                }

                                else -> {
                                    val selectedSection = when (designRoute) {
                                        DesignRoute.THEME_MANAGER -> DesignSection.THEMES
                                        DesignRoute.WIDGET_LIST -> DesignSection.WIDGETS
                                        DesignRoute.DESIGN_MANAGER -> DesignSection.DESIGNS
                                        DesignRoute.TRANSLATOR_MANAGER -> DesignSection.TRANSLATORS
                                        else -> null
                                    }

                                    // Cache the last active detail route so the closing animation
                                    // keeps rendering the detail content while sliding out.
                                    var lastDetailRoute by rememberSaveable {
                                        mutableStateOf(DesignRoute.THEME_MANAGER)
                                    }
                                    if (selectedSection != null) {
                                        lastDetailRoute = designRoute
                                    }
                                    val renderedDetailRoute = if (selectedSection != null) designRoute else lastDetailRoute

                                    DesignScreen(
                                        onNavigateToWidgets = {
                                            designRoute = if (designRoute == DesignRoute.WIDGET_LIST) {
                                                DesignRoute.DASHBOARD
                                            } else {
                                                DesignRoute.WIDGET_LIST
                                            }
                                        },
                                        onNavigateToThemes = {
                                            designRoute = if (designRoute == DesignRoute.THEME_MANAGER) {
                                                DesignRoute.DASHBOARD
                                            } else {
                                                DesignRoute.THEME_MANAGER
                                            }
                                        },
                                        onEditTheme = { themeId ->
                                            editingThemeId = themeId
                                            designRoute = DesignRoute.THEME_CREATOR
                                        },
                                        onNavigateToDesigns = {
                                            designRoute = if (designRoute == DesignRoute.DESIGN_MANAGER) {
                                                DesignRoute.DASHBOARD
                                            } else {
                                                DesignRoute.DESIGN_MANAGER
                                            }
                                        },
                                        onNavigateToTranslators = {
                                            designRoute = if (designRoute == DesignRoute.TRANSLATOR_MANAGER) {
                                                DesignRoute.DASHBOARD
                                            } else {
                                                DesignRoute.TRANSLATOR_MANAGER
                                            }
                                        },
                                        onCreateTranslator = {
                                            editingTranslatorId = null
                                            newTranslatorPackageName = null
                                            previousTranslatorManagerRoute = DesignRoute.TRANSLATOR_MANAGER
                                            designRoute = DesignRoute.TRANSLATOR_EDITOR
                                        },
                                        onCreateCustomDesign = {
                                            editingCustomWidgetId = null
                                            previousStudioRoute = DesignRoute.DASHBOARD
                                            designRoute = DesignRoute.STUDIO_EDITOR
                                        },
                                        onLaunchPicker = { showWidgetPicker = true },
                                        onSettingsClick = onSettingsClick,
                                        showSettingsButton = false,
                                        selectedSection = selectedSection,
                                        detailContent = {
                                            AnimatedContent(
                                                targetState = renderedDetailRoute,
                                                transitionSpec = {
                                                    (fadeIn(
                                                        animationSpec = tween(180)
                                                    ) + slideInHorizontally(
                                                        initialOffsetX = { it / 10 },
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )).togetherWith(
                                                        fadeOut(
                                                            animationSpec = tween(120)
                                                        )
                                                    )
                                                },
                                                label = "DesignDetailContentTransition"
                                            ) { targetRoute ->
                                                when (targetRoute) {
                                                    DesignRoute.WIDGET_LIST -> {
                                                        SavedAppWidgetsScreen(
                                                            onBack = { designRoute = DesignRoute.DASHBOARD },
                                                            onEditWidget = { id -> editingWidgetId = id },
                                                            onAddMore = { showWidgetPicker = true },
                                                            isDetailPane = true
                                                        )
                                                    }

                                                    DesignRoute.THEME_MANAGER -> {
                                                        ThemeManagerScreen(
                                                            onBack = { designRoute = DesignRoute.DASHBOARD },
                                                            onFindThemes = openPlayStoreThemes,
                                                            onCreateTheme = {
                                                                editingThemeId = null
                                                                designRoute = DesignRoute.THEME_CREATOR
                                                            },
                                                            onEditTheme = { id ->
                                                                editingThemeId = id
                                                                designRoute = DesignRoute.THEME_CREATOR
                                                            },
                                                            isDetailPane = true
                                                        )
                                                    }

                                                    DesignRoute.DESIGN_MANAGER -> {
                                                        DesignManagerScreen(
                                                            onBack = { designRoute = DesignRoute.DASHBOARD },
                                                            onAddDesign = {
                                                                editingCustomWidgetId = null
                                                                previousStudioRoute = DesignRoute.DESIGN_MANAGER
                                                                designRoute = DesignRoute.STUDIO_EDITOR
                                                            },
                                                            onEditDesign = { id ->
                                                                editingTranslatorId = id
                                                                newTranslatorPackageName = null
                                                                previousTranslatorManagerRoute = DesignRoute.DESIGN_MANAGER
                                                                designRoute = DesignRoute.TRANSLATOR_EDITOR
                                                            },
                                                            onOpenStudio = { widgetId ->
                                                                editingCustomWidgetId = widgetId
                                                                previousStudioRoute = DesignRoute.DESIGN_MANAGER
                                                                designRoute = DesignRoute.STUDIO_EDITOR
                                                            },
                                                            isDetailPane = true
                                                        )
                                                    }

                                                    DesignRoute.TRANSLATOR_MANAGER -> {
                                                        com.d4viddf.hyperbridge.ui.screens.translators.TranslatorManagerScreen(
                                                            onBack = { designRoute = DesignRoute.DASHBOARD },
                                                            onCreateTranslator = { pkg ->
                                                                editingTranslatorId = null
                                                                newTranslatorPackageName = pkg
                                                                previousTranslatorManagerRoute = DesignRoute.TRANSLATOR_MANAGER
                                                                designRoute = DesignRoute.TRANSLATOR_EDITOR
                                                            },
                                                            onEditTranslator = { id ->
                                                                editingTranslatorId = id
                                                                newTranslatorPackageName = null
                                                                previousTranslatorManagerRoute = DesignRoute.TRANSLATOR_MANAGER
                                                                designRoute = DesignRoute.TRANSLATOR_EDITOR
                                                            },
                                                            isDetailPane = true
                                                        )
                                                    }

                                                    else -> {}
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        1 -> {
                            AdaptiveAppsPage(
                                isLibraryPage = false,
                                apps = activeApps,
                                isLoading = isLoading,
                                systemIntegrations = systemIntegrations,
                                viewModel = viewModel,
                                showWarning = showWarning,
                                onWarningClick = {
                                    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(
                                            android.provider.Settings.EXTRA_APP_PACKAGE,
                                            context.packageName
                                        )
                                    }
                                    context.startActivity(intent)
                                },
                                onDismissWarning = {
                                    scope.launch {
                                        prefs.setFeaturedPermissionWarning(false)
                                    }
                                },
                                onNavConfigClick = onNavConfigClick,
                                onCreateTranslator = onCreateTranslatorForApp,
                                onEditTranslator = onEditTranslatorFromApp
                            )
                        }

                        2 -> {
                            AdaptiveAppsPage(
                                isLibraryPage = true,
                                apps = libraryApps,
                                isLoading = isLoading,
                                systemIntegrations = systemIntegrations,
                                viewModel = viewModel,
                                showWarning = false,
                                onNavConfigClick = onNavConfigClick,
                                onCreateTranslator = onCreateTranslatorForApp,
                                onEditTranslator = onEditTranslatorFromApp
                            )
                        }

                        3 -> {
                            com.d4viddf.hyperbridge.ui.screens.settings.AdaptiveSettingsScreen(
                                showRootBackButton = false,
                                onExit = { selectedTab = 1 }
                            )
                        }
                    }
                }
            }
        } else {
            // COMPACT / PHONE LAYOUT: ShortNavigationBar at bottom
            val phoneSelectedTab = if (selectedTab > 2) 1 else selectedTab
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = (phoneSelectedTab != 0 || designRoute == DesignRoute.DASHBOARD) && !showWidgetPicker && editingWidgetId == null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        ShortNavigationBar {
                            ShortNavigationBarItem(
                                selected = phoneSelectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(if (phoneSelectedTab == 0) Icons.Filled.Brush else Icons.Outlined.Brush, null) },
                                label = { Text(stringResource(R.string.design)) }
                            )
                            ShortNavigationBarItem(
                                selected = phoneSelectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(if (phoneSelectedTab == 1) Icons.Filled.ToggleOn else Icons.Outlined.ToggleOff, null) },
                                label = { Text(stringResource(R.string.tab_active)) }
                            )
                            ShortNavigationBarItem(
                                selected = phoneSelectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(if (phoneSelectedTab == 2) Icons.Filled.Apps else Icons.Outlined.Apps, null) },
                                label = { Text(stringResource(R.string.tab_library)) }
                            )
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(bottom = padding.calculateBottomPadding())
                        .fillMaxSize()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (phoneSelectedTab) {
                            0 -> {
                                AnimatedContent(
                                    targetState = designRoute,
                                    transitionSpec = {
                                        if (targetState.ordinal > initialState.ordinal) {
                                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
                                        } else {
                                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it / 3 } + fadeOut()
                                        }
                                    },
                                    label = "DesignTabNav"
                                ) { route ->
                                    when (route) {
                                        DesignRoute.DASHBOARD -> {
                                            DesignScreen(
                                                onNavigateToWidgets = { designRoute = DesignRoute.WIDGET_LIST },
                                                onNavigateToThemes = { designRoute = DesignRoute.THEME_MANAGER },
                                                onEditTheme = { themeId ->
                                                    editingThemeId = themeId
                                                    designRoute = DesignRoute.THEME_CREATOR
                                                },
                                                onNavigateToDesigns = { designRoute = DesignRoute.DESIGN_MANAGER },
                                                onNavigateToTranslators = { designRoute = DesignRoute.TRANSLATOR_MANAGER },
                                                onCreateTranslator = {
                                                    editingTranslatorId = null
                                                    newTranslatorPackageName = null
                                                    previousTranslatorManagerRoute = DesignRoute.TRANSLATOR_MANAGER
                                                    designRoute = DesignRoute.TRANSLATOR_EDITOR
                                                },
                                                onCreateCustomDesign = {
                                                    editingCustomWidgetId = null
                                                    previousStudioRoute = DesignRoute.DASHBOARD
                                                    designRoute = DesignRoute.STUDIO_EDITOR
                                                },
                                                onLaunchPicker = { showWidgetPicker = true },
                                                onSettingsClick = onSettingsClick
                                            )
                                        }

                                        DesignRoute.STUDIO_EDITOR -> {
                                            StudioDesignScreen(
                                                widgetId = editingCustomWidgetId,
                                                onBack = {
                                                    editingCustomWidgetId = null
                                                    designRoute = previousStudioRoute
                                                }
                                            )
                                        }

                                        DesignRoute.WIDGET_LIST -> {
                                            SavedAppWidgetsScreen(
                                                onBack = { designRoute = DesignRoute.DASHBOARD },
                                                onEditWidget = { id -> editingWidgetId = id },
                                                onAddMore = { showWidgetPicker = true }
                                            )
                                        }

                                        DesignRoute.THEME_MANAGER -> {
                                            ThemeManagerScreen(
                                                onBack = { designRoute = DesignRoute.DASHBOARD },
                                                onFindThemes = openPlayStoreThemes,
                                                onCreateTheme = {
                                                    editingThemeId = null
                                                    designRoute = DesignRoute.THEME_CREATOR
                                                },
                                                onEditTheme = { id ->
                                                    editingThemeId = id
                                                    designRoute = DesignRoute.THEME_CREATOR
                                                }
                                            )
                                        }

                                        DesignRoute.THEME_CREATOR -> {
                                            ThemeCreatorScreen(
                                                editThemeId = editingThemeId,
                                                onBack = {
                                                    designRoute = DesignRoute.THEME_MANAGER
                                                    editingThemeId = null
                                                },
                                                onThemeCreated = {
                                                    designRoute = DesignRoute.THEME_MANAGER
                                                    editingThemeId = null
                                                }
                                            )
                                        }

                                        DesignRoute.DESIGN_MANAGER -> {
                                            DesignManagerScreen(
                                                onBack = { designRoute = DesignRoute.DASHBOARD },
                                                onAddDesign = {
                                                    editingCustomWidgetId = null
                                                    previousStudioRoute = DesignRoute.DESIGN_MANAGER
                                                    designRoute = DesignRoute.STUDIO_EDITOR
                                                },
                                                onEditDesign = { id ->
                                                    editingTranslatorId = id
                                                    newTranslatorPackageName = null
                                                    previousTranslatorManagerRoute = DesignRoute.DESIGN_MANAGER
                                                    designRoute = DesignRoute.TRANSLATOR_EDITOR
                                                },
                                                onOpenStudio = { widgetId ->
                                                    editingCustomWidgetId = widgetId
                                                    previousStudioRoute = DesignRoute.DESIGN_MANAGER
                                                    designRoute = DesignRoute.STUDIO_EDITOR
                                                }
                                            )
                                        }

                                        DesignRoute.TRANSLATOR_MANAGER -> {
                                            com.d4viddf.hyperbridge.ui.screens.translators.TranslatorManagerScreen(
                                                onBack = { designRoute = DesignRoute.DASHBOARD },
                                                onCreateTranslator = { pkg ->
                                                    editingTranslatorId = null
                                                    newTranslatorPackageName = pkg
                                                    previousTranslatorManagerRoute = DesignRoute.TRANSLATOR_MANAGER
                                                    designRoute = DesignRoute.TRANSLATOR_EDITOR
                                                },
                                                onEditTranslator = { id ->
                                                    editingTranslatorId = id
                                                    newTranslatorPackageName = null
                                                    previousTranslatorManagerRoute = DesignRoute.TRANSLATOR_MANAGER
                                                    designRoute = DesignRoute.TRANSLATOR_EDITOR
                                                }
                                            )
                                        }

                                        DesignRoute.TRANSLATOR_EDITOR -> {
                                            com.d4viddf.hyperbridge.ui.screens.translators.TranslatorEditorScreen(
                                                translatorId = editingTranslatorId,
                                                initialPackageName = newTranslatorPackageName,
                                                onBack = {
                                                    designRoute = previousTranslatorManagerRoute
                                                    editingTranslatorId = null
                                                    newTranslatorPackageName = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> ActiveAppsPage(
                                apps = activeApps,
                                isLoading = isLoading,
                                systemIntegrations = systemIntegrations,
                                viewModel = viewModel,
                                showWarning = showWarning,
                                onWarningClick = {
                                    val intent =
                                        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(
                                                android.provider.Settings.EXTRA_APP_PACKAGE,
                                                context.packageName
                                            )
                                        }
                                    context.startActivity(intent)
                                },
                                onDismissWarning = {
                                    scope.launch {
                                        prefs.setFeaturedPermissionWarning(false)
                                    }
                                },
                                onConfig = { onAppConfigClick(it.packageName) },
                                onSystemConfig = { integration ->
                                    when (integration.id) {
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.SCREEN_RECORDER -> onScreenRecordingConfigClick()
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.SYSTEM_UPDATER -> onSystemUpdateConfigClick()
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.CHARGING -> onChargingConfigClick()
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.VPN -> onVpnConfigClick()
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.DEBUGGING -> {}
                                    }
                                },
                                onSettingsClick = onSettingsClick
                            )

                            2 -> LibraryPage(
                                apps = libraryApps,
                                isLoading = isLoading,
                                systemIntegrations = systemIntegrations,
                                viewModel = viewModel,
                                onConfig = { onAppConfigClick(it.packageName) },
                                onSystemConfig = { integration ->
                                    when (integration.id) {
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.SCREEN_RECORDER -> onScreenRecordingConfigClick()
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.SYSTEM_UPDATER -> onSystemUpdateConfigClick()
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.CHARGING -> onChargingConfigClick()
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.VPN -> onVpnConfigClick()
                                        com.d4viddf.hyperbridge.ui.SystemIntegrationId.DEBUGGING -> {}
                                    }
                                },
                                onSettingsClick = onSettingsClick
                            )
                        }
                    }
                }
            }
        }

        // --- OVERLAYS ---
        AnimatedVisibility(
            visible = showWidgetPicker,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(220)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(tween(140)),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            WidgetPickerScreen(
                onBack = { showWidgetPicker = false },
                onWidgetSelected = { newId ->
                    showWidgetPicker = false
                    editingWidgetId = newId
                }
            )
        }

        AnimatedVisibility(
            visible = editingWidgetId != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(220)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(tween(140)),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (editingWidgetId != null) {
                WidgetConfigScreen(
                    widgetId = editingWidgetId!!,
                    onBack = { editingWidgetId = null }
                )
            }
        }
    }
}
