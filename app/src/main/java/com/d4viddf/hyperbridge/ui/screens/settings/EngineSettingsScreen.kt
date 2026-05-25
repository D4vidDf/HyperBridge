package com.d4viddf.hyperbridge.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.service.NotificationReaderService
import com.d4viddf.hyperbridge.ui.components.EngineOptionCard
import com.d4viddf.hyperbridge.ui.components.EnginePreview
import com.d4viddf.hyperbridge.ui.screens.onboarding.ListOptionCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val repo = remember { ThemeRepository(context) }
    val scope = rememberCoroutineScope()

    // 1. Observe both the Global Preference and the Active Theme
    val defaultEnginePref by prefs.useNativeLiveUpdates.collectAsState(initial = false)
    val activeTheme by repo.activeTheme.collectAsState(initial = null)

    // 2. Calculate which engine is ACTUALLY being used right now
    val isCustomTheme = activeTheme != null && activeTheme!!.id.isNotEmpty()
    val isNative = if (isCustomTheme) {
        // [FIX] If the theme inherits (null), fall back to the global preference!
        activeTheme!!.global.useNativeLiveUpdates ?: defaultEnginePref
    } else {
        defaultEnginePref
    }

    // 3. Handle Engine Changes intelligently based on active state
    val onEngineChange = { useNative: Boolean ->
        scope.launch {
            if (isCustomTheme) {
                // Patch the active Custom Theme so the setting actually sticks!
                val currentTheme = activeTheme!!
                val updatedGlobal = currentTheme.global.copy(useNativeLiveUpdates = useNative)
                val updatedTheme = currentTheme.copy(global = updatedGlobal)
                repo.saveTheme(updatedTheme)
                repo.activateTheme(updatedTheme.id)
            } else {
                // Save to standard AppPreferences
                prefs.setUseNativeLiveUpdates(useNative)
            }

            // Immediately tell the background service to reload the engine config
            val intent = Intent(context, NotificationReaderService::class.java).apply {
                action = NotificationReaderService.ACTION_RELOAD_THEME
            }
            context.startService(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.engine), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.engine_preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            EnginePreview(isNative = isNative)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.engine_section_design),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            EngineOptionCard(
                title = stringResource(R.string.engine_xiaomi_title),
                description = stringResource(R.string.engine_xiaomi_desc),
                isSelected = !isNative,
                onClick = { onEngineChange(false) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            EngineOptionCard(
                title = stringResource(R.string.engine_native_title),
                description = stringResource(R.string.engine_native_desc),
                isSelected = isNative,
                onClick = { onEngineChange(true) }
            )

            if (!isNative) {
                Spacer(modifier = Modifier.height(32.dp))

                val isShizukuInstalled = remember { com.d4viddf.hyperbridge.util.ShizukuManager.isShizukuInstalled(context) }
                val isShizukuRunning by com.d4viddf.hyperbridge.util.ShizukuManager.isShizukuRunning.collectAsState()
                val isPermissionGranted by com.d4viddf.hyperbridge.util.ShizukuManager.isPermissionGranted.collectAsState()
                val isWorkaroundEnabled by prefs.isShizukuWorkaroundEnabled.collectAsState(initial = false)

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.shizuku_workaround_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.material3.Switch(
                        checked = isWorkaroundEnabled,
                        onCheckedChange = { scope.launch { prefs.setShizukuWorkaroundEnabled(it) } },
                        enabled = isShizukuInstalled
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.shizuku_workaround_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (isShizukuInstalled && isWorkaroundEnabled) {
                    ListOptionCard(
                        title = stringResource(if (isPermissionGranted) R.string.shizuku_permission_granted else R.string.shizuku_status_running),
                        subtitle = stringResource(if (isPermissionGranted) R.string.shizuku_status_running else R.string.shizuku_permission_denied),
                        icon = if (isPermissionGranted) Icons.Default.Security else Icons.Default.Warning,
                        shape = RoundedCornerShape(16.dp),
                        onClick = {},
                        trailingContent = {}
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            if (!isPermissionGranted) {
                                com.d4viddf.hyperbridge.util.ShizukuManager.requestPermission(context, 1)
                            }
                        },
                        enabled = !isPermissionGranted,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        )
                    ) {
                        Text(
                            stringResource(if (isPermissionGranted) R.string.perm_granted else R.string.shizuku_request_permission),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (!isShizukuInstalled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.shizuku_not_installed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}