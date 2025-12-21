package com.d4viddf.hyperbridge.ui.screens.design

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.WidgetRenderMode
import com.d4viddf.hyperbridge.models.WidgetSize
import com.d4viddf.hyperbridge.service.NotificationReaderService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    widgetId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPreferences = remember { AppPreferences(context.applicationContext) }

    // --- CONFIGURATION STATES ---
    var isShowShade by remember { mutableStateOf(false) }
    var timeoutSeconds by remember { mutableFloatStateOf(5f) }
    var selectedSize by remember { mutableStateOf(WidgetSize.MEDIUM) }
    var renderMode by remember { mutableStateOf(WidgetRenderMode.INTERACTIVE) }

    // UI Helpers
    var sizeExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Check if this widget is configurable
    val configureIntent = remember(widgetId) {
        val comp = WidgetManager.getConfigurationActivity(context, widgetId)
        if (comp != null) {
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = comp
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
        } else null
    }

    // LOAD SAVED SETTINGS PER ID
    LaunchedEffect(widgetId) {
        val config = appPreferences.getWidgetConfigFlow(widgetId).first()
        isShowShade = config.isShowShade ?: false
        timeoutSeconds = (config.timeout ?: 5000L).toFloat() / 1000f
        selectedSize = config.widgetSize ?: WidgetSize.MEDIUM
        renderMode = config.renderMode ?: WidgetRenderMode.INTERACTIVE
    }

    // RE-CONFIGURE LAUNCHER
    val reconfigureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Widget updated", Toast.LENGTH_SHORT).show()
        }
    }

    fun safeLaunchEdit() {
        if (configureIntent != null) {
            try {
                configureIntent.flags = 0
                reconfigureLauncher.launch(configureIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot edit this widget settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Widget", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (configureIntent != null) {
                        IconButton(onClick = { safeLaunchEdit() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Edit Widget Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState) // [FIX] Made Scrollable
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. PREVIEW SECTION ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(220.dp), // Fixed height container for preview
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val wrapper = FrameLayout(ctx)
                            // Initial creation
                            val hostView = WidgetManager.createPreview(ctx, widgetId)
                            if (hostView != null) {
                                val info = WidgetManager.getWidgetInfo(ctx, widgetId)
                                hostView.setAppWidget(widgetId, info)
                                wrapper.addView(hostView)
                            }
                            wrapper
                        },
                        // [FIX] Update logic is now here to react to size/mode changes
                        update = { wrapper ->
                            val hostView = wrapper.getChildAt(0)
                            if (hostView != null) {
                                val density = context.resources.displayMetrics.density
                                val wDp = 350

                                // Determine height based on selected size
                                val hDp = when (selectedSize) {
                                    WidgetSize.SMALL -> 100
                                    WidgetSize.MEDIUM -> 180
                                    WidgetSize.LARGE -> 280
                                    WidgetSize.XLARGE -> 380
                                    else -> 180
                                }

                                val widthPx = (wDp * density).toInt()
                                val heightPx = (hDp * density).toInt()

                                // Force Measure & Layout
                                val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
                                val heightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.AT_MOST)

                                hostView.measure(widthSpec, heightSpec)
                                hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)

                                // Legal Force Update via Options bundle
                                val options = Bundle().apply {
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, wDp)
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, hDp)
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, wDp)
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, hDp)
                                }
                                // We cast safely to AppWidgetHostView
                                (hostView as? android.appwidget.AppWidgetHostView)?.updateAppWidgetOptions(options)
                            }
                        },
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. RENDER SETTINGS ---
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Render Mode (Segmented Button)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WidgetRenderMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = renderMode == mode,
                                onClick = { renderMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = WidgetRenderMode.entries.size),
                                icon = {
                                    if (renderMode == mode) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                }
                            ) {
                                Text(mode.label)
                            }
                        }
                    }

                    Text(
                        text = renderMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. DIMENSIONS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Container Size", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))

                    ExposedDropdownMenuBox(
                        expanded = sizeExpanded,
                        onExpandedChange = { sizeExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedSize.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = sizeExpanded,
                            onDismissRequest = { sizeExpanded = false }
                        ) {
                            WidgetSize.entries.forEach { sizeOption ->
                                DropdownMenuItem(
                                    text = { Text(sizeOption.label) },
                                    onClick = {
                                        selectedSize = sizeOption
                                        sizeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. BEHAVIOR ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column {
                    // Shade Toggle
                    ListItem(
                        headlineContent = { Text("Keep in Notification Shade") },
                        supportingContent = { Text("Prevents dismissal when clicking outside") },
                        leadingContent = { Icon(Icons.Outlined.History, null) },
                        trailingContent = {
                            Switch(
                                checked = isShowShade,
                                onCheckedChange = { isShowShade = it }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    // Timeout Slider
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text("Timeout: ${timeoutSeconds.roundToInt()}s", style = MaterialTheme.typography.bodyLarge)
                        }
                        Slider(
                            value = timeoutSeconds,
                            onValueChange = { timeoutSeconds = it },
                            valueRange = 2f..30f,
                            steps = 28,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SAVE BUTTON ---
            Button(
                onClick = {
                    scope.launch {
                        appPreferences.saveWidgetConfig(
                            id = widgetId,
                            isShowShade = isShowShade,
                            timeout = (timeoutSeconds * 1000).toLong(),
                            size = selectedSize,
                            renderMode = renderMode
                        )
                        val intent = Intent(context, NotificationReaderService::class.java).apply {
                            action = "ACTION_TEST_WIDGET"
                            putExtra("WIDGET_ID", widgetId)
                        }
                        context.startService(intent)
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.padding(4.dp))
                Text("Save & Show on Island", style = MaterialTheme.typography.titleMedium)
            }

            // Bottom padding for scrolling
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}