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
import com.d4viddf.hyperbridge.ui.screens.design.WidgetConfigScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetPickerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppListViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onNavConfigClick: (String) -> Unit
) {
    // --- Navigation State ---
    // 0 = Design, 1 = Active, 2 = Library
    var selectedTab by remember { mutableIntStateOf(1) }

    // Overlay States
    var showWidgetPicker by remember { mutableStateOf(false) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var configApp by remember { mutableStateOf<AppInfo?>(null) }

    // --- Data State ---
    val activeApps by viewModel.activeAppsState.collectAsState()
    val libraryApps by viewModel.libraryAppsState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // --- Back Handling ---
    // We handle back presses for overlays manually to keep the user in the app
    if (showWidgetPicker) {
        BackHandler { showWidgetPicker = false }
    } else if (editingWidgetId != null) {
        BackHandler { editingWidgetId = null }
    }

    // --- Screen Content Switching ---
    when {
        // 1. Show Widget Picker Overlay (Custom List of Widgets)
        showWidgetPicker -> {
            WidgetPickerScreen(
                onBack = { showWidgetPicker = false },
                onWidgetSelected = { newWidgetId ->
                    showWidgetPicker = false
                    editingWidgetId = newWidgetId // Proceed to config
                }
            )
        }

        // 2. Show Widget Configuration Overlay (Preview)
        editingWidgetId != null -> {
            WidgetConfigScreen(
                widgetId = editingWidgetId!!,
                onBack = { editingWidgetId = null }
            )
        }

        // 3. Show Main Tabs
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Bold)
                        },
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    NavigationBar {
                        // Design Tab
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(if (selectedTab == 0) Icons.Filled.Brush else Icons.Outlined.Brush, null) },
                            label = { Text("Design") }
                        )
                        // Active Tab
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(if (selectedTab == 1) Icons.Filled.ToggleOn else Icons.Outlined.ToggleOff, null) },
                            label = { Text(stringResource(R.string.tab_active)) }
                        )
                        // Library Tab
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
                            onOpenWidgetConfig = { widgetId -> editingWidgetId = widgetId },
                            onLaunchPicker = { showWidgetPicker = true } // Trigger the overlay
                        )
                        1 -> ActiveAppsPage(activeApps, isLoading, viewModel) { configApp = it }
                        2 -> LibraryPage(libraryApps, isLoading, viewModel) { configApp = it }
                    }
                }
            }
        }
    }

    // --- App Config Bottom Sheet (Existing Logic) ---
    if (configApp != null) {
        val safeApp = configApp!!
        AppConfigBottomSheet(
            app = safeApp,
            viewModel = viewModel,
            onDismiss = { configApp = null },
            onNavConfigClick = {
                onNavConfigClick(safeApp.packageName)
                configApp = null
            }
        )
    }
}