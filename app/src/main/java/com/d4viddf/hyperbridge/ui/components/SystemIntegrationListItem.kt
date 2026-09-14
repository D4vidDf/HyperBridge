package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.SystemIntegrationId
import com.d4viddf.hyperbridge.ui.SystemIntegrationInfo

import androidx.compose.ui.res.painterResource

@Composable
fun SystemIntegrationListItem(
    integration: SystemIntegrationInfo,
    onToggle: (Boolean) -> Unit,
    onSettingsClick: (() -> Unit)?
) {
    val settingsAction = if (integration.id != SystemIntegrationId.VPN) onSettingsClick else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (settingsAction != null) Modifier.clickable(onClick = settingsAction) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (integration.icon != null) {
                    Image(
                        bitmap = integration.icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    )
                } else if (integration.id == SystemIntegrationId.VPN) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vpn),
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = when (integration.id) {
                            SystemIntegrationId.SCREEN_RECORDER -> Icons.Outlined.Videocam
                            SystemIntegrationId.VPN -> Icons.Outlined.Videocam
                        },
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    when (integration.id) {
                        SystemIntegrationId.SCREEN_RECORDER -> R.string.screen_recording_title
                        SystemIntegrationId.VPN -> R.string.vpn_title
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    !integration.available -> stringResource(R.string.system_integration_unavailable)
                    integration.enabled -> stringResource(R.string.system_integration_enabled)
                    else -> stringResource(R.string.system_integration_disabled)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (settingsAction != null) {
            IconButton(onClick = settingsAction) {
                Icon(Icons.Default.Settings, stringResource(R.string.settings_action), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Switch(
            checked = integration.enabled,
            onCheckedChange = onToggle,
            enabled = integration.available
        )
    }
}

