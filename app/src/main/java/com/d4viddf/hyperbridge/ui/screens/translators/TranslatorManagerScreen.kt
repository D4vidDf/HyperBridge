package com.d4viddf.hyperbridge.ui.screens.translators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val filterState by viewModel.filterState.collectAsState()

    TranslatorManagerContent(
        translators = allTranslators,
        searchQuery = searchQuery,
        filterState = filterState,
        onBack = onBack,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onFilterStateChange = { viewModel.setFilterState(it) },
        onResetFilter = { viewModel.resetFilterState() },
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
    filterState: TranslatorFilterState = TranslatorFilterState(),
    selectedFilter: TranslatorFilterScope = filterState.statusScope,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterStateChange: ((TranslatorFilterState) -> Unit)? = null,
    onResetFilter: (() -> Unit)? = null,
    onFilterSelected: ((TranslatorFilterScope) -> Unit)? = null,
    onToggleTranslator: (String, Boolean) -> Unit,
    onCreateTranslator: (initialPackage: String?) -> Unit,
    onEditTranslator: (id: String) -> Unit,
    onDuplicateTranslator: (CustomTranslator) -> Unit,
    onDeleteTranslator: (String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showFilterSheet by remember { mutableStateOf(false) }

    val effectiveFilterState = remember(filterState, selectedFilter) {
        if (filterState.statusScope != selectedFilter && selectedFilter != TranslatorFilterScope.ALL && filterState.statusScope == TranslatorFilterScope.ALL) {
            filterState.copy(statusScope = selectedFilter)
        } else {
            filterState
        }
    }

    val filteredTranslators = remember(translators, searchQuery, effectiveFilterState) {
        translators.filter { translator ->
            val matchesQuery = searchQuery.isBlank() ||
                    translator.meta.name.contains(searchQuery, ignoreCase = true) ||
                    translator.meta.description.contains(searchQuery, ignoreCase = true) ||
                    translator.meta.author.contains(searchQuery, ignoreCase = true) ||
                    translator.targetPackages.any { it.contains(searchQuery, ignoreCase = true) } ||
                    translator.targetNotificationTypes.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesStatusScope = when (effectiveFilterState.statusScope) {
                TranslatorFilterScope.ALL -> true
                TranslatorFilterScope.ACTIVE -> translator.isEnabled
                TranslatorFilterScope.INACTIVE -> !translator.isEnabled
                TranslatorFilterScope.GLOBAL -> translator.targetScope == TargetScope.GLOBAL
                TranslatorFilterScope.APPS -> translator.targetScope == TargetScope.SPECIFIC_APPS
                TranslatorFilterScope.NOTIF_TYPES -> translator.targetScope == TargetScope.NOTIFICATION_TYPE
            }

            val matchesTypes = effectiveFilterState.selectedNotificationTypes.isEmpty() ||
                    translator.targetNotificationTypes.any { it in effectiveFilterState.selectedNotificationTypes }

            val matchesPackages = effectiveFilterState.selectedPackages.isEmpty() ||
                    translator.targetPackages.any { it in effectiveFilterState.selectedPackages }

            val matchesAuthors = effectiveFilterState.selectedAuthors.isEmpty() ||
                    translator.meta.author in effectiveFilterState.selectedAuthors

            val matchesIcons = effectiveFilterState.selectedIcons.isEmpty() ||
                    translator.meta.iconName in effectiveFilterState.selectedIcons

            matchesQuery && matchesStatusScope && matchesTypes && matchesPackages && matchesAuthors && matchesIcons
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    val isAllActive = !effectiveFilterState.isCustomFilterActive

                    FilterChip(
                        selected = isAllActive,
                        onClick = {
                            onResetFilter?.invoke() ?: onFilterStateChange?.invoke(TranslatorFilterState()) ?: onFilterSelected?.invoke(TranslatorFilterScope.ALL)
                        },
                        label = { Text(stringResource(R.string.translators_filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    val activeCount = (if (effectiveFilterState.statusScope != TranslatorFilterScope.ALL) 1 else 0) +
                            effectiveFilterState.selectedNotificationTypes.size +
                            effectiveFilterState.selectedPackages.size +
                            effectiveFilterState.selectedAuthors.size +
                            effectiveFilterState.selectedIcons.size

                    FilterChip(
                        selected = effectiveFilterState.isCustomFilterActive,
                        onClick = { showFilterSheet = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (activeCount > 0) {
                            {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$activeCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else null,
                        label = { Text(stringResource(R.string.translators_filter_btn)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            if (showFilterSheet) {
                TranslatorFilterSheet(
                    filterState = effectiveFilterState,
                    availableTranslators = translators,
                    onDismiss = { showFilterSheet = false },
                    onApply = { newState ->
                        onFilterStateChange?.invoke(newState)
                        showFilterSheet = false
                    },
                    onReset = {
                        onResetFilter?.invoke() ?: onFilterStateChange?.invoke(TranslatorFilterState())
                        showFilterSheet = false
                    }
                )
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
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (translator.isEnabled) MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
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
                // Outlined Icon Badge (Expressive container tinting based on scope and enabled status)
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (translator.isEnabled) {
                        when (translator.targetScope) {
                            TargetScope.GLOBAL -> MaterialTheme.colorScheme.primaryContainer
                            TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.secondaryContainer
                            TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getTranslatorOutlinedIcon(translator.meta.iconName),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (translator.isEnabled) {
                                when (translator.targetScope) {
                                    TargetScope.GLOBAL -> MaterialTheme.colorScheme.onPrimaryContainer
                                    TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.onSecondaryContainer
                                    TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.onTertiaryContainer
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translator.meta.name.ifBlank { stringResource(R.string.translators_empty_title) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (translator.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Scope chip / badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (translator.targetScope) {
                                TargetScope.GLOBAL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            }
                        ) {
                            Text(
                                text = when (translator.targetScope) {
                                    TargetScope.GLOBAL -> stringResource(R.string.translators_scope_global)
                                    TargetScope.SPECIFIC_APPS -> stringResource(R.string.translators_scope_apps, translator.targetPackages.size)
                                    TargetScope.NOTIFICATION_TYPE -> stringResource(R.string.translators_scope_types, translator.targetNotificationTypes.size)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (translator.targetScope) {
                                    TargetScope.GLOBAL -> MaterialTheme.colorScheme.primary
                                    TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.secondary
                                    TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.tertiary
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (translator.meta.author.isNotBlank()) {
                            Text(
                                text = "• " + translator.meta.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

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
                // Priority pill badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.translators_priority_label, translator.priority),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.translators_action_duplicate),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onClick,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.translators_action_edit),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.translators_action_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TranslatorFilterSheet(
    filterState: TranslatorFilterState,
    availableTranslators: List<CustomTranslator>,
    onDismiss: () -> Unit,
    onApply: (TranslatorFilterState) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftState by remember { mutableStateOf(filterState) }

    // Translators filtered by the currently selected Status & Scope
    val scopeFilteredTranslators = remember(availableTranslators, draftState.statusScope) {
        when (draftState.statusScope) {
            TranslatorFilterScope.ALL -> availableTranslators
            TranslatorFilterScope.ACTIVE -> availableTranslators.filter { it.isEnabled }
            TranslatorFilterScope.INACTIVE -> availableTranslators.filter { !it.isEnabled }
            TranslatorFilterScope.GLOBAL -> availableTranslators.filter { it.targetScope == TargetScope.GLOBAL }
            TranslatorFilterScope.APPS -> availableTranslators.filter { it.targetScope == TargetScope.SPECIFIC_APPS }
            TranslatorFilterScope.NOTIF_TYPES -> availableTranslators.filter { it.targetScope == TargetScope.NOTIFICATION_TYPE }
        }
    }

    // Aggregate available options dynamically from translators matching the chosen status & scope
    val availableNotificationTypes = remember(scopeFilteredTranslators) {
        scopeFilteredTranslators.flatMap { it.targetNotificationTypes }.distinct().sorted()
    }
    val availablePackages = remember(scopeFilteredTranslators) {
        scopeFilteredTranslators.flatMap { it.targetPackages }.distinct().sorted()
    }
    val availableAuthors = remember(scopeFilteredTranslators) {
        scopeFilteredTranslators.map { it.meta.author.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    val availableIcons = remember(scopeFilteredTranslators) {
        scopeFilteredTranslators.mapNotNull { it.meta.iconName }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    // Clean up selections that are no longer present in the active scope filter
    val sanitizedSelectedTypes = draftState.selectedNotificationTypes.filter { it in availableNotificationTypes }.toSet()
    val sanitizedSelectedPackages = draftState.selectedPackages.filter { it in availablePackages }.toSet()
    val sanitizedSelectedAuthors = draftState.selectedAuthors.filter { it in availableAuthors }.toSet()
    val sanitizedSelectedIcons = draftState.selectedIcons.filter { it in availableIcons }.toSet()

    val currentDraftState = draftState.copy(
        selectedNotificationTypes = sanitizedSelectedTypes,
        selectedPackages = sanitizedSelectedPackages,
        selectedAuthors = sanitizedSelectedAuthors,
        selectedIcons = sanitizedSelectedIcons
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header (fixed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.translators_filter_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.translators_filter_sheet_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Scrollable filter options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Status & Scope Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.translators_filter_status_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scopeOptions = listOf(
                            TranslatorFilterScope.ALL to R.string.translators_filter_all,
                            TranslatorFilterScope.ACTIVE to R.string.translators_filter_active,
                            TranslatorFilterScope.INACTIVE to R.string.translators_filter_inactive,
                            TranslatorFilterScope.GLOBAL to R.string.translators_filter_global,
                            TranslatorFilterScope.APPS to R.string.translators_filter_apps,
                            TranslatorFilterScope.NOTIF_TYPES to R.string.translators_filter_types
                        )
                        scopeOptions.forEach { (scope, labelRes) ->
                            val isSelected = currentDraftState.statusScope == scope
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    draftState = currentDraftState.copy(statusScope = scope)
                                },
                                label = { Text(stringResource(labelRes)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // 2. Notification Types Section (if available under active scope)
                if (availableNotificationTypes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.translators_filter_types_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableNotificationTypes.forEach { type ->
                                val isSelected = type in currentDraftState.selectedNotificationTypes
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newTypes = if (isSelected) {
                                            currentDraftState.selectedNotificationTypes - type
                                        } else {
                                            currentDraftState.selectedNotificationTypes + type
                                        }
                                        draftState = currentDraftState.copy(selectedNotificationTypes = newTypes)
                                    },
                                    label = { Text(type) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Target Apps Section (if available under active scope)
                if (availablePackages.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.translators_filter_apps_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availablePackages.forEach { pkg ->
                                val isSelected = pkg in currentDraftState.selectedPackages
                                val displayLabel = pkg.substringAfterLast('.')
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newPkgs = if (isSelected) {
                                            currentDraftState.selectedPackages - pkg
                                        } else {
                                            currentDraftState.selectedPackages + pkg
                                        }
                                        draftState = currentDraftState.copy(selectedPackages = newPkgs)
                                    },
                                    label = { Text(displayLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Creators / Authors Section (if available under active scope)
                if (availableAuthors.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.translators_filter_creators_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableAuthors.forEach { author ->
                                val isSelected = author in currentDraftState.selectedAuthors
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newAuthors = if (isSelected) {
                                            currentDraftState.selectedAuthors - author
                                        } else {
                                            currentDraftState.selectedAuthors + author
                                        }
                                        draftState = currentDraftState.copy(selectedAuthors = newAuthors)
                                    },
                                    label = { Text(author) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // 5. Configured Icons Section (if available under active scope)
                if (availableIcons.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.translators_filter_icons_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableIcons.forEach { iconName ->
                                val isSelected = iconName in currentDraftState.selectedIcons
                                val iconOption = TRANSLATOR_OUTLINED_ICONS.firstOrNull { it.id == iconName }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newIcons = if (isSelected) {
                                            currentDraftState.selectedIcons - iconName
                                        } else {
                                            currentDraftState.selectedIcons + iconName
                                        }
                                        draftState = currentDraftState.copy(selectedIcons = newIcons)
                                    },
                                    leadingIcon = iconOption?.let { opt ->
                                        {
                                            Icon(
                                                imageVector = opt.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    label = { Text(iconOption?.label ?: iconName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Fixed bottom actions bar with divider
            HorizontalDivider()

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            draftState = TranslatorFilterState()
                            onReset()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.translators_filter_reset))
                    }

                    Button(
                        onClick = { onApply(currentDraftState) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.translators_filter_apply))
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
            filterState = TranslatorFilterState(),
            onBack = {},
            onSearchQueryChange = {},
            onFilterStateChange = {},
            onResetFilter = {},
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
            filterState = TranslatorFilterState(),
            onBack = {},
            onSearchQueryChange = {},
            onFilterStateChange = {},
            onResetFilter = {},
            onToggleTranslator = { _, _ -> },
            onCreateTranslator = {},
            onEditTranslator = {},
            onDuplicateTranslator = {},
            onDeleteTranslator = {}
        )
    }
}
