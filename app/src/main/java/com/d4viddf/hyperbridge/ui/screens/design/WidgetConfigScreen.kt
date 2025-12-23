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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.WidgetConfig
import com.d4viddf.hyperbridge.models.WidgetRenderMode
import com.d4viddf.hyperbridge.models.WidgetSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WidgetConfigScreen(
    widgetId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPreferences = remember { AppPreferences(context.applicationContext) }
    val configuration = LocalConfiguration.current

    val screenWidthDp = configuration.screenWidthDp
    val previewWidthDp = screenWidthDp - 32

    // --- CONFIGURATION STATES ---
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Appearance, 1 = Behavior

    // Appearance
    var selectedSize by remember { mutableStateOf(WidgetSize.MEDIUM) }
    var renderMode by remember { mutableStateOf(WidgetRenderMode.INTERACTIVE) }

    // Behavior
    var isShowShade by remember { mutableStateOf(true) }
    var isTimeoutEnabled by remember { mutableStateOf(false) }
    var timeoutSeconds by remember { mutableFloatStateOf(5f) }

    // Auto Update
    var autoUpdateEnabled by remember { mutableStateOf(false) }
    var updateInterval by remember { mutableFloatStateOf(15f) }

    // Helpers
    var sizeExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val configureIntent = remember(widgetId) {
        val comp = WidgetManager.getConfigurationActivity(context, widgetId)
        if (comp != null) {
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = comp
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
        } else null
    }

    LaunchedEffect(widgetId) {
        val config = appPreferences.getWidgetConfigFlow(widgetId).first()
        selectedSize = config.size
        renderMode = config.renderMode
        isShowShade = config.isShowShade

        // Timeout Logic
        config.timeout?.let {
            if (it <= 0) {
                isTimeoutEnabled = false
                timeoutSeconds = 0f
            } else {
                isTimeoutEnabled = true
                timeoutSeconds = config.timeout.toFloat()
            }
        }

        autoUpdateEnabled = config.autoUpdate
        updateInterval = config.updateIntervalMinutes.toFloat()
    }

    val reconfigureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, context.getString(R.string.widget_updated_toast), Toast.LENGTH_SHORT).show()
        }
    }

    fun safeLaunchEdit() {
        if (configureIntent != null) {
            try {
                configureIntent.flags = 0
                reconfigureLauncher.launch(configureIntent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.widget_settings_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_config_title), fontWeight = FontWeight.Bold) },
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
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            // 1. SCROLLABLE PREVIEW LAYER (Background)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 400.dp), // Space for bottom panel
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Settings Link
                if (configureIntent != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { safeLaunchEdit() }) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.widget_settings_button))
                        }
                    }
                }

                // Preview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .animateContentSize(),
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
                                }
                                wrapper
                            },
                            update = { wrapper ->
                                val hostView = wrapper.getChildAt(0)
                                if (hostView != null) {
                                    val density = context.resources.displayMetrics.density
                                    val hDp = when (selectedSize) {
                                        WidgetSize.SMALL -> 100
                                        WidgetSize.MEDIUM -> 180
                                        WidgetSize.LARGE -> 280
                                        WidgetSize.XLARGE -> 380
                                        else -> 180
                                    }

                                    val widthPx = (previewWidthDp * density).toInt()
                                    val heightPx = (hDp * density).toInt()

                                    val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
                                    val heightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)

                                    hostView.measure(widthSpec, heightSpec)
                                    hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)

                                    val options = Bundle().apply {
                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, previewWidthDp)
                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, hDp)
                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, previewWidthDp)
                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, hDp)
                                    }
                                    (hostView as? android.appwidget.AppWidgetHostView)?.updateAppWidgetOptions(options)
                                }
                            },
                            modifier = Modifier
                                .width(previewWidthDp.dp)
                                .height(
                                    when (selectedSize) {
                                        WidgetSize.SMALL -> 100.dp
                                        WidgetSize.MEDIUM -> 180.dp
                                        WidgetSize.LARGE -> 280.dp
                                        WidgetSize.XLARGE -> 380.dp
                                        else -> 180.dp
                                    }
                                )
                        )
                    }
                }
            }

            // 2. FLOATING CONTROL PANEL (Fixed to Bottom)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // A. Dynamic Settings Container
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { width -> direction * width } + fadeIn())
                                .togetherWith(slideOutHorizontally { width -> -direction * width } + fadeOut())
                                .using(SizeTransform(clip = false))
                        },
                        label = "SettingsHorizontalSwitch"
                    ) { tabIndex ->
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (tabIndex) {
                                0 -> AppearanceSettings(
                                    renderMode = renderMode,
                                    onRenderModeChange = { renderMode = it },
                                    selectedSize = selectedSize,
                                    onSizeChange = { selectedSize = it },
                                    sizeExpanded = sizeExpanded,
                                    onSizeExpandChange = { sizeExpanded = it }
                                )
                                1 -> BehaviorSettings(
                                    isShowShade = isShowShade,
                                    onShowShadeChange = { isShowShade = it },
                                    isTimeoutEnabled = isTimeoutEnabled,
                                    onTimeoutEnabledChange = { isTimeoutEnabled = it },
                                    timeoutSeconds = timeoutSeconds,
                                    onTimeoutChange = { timeoutSeconds = it },
                                    renderMode = renderMode,
                                    autoUpdateEnabled = autoUpdateEnabled,
                                    onAutoUpdateChange = { autoUpdateEnabled = it },
                                    updateInterval = updateInterval,
                                    onUpdateIntervalChange = { updateInterval = it }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // B. FLOATING TOOLBAR
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = {
                        FloatingToolbarDefaults.StandardFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    val finalTimeout = if (isTimeoutEnabled) (timeoutSeconds * 1000).toLong() else 0L
                                    val finalConfig = WidgetConfig(
                                        size = selectedSize,
                                        renderMode = renderMode,
                                        isShowShade = isShowShade,
                                        timeout = finalTimeout,
                                        autoUpdate = autoUpdateEnabled,
                                        updateIntervalMinutes = updateInterval.roundToInt()
                                    )
                                    appPreferences.saveWidgetConfig(widgetId, finalConfig)
                                    val intent = Intent(context, com.d4viddf.hyperbridge.service.WidgetOverlayService::class.java).apply {
                                        action = "ACTION_TEST_WIDGET"
                                        putExtra("WIDGET_ID", widgetId)
                                    }
                                    context.startService(intent)
                                    onBack()
                                }
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.save))
                        }
                    },
                    content = {
                        Row(
                            modifier = Modifier.wrapContentWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ToolbarOption(
                                selected = selectedTab == 0,
                                icon = Icons.Outlined.Palette,
                                text = stringResource(R.string.config_tab_appearance),
                                onClick = { selectedTab = 0 }
                            )

                            ToolbarOption(
                                selected = selectedTab == 1,
                                icon = Icons.Outlined.Layers,
                                text = stringResource(R.string.config_tab_behavior),
                                onClick = { selectedTab = 1 }
                            )
                        }
                    }
                )
            }
        }
    }
}

