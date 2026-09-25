package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.SystemUpdateLeftDesign
import com.d4viddf.hyperbridge.models.SystemUpdateRightDesign
import com.d4viddf.hyperbridge.ui.components.formatSeconds
import com.d4viddf.hyperbridge.ui.components.timeoutSteps
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.launch

@Composable
fun SystemUpdateSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val leftDesign by preferences.systemUpdateLeftDesignFlow.collectAsState(
        initial = SystemUpdateLeftDesign.ICON_AND_TEXT
    )
    val rightDesign by preferences.systemUpdateRightDesignFlow.collectAsState(
        initial = SystemUpdateRightDesign.PERCENTAGE
    )
    val iconSource by preferences.systemUpdateIconSourceFlow.collectAsState(
        initial = com.d4viddf.hyperbridge.models.SystemUpdateIconSource.NOTIFICATION_ICON
    )
    val savedTimeout by preferences.systemUpdateTimeoutFlow.collectAsState(
        initial = AppPreferences.SYSTEM_ISLAND_DEFAULT_TIMEOUT
    )

    SystemUpdateSettingsContent(
        leftDesign = leftDesign,
        rightDesign = rightDesign,
        iconSource = iconSource,
        savedTimeout = savedTimeout,
        onLeftDesignChange = { scope.launch { preferences.setSystemUpdateLeftDesign(it) } },
        onRightDesignChange = { scope.launch { preferences.setSystemUpdateRightDesign(it) } },
        onIconSourceChange = { scope.launch { preferences.setSystemUpdateIconSource(it) } },
        onSavedTimeoutChange = { scope.launch { preferences.setSystemUpdateTimeout(it) } },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemUpdateSettingsContent(
    leftDesign: SystemUpdateLeftDesign,
    rightDesign: SystemUpdateRightDesign,
    iconSource: com.d4viddf.hyperbridge.models.SystemUpdateIconSource,
    savedTimeout: Int,
    onLeftDesignChange: (SystemUpdateLeftDesign) -> Unit,
    onRightDesignChange: (SystemUpdateRightDesign) -> Unit,
    onIconSourceChange: (com.d4viddf.hyperbridge.models.SystemUpdateIconSource) -> Unit,
    onSavedTimeoutChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    var showLeftSheet by remember { mutableStateOf(false) }
    var showRightSheet by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.system_updater_customization_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.preview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            SystemUpdateIslandPreview(
                left = leftDesign,
                right = rightDesign,
                iconSource = iconSource
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                stringResource(R.string.group_configuration),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Configuration Options Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    // Left Content Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLeftSheet = true }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.left_content),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when (leftDesign) {
                                    SystemUpdateLeftDesign.ICON_AND_TEXT -> stringResource(R.string.system_updater_left_option_icon_and_text)
                                    SystemUpdateLeftDesign.ICON_ONLY -> stringResource(R.string.system_updater_left_option_icon_only)
                                    SystemUpdateLeftDesign.TEXT_ONLY -> stringResource(R.string.system_updater_left_option_text_only)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Right Content Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRightSheet = true }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.right_content),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when (rightDesign) {
                                    SystemUpdateRightDesign.PERCENTAGE -> stringResource(R.string.system_updater_right_option_percentage)
                                    SystemUpdateRightDesign.PROGRESS_CIRCLE -> stringResource(R.string.system_updater_right_option_progress_circle)
                                    SystemUpdateRightDesign.NONE -> stringResource(R.string.system_updater_right_option_none)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Icon Selection Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showIconSheet = true }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.system_updater_icon_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when (iconSource) {
                                    com.d4viddf.hyperbridge.models.SystemUpdateIconSource.NOTIFICATION_ICON -> stringResource(R.string.system_updater_icon_option_notification)
                                    com.d4viddf.hyperbridge.models.SystemUpdateIconSource.APP_PACKAGE_ICON -> stringResource(R.string.system_updater_icon_option_package)
                                    com.d4viddf.hyperbridge.models.SystemUpdateIconSource.PROVIDED_SYSTEM_UPDATE -> stringResource(R.string.system_updater_icon_option_system_update)
                                    com.d4viddf.hyperbridge.models.SystemUpdateIconSource.PROVIDED_DOWNLOAD -> stringResource(R.string.system_updater_icon_option_download)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Completion Auto-Hide Timeout Section
            val isTimeoutEnabled = savedTimeout > 0
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.auto_hide_island),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.system_updater_saved_auto_hide_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTimeoutEnabled,
                            onCheckedChange = { checked ->
                                onSavedTimeoutChange(if (checked) AppPreferences.SYSTEM_ISLAND_DEFAULT_TIMEOUT else 0)
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isTimeoutEnabled,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = formatSeconds(savedTimeout),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            val currentIndex = timeoutSteps.indexOf(savedTimeout).coerceAtLeast(0).toFloat()
                            Slider(
                                value = currentIndex,
                                onValueChange = { index ->
                                    val selectedSeconds = timeoutSteps[index.toInt()]
                                    onSavedTimeoutChange(selectedSeconds)
                                },
                                valueRange = 0f..(timeoutSteps.size - 1).toFloat(),
                                steps = timeoutSteps.size - 2
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLeftSheet) {
        SystemUpdateOptionBottomSheet(
            title = stringResource(R.string.left_content),
            options = SystemUpdateLeftDesign.entries,
            selected = leftDesign,
            labelFor = { option ->
                when (option) {
                    SystemUpdateLeftDesign.ICON_AND_TEXT -> stringResource(R.string.system_updater_left_option_icon_and_text)
                    SystemUpdateLeftDesign.ICON_ONLY -> stringResource(R.string.system_updater_left_option_icon_only)
                    SystemUpdateLeftDesign.TEXT_ONLY -> stringResource(R.string.system_updater_left_option_text_only)
                }
            },
            onSelect = {
                onLeftDesignChange(it)
                showLeftSheet = false
            },
            onDismiss = { showLeftSheet = false }
        )
    }

    if (showRightSheet) {
        SystemUpdateOptionBottomSheet(
            title = stringResource(R.string.right_content),
            options = SystemUpdateRightDesign.entries,
            selected = rightDesign,
            labelFor = { option ->
                when (option) {
                    SystemUpdateRightDesign.PERCENTAGE -> stringResource(R.string.system_updater_right_option_percentage)
                    SystemUpdateRightDesign.PROGRESS_CIRCLE -> stringResource(R.string.system_updater_right_option_progress_circle)
                    SystemUpdateRightDesign.NONE -> stringResource(R.string.system_updater_right_option_none)
                }
            },
            onSelect = {
                onRightDesignChange(it)
                showRightSheet = false
            },
            onDismiss = { showRightSheet = false }
        )
    }

    if (showIconSheet) {
        SystemUpdateOptionBottomSheet(
            title = stringResource(R.string.system_updater_icon_title),
            options = com.d4viddf.hyperbridge.models.SystemUpdateIconSource.entries,
            selected = iconSource,
            labelFor = { option ->
                when (option) {
                    com.d4viddf.hyperbridge.models.SystemUpdateIconSource.NOTIFICATION_ICON -> stringResource(R.string.system_updater_icon_option_notification)
                    com.d4viddf.hyperbridge.models.SystemUpdateIconSource.APP_PACKAGE_ICON -> stringResource(R.string.system_updater_icon_option_package)
                    com.d4viddf.hyperbridge.models.SystemUpdateIconSource.PROVIDED_SYSTEM_UPDATE -> stringResource(R.string.system_updater_icon_option_system_update)
                    com.d4viddf.hyperbridge.models.SystemUpdateIconSource.PROVIDED_DOWNLOAD -> stringResource(R.string.system_updater_icon_option_download)
                }
            },
            onSelect = {
                onIconSourceChange(it)
                showIconSheet = false
            },
            onDismiss = { showIconSheet = false }
        )
    }
}

@Composable
private fun SystemUpdateIslandPreview(
    left: SystemUpdateLeftDesign,
    right: SystemUpdateRightDesign,
    iconSource: com.d4viddf.hyperbridge.models.SystemUpdateIconSource,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                SymmetricalUpdateIslandLayout(
                    left = left,
                    right = right,
                    iconSource = iconSource
                )
            }
        }
    }
}

