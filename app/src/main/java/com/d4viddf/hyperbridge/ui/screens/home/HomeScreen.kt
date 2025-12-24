package com.d4viddf.hyperbridge.ui.screens.home

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

private enum class DesignRoute {
    DASHBOARD,
    WIDGET_LIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppListViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onNavConfigClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) }

    // Internal Design Navigation
    var designRoute by remember { mutableStateOf(DesignRoute.DASHBOARD) }

    // Overlay States
    var showWidgetPicker by remember { mutableStateOf(false) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var configApp by remember { mutableStateOf<AppInfo?>(null) }

    val activeApps by viewModel.activeAppsState.collectAsState()
    val libraryApps by viewModel.libraryAppsState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // --- PREDICTIVE BACK HANDLERS ---
    // The order matters: Last added = First handled

    // 1. Config App Bottom Sheet (Handled internally by ModalBottomSheet usually, but we track state)
    if (configApp != null) {
        BackHandler { configApp = null }
    }

    // 2. Widget Config Screen (Deepest overlay)
    if (editingWidgetId != null) {
        BackHandler { editingWidgetId = null }
    }

    // 3. Widget Picker Screen
    if (showWidgetPicker) {
        BackHandler { showWidgetPicker = false }
    }

    // 4. Design Tab Internal Navigation
    if (selectedTab == 0 && designRoute == DesignRoute.WIDGET_LIST) {
        BackHandler { designRoute = DesignRoute.DASHBOARD }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                // Hide TopBar only inside the "Widget List" sub-screen to allow its own toolbar
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
                // Hide BottomBar if deeply navigated (Standard Android Pattern)
                // This gives more space to the list
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
            // If we are in the sub-screen, we consume the padding manually or ignore it
            // to allow full screen content (since we hide bars)
            val effectivePadding = if (selectedTab == 0 && designRoute == DesignRoute.WIDGET_LIST) PaddingValues(0.dp) else padding

            Box(modifier = Modifier.padding(effectivePadding)) {
                when (selectedTab) {
                    0 -> {
                        // [ANIMATION] Smooth Slide Transition between Dashboard and List
                        AnimatedContent(
                            targetState = designRoute,
                            transitionSpec = {
                                if (targetState == DesignRoute.WIDGET_LIST) {
                                    // Going Deeper: Slide In from Right
                                    slideInHorizontally { it } + fadeIn() togetherWith
                                            slideOutHorizontally { -it / 3 } + fadeOut()
                                } else {
                                    // Going Back: Slide In from Left
                                    slideInHorizontally { -it } + fadeIn() togetherWith
                                            slideOutHorizontally { it / 3 } + fadeOut()
                                }
                            },
                            label = "DesignTabNav"
                        ) { route ->
                            if (route == DesignRoute.DASHBOARD) {
                                DesignScreen(
                                    onNavigateToWidgets = { designRoute = DesignRoute.WIDGET_LIST },
                                    onLaunchPicker = { showWidgetPicker = true }
                                )
                            } else {
                                SavedAppWidgetsScreen(
                                    onBack = { designRoute = DesignRoute.DASHBOARD },
                                    onEditWidget = { id -> editingWidgetId = id },
                                    onAddMore = { showWidgetPicker = true }
                                )
                            }
                        }
                    }
                    1 -> ActiveAppsPage(activeApps, isLoading, viewModel) { configApp = it }
                    2 -> LibraryPage(libraryApps, isLoading, viewModel) { configApp = it }
                }
            }
        }

        // --- GLOBAL OVERLAYS (Full Screen Slide-Up) ---

        // 1. Widget Picker
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
                    // Small delay to allow picker to close before opening config
                    editingWidgetId = newId
                }
            )
        }

        // 2. Widget Configuration
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