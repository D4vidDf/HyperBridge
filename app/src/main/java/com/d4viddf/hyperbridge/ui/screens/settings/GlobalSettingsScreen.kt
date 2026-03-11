package com.d4viddf.hyperbridge.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.ui.components.IslandSettingsControl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(
    onBack: () -> Unit,
    onNavSettingsClick: () -> Unit // New Callback
    ) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val globalConfig by preferences.globalConfigFlow.collectAsState(initial = IslandConfig(true, true, 5))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.global_settings)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(16.dp)) {
                    IslandSettingsControl(
                        config = globalConfig,
                        onUpdate = { newConfig ->
                            scope.launch { preferences.updateGlobalConfig(newConfig) }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // NEW: Navigation Layout Card
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                SettingsItem(
                    icon = Icons.Default.Navigation,
                    title = stringResource(R.string.nav_layout_title),
                    subtitle = stringResource(R.string.nav_layout_desc),
                    onClick = onNavSettingsClick
                )
            }
            var useNativeLiveUpdates by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val prefs = context.getSharedPreferences("hyperbridge_settings", Context.MODE_PRIVATE)
                useNativeLiveUpdates = prefs.getBoolean("use_native_live_updates", false)
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_live_updates_title), fontWeight = FontWeight.SemiBold) },
                supportingContent = {
                    Text(
                        stringResource(R.string.settings_live_updates_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                },
                trailingContent = {
                    Switch(
                        checked = useNativeLiveUpdates,
                        onCheckedChange = { isChecked ->
                            useNativeLiveUpdates = isChecked
                            context.getSharedPreferences("hyperbridge_settings", Context.MODE_PRIVATE)
                                .edit().putBoolean("use_native_live_updates", isChecked).apply()
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
        // Use AutoMirrored icon for RTL support
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}