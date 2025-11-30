package com.d4viddf.hyperbridge.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
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
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (apps.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_active_bridges),
                    description = stringResource(R.string.no_active_bridges_desc),
                    icon = Icons.Outlined.NotificationsOff
                )
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
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}