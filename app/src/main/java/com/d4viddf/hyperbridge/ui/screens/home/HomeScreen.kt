package com.d4viddf.hyperbridge.ui.screens.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.screens.design.DesignManagerScreen
import com.d4viddf.hyperbridge.ui.screens.design.DesignScreen
import com.d4viddf.hyperbridge.ui.screens.design.SavedAppWidgetsScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetConfigScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetPickerScreen
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
    TRANSLATOR_EDITOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppListViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onNavConfigClick: (String) -> Unit,
    onScreenRecordingConfigClick: () -> Unit = {},
    onSystemUpdateConfigClick: () -> Unit = {},
    onAppConfigClick: (String) -> Unit = {},
    onNavigateToTranslators: () -> Unit = {},
    onCreateTranslator: (String?) -> Unit = {},
    onEditTranslator: (String) -> Unit = {}
) {

    var selectedTab by remember { mutableIntStateOf(1) }
    var designRoute by remember { mutableStateOf(DesignRoute.DASHBOARD) }
    var editingThemeId by remember { mutableStateOf<String?>(null) }
    var editingTranslatorId by remember { mutableStateOf<String?>(null) }
    var newTranslatorPackageName by remember { mutableStateOf<String?>(null) }
    var previousTranslatorManagerRoute by remember { mutableStateOf(DesignRoute.TRANSLATOR_MANAGER) }

    var showWidgetPicker by remember { mutableStateOf(false) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }

    val activeApps by viewModel.activeAppsState.collectAsState()
    val libraryApps by viewModel.libraryAppsState.collectAsState()
    val systemIntegrations by viewModel.systemIntegrationsState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current

    if (editingWidgetId != null) BackHandler { editingWidgetId = null }
    if (showWidgetPicker) BackHandler { showWidgetPicker = false }

    if (selectedTab == 0 && designRoute != DesignRoute.DASHBOARD) {
        BackHandler {
            designRoute = when (designRoute) {
                DesignRoute.THEME_CREATOR -> {
                    editingThemeId = null
                    DesignRoute.THEME_MANAGER
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = (selectedTab != 0 || designRoute == DesignRoute.DASHBOARD) && !showWidgetPicker && editingWidgetId == null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    ShortNavigationBar {
                        ShortNavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(if (selectedTab == 0) Icons.Filled.Brush else Icons.Outlined.Brush, null) },
                            label = { Text(stringResource(R.string.design)) }
                        )
                        ShortNavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(if (selectedTab == 1) Icons.Filled.ToggleOn else Icons.Outlined.ToggleOff, null) },
                            label = { Text(stringResource(R.string.tab_active)) }
                        )
                        ShortNavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(if (selectedTab == 2) Icons.Filled.Apps else Icons.Outlined.Apps, null) },
                            label = { Text(stringResource(R.string.tab_library)) }
                        )
                    }
                }
            }
        ) { padding ->
            val prefs = remember { com.d4viddf.hyperbridge.data.AppPreferences(context) }
            val showWarning by prefs.featuredPermissionWarningFlow.collectAsState(initial = false)
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .padding(bottom = padding.calculateBottomPadding())
                    .fillMaxSize()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
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
                                            onNavigateToWidgets = {
                                                designRoute = DesignRoute.WIDGET_LIST
                                            },
                                            onNavigateToThemes = {
                                                designRoute = DesignRoute.THEME_MANAGER
                                            },
                                            onEditTheme = { themeId ->
                                                editingThemeId = themeId
                                                designRoute = DesignRoute.THEME_CREATOR
                                            },
                                            onNavigateToDesigns = {
                                                designRoute = DesignRoute.DESIGN_MANAGER
                                            },
                                            onNavigateToTranslators = {
                                                designRoute = DesignRoute.TRANSLATOR_MANAGER
                                            },
                                            onCreateTranslator = {
                                                editingTranslatorId = null
                                                newTranslatorPackageName = null
                                                previousTranslatorManagerRoute = DesignRoute.TRANSLATOR_MANAGER
                                                designRoute = DesignRoute.TRANSLATOR_EDITOR
                                            },
                                            onLaunchPicker = { showWidgetPicker = true },
                                            onSettingsClick = onSettingsClick
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
                                            onFindThemes = {
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
                                            },
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
                                                // Create a new design in editor
                                                editingTranslatorId = null
                                                newTranslatorPackageName = null
                                                previousTranslatorManagerRoute = DesignRoute.DESIGN_MANAGER
                                                designRoute = DesignRoute.TRANSLATOR_EDITOR
                                            },
                                            onEditDesign = { id ->
                                                editingTranslatorId = id
                                                newTranslatorPackageName = null
                                                previousTranslatorManagerRoute = DesignRoute.DESIGN_MANAGER
                                                designRoute = DesignRoute.TRANSLATOR_EDITOR
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
                                    com.d4viddf.hyperbridge.ui.SystemIntegrationId.VPN -> {}
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
                                    com.d4viddf.hyperbridge.ui.SystemIntegrationId.VPN -> {}
                                }
                            },
                            onSettingsClick = onSettingsClick
                        )

                    }
                }
            }

            // --- OVERLAYS ---
            AnimatedVisibility(
                visible = showWidgetPicker,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(400)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400)
                ) + fadeOut(),
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
                    animationSpec = tween(400)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400)
                ) + fadeOut(),
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
}