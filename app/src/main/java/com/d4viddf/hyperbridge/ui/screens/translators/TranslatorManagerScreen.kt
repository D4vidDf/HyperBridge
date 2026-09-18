package com.d4viddf.hyperbridge.ui.screens.translators

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorManagerScreen(
    onBack: () -> Unit,
    onCreateTranslator: (initialPackage: String?) -> Unit,
    onEditTranslator: (id: String) -> Unit,
    viewModel: TranslatorViewModel = viewModel()
) {
    val allTranslators by viewModel.allTranslators.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    TranslatorManagerContent(
        translators = allTranslators,
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        onBack = onBack,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onFilterSelected = { viewModel.setFilter(it) },
        onToggleTranslator = { id, enabled -> viewModel.toggleTranslator(id, enabled) },
        onCreateTranslator = onCreateTranslator,
        onEditTranslator = onEditTranslator,
        onDuplicateTranslator = { viewModel.duplicateTranslator(it) },
        onDeleteTranslator = { viewModel.deleteTranslator(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorManagerContent(
    translators: List<CustomTranslator>,
    searchQuery: String,
    selectedFilter: TranslatorFilterScope,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (TranslatorFilterScope) -> Unit,
    onToggleTranslator: (String, Boolean) -> Unit,
    onCreateTranslator: (initialPackage: String?) -> Unit,
    onEditTranslator: (id: String) -> Unit,
    onDuplicateTranslator: (CustomTranslator) -> Unit,
    onDeleteTranslator: (String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val filteredTranslators = remember(translators, searchQuery, selectedFilter) {
        translators.filter { translator ->
            val matchesQuery = searchQuery.isBlank() ||
                    translator.meta.name.contains(searchQuery, ignoreCase = true) ||
                    translator.meta.description.contains(searchQuery, ignoreCase = true) ||
                    translator.targetPackages.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (selectedFilter) {
                TranslatorFilterScope.ALL -> true
                TranslatorFilterScope.ACTIVE -> translator.isEnabled
                TranslatorFilterScope.INACTIVE -> !translator.isEnabled
                TranslatorFilterScope.GLOBAL -> translator.targetScope == TargetScope.GLOBAL
                TranslatorFilterScope.APPS -> translator.targetScope == TargetScope.SPECIFIC_APPS
                TranslatorFilterScope.NOTIF_TYPES -> translator.targetScope == TargetScope.NOTIFICATION_TYPE
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.translators_manager_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.translators_manager_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateTranslator(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.translators_create_button)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search & Filter Bar (M3 Expressive)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.translators_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    val filters = listOf(
                        TranslatorFilterScope.ALL to R.string.translators_filter_all,
                        TranslatorFilterScope.ACTIVE to R.string.translators_filter_active,
                        TranslatorFilterScope.INACTIVE to R.string.translators_filter_inactive,
                        TranslatorFilterScope.GLOBAL to R.string.translators_filter_global,
                        TranslatorFilterScope.APPS to R.string.translators_filter_apps,
                        TranslatorFilterScope.NOTIF_TYPES to R.string.translators_filter_types
                    )

                    items(filters) { (filter, stringRes) ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(stringResource(stringRes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            if (filteredTranslators.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.translators_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = stringResource(R.string.translators_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Button(
                            onClick = { onCreateTranslator(null) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.translators_create_button))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(filteredTranslators, key = { _, item -> item.id }) { index, translator ->
                        val shape = getExpressiveShape(filteredTranslators.size, index, ShapeStyle.Large)

                        TranslatorCardItem(
                            translator = translator,
                            shape = shape,
                            onToggle = { isEnabled -> onToggleTranslator(translator.id, isEnabled) },
                            onClick = { onEditTranslator(translator.id) },
                            onDuplicate = { onDuplicateTranslator(translator) },
                            onDelete = { onDeleteTranslator(translator.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TranslatorCardItem(
    translator: CustomTranslator,
    shape: androidx.compose.ui.graphics.Shape,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (translator.isEnabled) MaterialTheme.colorScheme.surfaceContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = when (translator.targetScope) {
                        TargetScope.GLOBAL -> MaterialTheme.colorScheme.primaryContainer
                        TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.secondaryContainer
                        TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.tertiaryContainer
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (translator.targetScope) {
                                TargetScope.GLOBAL -> Icons.Default.Public
                                TargetScope.SPECIFIC_APPS -> Icons.Outlined.Apps
                                TargetScope.NOTIFICATION_TYPE -> Icons.Outlined.Category
                            },
                            contentDescription = null,
                            tint = when (translator.targetScope) {
                                TargetScope.GLOBAL -> MaterialTheme.colorScheme.onPrimaryContainer
                                TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.onSecondaryContainer
                                TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.onTertiaryContainer
                            }
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translator.meta.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (translator.targetScope) {
                            TargetScope.GLOBAL -> stringResource(R.string.translators_scope_global)
                            TargetScope.SPECIFIC_APPS -> stringResource(R.string.translators_scope_apps, translator.targetPackages.size)
                            TargetScope.NOTIFICATION_TYPE -> stringResource(R.string.translators_scope_types, translator.targetNotificationTypes.size)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = translator.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            if (translator.meta.description.isNotEmpty()) {
                Text(
                    text = translator.meta.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = stringResource(R.string.translators_priority_label, translator.priority),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.translators_action_duplicate), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.translators_action_edit), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.translators_action_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// --- PREVIEWS ---

@androidx.compose.ui.tooling.preview.Preview(name = "Translator Manager - Populated", showBackground = true)
@Composable
private fun TranslatorManagerPopulatedPreview() {
    val sampleTranslators = listOf(
        CustomTranslator(
            id = "sample_1",
            meta = com.d4viddf.hyperbridge.models.translator.TranslatorMetadata(
                name = "Telegram Sender Formatter",
                author = "Community",
                description = "Extracts sender name and formats chat messages nicely on HyperOS."
            ),
            targetScope = TargetScope.SPECIFIC_APPS,
            targetPackages = listOf("org.telegram.messenger"),
            priority = 100,
            isEnabled = true
        ),
        CustomTranslator(
            id = "sample_2",
            meta = com.d4viddf.hyperbridge.models.translator.TranslatorMetadata(
                name = "Global OTP Extractor",
                author = "HyperBridge",
                description = "Extracts 4-8 digit verification codes and adds a direct Copy button."
            ),
            targetScope = TargetScope.GLOBAL,
            priority = 200,
            isEnabled = true
        ),
        CustomTranslator(
            id = "sample_3",
            meta = com.d4viddf.hyperbridge.models.translator.TranslatorMetadata(
                name = "Delivery Status Tracker",
                author = "David",
                description = "Extracts tracking progress and displays waypoint badges."
            ),
            targetScope = TargetScope.NOTIFICATION_TYPE,
            targetNotificationTypes = listOf("PROGRESS"),
            priority = 50,
            isEnabled = false
        )
    )

    MaterialTheme {
        TranslatorManagerContent(
            translators = sampleTranslators,
            searchQuery = "",
            selectedFilter = TranslatorFilterScope.ALL,
            onBack = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onToggleTranslator = { _, _ -> },
            onCreateTranslator = {},
            onEditTranslator = {},
            onDuplicateTranslator = {},
            onDeleteTranslator = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Translator Manager - Empty", showBackground = true)
@Composable
private fun TranslatorManagerEmptyPreview() {
    MaterialTheme {
        TranslatorManagerContent(
            translators = emptyList(),
            searchQuery = "",
            selectedFilter = TranslatorFilterScope.ALL,
            onBack = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onToggleTranslator = { _, _ -> },
            onCreateTranslator = {},
            onEditTranslator = {},
            onDuplicateTranslator = {},
            onDeleteTranslator = {}
        )
    }
}
