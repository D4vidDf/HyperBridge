package com.d4viddf.hyperbridge.ui.screens.theme.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.AppThemeOverride
import com.d4viddf.hyperbridge.ui.components.EmptyState
import com.d4viddf.hyperbridge.ui.screens.theme.AppItem
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.ThemeViewModel
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape

@Composable
fun AppsDetailContent(viewModel: ThemeViewModel) {
    val overrides by viewModel.appOverrides.collectAsState()
    val apps by viewModel.installedApps.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Filter logic for the main list (configured apps)
    val filteredOverrides = remember(overrides, searchQuery) {
        val list = overrides.entries.toList()
        if (searchQuery.isBlank()) {
            list
        } else {
            list.filter { (pkg, _) ->
                val label = apps.find { it.packageName == pkg }?.label ?: pkg
                label.contains(searchQuery, true) || pkg.contains(searchQuery, true)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAppPicker = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .fillMaxSize()
        ) {
            // --- SEARCH BAR ---
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.actions_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } }
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

            // --- LIST OR EMPTY STATE ---
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                if (overrides.isEmpty() && searchQuery.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = stringResource(R.string.apps_empty),
                            description = stringResource(R.string.app_customization_desc),
                            icon = Icons.Outlined.Apps
                        )
                    }
                } else if (filteredOverrides.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.actions_search_empty_fmt, searchQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredOverrides.size) { index ->
                            val (pkg, ov) = filteredOverrides[index]
                            val label = apps.find { it.packageName == pkg }?.label ?: pkg

                            // Use expressive shape logic
                            val shape = getExpressiveShape(filteredOverrides.size, index, ShapeStyle.Large)

                            AppOverrideCard(
                                label = label,
                                packageName = pkg,
                                override = ov,
                                shape = shape,
                                onClick = { viewModel.loadAppForEditing(pkg, label) },
                                onDelete = { viewModel.removeAppOverride(pkg) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- APP SELECTION SHEET (Full Screen) ---
    if (showAppPicker) {
        AppSelectionSheet(
            apps = apps,
            onDismiss = { showAppPicker = false },
            onAppSelected = { app ->
                showAppPicker = false
                viewModel.loadAppForEditing(app.packageName, app.label)
            }
        )
    }
}

// --- COMPONENTS ---

@Composable
fun AppOverrideCard(
    label: String,
    packageName: String,
    override: AppThemeOverride,
    shape: Shape,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            AppIcon(packageName = packageName, modifier = Modifier.size(48.dp))

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)

                // Summary of active overrides
                val details = mutableListOf<String>()
                if (override.highlightColor != null) details.add("Color")
                if (override.iconShapeId != null) details.add("Shape")
                if (override.callConfig != null) details.add("Call")
                if (!override.actions.isNullOrEmpty()) details.add("${override.actions.size} Actions")

                Text(
                    text = if (details.isEmpty()) "Configured" else details.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            ) {
                Icon(Icons.Rounded.Delete, null)
            }

            Spacer(Modifier.width(4.dp))

            Icon(
                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionSheet(
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onAppSelected: (AppItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface, // Standard background
        dragHandle = null // Remove drag handle for a cleaner full-screen-like look
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Make sheet full screen height
                .padding(top = 24.dp) // Status bar padding/top margin
        ) {
            // Sheet Header
            Text(
                stringResource(R.string.apps_sheet_select_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.actions_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } }
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

            Spacer(Modifier.height(16.dp))

            // App List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // [DESIGN] Spacing for card look
            ) {
                val filtered = if (searchQuery.isBlank()) apps else apps.filter {
                    it.label.contains(searchQuery, true) || it.packageName.contains(searchQuery, true)
                }

                items(filtered) { app ->
                    AppSelectionCard(
                        app = app,
                        onClick = { onAppSelected(app) }
                    )
                }
            }
        }
    }
}

// [DESIGN] Card style for the App Selection list to match the rest of the app
@Composable
fun AppSelectionCard(
    app: AppItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp), // Standard rounded card
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Helper to fetch and display App Icon from PackageManager
@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // We use a simple remember block here. In production, use Coil or Glide for better async loading/caching.
    val iconBitmap = remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap().asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap,
            contentDescription = null,
            modifier = modifier.clip(CircleShape), // Circular app icon
            contentScale = ContentScale.Fit
        )
    } else {
        // Fallback Icon
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize()
            )
        }
    }
}