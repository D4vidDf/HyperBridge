package com.d4viddf.hyperbridge.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.components.AppConfigBottomSheet
import com.d4viddf.hyperbridge.ui.screens.design.DesignScreen
import com.d4viddf.hyperbridge.ui.screens.design.SavedAppWidgetsScreen
import com.d4viddf.hyperbridge.ui.screens.design.SavedWidgetsListScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetConfigScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetPickerScreen

private sealed class DesignRoute {
    data object Dashboard : DesignRoute()
    data object WidgetList : DesignRoute()
    data class AppDetail(val packageName: String, val appName: String) : DesignRoute()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppListViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onNavConfigClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) }
    var designRoute by remember { mutableStateOf<DesignRoute>(DesignRoute.Dashboard) }

    // Overlay States
    var showWidgetPicker by remember { mutableStateOf(false) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var configApp by remember { mutableStateOf<AppInfo?>(null) }

    val activeApps by viewModel.activeAppsState.collectAsState()
    val libraryApps by viewModel.libraryAppsState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // --- Back Handling ---
    if (showWidgetPicker) BackHandler { showWidgetPicker = false }
    else if (editingWidgetId != null) BackHandler { editingWidgetId = null }
    else if (selectedTab == 0 && designRoute !is DesignRoute.Dashboard) {
        // Handle nested design navigation
        when (designRoute) {
            is DesignRoute.AppDetail -> BackHandler { designRoute = DesignRoute.WidgetList }
            is DesignRoute.WidgetList -> BackHandler { designRoute = DesignRoute.Dashboard }
            else -> {}
        }
    }

    // --- MAIN UI ---
    // We determine if we are in a "Full Screen" mode (Deep inside Design Tab)
    // If so, we bypass the Main Scaffold with BottomBar
    val isFullScreenDesign = selectedTab == 0 && designRoute !is DesignRoute.Dashboard

    Box {
        if (isFullScreenDesign) {
            // [NEW SCREEN] Render full screen content without Bottom Nav
            when (val route = designRoute) {
                DesignRoute.WidgetList -> SavedWidgetsListScreen(
                    onNavigateToDetail = { pkg, name ->
                        designRoute = DesignRoute.AppDetail(pkg, name)
                    },
                    onLaunchPicker = { showWidgetPicker = true },
                    onBack = { designRoute = DesignRoute.Dashboard }
                )
                is DesignRoute.AppDetail -> SavedAppWidgetsScreen(
                    packageName = route.packageName,
                    appName = route.appName,
                    onBack = { designRoute = DesignRoute.WidgetList },
                    onEditWidget = { id -> editingWidgetId = id },
                    onAddMore = { showWidgetPicker = true }
                )
                else -> {}
            }
        } else {
            // [MAIN SCREEN] Render standard tabs with Bottom Nav
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(if (selectedTab == 0) Icons.Filled.Brush else Icons.Outlined.Brush, null) },
                            label = { Text("Design") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(if (selectedTab == 1) Icons.Filled.ToggleOn else Icons.Outlined.ToggleOff, null) },
                            label = { Text(stringResource(R.string.tab_active)) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(if (selectedTab == 2) Icons.Filled.Apps else Icons.Outlined.Apps, null) },
                            label = { Text(stringResource(R.string.tab_library)) }
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (selectedTab) {
                        0 -> DesignScreen(
                            onNavigateToWidgets = { designRoute = DesignRoute.WidgetList },
                            onLaunchPicker = { showWidgetPicker = true } // FAB Action
                        )
                        1 -> ActiveAppsPage(activeApps, isLoading, viewModel) { configApp = it }
                        2 -> LibraryPage(libraryApps, isLoading, viewModel) { configApp = it }
                    }
                }
            }
        }

        // --- OVERLAYS (Cover everything) ---

        // 1. Widget Picker (The "Bottom Sheet" style picker)
        if (showWidgetPicker) {
            WidgetPickerScreen(
                onBack = { showWidgetPicker = false },
                onWidgetSelected = { newId ->
                    showWidgetPicker = false
                    editingWidgetId = newId
                }
            )
        }

        // 2. Widget Config (The Preview Screen)
        if (editingWidgetId != null) {
            WidgetConfigScreen(
                widgetId = editingWidgetId!!,
                onBack = { editingWidgetId = null }
            )
        }

        // 3. App Config Bottom Sheet
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