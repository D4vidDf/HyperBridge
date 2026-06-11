package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.NotificationsPaused
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.ui.components.ListOptionCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val isDndModeEnabled by prefs.isDndModeEnabledFlow.collectAsState(initial = false)
    val autoDetectDnd by prefs.autoDetectDndFlow.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dnd_mode_title)) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.dnd_mode_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
            )

            ListOptionCard(
                title = stringResource(R.string.dnd_auto_detect),
                subtitle = stringResource(R.string.dnd_auto_detect_desc),
                icon = Icons.Outlined.DoNotDisturbOn,
                shape = RoundedCornerShape(24.dp, 24.dp, 4.dp, 4.dp),
                onClick = {
                    scope.launch { prefs.setAutoDetectDnd(!autoDetectDnd) }
                },
                trailingContent = {
                    Switch(checked = autoDetectDnd, onCheckedChange = {
                        scope.launch { prefs.setAutoDetectDnd(it) }
                    })
                }
            )

            Spacer(Modifier.height(2.dp))

            ListOptionCard(
                title = stringResource(R.string.dnd_manual_toggle),
                subtitle = stringResource(R.string.dnd_manual_toggle_desc),
                icon = Icons.Outlined.NotificationsPaused,
                shape = RoundedCornerShape(4.dp, 4.dp, 24.dp, 24.dp),
                onClick = {
                    scope.launch { prefs.setDndModeEnabled(!isDndModeEnabled) }
                },
                trailingContent = {
                    Switch(checked = isDndModeEnabled, onCheckedChange = {
                        scope.launch { prefs.setDndModeEnabled(it) }
                    })
                }
            )
        }
    }
}
