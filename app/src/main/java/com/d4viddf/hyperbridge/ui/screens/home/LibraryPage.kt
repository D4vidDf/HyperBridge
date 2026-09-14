package com.d4viddf.hyperbridge.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.AppCategory
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.SystemIntegrationId
import com.d4viddf.hyperbridge.ui.SystemIntegrationInfo
import com.d4viddf.hyperbridge.ui.components.AppListFilterSection
import com.d4viddf.hyperbridge.ui.components.AppListItem
import com.d4viddf.hyperbridge.ui.components.EmptyState
import com.d4viddf.hyperbridge.ui.components.SystemIntegrationListItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryPage(
    apps: List<AppInfo>,
    isLoading: Boolean,
    systemIntegrations: List<SystemIntegrationInfo>,
    viewModel: AppListViewModel,
    onConfig: (AppInfo) -> Unit,
    onSystemConfig: (SystemIntegrationInfo) -> Unit,
    onSettingsClick: () -> Unit
) {
    val searchQuery = viewModel.librarySearch.collectAsState().value
    val selectedCategory = viewModel.libraryCategory.collectAsState().value
    val sortOption = viewModel.librarySort.collectAsState().value
    val systemSelected = viewModel.librarySystemSelected.collectAsState().value
    val showSystem = systemSelected || (selectedCategory == AppCategory.ALL && searchQuery.isBlank())
    val hasSystemContent = showSystem && systemIntegrations.isNotEmpty()

    val isRefreshing = isLoading && apps.isNotEmpty()
    val pullState = rememberPullToRefreshState()

    Scaffold(
        // [FIX] Only respect Status Bars here.
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name),style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                actions = {
                    Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp)
                        .clip(CircleShape) // Ensure ripple is circular
                        .clickable(onClick = onSettingsClick), // [NEW] Added Clickable here
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Settings, stringResource(R.string.settings), modifier = Modifier.size(20.dp))
                    }
                }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AppListFilterSection(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.librarySearch.value = it },
                selectedCategory = selectedCategory,
                onCategoryChange = viewModel::selectLibraryAppCategory,
                sortOption = sortOption,
                onSortChange = { viewModel.librarySort.value = it },
                showSystemCategory = true,
                systemSelected = systemSelected,
                onSystemSelected = viewModel::selectLibrarySystem
            )

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
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                ) {
                    if (apps.isEmpty() && !hasSystemContent && isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    }
                    else if (apps.isEmpty() && !hasSystemContent) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                title = stringResource(R.string.no_apps_found),
                                description = "",
                                icon = Icons.Default.SearchOff
                            )
                        }
                    }
                    else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            if (hasSystemContent) {
                                item(key = "system_header") {
                                    Text(
                                        text = stringResource(R.string.system_integrations),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                                items(systemIntegrations, key = { "system_${it.id.name}" }) { integration ->
                                    Column(modifier = Modifier.animateItem()) {
                                        SystemIntegrationListItem(
                                            integration = integration,
                                            onToggle = { viewModel.toggleSystemIntegration(integration.id, it) },
                                            onSettingsClick = if (integration.available && integration.id != SystemIntegrationId.VPN) {
                                                { onSystemConfig(integration) }
                                            } else null
                                        )
                                        HorizontalDivider(

                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                            items(apps, key = { it.packageName }) { app ->
                                Column(modifier = Modifier.animateItem()) {
                                    AppListItem(
                                        app = app,
                                        onToggle = { viewModel.toggleApp(app.packageName, it) },
                                        onSettingsClick = { onConfig(app) },
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    )
                                }
                            }

                            if (isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LoadingIndicator(modifier = Modifier.width(40.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}