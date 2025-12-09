package com.d4viddf.hyperbridge.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.outlined.Apps
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
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.components.AppConfigBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppListViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onNavConfigClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val activeApps by viewModel.activeAppsState.collectAsState()
    val libraryApps by viewModel.libraryAppsState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // State to hold the app being configured
    var configApp by remember { mutableStateOf<com.d4viddf.hyperbridge.ui.AppInfo?>(null) }

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
                NavigationBarItem(
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(if(selectedTab==0) Icons.Filled.ToggleOn else Icons.Outlined.ToggleOff, null) },
                    label = { Text(stringResource(R.string.tab_active)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(if(selectedTab==1) Icons.Filled.Apps else Icons.Outlined.Apps, null) },
                    label = { Text(stringResource(R.string.tab_library)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (selectedTab == 0) {
                ActiveAppsPage(activeApps, isLoading, viewModel) { configApp = it }
            } else {
                LibraryPage(libraryApps, isLoading, viewModel) { configApp = it }
            }
        }
    }

    // --- BOTTOM SHEET HANDLER (CRASH FIX) ---
    if (configApp != null) {
        // Capture the non-null app object locally
        val safeApp = configApp!!

        AppConfigBottomSheet(
            app = safeApp,
            viewModel = viewModel,
            onDismiss = { configApp = null },
            onNavConfigClick = {
                // 1. Trigger Navigation using the SAFE local variable
                onNavConfigClick(safeApp.packageName)
                // 2. Close Sheet
                configApp = null
            }
        )
    }
}