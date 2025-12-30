package com.d4viddf.hyperbridge.ui.screens.design

import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignScreen(
    onNavigateToWidgets: () -> Unit,
    onNavigateToThemes: () -> Unit,
    onLaunchPicker: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }

    val themeRepo = remember { ThemeRepository(context.applicationContext) }
    val activeTheme by themeRepo.activeTheme.collectAsState()
    val activeThemeId by preferences.activeThemeIdFlow.collectAsState(initial = null)

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val savedWidgetIds by preferences.savedWidgetIdsFlow.collectAsState(initial = emptyList())
    var widgetIcons by remember { mutableStateOf<List<Drawable>>(emptyList()) }

    LaunchedEffect(activeThemeId) {
        if (activeThemeId != null) {
            themeRepo.activateTheme(activeThemeId!!)
        }
    }

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
                onClick = { showBottomSheet = true },
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
                showBetaBadge = true,
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

            // --- 2. NOTIFICATION THEMES CARD ---
            DesignCategoryCard(
                title = "Themes",
                icon = Icons.Rounded.Palette,
                enabled = true,
                showBetaBadge = true, // [NEW] Added Beta Badge here
                onClick = onNavigateToThemes,
                content = {
                    val theme = activeTheme
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (theme != null) {
                            // Custom Theme Active
                            val color = try {
                                Color((theme.global.highlightColor ?: "#000000").toColorInt())
                            } catch (e: Exception) { MaterialTheme.colorScheme.primary }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(theme.meta.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("by ${theme.meta.author}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            // System Default
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.PhoneAndroid, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("System Default", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Standard Look", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
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

            Spacer(modifier = Modifier.height(80.dp))
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
                    .padding(bottom = 48.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add to Island",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        showBottomSheet = false
                        onLaunchPicker()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.Widgets, null, modifier = Modifier.padding(end = 8.dp))
                    Text("System Widget (Beta)", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        showBottomSheet = false
                        onNavigateToThemes()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Palette, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Get Themes", style = MaterialTheme.typography.titleMedium)
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
    showBetaBadge: Boolean = false,
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

                if (showBetaBadge && enabled) {
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
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}