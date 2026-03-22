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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
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
            title = stringResource(R.string.engine),
            subtitle = stringResource(R.string.engine_desc),
            icon = Icons.Outlined.Memory,
            shape = getExpressiveShape(3, 0, ShapeStyle.Large),
            onClick = { onNavigate(CreatorRoute.BEHAVIOR_ENGINE) }
        )
        Spacer(Modifier.height(2.dp))
        CreatorOptionCard(
            title = stringResource(R.string.island_behavior),
            subtitle = stringResource(R.string.island_behavior_desc),
            icon = Icons.Outlined.DisplaySettings,
            shape = getExpressiveShape(3, 1, ShapeStyle.Large),
            onClick = { onNavigate(CreatorRoute.BEHAVIOR_ISLAND) }
        )
        Spacer(Modifier.height(2.dp))
        CreatorOptionCard(
            title = stringResource(R.string.active_notifications_title),
            subtitle = stringResource(R.string.select_triggered_events),
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    // Read global states, defaulting to everything enabled
    val enabledTypesStr by preferences.globalNotificationTypesFlow.collectAsState(
        initial = NotificationType.entries.map { it.name }.toSet()
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.active_triggers), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.active_triggers_global_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        // We map and render each category
        NotificationType.entries.forEachIndexed { index, type ->
            val (icon, subtitle) = when (type) {
                NotificationType.STANDARD -> Icons.AutoMirrored.Outlined.Message to stringResource(R.string.type_standard_desc)
                NotificationType.PROGRESS -> Icons.Outlined.CloudDownload to stringResource(R.string.type_progress_desc)
                NotificationType.MEDIA -> Icons.Outlined.MusicNote to stringResource(R.string.type_media_desc)
                NotificationType.NAVIGATION -> Icons.Outlined.Map to stringResource(R.string.type_nav_desc)
                NotificationType.CALL -> Icons.Outlined.Call to stringResource(R.string.type_call_desc)
                NotificationType.TIMER -> Icons.Outlined.Timer to stringResource(R.string.type_timer_desc)
            }

            // Calculate expressive rounded corners to group them beautifully
            val shape = when {
                NotificationType.entries.size == 1 -> RoundedCornerShape(24.dp)
                index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                index == NotificationType.entries.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                else -> RoundedCornerShape(4.dp)
            }

            SettingsToggleCard(
                title = stringResource(type.labelRes),
                subtitle = subtitle,
                icon = icon,
                checked = enabledTypesStr.contains(type.name),
                onCheckedChange = { isChecked ->
                    scope.launch {
                        preferences.updateGlobalNotificationType(type, isChecked)
                    }
                },
                shape = shape
            )

            // Add a small spacer between cards to make the 4dp corners distinct
            if (index < NotificationType.entries.size - 1) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

// Replaced SettingsSwitchRow with your requested SettingsToggleCard implementation
@Composable
fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
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
}