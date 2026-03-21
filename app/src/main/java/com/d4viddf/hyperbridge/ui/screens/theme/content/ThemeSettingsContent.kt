package com.d4viddf.hyperbridge.ui.screens.theme.content

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.ui.components.IslandSettingsControl
import com.d4viddf.hyperbridge.ui.screens.theme.CreatorOptionCard
import com.d4viddf.hyperbridge.ui.screens.theme.CreatorRoute
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import kotlinx.coroutines.launch

@Composable
fun BehaviourMenuContent(onNavigate: (CreatorRoute) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        CreatorOptionCard(
            title = "Engine",
            subtitle = "Live Updates vs Xiaomi Island",
            icon = Icons.Outlined.Memory,
            shape = getExpressiveShape(3, 0, ShapeStyle.Large),
            onClick = { onNavigate(CreatorRoute.BEHAVIOR_ENGINE) }
        )
        Spacer(Modifier.height(2.dp))
        CreatorOptionCard(
            title = "Island Behaviour",
            subtitle = "Timeouts, duplicates & visibility",
            icon = Icons.Outlined.DisplaySettings,
            shape = getExpressiveShape(3, 1, ShapeStyle.Large),
            onClick = { onNavigate(CreatorRoute.BEHAVIOR_ISLAND) }
        )
        Spacer(Modifier.height(2.dp))
        CreatorOptionCard(
            title = "Notification Types",
            subtitle = "Select triggered events",
            icon = Icons.Outlined.NotificationsActive,
            shape = getExpressiveShape(3, 2, ShapeStyle.Large),
            onClick = { onNavigate(CreatorRoute.BEHAVIOR_TYPES) }
        )
    }
}

@Composable
fun ThemeBehaviourContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val globalConfig by preferences.globalConfigFlow.collectAsState(initial = IslandConfig(false, false, 10))

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        IslandSettingsControl(
            config = globalConfig,
            onUpdate = { newConfig ->
                scope.launch { preferences.updateGlobalConfig(newConfig) }
            }
        )
    }
}

@Composable
fun NotificationTypesContent() {
    // Local state for UI representation.
    // TODO: Connect to global system / AppPreferences in the future.
    var enabledTypes by remember { mutableStateOf(NotificationType.entries.associateWith { true }) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Active Triggers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column {
                NotificationType.entries.forEachIndexed { index, type ->
                    val (icon, subtitle) = when (type) {
                        NotificationType.STANDARD -> Icons.AutoMirrored.Outlined.Message to "Regular messages, emails, and alerts"
                        NotificationType.PROGRESS -> Icons.Outlined.CloudDownload to "Downloads, uploads, and ongoing tasks"
                        NotificationType.MEDIA -> Icons.Outlined.MusicNote to "Music and video playback controls"
                        NotificationType.NAVIGATION -> Icons.Outlined.Map to "Turn-by-turn directions and ETA"
                        NotificationType.CALL -> Icons.Outlined.Call to "Incoming and ongoing phone calls"
                        NotificationType.TIMER -> Icons.Outlined.Timer to "Active timers and stopwatches"
                    }

                    SettingsSwitchRow(
                        icon = icon,
                        title = stringResource(type.labelRes),
                        subtitle = subtitle,
                        checked = enabledTypes[type] == true,
                        onCheckedChange = { isChecked ->
                            val newMap = enabledTypes.toMutableMap()
                            newMap[type] = isChecked
                            enabledTypes = newMap
                        }
                    )

                    if (index < NotificationType.entries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

// Reusable descriptive row matching the global settings design
@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}