package com.d4viddf.hyperbridge.ui.screens.design

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.WidgetSize
import com.d4viddf.hyperbridge.ui.components.EmptyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

// --- DATA MODEL ---
data class SavedWidgetGroup(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?,
    val widgetIds: List<Int>,
    val isExpanded: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SavedAppWidgetsScreen(
    onBack: () -> Unit,
    onEditWidget: (Int) -> Unit,
    onAddMore: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context.applicationContext) }

    val favorites by preferences.favoriteWidgetAppsFlow.collectAsState(initial = emptySet())
    val savedIds by preferences.savedWidgetIdsFlow.collectAsState(initial = null) // Reactive listening for newly added widgets!

    var allGroups by remember { mutableStateOf<List<SavedWidgetGroup>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var tabIndex by remember { mutableIntStateOf(1) } // 0 = Favorites, 1 = All

    var showPermissionReminder by remember { mutableStateOf(false) }
    val refreshTrigger = remember { mutableIntStateOf(0) }

    val pullState = rememberPullToRefreshState()
    val isRefreshing = isLoading && allGroups.isNotEmpty()

    // App Update Permission Reminder Logic
    LaunchedEffect(Unit) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersion = PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
        val sharedPrefs = context.getSharedPreferences("hyperbridge_internal", Context.MODE_PRIVATE)
        val lastVersion = sharedPrefs.getInt("last_seen_version", currentVersion)

        if (lastVersion in 1 until currentVersion) {
            showPermissionReminder = true
        }
        sharedPrefs.edit { putInt("last_seen_version", currentVersion) }
    }

    // Fetch Saved Widgets (Reacts to new widgets being added dynamically)
    LaunchedEffect(savedIds, refreshTrigger.value) {
        if (savedIds == null) return@LaunchedEffect

        isLoading = true
        withContext(Dispatchers.IO) {
            val groupsMap = mutableMapOf<String, MutableList<Int>>()
            savedIds!!.forEach { id ->
                val info = WidgetManager.getWidgetInfo(context, id)
                val pkg = info?.provider?.packageName ?: return@forEach
                groupsMap.getOrPut(pkg) { mutableListOf() }.add(id)
            }

            val uiGroups = groupsMap.map { (pkg, ids) ->
                val appName = try {
                    val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) { pkg }

                val icon = try {
                    context.packageManager.getApplicationIcon(pkg)
                } catch (e: Exception) { null }

                SavedWidgetGroup(pkg, appName, icon, ids)
            }.sortedBy { it.appName }

            if (uiGroups.isEmpty()) delay(300)
            allGroups = uiGroups
        }
        isLoading = false
    }

    // Reactive Filtering (Favorites + Search)
    val displayedGroups = remember(allGroups, searchQuery, tabIndex, favorites) {
        var filtered = allGroups

        // Filter by Favorites
        if (tabIndex == 0) {
            filtered = filtered.filter { favorites.contains(it.packageName) }
        }

        // Filter by Search Query
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.appName.contains(searchQuery, ignoreCase = true)
            }.map {
                it.copy(isExpanded = true)
            }
        }

        filtered
    }

    if (showPermissionReminder) {
        AlertDialog(
            onDismissRequest = { showPermissionReminder = false },
            title = { Text(stringResource(R.string.permission_reminder_title)) },
            text = { Text(stringResource(R.string.permission_reminder_desc)) },
            confirmButton = {
                Button(onClick = { showPermissionReminder = false }) {
                    Text(stringResource(R.string.got_it))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.saved_widgets_title), fontWeight = FontWeight.Bold)

                        // Beta Badge
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "BETA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (allGroups.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = {
                                val intent = Intent(context, com.d4viddf.hyperbridge.service.WidgetOverlayService::class.java).apply {
                                    action = "ACTION_KILL_ALL_WIDGETS"
                                }
                                context.startService(intent)
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Outlined.Block, contentDescription = stringResource(R.string.kill_all_widgets))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            // A full-width Box so the Toolbar can be centered and the FAB right-aligned
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Centered Filter Toolbar
                Box(modifier = Modifier.align(Alignment.Center)) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        content = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                TextButton(
                                    onClick = { tabIndex = 0 },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = if (tabIndex == 0) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                        contentColor = if (tabIndex == 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(stringResource(R.string.favorites), fontWeight = FontWeight.SemiBold)
                                }

                                TextButton(
                                    onClick = { tabIndex = 1 },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = if (tabIndex == 1) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                        contentColor = if (tabIndex == 1) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(stringResource(R.string.all), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    )
                }

                // Add FAB aligned to the far right
                FloatingActionButton(
                    onClick = onAddMore,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Widget")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            // SEARCH
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_apps_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.search)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, stringResource(R.string.close)) } }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }

            // LIST
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { refreshTrigger.intValue++ },
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
                    if (displayedGroups.isEmpty() && isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    } else if (displayedGroups.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                title = stringResource(R.string.no_saved_widgets),
                                description = "",
                                icon = Icons.Outlined.Widgets
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayedGroups, key = { it.packageName }) { group ->
                                var expanded by remember(group.packageName, searchQuery) { mutableStateOf(group.isExpanded) }

                                Box(modifier = Modifier.animateItem()) {
                                    SavedGroupItem(
                                        group = group,
                                        isExpanded = expanded,
                                        onToggle = { expanded = !expanded },
                                        onEdit = onEditWidget,
                                        onDelete = { widgetId ->
                                            // 1. Instantly kill the active Island notification
                                            val killIntent = Intent(context, com.d4viddf.hyperbridge.service.WidgetOverlayService::class.java).apply {
                                                action = "ACTION_KILL_WIDGET"
                                                putExtra("WIDGET_ID", widgetId)
                                            }
                                            context.startService(killIntent)

                                            // 2. Remove from database and refresh UI
                                            scope.launch {
                                                preferences.removeWidgetId(widgetId)
                                                refreshTrigger.intValue++
                                            }
                                        }
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

@Composable
fun SavedGroupItem(
    group: SavedWidgetGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    val numWidget = pluralStringResource(R.plurals.widget_count_fmt, group.widgetIds.size, group.widgetIds.size)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (group.appIcon != null) {
                    Image(
                        bitmap = group.appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(Icons.Outlined.Widgets, null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = numWidget,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Top
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    shrinkTowards = Alignment.Top
                ) + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    group.widgetIds.forEachIndexed { index, widgetId ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                        SavedWidgetChildItem(
                            widgetId = widgetId,
                            onEdit = { onEdit(widgetId) },
                            onDelete = { onDelete(widgetId) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SavedWidgetChildItem(
    widgetId: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }

    val config by preferences.getWidgetConfigFlow(widgetId).collectAsState(initial = null)

    val viewHeightDp = when(config?.size) {
        WidgetSize.SMALL -> 100
        WidgetSize.MEDIUM -> 180
        WidgetSize.LARGE -> 280
        WidgetSize.XLARGE -> 380
        else -> 180
    }

    val containerHeight = viewHeightDp.dp + 40.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.widget_id_fmt, widgetId),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val wrapper = FrameLayout(ctx)
                    val hostView = WidgetManager.createPreview(ctx, widgetId)
                    if (hostView != null) {
                        val info = WidgetManager.getWidgetInfo(ctx, widgetId)
                        hostView.setAppWidget(widgetId, info)
                        wrapper.addView(hostView)

                        val density = ctx.resources.displayMetrics.density
                        val w = (300 * density).toInt()
                        val h = (viewHeightDp * density).toInt()

                        hostView.measure(
                            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST)
                        )
                        hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)
                    }
                    wrapper
                },
                modifier = Modifier.padding(16.dp).fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val intent = Intent(context, com.d4viddf.hyperbridge.service.WidgetOverlayService::class.java).apply {
                    action = "ACTION_TEST_WIDGET"
                    putExtra("WIDGET_ID", widgetId)
                }
                context.startService(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors()
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.show_on_island))
        }
    }
}