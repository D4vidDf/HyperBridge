package com.d4viddf.hyperbridge.ui.screens.design

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WidgetAppGroup(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>,
    val isExpanded: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WidgetPickerScreen(
    onBack: () -> Unit,
    onWidgetSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context.applicationContext) }

    val favorites by preferences.favoriteWidgetAppsFlow.collectAsState(initial = emptySet())

    var allGroups by remember { mutableStateOf<List<WidgetAppGroup>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var tabIndex by remember { mutableIntStateOf(0) } // 0 = Recommended, 1 = All
    var pendingWidgetId by remember { mutableStateOf(-1) }

    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != -1) {
            onWidgetSelected(pendingWidgetId)
        } else if (pendingWidgetId != -1) {
            WidgetManager.deleteId(context, pendingWidgetId)
            pendingWidgetId = -1
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val manager = AppWidgetManager.getInstance(context)
            val providers = manager.installedProviders
            val grouped = providers.groupBy { it.provider.packageName }

            val uiGroups = grouped.mapNotNull { (pkg, list) ->
                try {
                    val appName = context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
                    val icon = context.packageManager.getApplicationIcon(pkg)
                    WidgetAppGroup(pkg, appName, icon, list)
                } catch (e: Exception) { null }
            }.sortedBy { it.appName }

            allGroups = uiGroups
        }
    }

    val displayedGroups = allGroups.mapNotNull { group ->
        val filteredWidgets = if (tabIndex == 0) {
            group.widgets.filter { w ->
                // Filter specifically for 1x4 and 2x4 layouts (Compatible with Island sizes)
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    w.targetCellWidth == 4 && (w.targetCellHeight == 1 || w.targetCellHeight == 2)
                } else {
                    w.minWidth >= 200 && w.minHeight <= 150 // Rough estimation for older Android versions
                }
            }
        } else group.widgets

        if (filteredWidgets.isNotEmpty()) group.copy(widgets = filteredWidgets) else null
    }.filter {
        if (searchQuery.isNotEmpty()) it.appName.contains(searchQuery, ignoreCase = true) else true
    }.map {
        if (searchQuery.isNotEmpty()) it.copy(isExpanded = true) else it
    }.sortedWith(
        compareByDescending<WidgetAppGroup> { favorites.contains(it.packageName) }
            .thenBy { it.appName }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.widget_picker_title), fontWeight = FontWeight.Bold)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            HorizontalFloatingToolbar(
                expanded = true,
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ToolbarOption(
                            selected = tabIndex == 0,
                            icon = Icons.Outlined.Star,
                            text = stringResource(R.string.recommended),
                            onClick = { tabIndex = 0 }
                        )
                        ToolbarOption(
                            selected = tabIndex == 1,
                            icon = Icons.Outlined.Widgets,
                            text = stringResource(R.string.all),
                            onClick = { tabIndex = 1 }
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
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
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp), // Extra padding for the floating bar
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedGroups, key = { it.packageName }) { group ->
                    var expanded by remember(group.packageName, searchQuery) { mutableStateOf(group.isExpanded) }
                    val isFavorite = favorites.contains(group.packageName)

                    Box(modifier = Modifier.animateItem()) {
                        AppGroupItem(
                            group = group,
                            isExpanded = expanded,
                            isFavorite = isFavorite,
                            onToggle = { expanded = !expanded },
                            onFavoriteToggle = {
                                scope.launch { preferences.toggleFavoriteWidgetApp(group.packageName, !isFavorite) }
                            },
                            onSelectWidget = { provider ->
                                val widgetId = WidgetManager.allocateId(context)
                                val allowed = WidgetManager.bindWidget(context, widgetId, provider.provider)

                                if (allowed) {
                                    onWidgetSelected(widgetId)
                                } else {
                                    pendingWidgetId = widgetId
                                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                                    }
                                    bindLauncher.launch(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppGroupItem(
    group: WidgetAppGroup,
    isExpanded: Boolean,
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit
) {
    val numWidget = pluralStringResource(R.plurals.widget_count_fmt, group.widgets.size, group.widgets.size)
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

                // [NEW] Star Button
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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

                    group.widgets.forEachIndexed { index, widget ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                        WidgetChildItem(widget, onSelectWidget)
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WidgetChildItem(
    info: AppWidgetProviderInfo,
    onClick: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    val label = info.loadLabel(context.packageManager)

    // Convert dp dimensions to Android Grid Proportions (e.g., 4x1, 2x2)
    val cols = if (android.os.Build.VERSION.SDK_INT >= 31) {
        info.targetCellWidth
    } else {
        maxOf(1, Math.ceil((info.minWidth + 30) / 70.0).toInt())
    }

    val rows = if (android.os.Build.VERSION.SDK_INT >= 31) {
        info.targetCellHeight
    } else {
        maxOf(1, Math.ceil((info.minHeight + 30) / 70.0).toInt())
    }

    val dims = "$cols × $rows"

    var preview by remember { mutableStateOf<Drawable?>(null) }
    LaunchedEffect(info) {
        withContext(Dispatchers.IO) {
            preview = try {
                info.loadPreviewImage(context, 0) ?: info.loadIcon(context, 0)
            } catch (e: Exception) { null }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(info) }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Box(
                modifier = Modifier.padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview!!.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        alignment = Alignment.Center
                    )
                } else {
                    Icon(
                        Icons.Outlined.Widgets,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = dims,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}