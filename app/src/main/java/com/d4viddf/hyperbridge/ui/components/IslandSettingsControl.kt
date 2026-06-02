package com.d4viddf.hyperbridge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme

// Define our snap points (in seconds) for the auto-hide island
val timeoutSteps = listOf(
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 45,
    60, 300, 900, 1800, 3600
)
private val timePopUpSteps = listOf(
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30
)

@Composable
fun IslandSettingsControl(
    config: IslandConfig,
    defaultConfig: IslandConfig? = null,
    onUpdate: (IslandConfig) -> Unit
) {
    val isOverridden = config.isFloat != null
    val displayConfig = if (isOverridden) config else (defaultConfig ?: config)

    // Timeout is "Enabled" if it's > 0
    val currentTimeout = displayConfig.timeout ?: 10
    val isTimeoutEnabled = currentTimeout > 0

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

        Text(
            text = stringResource(R.string.global_behavior),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
        Spacer(Modifier.height(8.dp))
        // --- TIMEOUT CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                // Header with Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.auto_hide_island), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isTimeoutEnabled) stringResource(R.string.hides_after_a_set_time) else stringResource(
                                R.string.behavior_hide_desc
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTimeoutEnabled,
                        onCheckedChange = { enabled ->
                            // When changing this, we also ensure isFloat is set to track the override
                            val newTimeout = if (enabled) 5 else 0
                            val currentIsFloat = config.isFloat ?: defaultConfig?.isFloat ?: true
                            onUpdate(config.copy(timeout = newTimeout, isFloat = currentIsFloat))
                        }
                    )
                }

                // Expandable Slider Section
                AnimatedVisibility(
                    visible = isTimeoutEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))

                        // Time Display Label
                        Text(
                            text = formatSeconds(currentTimeout),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        // Slider mapping to our steps list
                        val currentIndex = timeoutSteps.indexOf(currentTimeout).coerceAtLeast(0).toFloat()

                        Slider(
                            value = currentIndex,
                            onValueChange = { index ->
                                val selectedSeconds = timeoutSteps[index.toInt()]
                                val currentIsFloat = config.isFloat ?: defaultConfig?.isFloat ?: true
                                onUpdate(config.copy(timeout = selectedSeconds, isFloat = currentIsFloat))
                            },
                            valueRange = 0f..(timeoutSteps.size - 1).toFloat(),
                            steps = timeoutSteps.size - 2
                        )

                        Text(
                            text = stringResource(R.string.behavior_desc_hide_long),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.xiaomi_featured_notifications),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        // --- FLOAT SETTINGS (Heads-up Popup) ---
        val isFloatEnabled = displayConfig.isFloat ?: true
        val currentFloatTimeout = displayConfig.floatTimeout ?: 10

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.setting_float), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.setting_float_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isFloatEnabled,
                        onCheckedChange = { onUpdate(config.copy(isFloat = it)) }
                    )
                }

                AnimatedVisibility(
                    visible = isFloatEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.seconds_suffix, currentFloatTimeout),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )


                        // Slider mapping to our steps list
                        val currentIndexPop = timePopUpSteps.indexOf(currentFloatTimeout).coerceAtLeast(1).toFloat()

                        Slider(
                            value = currentIndexPop,
                            onValueChange = { index ->
                                val selectedSeconds = timePopUpSteps[index.toInt()]
                                onUpdate(config.copy(floatTimeout = selectedSeconds))
                            },
                            valueRange = 0f..(timePopUpSteps.size - 1).toFloat(),
                            steps = timePopUpSteps.size - 2,

                        )
                        Text(
                            text = stringResource(R.string.setting_float_timeout_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        SettingsToggleCard(
            title = stringResource(R.string.setting_shade),
            subtitle = stringResource(R.string.setting_shade_desc),
            icon = Icons.Default.Layers,
            checked = displayConfig.isShowShade ?: true,
            onCheckedChange = { 
                val currentIsFloat = config.isFloat ?: defaultConfig?.isFloat ?: true
                onUpdate(config.copy(isShowShade = it, isFloat = currentIsFloat)) 
            },
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.live_updates),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        SettingsToggleCard(
            title = stringResource(R.string.remove_original_notification),
            subtitle = stringResource(R.string.remove_original_notification_desc),
            icon = Icons.Default.DeleteSweep,
            checked = displayConfig.removeOriginalNotification ?: false,
            onCheckedChange = { 
                val currentIsFloat = config.isFloat ?: defaultConfig?.isFloat ?: true
                onUpdate(config.copy(removeOriginalNotification = it, isFloat = currentIsFloat)) 
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/**
 * Formats seconds into a readable string (e.g. "10s", "5m", "1h")
 */
fun formatSeconds(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        else -> "${seconds / 3600}h"
    }
}

@Composable
fun SettingsToggleCard(
    title: String, subtitle: String, icon: ImageVector,
    checked: Boolean, shape: Shape, onCheckedChange: (Boolean) -> Unit
) {
    Card(
        onClick = { onCheckedChange(!checked) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = shape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IslandSettingsControlPreview() {
    HyperBridgeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            IslandSettingsControl(
                config = IslandConfig(
                    isFloat = true,
                    timeout = 5,
                    floatTimeout = 5,
                    isShowShade = true,
                    removeOriginalNotification = false
                ),
                onUpdate = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsToggleCardPreview() {
    HyperBridgeTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SettingsToggleCard(
                title = "Example Title",
                subtitle = "Example subtitle for the toggle card",
                icon = Icons.Default.Layers,
                checked = true,
                shape = RoundedCornerShape(24.dp),
                onCheckedChange = {}
            )
        }
    }
}
