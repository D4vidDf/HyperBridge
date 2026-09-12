package com.d4viddf.hyperbridge.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.AppCategory
import com.d4viddf.hyperbridge.ui.SortOption

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppListFilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: AppCategory,
    onCategoryChange: (AppCategory) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    showSystemCategory: Boolean = false,
    systemSelected: Boolean = false,
    onSystemSelected: (Boolean) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val categories = AppCategory.entries.toTypedArray()

    Column {
        // 1. SEARCH BAR
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                onSearch = { focusManager.clearFocus() },
                active = false,
                onActiveChange = { },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, stringResource(R.string.clear))
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.fillMaxWidth(),
                content = {}
            )
        }

        // 2. TOOLBAR ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // A. SORT TOGGLE
            IconToggleButton(
                checked = sortOption == SortOption.NAME_ZA,
                onCheckedChange = { isChecked ->
                    onSortChange(if (isChecked) SortOption.NAME_ZA else SortOption.NAME_AZ)
                },
                colors = IconButtonDefaults.iconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (sortOption == SortOption.NAME_AZ) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.sort_order)
                )
            }

            VerticalDivider(
                modifier = Modifier.height(32.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            if (showSystemCategory) {
                ToggleButton(
                    checked = systemSelected,
                    onCheckedChange = { onSystemSelected(true) },
                    modifier = Modifier.semantics { role = Role.RadioButton },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = if (systemSelected) Icons.Filled.Android else Icons.Outlined.Android,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.system_integrations))
                }
            }

            // B. CONNECTED BUTTON GROUP
            Row(
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                categories.forEachIndexed { index, category ->
                    val isSelected = !systemSelected && selectedCategory == category

                    // Define Connected Shapes using Defaults
                    val shape = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        categories.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }

                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onCategoryChange(category) },
                        shapes = shape,
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Icon Animation
                        AnimatedContent(
                            targetState = isSelected,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(200))).togetherWith(fadeOut(animationSpec = tween(200)))
                            },
                            label = "IconTransition"
                        ) { selected ->
                            Icon(
                                imageVector = getCategoryIcon(category, selected),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                        // Localized Text
                        Text(stringResource(getCategoryLabelRes(category)))
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    }
}

// Helper: Map Category to Resource ID
@StringRes
private fun getCategoryLabelRes(category: AppCategory): Int {
    return when (category) {
        AppCategory.ALL -> R.string.cat_all
        AppCategory.MUSIC -> R.string.cat_music
        AppCategory.MAPS -> R.string.cat_nav
        AppCategory.TIMER -> R.string.cat_timer
        AppCategory.OTHER -> R.string.cat_other
    }
}

// Helper: Map Category to Icons
private fun getCategoryIcon(category: AppCategory, isSelected: Boolean): ImageVector {
    return if (isSelected) {
        when (category) {
            AppCategory.ALL -> Icons.Filled.Apps
            AppCategory.MUSIC -> Icons.Filled.MusicNote
            AppCategory.MAPS -> Icons.Filled.Place
            AppCategory.TIMER -> Icons.Filled.Timer
            AppCategory.OTHER -> Icons.Filled.Category
        }
    } else {
        when (category) {
            AppCategory.ALL -> Icons.Outlined.Apps
            AppCategory.MUSIC -> Icons.Outlined.MusicNote
            AppCategory.MAPS -> Icons.Outlined.Place
            AppCategory.TIMER -> Icons.Outlined.Timer
            AppCategory.OTHER -> Icons.Outlined.Category
        }
    }
}