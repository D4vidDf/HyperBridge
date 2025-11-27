package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.IslandConfig
import kotlin.math.roundToInt

@Composable
fun IslandSettingsControl(
    config: IslandConfig,
    // If not null, represents Global Defaults to fall back on
    defaultConfig: IslandConfig? = null,
    onUpdate: (IslandConfig) -> Unit
) {
    val isOverridden = config.isFloat != null
    val displayConfig = if (isOverridden) config else (defaultConfig ?: config)

    Column {
        // 1. FLOAT TOGGLE
        SettingsSwitchRow(
            title = stringResource(R.string.setting_float),
            description = stringResource(R.string.setting_float_desc),
            icon = Icons.Default.Visibility,
            checked = displayConfig.isFloat ?: true,
            onCheckedChange = {
                onUpdate(config.copy(isFloat = it))
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp).padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(0.2f)
        )

        // 2. SHADE TOGGLE
        SettingsSwitchRow(
            title = stringResource(R.string.setting_shade),
            description = stringResource(R.string.setting_shade_desc),
            icon = Icons.Default.Layers,
            checked = displayConfig.isShowShade ?: true,
            onCheckedChange = {
                onUpdate(config.copy(isShowShade = it))
            }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp).padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(0.2f)
        )

        // 3. TIMEOUT SLIDER
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccessTime,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.setting_timeout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val timeoutSec = ((displayConfig.timeout ?: 5000L) / 1000f)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        // Display "0s" or "5s"
                        text = stringResource(R.string.seconds_suffix, timeoutSec.roundToInt()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(32.dp) // Fixed width to prevent jitter
                    )
                    Slider(
                        value = timeoutSec,
                        onValueChange = {
                            // Update config on slide
                            onUpdate(config.copy(timeout = (it * 1000).toLong()))
                        },
                        // CHANGED: Allow 0s to 10s
                        valueRange = 0f..10f,
                        // Steps: 0,1,2,3,4,5,6,7,8,9,10 = 11 positions.
                        // Steps parameter counts "ticks inside the range".
                        // 11 positions - 2 (start/end) = 9 steps.
                        steps = 9,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String, description: String, icon: ImageVector,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}