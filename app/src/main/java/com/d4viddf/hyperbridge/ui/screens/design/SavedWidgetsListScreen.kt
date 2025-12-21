package com.d4viddf.hyperbridge.ui.screens.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.AppGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedWidgetsListScreen(
    onNavigateToDetail: (String, String) -> Unit,
    onLaunchPicker: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val savedWidgetIds by preferences.savedWidgetIdsFlow.collectAsState(initial = emptyList())
    var appGroups by remember { mutableStateOf<List<AppGroup>>(emptyList()) }

    LaunchedEffect(savedWidgetIds) {
        if (savedWidgetIds.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val rawList = savedWidgetIds.mapNotNull { id ->
                    val info = WidgetManager.getWidgetInfo(context, id)
                    if (info != null) id to info else null
                }
                val grouped = rawList.groupBy { it.second.provider.packageName }

                appGroups = grouped.map { (pkg, list) ->
                    val firstInfo = list.first().second
                    val appName = firstInfo.loadLabel(context.packageManager)
                    val icon = try { context.packageManager.getApplicationIcon(pkg) } catch(e:Exception) { null }

                    AppGroup(pkg, appName, icon, list.map { it.first })
                }
            }
        } else {
            appGroups = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configured Widgets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onLaunchPicker,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (appGroups.isEmpty() && savedWidgetIds.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No saved widgets yet.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(appGroups) { group ->
                    AppWidgetsSummaryCard(group, onClick = { onNavigateToDetail(group.packageName, group.appName) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// Reusing the summary card logic
@Composable
fun AppWidgetsSummaryCard(group: AppGroup, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (group.appIcon != null) {
                    Image(
                        bitmap = group.appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(Icons.Default.Widgets, null, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = group.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.height(24.dp)
                ) {
                    Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text("${group.widgetIds.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val displayIds = group.widgetIds.take(5)
                val remaining = group.widgetIds.size - 5

                items(displayIds) { widgetId -> SavedWidgetMiniPreview(widgetId) }

                if (remaining > 0) {
                    item {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+$remaining", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedWidgetMiniPreview(widgetId: Int) {
    val context = LocalContext.current
    // Use remember to avoid reloading on recomposition
    val previewDrawable = remember(widgetId) {
        val info = WidgetManager.getWidgetInfo(context, widgetId)
        // Try to get specific preview image, fallback to icon
        try { info?.loadPreviewImage(context, 0) ?: info?.loadIcon(context, 0) } catch (e: Exception) { null }
    }

    Box(
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        if (previewDrawable != null) {
            Image(bitmap = previewDrawable.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.padding(6.dp).fillMaxSize())
        } else {
            Icon(Icons.Default.Widgets, null, tint = Color.Gray)
        }
    }
}