// --- SUB COMPONENTS ---

@Composable
fun ToolbarOption(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, fontWeight = fontWeight)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    renderMode: WidgetRenderMode,
    onRenderModeChange: (WidgetRenderMode) -> Unit,
    selectedSize: WidgetSize,
    onSizeChange: (WidgetSize) -> Unit,
    sizeExpanded: Boolean,
    onSizeExpandChange: (Boolean) -> Unit
) {
    Column {
        Text(stringResource(R.string.config_render_mode_label), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            WidgetRenderMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = renderMode == mode,
                    onClick = { onRenderModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = WidgetRenderMode.entries.size),
                    icon = { if (renderMode == mode) Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                ) { Text(stringResource(mode.labelRes)) }
            }
        }
        Text(
            text = stringResource(renderMode.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.config_container_size_label), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        ExposedDropdownMenuBox(
            expanded = sizeExpanded,
            onExpandedChange = onSizeExpandChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = stringResource(selectedSize.labelRes),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = sizeExpanded,
                onDismissRequest = { onSizeExpandChange(false) }
            ) {
                WidgetSize.entries.forEach { sizeOption ->
                    DropdownMenuItem(
                        text = { Text(stringResource(sizeOption.labelRes)) },
                        onClick = { onSizeChange(sizeOption); onSizeExpandChange(false) }
                    )
                }
            }
        }
    }
}

@Composable
fun BehaviorSettings(
    isShowShade: Boolean,
    onShowShadeChange: (Boolean) -> Unit,
    isTimeoutEnabled: Boolean,
    onTimeoutEnabledChange: (Boolean) -> Unit,
    timeoutSeconds: Float,
    onTimeoutChange: (Float) -> Unit,
    renderMode: WidgetRenderMode,
    autoUpdateEnabled: Boolean,
    onAutoUpdateChange: (Boolean) -> Unit,
    updateInterval: Float,
    onUpdateIntervalChange: (Float) -> Unit
) {
    Column {
        ListItem(
            headlineContent = { Text(stringResource(R.string.config_shade_title)) },
            supportingContent = { Text(stringResource(R.string.config_shade_desc)) },
            leadingContent = { Icon(Icons.Outlined.Visibility, null) },
            trailingContent = { Switch(checked = isShowShade, onCheckedChange = onShowShadeChange) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        ListItem(
            headlineContent = { Text(stringResource(R.string.config_timeout_title)) },
            supportingContent = { Text(stringResource(R.string.config_timeout_desc)) },
            leadingContent = { Icon(Icons.Outlined.AccessTime, null) },
            trailingContent = { Switch(checked = isTimeoutEnabled, onCheckedChange = onTimeoutEnabledChange) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        AnimatedVisibility(visible = isTimeoutEnabled) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.config_timeout_fmt, timeoutSeconds.roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = timeoutSeconds,
                    onValueChange = onTimeoutChange,
                    valueRange = 2f..30f,
                    steps = 28
                )
            }
        }

        AnimatedVisibility(visible = renderMode == WidgetRenderMode.SNAPSHOT) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.config_refresh_title)) },
                    supportingContent = { Text(stringResource(R.string.config_refresh_desc)) },
                    leadingContent = { Icon(Icons.Outlined.Autorenew, null) },
                    trailingContent = { Switch(checked = autoUpdateEnabled, onCheckedChange = onAutoUpdateChange) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                AnimatedVisibility(visible = autoUpdateEnabled) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            stringResource(R.string.config_refresh_fmt, updateInterval.roundToInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = updateInterval,
                            onValueChange = onUpdateIntervalChange,
                            valueRange = 0f..60f,
                            steps = 10
                        )
                        Text(
                            stringResource(R.string.config_refresh_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}