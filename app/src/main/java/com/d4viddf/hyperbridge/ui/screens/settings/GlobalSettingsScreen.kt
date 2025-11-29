package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

    val globalConfig by preferences.globalConfigFlow.collectAsState(initial = IslandConfig(true, true, 5000L))

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
        }
    }
}