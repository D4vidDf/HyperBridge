package com.d4viddf.hyperbridge.ui.screens.design

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.data.widget.WidgetManager
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

    var pendingWidgetId by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        viewModel.loadWidgets(context)
    }

    // Permission Handler
    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (pendingWidgetId != -1) {
                onWidgetSelected(pendingWidgetId)
            }
        } else {
            if (pendingWidgetId != -1) {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
                pendingWidgetId = -1
            }
        }
    }

    // Function to handle click logic
    fun handleWidgetClick(provider: AppWidgetProviderInfo) {
        val newId = WidgetManager.allocateId(context)
        if (newId == -1) {
            Toast.makeText(context, "Error allocating ID", Toast.LENGTH_SHORT).show()
            return
        }
        pendingWidgetId = newId

        val isBound = WidgetManager.bindWidget(context, newId, provider.provider)
        if (isBound) {
            onWidgetSelected(newId)
        } else {
            try {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                }
                bindWidgetLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot launch bind intent", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Widget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(groups) { group ->
                    AppWidgetGroupItem(
                        group = group,
                        onWidgetClick = { handleWidgetClick(it) }
                    )
                    Divider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppWidgetGroupItem(
    group: WidgetAppGroup,
    onWidgetClick: (AppWidgetProviderInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // --- APP HEADER ---
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
            Text(
                text = group.appName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${group.widgets.size})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- WIDGETS SCROLL ROW ---
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

@Composable
fun WidgetPreviewItem(
    info: AppWidgetProviderInfo,
    onClick: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current

    // Async load the preview image to avoid stuttering
    val previewDrawable by produceState<Drawable?>(initialValue = null, key1 = info) {
        value = withContext(Dispatchers.IO) {
            try {
                // Try fetching the specific preview image
                info.loadPreviewImage(context, 0) ?: info.loadIcon(context, 0)
            } catch (e: Exception) {
                null
            }
        }
    }

    val label = info.loadLabel(context.packageManager)
    val dims = "${info.minWidth} x ${info.minHeight} dp" // Or rough grid size

    Column(
        modifier = Modifier
            .width(160.dp) // Fixed width for consistent cards
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(info) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview Box
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
                    modifier = Modifier.padding(8.dp).fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Loading or Fallback
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = dims, // Optional dimension text
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}