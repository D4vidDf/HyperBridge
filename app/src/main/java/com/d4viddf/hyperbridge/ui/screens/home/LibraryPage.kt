package com.d4viddf.hyperbridge.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
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
fun LibraryPage(
    apps: List<AppInfo>,
    isLoading: Boolean,
    viewModel: AppListViewModel,
    onConfig: (AppInfo) -> Unit
) {
    val searchQuery = viewModel.librarySearch.collectAsState().value
    val selectedCategory = viewModel.libraryCategory.collectAsState().value
    val sortOption = viewModel.librarySort.collectAsState().value

    // [LOGIC] Only show the Pull Indicator if we have content.
    // On first load (apps empty), we hide it so we can show the big centered loader instead.
    val isRefreshing = isLoading && apps.isNotEmpty()
    val pullState = rememberPullToRefreshState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppListFilterSection(
            searchQuery = searchQuery,
            onSearchChange = { viewModel.librarySearch.value = it },
            selectedCategory = selectedCategory,
            onCategoryChange = { viewModel.libraryCategory.value = it },
            sortOption = sortOption,
            onSortChange = { viewModel.librarySort.value = it }
        )

        // Wrapper Box to ensure weight is applied correctly
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
                contentAlignment = Alignment.TopCenter, // Updated to TopCenter
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            ) {
                // 1. Initial Load / Refreshing Empty State
                if (apps.isEmpty() && isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }
                // 2. Empty State (No results)
                else if (apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = stringResource(R.string.no_apps_found),
                            description = "",
                            icon = Icons.Default.SearchOff
                        )
                    }
                }
                // 3. List Content
                else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
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

                        // Bottom Pagination Loader (only if we have content)
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