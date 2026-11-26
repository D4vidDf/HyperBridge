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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.d4viddf.hyperbridge.models.PrivacyIndicatorStyle
import com.d4viddf.hyperbridge.service.privacy.PrivacyIndicatorKind
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.launch

@Composable
fun PrivacyIndicatorSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val style by preferences.privacyIndicatorStyleFlow.collectAsState(
        initial = PrivacyIndicatorStyle.ICON
    )
    val showText by preferences.privacyIndicatorShowTextFlow.collectAsState(
        initial = true
    )
    val alwaysShow by preferences.privacyIndicatorIslandAlwaysShowFlow.collectAsState(
        initial = false
    )

    PrivacyIndicatorSettingsContent(
        style = style,
        showText = showText,
        alwaysShow = alwaysShow,
        onStyleChange = { scope.launch { preferences.setPrivacyIndicatorStyle(it) } },
        onShowTextChange = { scope.launch { preferences.setPrivacyIndicatorShowText(it) } },
        onAlwaysShowChange = { scope.launch { preferences.setPrivacyIndicatorIslandAlwaysShow(it) } },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyIndicatorSettingsContent(
    style: PrivacyIndicatorStyle,
    showText: Boolean,
    alwaysShow: Boolean,
    onStyleChange: (PrivacyIndicatorStyle) -> Unit,
    onShowTextChange: (Boolean) -> Unit,
    onAlwaysShowChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showStyleSheet by remember { mutableStateOf(false) }
    var previewKind by remember { mutableStateOf(PrivacyIndicatorKind.MICROPHONE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_island_title)) },
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

            PrivacyIslandPreview(
                kind = previewKind,
                style = style,
                showText = showText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Preview Sensor Switcher Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = previewKind == PrivacyIndicatorKind.MICROPHONE,
                    onClick = { previewKind = PrivacyIndicatorKind.MICROPHONE },
                    label = { Text(stringResource(R.string.privacy_island_mic_in_use)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF9500).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFFF9500)
                    )
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = previewKind == PrivacyIndicatorKind.CAMERA,
                    onClick = { previewKind = PrivacyIndicatorKind.CAMERA },
                    label = { Text(stringResource(R.string.privacy_island_camera_in_use)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF34C759).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF34C759)
                    )
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = previewKind == PrivacyIndicatorKind.MICROPHONE_AND_CAMERA,
                    onClick = { previewKind = PrivacyIndicatorKind.MICROPHONE_AND_CAMERA },
                    label = { Text(stringResource(R.string.privacy_island_mic_and_camera)) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

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
                    // Indicator Style Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStyleSheet = true }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.privacy_island_indicator_style),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when (style) {
                                    PrivacyIndicatorStyle.ICON -> stringResource(R.string.privacy_island_style_icon)
                                    PrivacyIndicatorStyle.DOT -> stringResource(R.string.privacy_island_style_dot)
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

                    // Show Text Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(R.string.privacy_island_show_text),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.privacy_island_show_text_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showText,
                            onCheckedChange = onShowTextChange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Always Show Card
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
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(R.string.privacy_island_always_show),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.privacy_island_always_show_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = alwaysShow,
                        onCheckedChange = onAlwaysShowChange
                    )
                }
            }
        }
    }

    if (showStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStyleSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.privacy_island_indicator_style),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                StyleOptionItem(
                    title = stringResource(R.string.privacy_island_style_icon),
                    subtitle = stringResource(R.string.privacy_island_style_icon_desc),
                    selected = style == PrivacyIndicatorStyle.ICON,
                    onClick = {
                        onStyleChange(PrivacyIndicatorStyle.ICON)
                        showStyleSheet = false
                    }
                )

                Spacer(Modifier.height(8.dp))

                StyleOptionItem(
                    title = stringResource(R.string.privacy_island_style_dot),
                    subtitle = stringResource(R.string.privacy_island_style_dot_desc),
                    selected = style == PrivacyIndicatorStyle.DOT,
                    onClick = {
                        onStyleChange(PrivacyIndicatorStyle.DOT)
                        showStyleSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StyleOptionItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}

@Composable
fun PrivacyIslandPreview(
    kind: PrivacyIndicatorKind,
    style: PrivacyIndicatorStyle,
    showText: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                SymmetricalPrivacyIslandLayout(
                    kind = kind,
                    style = style,
                    showText = showText
                )
            }
        }
    }
}

