package com.d4viddf.hyperbridge.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.service.floating.FloatingNotificationSetup
import com.d4viddf.hyperbridge.service.floating.FloatingSetupStatus
import com.d4viddf.hyperbridge.util.DeviceUtils
import com.d4viddf.hyperbridge.util.NotificationSettingsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingNotificationSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val selectedPackages by preferences.allowedPackagesFlow.collectAsState(initial = emptySet())
    val confirmedPackages by preferences.floatingSetupConfirmedPackagesFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val requiresManualSetup = remember { DeviceUtils.isXiaomi && DeviceUtils.isCompatibleOS() }

    LaunchedEffect(Unit) {
        preferences.setFloatingSetupNoticePending(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.floating_setup_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            stringResource(R.string.floating_setup_intro),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.floating_setup_limitation),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedPackages.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.floating_setup_no_apps),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(selectedPackages.sorted(), key = { it }) { packageName ->
                val label = remember(packageName) {
                    try {
                        val info = context.packageManager.getApplicationInfo(packageName, 0)
                        context.packageManager.getApplicationLabel(info).toString()
                    } catch (_: Exception) {
                        packageName
                    }
                }
                val status = FloatingNotificationSetup.status(
                    isSelected = true,
                    userConfirmedDisabled = packageName in confirmedPackages,
                    requiresManualSetup = requiresManualSetup
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (status == FloatingSetupStatus.USER_CONFIRMED) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = if (status == FloatingSetupStatus.USER_CONFIRMED) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.SemiBold)
                                Text(
                                    when (status) {
                                        FloatingSetupStatus.USER_CONFIRMED -> stringResource(R.string.floating_status_user_confirmed)
                                        FloatingSetupStatus.NEEDS_REVIEW -> stringResource(R.string.floating_status_needs_review)
                                        FloatingSetupStatus.NOT_REQUIRED -> stringResource(R.string.floating_status_not_required)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                if (!NotificationSettingsNavigator.openForApp(context, packageName)) {
                                    Toast.makeText(context, R.string.settings_not_available, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsOff, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.open_notification_settings))
                        }

                        if (requiresManualSetup) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = packageName in confirmedPackages,
                                    onCheckedChange = { checked ->
                                        scope.launch { preferences.setFloatingSetupConfirmed(packageName, checked) }
                                    }
                                )
                                Text(stringResource(R.string.floating_disabled_confirmation))
                            }
                        }
                    }
                }
            }
        }
    }
}
