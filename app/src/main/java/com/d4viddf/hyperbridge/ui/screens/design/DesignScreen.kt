package com.d4viddf.hyperbridge.ui.screens.design

import android.graphics.drawable.Drawable
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignScreen(
    onNavigateToWidgets: () -> Unit,
    onLaunchPicker: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }

    // State for Bottom Sheet
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Data Loading
    val savedWidgetIds by preferences.savedWidgetIdsFlow.collectAsState(initial = emptyList())
    var widgetIcons by remember { mutableStateOf<List<Drawable>>(emptyList()) }

    LaunchedEffect(savedWidgetIds) {
        if (savedWidgetIds.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val icons = savedWidgetIds.mapNotNull { id ->
                    val info = WidgetManager.getWidgetInfo(context, id)
                    try {
                        val pkg = info?.provider?.packageName
                        if (pkg != null) context.packageManager.getApplicationIcon(pkg) else null
                    } catch (e: Exception) { null }
                }.distinctBy { it.constantState }.take(6)
                widgetIcons = icons
            }
        } else {
            widgetIcons = emptyList()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true }, // Opens the Sheet
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Design")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. WIDGETS CARD ---
            DesignCategoryCard(
                title = "Widgets",
                icon = Icons.Default.Widgets,
                onClick = onNavigateToWidgets,
                content = {
                    if (savedWidgetIds.isEmpty()) {
                        Text("No widgets configured", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy((-8).dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(widgetIcons) { drawable ->
                                    Image(
                                        bitmap = drawable.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(1.dp)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                    Text("${savedWidgetIds.size} Active", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            )

            // --- 2. APP DESIGNS CARD ---
            DesignCategoryCard(
                title = "App Designs",
                icon = Icons.Default.AppSettingsAlt,
                enabled = false,
                onClick = {},
                content = {
                    Text("Customize notification style per app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) { Text("Coming Soon", modifier = Modifier.padding(4.dp)) }
                }
            )

            // --- 3. CUSTOM LAYOUTS CARD ---
            DesignCategoryCard(
                title = "Custom Layouts",
                icon = Icons.Default.Brush,
                enabled = false,
                onClick = {},
                content = {
                    Text("Create custom XML layouts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) { Text("Coming Soon", modifier = Modifier.padding(4.dp)) }
                }
            )

            // Spacer for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // --- BOTTOM SHEET ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add to Island",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Option 1: System Widget
                Button(
                    onClick = {
                        showBottomSheet = false
                        onLaunchPicker() // Calls back to HomeScreen to show Picker Overlay
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.Widgets, null, modifier = Modifier.padding(end = 8.dp))
                    Text("System Widget", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Custom Layout (Placeholder)
                FilledTonalButton(
                    onClick = {
                        Toast.makeText(context, "Custom Layouts Coming Soon", Toast.LENGTH_SHORT).show()
                        showBottomSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Brush, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Custom Layout", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun DesignCategoryCard(
    title: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = if (enabled) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}