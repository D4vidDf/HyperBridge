package com.d4viddf.hyperbridge.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.service.floating.FloatingNotificationSetup
import com.d4viddf.hyperbridge.service.floating.FloatingSetupStatus
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import com.d4viddf.hyperbridge.util.DeviceUtils
import com.d4viddf.hyperbridge.util.NotificationSettingsNavigator
import com.d4viddf.hyperbridge.util.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FloatingSetupAppItem(
    val packageName: String,
    val label: String,
    val status: FloatingSetupStatus,
    val isConfirmed: Boolean,
    val previewIcon: ImageBitmap? = null
)

enum class FloatingFilterOption {
    ALL,
    NEEDS_REVIEW,
    CONFIRMED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingNotificationSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val selectedPackages by preferences.allowedPackagesFlow.collectAsState(initial = emptySet())
    val confirmedPackages by preferences.floatingSetupConfirmedPackagesFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val requiresManualSetup = remember { DeviceUtils.isXiaomi && DeviceUtils.isCompatibleOS() }

    LaunchedEffect(Unit) {
        preferences.setFloatingSetupNoticePending(false)
    }

    val appItems = remember(selectedPackages, confirmedPackages, requiresManualSetup) {
        selectedPackages.sorted().map { packageName ->
            val label = try {
                val info = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                packageName
            }
            val status = FloatingNotificationSetup.status(
                isSelected = true,
                userConfirmedDisabled = packageName in confirmedPackages,
                requiresManualSetup = requiresManualSetup
            )
            FloatingSetupAppItem(
                packageName = packageName,
                label = label,
                status = status,
                isConfirmed = packageName in confirmedPackages
            )
        }
    }

