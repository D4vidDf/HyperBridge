package com.d4viddf.hyperbridge.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.components.AppListFilterSection
import com.d4viddf.hyperbridge.ui.components.AppListItem
import com.d4viddf.hyperbridge.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActiveAppsPage(
    apps: List<AppInfo>,
    isLoading: Boolean,
    viewModel: AppListViewModel,
    onConfig: (AppInfo) -> Unit
) {
    val searchQuery = viewModel.activeSearch.collectAsState().value
    val selectedCategory = viewModel.activeCategory.collectAsState().value
    val sortOption = viewModel.activeSort.collectAsState().value

    // [LOGIC] Hide pull indicator on first load
    val isRefreshing = isLoading && apps.isNotEmpty()
    val pullState = rememberPullToRefreshState()

    Column(modifier = Modifier.fillMaxSize()) {

        AppListFilterSection(
            searchQuery = searchQuery,
            onSearchChange = { viewModel.activeSearch.value = it },
            selectedCategory = selectedCategory,
            onCategoryChange = { viewModel.activeCategory.value = it },
            sortOption = sortOption,
            onSortChange = { viewModel.activeSort.value = it }
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
                contentAlignment = Alignment.TopCenter, // Updated
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            ) {
                if (apps.isEmpty() && isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                } else if (apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = stringResource(R.string.no_active_bridges),
                            description = stringResource(R.string.no_active_bridges_desc),
                            icon = Icons.Outlined.NotificationsOff
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            Column(modifier = Modifier.animateItem()) {
                                AppListItem(
                                    app = app,
                                    onToggle = { viewModel.toggleApp(app.packageName, false) },
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
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
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