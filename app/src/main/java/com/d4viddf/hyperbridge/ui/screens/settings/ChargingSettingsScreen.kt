package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Lock
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
import com.d4viddf.hyperbridge.models.ChargingLeftDesign
import com.d4viddf.hyperbridge.models.ChargingRightDesign
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.launch

@Composable
fun ChargingSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val leftDesign by preferences.chargingLeftDesignFlow.collectAsState(
        initial = ChargingLeftDesign.CHARGING_SPEED
    )
    val rightDesign by preferences.chargingRightDesignFlow.collectAsState(
        initial = ChargingRightDesign.BATTERY_PERCENTAGE
    )
    val showExpandedProgressBar by preferences.chargingExpandedProgressBarFlow.collectAsState(
        initial = true
    )
    val showOnLockscreen by preferences.chargingIslandLockscreenFlow.collectAsState(
        initial = false
    )

    ChargingSettingsContent(
        leftDesign = leftDesign,
        rightDesign = rightDesign,
        showExpandedProgressBar = showExpandedProgressBar,
        showOnLockscreen = showOnLockscreen,
        onLeftDesignChange = { scope.launch { preferences.setChargingLeftDesign(it) } },
        onRightDesignChange = { scope.launch { preferences.setChargingRightDesign(it) } },
        onShowExpandedProgressBarChange = { scope.launch { preferences.setChargingExpandedProgressBar(it) } },
        onShowOnLockscreenChange = { scope.launch { preferences.setChargingIslandLockscreen(it) } },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingSettingsContent(
    leftDesign: ChargingLeftDesign = ChargingLeftDesign.CHARGING_SPEED,
    rightDesign: ChargingRightDesign = ChargingRightDesign.BATTERY_PERCENTAGE,
    showExpandedProgressBar: Boolean = true,
    showOnLockscreen: Boolean,
    onLeftDesignChange: (ChargingLeftDesign) -> Unit = {},
    onRightDesignChange: (ChargingRightDesign) -> Unit = {},
    onShowExpandedProgressBarChange: (Boolean) -> Unit = {},
    onShowOnLockscreenChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showLeftSheet by remember { mutableStateOf(false) }
    var showRightSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.charging_island_title)) },
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

            ChargingIslandPreview(left = leftDesign, right = rightDesign)

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
                                    ChargingLeftDesign.CHARGING_SPEED -> stringResource(R.string.charging_left_option_speed)
                                    ChargingLeftDesign.REMAINING_TIME -> stringResource(R.string.charging_left_option_remaining_time)
                                    ChargingLeftDesign.BATTERY_PERCENTAGE -> stringResource(R.string.charging_left_option_percentage)
                                    ChargingLeftDesign.ICON_ONLY -> stringResource(R.string.charging_left_option_icon_only)
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
                                    ChargingRightDesign.BATTERY_PERCENTAGE -> stringResource(R.string.charging_right_option_percentage)
                                    ChargingRightDesign.PROGRESS_CIRCLE -> stringResource(R.string.charging_right_option_progress_circle)
                                    ChargingRightDesign.CHARGING_SPEED -> stringResource(R.string.charging_right_option_speed)
                                    ChargingRightDesign.REMAINING_TIME -> stringResource(R.string.charging_right_option_remaining_time)
                                    ChargingRightDesign.NONE -> stringResource(R.string.charging_right_option_none)
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

            // Expanded Progress Bar Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_charging_bolt),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            stringResource(R.string.charging_island_expanded_progress),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.charging_island_expanded_progress_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showExpandedProgressBar,
                        onCheckedChange = onShowExpandedProgressBarChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lockscreen Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            stringResource(R.string.charging_island_lockscreen),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.charging_island_lockscreen_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = showOnLockscreen, onCheckedChange = onShowOnLockscreenChange)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Left Design Bottom Sheet
    if (showLeftSheet) {
        ChargingOptionBottomSheet(
            title = stringResource(R.string.left_content),
            options = ChargingLeftDesign.entries,
            selected = leftDesign,
            labelFor = { option ->
                when (option) {
                    ChargingLeftDesign.CHARGING_SPEED -> stringResource(R.string.charging_left_option_speed)
                    ChargingLeftDesign.REMAINING_TIME -> stringResource(R.string.charging_left_option_remaining_time)
                    ChargingLeftDesign.BATTERY_PERCENTAGE -> stringResource(R.string.charging_left_option_percentage)
                    ChargingLeftDesign.ICON_ONLY -> stringResource(R.string.charging_left_option_icon_only)
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
        ChargingOptionBottomSheet(
            title = stringResource(R.string.right_content),
            options = ChargingRightDesign.entries,
            selected = rightDesign,
            labelFor = { option ->
                when (option) {
                    ChargingRightDesign.BATTERY_PERCENTAGE -> stringResource(R.string.charging_right_option_percentage)
                    ChargingRightDesign.PROGRESS_CIRCLE -> stringResource(R.string.charging_right_option_progress_circle)
                    ChargingRightDesign.CHARGING_SPEED -> stringResource(R.string.charging_right_option_speed)
                    ChargingRightDesign.REMAINING_TIME -> stringResource(R.string.charging_right_option_remaining_time)
                    ChargingRightDesign.NONE -> stringResource(R.string.charging_right_option_none)
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
private fun ChargingIslandPreview(
    left: ChargingLeftDesign,
    right: ChargingRightDesign
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
                SymmetricalChargingIslandLayout(
                    left = left,
                    right = right
                )
            }
        }
    }
}

@Composable
private fun SymmetricalChargingIslandLayout(
    left: ChargingLeftDesign,
    right: ChargingRightDesign,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_charging_bolt),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    if (left != ChargingLeftDesign.ICON_ONLY) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (left) {
                                ChargingLeftDesign.CHARGING_SPEED -> stringResource(R.string.charging_island_watts, 67)
                                ChargingLeftDesign.REMAINING_TIME -> stringResource(R.string.charging_island_minutes, 25)
                                ChargingLeftDesign.BATTERY_PERCENTAGE -> "75%"
                                ChargingLeftDesign.ICON_ONLY -> ""
                            },
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
                    ChargingRightDesign.BATTERY_PERCENTAGE -> {
                        Text(
                            text = "75%",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                    ChargingRightDesign.PROGRESS_CIRCLE -> {
                        CircularProgressIndicator(
                            progress = { 0.75f },
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF34C759),
                            trackColor = Color.DarkGray,
                            strokeWidth = 2.dp
                        )
                    }
                    ChargingRightDesign.CHARGING_SPEED -> {
                        Text(
                            text = stringResource(R.string.charging_island_watts, 67),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                    ChargingRightDesign.REMAINING_TIME -> {
                        Text(
                            text = stringResource(R.string.charging_island_minutes, 25),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                    ChargingRightDesign.NONE -> {
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
private fun <T> ChargingOptionBottomSheet(
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
fun ChargingSettingsPreview() {
    HyperBridgeTheme {
        ChargingSettingsContent(
            leftDesign = ChargingLeftDesign.CHARGING_SPEED,
            rightDesign = ChargingRightDesign.BATTERY_PERCENTAGE,
            showExpandedProgressBar = true,
            showOnLockscreen = true,
            onLeftDesignChange = {},
            onRightDesignChange = {},
            onShowExpandedProgressBarChange = {},
            onShowOnLockscreenChange = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChargingIslandPreviewVariants() {
    HyperBridgeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ChargingIslandPreview(ChargingLeftDesign.CHARGING_SPEED, ChargingRightDesign.BATTERY_PERCENTAGE)
            ChargingIslandPreview(ChargingLeftDesign.REMAINING_TIME, ChargingRightDesign.PROGRESS_CIRCLE)
            ChargingIslandPreview(ChargingLeftDesign.ICON_ONLY, ChargingRightDesign.CHARGING_SPEED)
        }
    }
}
