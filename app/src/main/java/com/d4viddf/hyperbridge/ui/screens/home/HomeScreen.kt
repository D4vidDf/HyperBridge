package com.d4viddf.hyperbridge.ui.screens.home

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.components.AppConfigBottomSheet
import com.d4viddf.hyperbridge.ui.screens.design.DesignScreen
import com.d4viddf.hyperbridge.ui.screens.design.SavedAppWidgetsScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetConfigScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetPickerScreen
import com.d4viddf.hyperbridge.ui.screens.theme.ThemeCreatorScreen
import com.d4viddf.hyperbridge.ui.screens.theme.ThemeManagerScreen
import androidx.core.net.toUri

private enum class DesignRoute {
    DASHBOARD,
    WIDGET_LIST,
    THEME_MANAGER,
    THEME_CREATOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppListViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onNavConfigClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) }
    var designRoute by remember { mutableStateOf(DesignRoute.DASHBOARD) }

    // [FIX] Track which theme is being edited
    var editingThemeId by remember { mutableStateOf<String?>(null) }

    // Overlay States
    var showWidgetPicker by remember { mutableStateOf(false) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var configApp by remember { mutableStateOf<AppInfo?>(null) }

    val activeApps by viewModel.activeAppsState.collectAsState()
    val libraryApps by viewModel.libraryAppsState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current

    // --- PREDICTIVE BACK HANDLERS ---
    if (configApp != null) BackHandler { configApp = null }
    if (editingWidgetId != null) BackHandler { editingWidgetId = null }
    if (showWidgetPicker) BackHandler { showWidgetPicker = false }

    if (selectedTab == 0 && designRoute != DesignRoute.DASHBOARD) {
        BackHandler {
            designRoute = when (designRoute) {
                DesignRoute.THEME_CREATOR -> {
                    editingThemeId = null // Reset edit state when backing out
                    DesignRoute.THEME_MANAGER
                }
                else -> DesignRoute.DASHBOARD
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (selectedTab != 0 || designRoute == DesignRoute.DASHBOARD) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = selectedTab != 0 || designRoute == DesignRoute.DASHBOARD,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    ShortNavigationBar {
                        ShortNavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(if (selectedTab == 0) Icons.Filled.Brush else Icons.Outlined.Brush, null) },
                            label = { Text("Design") }
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
            val effectivePadding = if (selectedTab == 0 && designRoute != DesignRoute.DASHBOARD) PaddingValues(0.dp) else padding

            Box(modifier = Modifier.padding(effectivePadding)) {
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
                                        onNavigateToWidgets = { designRoute = DesignRoute.WIDGET_LIST },
                                        onNavigateToThemes = { designRoute = DesignRoute.THEME_MANAGER },
                                        onLaunchPicker = { showWidgetPicker = true }
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
                                            val query = "HyperBridge Theme"
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW,
                                                    "market://search?q=$query&c=apps".toUri())
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val intent = Intent(Intent.ACTION_VIEW,
                                                    "https://play.google.com/store/search?q=$query&c=apps".toUri())
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
                                        },
                                        onCreateTheme = {
                                            editingThemeId = null // Ensure we start fresh
                                            designRoute = DesignRoute.THEME_CREATOR
                                        },
                                        // [FIX] Implemented Callback
                                        onEditTheme = { id ->
                                            editingThemeId = id
                                            designRoute = DesignRoute.THEME_CREATOR
                                        }
                                    )
                                }
                                DesignRoute.THEME_CREATOR -> {
                                    ThemeCreatorScreen(
                                        editThemeId = editingThemeId, // [FIX] Passed ID
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
                            }
                        }
                    }
                    1 -> ActiveAppsPage(activeApps, isLoading, viewModel) { configApp = it }
                    2 -> LibraryPage(libraryApps, isLoading, viewModel) { configApp = it }
                }
            }
        }

        // --- GLOBAL OVERLAYS ---
        AnimatedVisibility(
            visible = showWidgetPicker,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(),
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
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
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(),
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
        ) {
            if (editingWidgetId != null) {
                WidgetConfigScreen(
                    widgetId = editingWidgetId!!,
                    onBack = { editingWidgetId = null }
                )
            }
        }

        if (configApp != null) {
            AppConfigBottomSheet(
                app = configApp!!,
                viewModel = viewModel,
                onDismiss = { configApp = null },
                onNavConfigClick = {
                    onNavConfigClick(configApp!!.packageName)
                    configApp = null
                }
            )
        }
    }
}