    FloatingNotificationSetupContent(
        apps = appItems,
        requiresManualSetup = requiresManualSetup,
        onBack = onBack,
        onOpenSettings = { packageName ->
            if (!NotificationSettingsNavigator.openForApp(context, packageName)) {
                Toast.makeText(context, R.string.settings_not_available, Toast.LENGTH_SHORT).show()
            }
        },
        onConfirmedChange = { packageName, confirmed ->
            scope.launch { preferences.setFloatingSetupConfirmed(packageName, confirmed) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingNotificationSetupContent(
    apps: List<FloatingSetupAppItem>,
    requiresManualSetup: Boolean,
    onBack: () -> Unit,
    onOpenSettings: (packageName: String) -> Unit,
    onConfirmedChange: (packageName: String, confirmed: Boolean) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(FloatingFilterOption.ALL) }
    var isInfoExpanded by rememberSaveable { mutableStateOf(false) }

    val totalCount = apps.size
    val confirmedCount = apps.count { it.isConfirmed }
    val needsReviewCount = apps.count { it.status == FloatingSetupStatus.NEEDS_REVIEW }

    val filteredApps = remember(apps, searchQuery, selectedFilter) {
        apps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                FloatingFilterOption.ALL -> true
                FloatingFilterOption.NEEDS_REVIEW -> app.status == FloatingSetupStatus.NEEDS_REVIEW
                FloatingFilterOption.CONFIRMED -> app.isConfirmed
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.floating_setup_title),
                        fontWeight = FontWeight.Bold
                    )
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // --- HERO STATUS & INFO CARD ---
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Title + Icon Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.floating_setup_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.floating_setup_settings_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Progress Indicator & Status Pill (when apps exist)
                            if (totalCount > 0) {
                                val isAllConfirmed = confirmedCount == totalCount
                                val progress = confirmedCount.toFloat() / totalCount.toFloat()
                                val animatedProgress by animateFloatAsState(
                                    targetValue = progress,
                                    animationSpec = tween(durationMillis = 400),
                                    label = "floatingProgress"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = if (isAllConfirmed) Color(0xFF34C759) else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isAllConfirmed) {
                                            Color(0xFF34C759).copy(alpha = 0.15f)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isAllConfirmed) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF248A3D),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isAllConfirmed) {
                                                    stringResource(R.string.floating_setup_completed_all)
                                                } else {
                                                    stringResource(R.string.floating_setup_progress_summary, confirmedCount, totalCount)
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isAllConfirmed) Color(0xFF248A3D) else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            // Collapsible Information & Alternatives
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { isInfoExpanded = !isInfoExpanded }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.floating_setup_info_toggle),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isInfoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = isInfoExpanded,
                                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.floating_setup_intro),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = stringResource(R.string.floating_setup_limitation),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.floating_setup_alternative_title),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = stringResource(R.string.floating_setup_alternative_desc),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- EMPTY APPS STATE ---
            if (apps.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.floating_setup_no_apps_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.floating_setup_no_apps),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // --- SEARCH BAR ---
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.clear)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                // --- FILTER CHIPS ROW ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == FloatingFilterOption.ALL,
                            onClick = { selectedFilter = FloatingFilterOption.ALL },
                            label = { Text(stringResource(R.string.floating_filter_all, totalCount)) },
                            leadingIcon = if (selectedFilter == FloatingFilterOption.ALL) {
                                { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )

                        FilterChip(
                            selected = selectedFilter == FloatingFilterOption.NEEDS_REVIEW,
                            onClick = { selectedFilter = FloatingFilterOption.NEEDS_REVIEW },
                            label = { Text(stringResource(R.string.floating_filter_needs_review, needsReviewCount)) },
                            leadingIcon = if (selectedFilter == FloatingFilterOption.NEEDS_REVIEW) {
                                { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )

                        FilterChip(
                            selected = selectedFilter == FloatingFilterOption.CONFIRMED,
                            onClick = { selectedFilter = FloatingFilterOption.CONFIRMED },
                            label = { Text(stringResource(R.string.floating_filter_confirmed, confirmedCount)) },
                            leadingIcon = if (selectedFilter == FloatingFilterOption.CONFIRMED) {
                                { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }

                // --- SEARCH / FILTER EMPTY STATE ---
                if (filteredApps.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(R.string.floating_setup_search_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // --- EXPRESSIVE APP LIST ITEMS ---
                itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                    val shape = getExpressiveShape(filteredApps.size, index, ShapeStyle.Large)
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        FloatingSetupAppCard(
                            app = app,
                            shape = shape,
                            requiresManualSetup = requiresManualSetup,
                            onOpenSettings = onOpenSettings,
                            onConfirmedChange = onConfirmedChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingSetupAppCard(
    app: FloatingSetupAppItem,
    shape: Shape,
    requiresManualSetup: Boolean,
    onOpenSettings: (packageName: String) -> Unit,
    onConfirmedChange: (packageName: String, confirmed: Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: App Icon + App details + Open settings button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(
                    packageName = app.packageName,
                    previewBitmap = app.previewIcon,
                    modifier = Modifier.size(46.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    StatusBadge(status = app.status)
                }

                Spacer(Modifier.width(8.dp))

                FilledTonalIconButton(
                    onClick = { onOpenSettings(app.packageName) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.open_notification_settings),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom Confirmation Switch Row (when device requires manual setup)
            if (requiresManualSetup) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onConfirmedChange(app.packageName, !app.isConfirmed) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.floating_disabled_confirmation),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = app.isConfirmed,
                        onCheckedChange = { onConfirmedChange(app.packageName, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: FloatingSetupStatus) {
    val icon = when (status) {
        FloatingSetupStatus.USER_CONFIRMED, FloatingSetupStatus.NOT_REQUIRED -> Icons.Default.CheckCircle
        FloatingSetupStatus.NEEDS_REVIEW -> Icons.Default.Warning
    }
    val labelRes = when (status) {
        FloatingSetupStatus.USER_CONFIRMED -> R.string.floating_status_user_confirmed
        FloatingSetupStatus.NEEDS_REVIEW -> R.string.floating_status_needs_review
        FloatingSetupStatus.NOT_REQUIRED -> R.string.floating_status_not_required
    }
    val containerColor = when (status) {
        FloatingSetupStatus.USER_CONFIRMED -> Color(0xFF34C759).copy(alpha = 0.15f)
        FloatingSetupStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.tertiaryContainer
        FloatingSetupStatus.NOT_REQUIRED -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = when (status) {
        FloatingSetupStatus.USER_CONFIRMED -> Color(0xFF248A3D)
        FloatingSetupStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.onTertiaryContainer
        FloatingSetupStatus.NOT_REQUIRED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    previewBitmap: ImageBitmap? = null
) {
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(initialValue = previewBitmap, packageName) {
        if (previewBitmap != null) {
            value = previewBitmap
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toBitmap().asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val currentBitmap = iconBitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FloatingNotificationSetupScreenPreview() {
    HyperBridgeTheme {
        FloatingNotificationSetupContent(
            apps = listOf(
                FloatingSetupAppItem(
                    packageName = "com.whatsapp",
                    label = "WhatsApp",
                    status = FloatingSetupStatus.NEEDS_REVIEW,
                    isConfirmed = false
                ),
                FloatingSetupAppItem(
                    packageName = "org.telegram.messenger",
                    label = "Telegram",
                    status = FloatingSetupStatus.USER_CONFIRMED,
                    isConfirmed = true
                ),
                FloatingSetupAppItem(
                    packageName = "com.google.android.gm",
                    label = "Gmail",
                    status = FloatingSetupStatus.NOT_REQUIRED,
                    isConfirmed = false
                )
            ),
            requiresManualSetup = true,
            onBack = {},
            onOpenSettings = {},
            onConfirmedChange = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FloatingNotificationSetupScreenEmptyPreview() {
    HyperBridgeTheme {
        FloatingNotificationSetupContent(
            apps = emptyList(),
            requiresManualSetup = true,
            onBack = {},
            onOpenSettings = {},
            onConfirmedChange = { _, _ -> }
        )
    }
}

