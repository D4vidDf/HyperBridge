package com.d4viddf.hyperbridge.ui.screens.design

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.service.NotificationReaderService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignScreen(
    onOpenWidgetConfig: (Int) -> Unit,
    onLaunchPicker: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context.applicationContext) }

    // Collect the List<Int> of IDs
    val savedWidgetIds by preferences.savedWidgetIdsFlow.collectAsState(initial = emptyList())

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Design")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Your Designs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (savedWidgetIds.isNotEmpty()) {
                    // Iterate through all saved IDs
                    items(savedWidgetIds) { widgetId ->
                        SavedWidgetRow(
                            widgetId = widgetId,
                            onLaunch = {
                                val intent = Intent(context, NotificationReaderService::class.java).apply {
                                    action = "ACTION_TEST_WIDGET"
                                    putExtra("WIDGET_ID", widgetId)
                                }
                                context.startService(intent)
                            },
                            onEdit = { onOpenWidgetConfig(widgetId) },
                            onDelete = {
                                scope.launch {
                                    preferences.removeWidgetId(widgetId)
                                }
                            }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active widgets",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add to Island",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        showBottomSheet = false
                        onLaunchPicker()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Widgets, null, modifier = Modifier.padding(end = 8.dp))
                    Text("System Widget")
                }
                Button(
                    onClick = {
                        Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show()
                        showBottomSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = false
                ) {
                    Text("Custom Layout")
                }
            }
        }
    }
}

@Composable
fun SavedWidgetRow(
    widgetId: Int,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Widget #$widgetId",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    FilledTonalButton(onClick = onLaunch) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.padding(4.dp))
                        Text("Show")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- MINI PREVIEW ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        val wrapper = FrameLayout(ctx)
                        val hostView = WidgetManager.createPreview(ctx, widgetId)
                        if (hostView != null) {
                            // Critical for previewing correctly
                            hostView.setAppWidget(widgetId, WidgetManager.getWidgetInfo(ctx, widgetId))

                            // Measure for list preview size
                            val widthSpec = View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.AT_MOST)
                            val heightSpec = View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST)
                            hostView.measure(widthSpec, heightSpec)
                            hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)

                            wrapper.addView(hostView)
                        }
                        wrapper
                    },
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
        }
    }
}