package com.d4viddf.hyperbridge.ui.screens.design

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.util.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerScreen(
    viewModel: WidgetPickerViewModel = viewModel(),
    onWidgetSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val groups by viewModel.widgetGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var currentWidgetId by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        viewModel.loadWidgets(context)
    }

    // --- CONFIGURATION HANDLER ---
    val configureWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Success: User configured the widget
            onWidgetSelected(currentWidgetId)
        } else {
            // Cancelled: User pressed back or cancelled setup
            // We must delete the allocated ID to prevent "ghost" widgets
            if (currentWidgetId != -1) {
                WidgetManager.deleteId(context, currentWidgetId)
            }
            Toast.makeText(context, "Setup cancelled", Toast.LENGTH_SHORT).show()
            currentWidgetId = -1
        }
    }

    // --- [FIXED] SAFE LAUNCH HELPER ---
    fun launchConfigOrFinish(id: Int, provider: AppWidgetProviderInfo) {
        val configComponent = provider.configure
        if (configComponent != null) {
            try {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = configComponent
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    // REMOVED: addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) -> This broke the result callback!
                }
                configureWidgetLauncher.launch(intent)
            } catch (e: SecurityException) {
                // If app restricts access (not exported), assume defaults and just add it
                onWidgetSelected(id)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Configuration screen not found", Toast.LENGTH_SHORT).show()
                onWidgetSelected(id)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot configure widget", Toast.LENGTH_SHORT).show()
                onWidgetSelected(id)
            }
        } else {
            // No configuration needed
            onWidgetSelected(id)
        }
    }

    // --- BIND PERMISSION HANDLER ---
    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val info = WidgetManager.getWidgetInfo(context, currentWidgetId)
            if (info != null) {
                launchConfigOrFinish(currentWidgetId, info)
            }
        } else {
            WidgetManager.deleteId(context, currentWidgetId)
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            currentWidgetId = -1
        }
    }

    // --- MAIN CLICK HANDLER ---
    fun handleWidgetClick(provider: AppWidgetProviderInfo) {
        val newId = WidgetManager.allocateId(context)
        if (newId == -1) {
            Toast.makeText(context, "Error allocating ID", Toast.LENGTH_SHORT).show()
            return
        }

        currentWidgetId = newId

        // Attempt Bind
        val isBound = WidgetManager.bindWidget(context, newId, provider.provider)

        if (isBound) {
            launchConfigOrFinish(newId, provider)
        } else {
            // Request Permission
            try {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                }
                bindWidgetLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Error requesting permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Widget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(groups) { group ->
                    AppWidgetGroupItem(group = group, onWidgetClick = { handleWidgetClick(it) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
@Composable
fun AppWidgetGroupItem(
    group: WidgetAppGroup, // Ensure this model matches your ViewModel definition
    onWidgetClick: (AppWidgetProviderInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // 1. App Header (Icon + Name)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (group.appIcon != null) {
                Image(
                    bitmap = group.appIcon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = group.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Optional: Show package name if needed for debugging
                // Text(text = group.packageName, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Widget Count Badge
            Text(
                text = "(${group.widgets.size})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. Horizontal Scroll for Widgets
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(group.widgets) { widget ->
                WidgetPreviewItem(widget, onWidgetClick)
            }
        }
    }
}

// --- PREVIEW ITEM (Individual Card) ---
@Composable
fun WidgetPreviewItem(
    info: AppWidgetProviderInfo,
    onClick: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current

    // Async load the preview image to prevent UI stuttering
    val previewDrawable by produceState<Drawable?>(initialValue = null, key1 = info) {
        value = withContext(Dispatchers.IO) {
            try {
                // Try loading specific preview -> fallback to app icon
                info.loadPreviewImage(context, 0) ?: info.loadIcon(context, 0)
            } catch (e: Exception) {
                null
            }
        }
    }

    val label = info.loadLabel(context.packageManager) ?: "Widget"
    val dims = "${info.minWidth}x${info.minHeight} dp"

    Column(
        modifier = Modifier
            .width(160.dp) // Fixed width for uniform cards
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(info) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview Image Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (previewDrawable != null) {
                Image(
                    bitmap = previewDrawable!!.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Loading / Error Placeholder
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Widget Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Dimensions Subtext
        Text(
            text = dims,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}