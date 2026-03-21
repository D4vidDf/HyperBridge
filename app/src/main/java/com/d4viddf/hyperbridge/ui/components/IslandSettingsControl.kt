package com.d4viddf.hyperbridge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
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

// Define our snap points (in seconds)
private val timeoutSteps = listOf(
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 45,
    60, 300, 900, 1800, 3600
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
                        Text("Auto-hide Island", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isTimeoutEnabled) "Hides after a set time" else "Visible until manually dismissed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTimeoutEnabled,
                        onCheckedChange = { enabled ->
                            // If enabling, set to 5s. If disabling, set to 0.
                            onUpdate(config.copy(timeout = if (enabled) 5 else 0))
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
                                onUpdate(config.copy(timeout = selectedSeconds))
                            },
                            valueRange = 0f..(timeoutSteps.size - 1).toFloat(),
                            steps = timeoutSteps.size - 2
                        )

                        Text(
                            text = "Determines how long the island stays on screen before disappearing automatically.",
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

        SettingsToggleCard(
            title = stringResource(R.string.setting_float),
            subtitle = stringResource(R.string.setting_float_desc),
            icon = Icons.Default.Visibility,
            checked = displayConfig.isFloat ?: true,
            onCheckedChange = { onUpdate(config.copy(isFloat = it)) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        )

        SettingsToggleCard(
            title = stringResource(R.string.setting_shade),
            subtitle = stringResource(R.string.setting_shade_desc),
            icon = Icons.Default.Layers,
            checked = displayConfig.isShowShade ?: true,
            onCheckedChange = { onUpdate(config.copy(isShowShade = it)) },
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )
    }
}

/**
 * Formats seconds into a readable string (e.g. "10s", "5m", "1h")
 */
private fun formatSeconds(seconds: Int): String {
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
fun PreviewTimeoutLogic() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var config by remember { mutableStateOf(IslandConfig(timeout = 5)) }
            Column(Modifier.padding(16.dp)) {
                IslandSettingsControl(config = config, onUpdate = { config = it })
            }
        }
    }
}