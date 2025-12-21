package com.d4viddf.hyperbridge.ui.screens.design

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.WidgetSize
import com.d4viddf.hyperbridge.service.NotificationReaderService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAppWidgetsScreen(
    packageName: String,
    appName: String,
    onBack: () -> Unit,
    onEditWidget: (Int) -> Unit,
    onAddMore: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context.applicationContext) }

    // Fetch only widgets for this package
    val allWidgetIds by preferences.savedWidgetIdsFlow.collectAsState(initial = emptyList())

    // Filter locally
    val filteredIds = remember(allWidgetIds) {
        allWidgetIds.filter { id ->
            val info = WidgetManager.getWidgetInfo(context, id)
            info?.provider?.packageName == packageName
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMore,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New Widget") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredIds) { widgetId ->
                FullSavedWidgetRow(
                    widgetId = widgetId,
                    onLaunch = {
                        val intent = Intent(context, NotificationReaderService::class.java).apply {
                            action = "ACTION_TEST_WIDGET"
                            putExtra("WIDGET_ID", widgetId)
                        }
                        context.startService(intent)
                    },
                    onEdit = { onEditWidget(widgetId) },
                    onDelete = {
                        scope.launch { preferences.removeWidgetId(widgetId) }
                    }
                )
            }
            // Spacer for FAB
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun FullSavedWidgetRow(
    widgetId: Int,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }

    // Load PER-WIDGET configuration
    val config by preferences.getWidgetConfigFlow(widgetId).collectAsState(initial = null)

    // Determine height based on saved size
    val cardHeight = when(config?.widgetSize) {
        WidgetSize.SMALL -> 140.dp
        WidgetSize.MEDIUM -> 220.dp
        WidgetSize.LARGE -> 320.dp
        WidgetSize.XLARGE -> 420.dp
        else -> 220.dp
    }

    // Determine view height for AndroidView measure spec
    val viewHeightDp = when(config?.widgetSize) {
        WidgetSize.SMALL -> 100
        WidgetSize.MEDIUM -> 180
        WidgetSize.LARGE -> 280
        WidgetSize.XLARGE -> 380
        else -> 180
    }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Widget #$widgetId",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onLaunch,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Show", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Actual Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
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

                            // Measure with SAVED SIZE
                            val density = ctx.resources.displayMetrics.density
                            val w = (300 * density).toInt()
                            val h = (viewHeightDp * density).toInt() // Use saved height

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
        }
    }
}