@Composable
private fun SymmetricalPrivacyIslandLayout(
    kind: PrivacyIndicatorKind,
    style: PrivacyIndicatorStyle,
    showText: Boolean,
    modifier: Modifier = Modifier
) {
    val horizontalPaddingPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    val cameraGapPx = with(LocalDensity.current) { 10.dp.roundToPx() }
    val minSideWidthPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    val pillHeightPx = with(LocalDensity.current) { 44.dp.roundToPx() }

    Layout(
        modifier = modifier,
        content = {
            // Measurable 0: Left Content (Sensor Icon or Dot + optional text)
            Box(contentAlignment = Alignment.CenterStart) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (style) {
                        PrivacyIndicatorStyle.ICON -> {
                            when (kind) {
                                PrivacyIndicatorKind.MICROPHONE -> {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_privacy_mic),
                                        contentDescription = null,
                                        tint = Color(0xFFFF9500),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                PrivacyIndicatorKind.CAMERA -> {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_privacy_camera),
                                        contentDescription = null,
                                        tint = Color(0xFF34C759),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                PrivacyIndicatorKind.MICROPHONE_AND_CAMERA -> {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_privacy_mic_camera),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        PrivacyIndicatorStyle.DOT -> {
                            when (kind) {
                                PrivacyIndicatorKind.MICROPHONE -> {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF9500))
                                    )
                                }
                                PrivacyIndicatorKind.CAMERA -> {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34C759))
                                    )
                                }
                                PrivacyIndicatorKind.MICROPHONE_AND_CAMERA -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF9500))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF34C759))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showText) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (kind) {
                                PrivacyIndicatorKind.MICROPHONE -> stringResource(R.string.privacy_island_mic_in_use)
                                PrivacyIndicatorKind.CAMERA -> stringResource(R.string.privacy_island_camera_in_use)
                                PrivacyIndicatorKind.MICROPHONE_AND_CAMERA -> stringResource(R.string.privacy_island_mic_and_camera)
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

            // Measurable 2: Right Content (Empty for privacy island)
            Box(contentAlignment = Alignment.CenterEnd) {
                // Empty slot to maintain symmetry
            }
        }
    ) { measurables, constraints ->
        val unconstrained = constraints.copy(minWidth = 0, minHeight = 0)
        val leftPlaceable = measurables[0].measure(unconstrained)
        val cameraPlaceable = measurables[1].measure(unconstrained)
        val rightPlaceable = measurables[2].measure(unconstrained)

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

            val rightX = cameraX + cameraPlaceable.width + cameraGapPx
            rightPlaceable.placeRelative(
                x = rightX,
                y = (totalHeight - rightPlaceable.height) / 2
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyIndicatorSettingsPreview() {
    HyperBridgeTheme {
        PrivacyIndicatorSettingsContent(
            style = PrivacyIndicatorStyle.ICON,
            showText = true,
            alwaysShow = true,
            onStyleChange = {},
            onShowTextChange = {},
            onAlwaysShowChange = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyIndicatorSettingsDotNoTextPreview() {
    HyperBridgeTheme {
        PrivacyIndicatorSettingsContent(
            style = PrivacyIndicatorStyle.DOT,
            showText = false,
            alwaysShow = false,
            onStyleChange = {},
            onShowTextChange = {},
            onAlwaysShowChange = {},
            onBack = {}
        )
    }
}
