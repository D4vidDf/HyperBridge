package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign
import com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign
import com.d4viddf.hyperbridge.ui.components.formatSeconds
import com.d4viddf.hyperbridge.ui.components.timeoutSteps
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.launch

@Composable
fun ScreenRecordingSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val leftDesign by preferences.screenRecordingLeftDesignFlow.collectAsState(
        initial = ScreenRecordingLeftDesign.ICON_AND_TEXT
    )
    val rightDesign by preferences.screenRecordingRightDesignFlow.collectAsState(
        initial = ScreenRecordingRightDesign.TIMER
    )
    val savedTimeout by preferences.screenRecordingTimeoutFlow.collectAsState(
        initial = AppPreferences.SYSTEM_ISLAND_DEFAULT_TIMEOUT
    )

    ScreenRecordingSettingsContent(
        leftDesign = leftDesign,
        rightDesign = rightDesign,
        savedTimeout = savedTimeout,
        onLeftDesignChange = { scope.launch { preferences.setScreenRecordingLeftDesign(it) } },
        onRightDesignChange = { scope.launch { preferences.setScreenRecordingRightDesign(it) } },
        onSavedTimeoutChange = { scope.launch { preferences.setScreenRecordingTimeout(it) } },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecordingSettingsContent(
    leftDesign: ScreenRecordingLeftDesign,
    rightDesign: ScreenRecordingRightDesign,
    savedTimeout: Int,
    onLeftDesignChange: (ScreenRecordingLeftDesign) -> Unit,
    onRightDesignChange: (ScreenRecordingRightDesign) -> Unit,
    onSavedTimeoutChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    var showLeftSheet by remember { mutableStateOf(false) }
    var showRightSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_recording_customization_title)) },
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

            ScreenRecordingIslandPreview(left = leftDesign, right = rightDesign)

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
                                    ScreenRecordingLeftDesign.ICON_ONLY -> stringResource(R.string.screen_recording_left_option_icon_only)
                                    ScreenRecordingLeftDesign.ICON_AND_TEXT -> stringResource(R.string.screen_recording_left_option_icon_and_text)
                                    ScreenRecordingLeftDesign.TEXT_ONLY -> stringResource(R.string.screen_recording_left_option_text_only)
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
                                    ScreenRecordingRightDesign.TIMER -> stringResource(R.string.screen_recording_right_option_timer)
                                    ScreenRecordingRightDesign.NONE -> stringResource(R.string.screen_recording_right_option_none)
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
                                text = stringResource(R.string.screen_recording_saved_auto_hide_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTimeoutEnabled,
                            onCheckedChange = { enabled ->
                                onSavedTimeoutChange(if (enabled) 4 else 0)
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Left Design Bottom Sheet
    if (showLeftSheet) {
        ScreenRecordingOptionBottomSheet(
            title = stringResource(R.string.left_content),
            options = ScreenRecordingLeftDesign.entries,
            selected = leftDesign,
            labelFor = { option ->
                when (option) {
                    ScreenRecordingLeftDesign.ICON_ONLY -> stringResource(R.string.screen_recording_left_option_icon_only)
                    ScreenRecordingLeftDesign.ICON_AND_TEXT -> stringResource(R.string.screen_recording_left_option_icon_and_text)
                    ScreenRecordingLeftDesign.TEXT_ONLY -> stringResource(R.string.screen_recording_left_option_text_only)
                }
            },
            onSelect = { newLeft ->
                onLeftDesignChange(newLeft)
            },
            onDismiss = { showLeftSheet = false }
        )
    }

    // Right Design Bottom Sheet
    if (showRightSheet) {
        ScreenRecordingOptionBottomSheet(
            title = stringResource(R.string.right_content),
            options = ScreenRecordingRightDesign.entries,
            selected = rightDesign,
            labelFor = { option ->
                when (option) {
                    ScreenRecordingRightDesign.TIMER -> stringResource(R.string.screen_recording_right_option_timer)
                    ScreenRecordingRightDesign.NONE -> stringResource(R.string.screen_recording_right_option_none)
                }
            },
            onSelect = { newRight ->
                onRightDesignChange(newRight)
            },
            onDismiss = { showRightSheet = false }
        )
    }
}

@Composable
private fun ScreenRecordingIslandPreview(
    left: ScreenRecordingLeftDesign,
    right: ScreenRecordingRightDesign
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
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
                SymmetricalIslandLayout(
                    left = left,
                    right = right
                )
            }
        }
    }
}

@Composable
private fun SymmetricalIslandLayout(
    left: ScreenRecordingLeftDesign,
    right: ScreenRecordingRightDesign,
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
                    ScreenRecordingLeftDesign.ICON_ONLY -> {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFB382F))
                        )
                    }
                    ScreenRecordingLeftDesign.ICON_AND_TEXT -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFB382F))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.screen_recording_compact),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                    ScreenRecordingLeftDesign.TEXT_ONLY -> {
                        Text(
                            text = stringResource(R.string.screen_recording_compact),
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
                    ScreenRecordingRightDesign.TIMER -> {
                        Text(
                            text = "00:05",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                    ScreenRecordingRightDesign.NONE -> {
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

        // Make left and right symmetrical by taking the width of the widest side
        val sideWidth = maxOf(leftPlaceable.width, rightPlaceable.width, minSideWidthPx)

        val totalWidth = (horizontalPaddingPx * 2) + (sideWidth * 2) + cameraPlaceable.width + (cameraGapPx * 2)
        val totalHeight = pillHeightPx

        layout(totalWidth, totalHeight) {
            // Left content aligned at start of left side
            leftPlaceable.placeRelative(
                x = horizontalPaddingPx,
                y = (totalHeight - leftPlaceable.height) / 2
            )

            // Camera placed in the exact center
            val cameraX = horizontalPaddingPx + sideWidth + cameraGapPx
            cameraPlaceable.placeRelative(
                x = cameraX,
                y = (totalHeight - cameraPlaceable.height) / 2
            )

            // Right content aligned at end of right side
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
private fun <T> ScreenRecordingOptionBottomSheet(
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
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    onClick = {
                        onSelect(option)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = labelFor(option),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScreenRecordingSettingsScreenPreview() {
    HyperBridgeTheme {
        ScreenRecordingSettingsContent(
            leftDesign = ScreenRecordingLeftDesign.ICON_AND_TEXT,
            rightDesign = ScreenRecordingRightDesign.TIMER,
            savedTimeout = 4,
            onLeftDesignChange = {},
            onRightDesignChange = {},
            onSavedTimeoutChange = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScreenRecordingIslandPreviewVariants() {
    HyperBridgeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenRecordingIslandPreview(ScreenRecordingLeftDesign.ICON_AND_TEXT, ScreenRecordingRightDesign.TIMER)
            ScreenRecordingIslandPreview(ScreenRecordingLeftDesign.ICON_ONLY, ScreenRecordingRightDesign.NONE)
            ScreenRecordingIslandPreview(ScreenRecordingLeftDesign.TEXT_ONLY, ScreenRecordingRightDesign.TIMER)
        }
    }
}