@Composable
private fun SymmetricalUpdateIslandLayout(
    left: SystemUpdateLeftDesign,
    right: SystemUpdateRightDesign,
    iconSource: com.d4viddf.hyperbridge.models.SystemUpdateIconSource,
    modifier: Modifier = Modifier
) {
    val horizontalPaddingPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    val cameraGapPx = with(LocalDensity.current) { 10.dp.roundToPx() }
    val minSideWidthPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    val pillHeightPx = with(LocalDensity.current) { 44.dp.roundToPx() }

    Layout(
        modifier = modifier,
        content = {
            // Measurable 0: Left Content
            Box(contentAlignment = Alignment.CenterStart) {
                when (left) {
                    SystemUpdateLeftDesign.ICON_ONLY -> {
                        SystemUpdateIconDisplay(iconSource = iconSource)
                    }
                    SystemUpdateLeftDesign.ICON_AND_TEXT -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SystemUpdateIconDisplay(iconSource = iconSource)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "3.0.304.0",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                    SystemUpdateLeftDesign.TEXT_ONLY -> {
                        Text(
                            text = "3.0.304.0",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            // Measurable 1: Camera Cutout (Realistic punch-hole)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F0F0F))
                )
            }

            // Measurable 2: Right Content
            Box(contentAlignment = Alignment.CenterEnd) {
                when (right) {
                    SystemUpdateRightDesign.PERCENTAGE -> {
                        Text(
                            text = "17%",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                    SystemUpdateRightDesign.PROGRESS_CIRCLE -> {
                        CircularProgressIndicator(
                            progress = { 0.17f },
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF007AFF),
                            trackColor = Color.DarkGray,
                            strokeWidth = 2.dp
                        )
                    }
                    SystemUpdateRightDesign.NONE -> {
                        // Empty slot
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val unconstrained = constraints.copy(minWidth = 0, minHeight = 0)
        val leftPlaceable = measurables[0].measure(unconstrained)
        val cameraPlaceable = measurables[1].measure(unconstrained)
        val rightPlaceable = measurables[2].measure(unconstrained)

        // Symmetrical layout
        val sideWidth = maxOf(leftPlaceable.width, rightPlaceable.width, minSideWidthPx)

        val totalWidth = (horizontalPaddingPx * 2) + (sideWidth * 2) + cameraPlaceable.width + (cameraGapPx * 2)
        val totalHeight = pillHeightPx

        layout(totalWidth, totalHeight) {
            leftPlaceable.placeRelative(
                x = horizontalPaddingPx,
                y = (totalHeight - leftPlaceable.height) / 2
            )

            val cameraX = horizontalPaddingPx + sideWidth + cameraGapPx
            cameraPlaceable.placeRelative(
                x = cameraX,
                y = (totalHeight - cameraPlaceable.height) / 2
            )

            val rightX = totalWidth - horizontalPaddingPx - rightPlaceable.width
            rightPlaceable.placeRelative(
                x = rightX,
                y = (totalHeight - rightPlaceable.height) / 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SystemUpdateOptionBottomSheet(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = null
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = labelFor(option),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (option == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemUpdateIconDisplay(
    iconSource: com.d4viddf.hyperbridge.models.SystemUpdateIconSource,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    when (iconSource) {
        com.d4viddf.hyperbridge.models.SystemUpdateIconSource.NOTIFICATION_ICON -> {
            Icon(
                painter = painterResource(R.drawable.ic_system_update_arrow),
                contentDescription = null,
                tint = Color.White,
                modifier = modifier.size(16.dp)
            )
        }
        com.d4viddf.hyperbridge.models.SystemUpdateIconSource.APP_PACKAGE_ICON -> {
            val appIconBitmap = remember(context) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(com.d4viddf.hyperbridge.service.updater.SystemUpdaterClassifier.PACKAGE_NAME)
                    val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
                    val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
                    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(canvas)
                    bmp
                } catch (_: Exception) {
                    null
                }
            }
            if (appIconBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = appIconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = modifier
                        .size(16.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = modifier.size(16.dp)
                )
            }
        }
        com.d4viddf.hyperbridge.models.SystemUpdateIconSource.PROVIDED_SYSTEM_UPDATE -> {
            Icon(
                painter = painterResource(R.drawable.ic_system_update_arrow),
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = modifier.size(16.dp)
            )
        }
        com.d4viddf.hyperbridge.models.SystemUpdateIconSource.PROVIDED_DOWNLOAD -> {
            Icon(
                painter = painterResource(R.drawable.ic_system_update_download),
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SystemUpdateSettingsPreview() {
    HyperBridgeTheme {
        SystemUpdateSettingsContent(
            leftDesign = SystemUpdateLeftDesign.ICON_AND_TEXT,
            rightDesign = SystemUpdateRightDesign.PERCENTAGE,
            iconSource = com.d4viddf.hyperbridge.models.SystemUpdateIconSource.NOTIFICATION_ICON,
            savedTimeout = 5,
            onLeftDesignChange = {},
            onRightDesignChange = {},
            onIconSourceChange = {},
            onSavedTimeoutChange = {},
            onBack = {}
        )
    }
}
