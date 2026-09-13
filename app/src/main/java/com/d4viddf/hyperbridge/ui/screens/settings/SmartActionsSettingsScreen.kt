package com.d4viddf.hyperbridge.ui.screens.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.SmartActionType
import com.d4viddf.hyperbridge.models.SmartActionsConfig
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.launch

@Composable
fun SmartActionsSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppListViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val config by preferences.smartActionsConfigFlow.collectAsState(initial = SmartActionsConfig.DISABLED)
    val activeApps by viewModel.activeAppsState.collectAsState()

    SmartActionsSettingsContent(
        config = config,
        apps = activeApps,
        onEnabledChange = { scope.launch { preferences.setSmartActionsEnabled(it) } },
        onTypeChange = { type, enabled -> scope.launch { preferences.setSmartActionTypeEnabled(type, enabled) } },
        onExcludedChange = { packageName, excluded ->
            val updated = if (excluded) config.excludedPackages + packageName else config.excludedPackages - packageName
            scope.launch { preferences.setSmartActionsExcludedPackages(updated) }
        },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartActionsSettingsContent(
    config: SmartActionsConfig,
    apps: List<AppInfo>,
    onEnabledChange: (Boolean) -> Unit,
    onTypeChange: (SmartActionType, Boolean) -> Unit,
    onExcludedChange: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.smart_actions_title)) },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Outlined.AutoAwesome,
                    title = stringResource(R.string.setting_smart_actions),
                    subtitle = stringResource(R.string.setting_smart_actions_desc),
                    checked = config.enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.smart_actions_group_detect))
            Spacer(Modifier.height(12.dp))

            // The type toggles stay editable while the master switch is off; they are just dimmed
            // so it is obvious nothing happens until Smart Actions are enabled.
            SettingsCard(modifier = Modifier.alpha(if (config.enabled) 1f else 0.55f)) {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Password,
                    title = stringResource(R.string.setting_smart_actions_otp),
                    subtitle = stringResource(R.string.setting_smart_actions_otp_desc),
                    checked = config.otp,
                    onCheckedChange = { onTypeChange(SmartActionType.OTP, it) }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    icon = Icons.Outlined.Link,
                    title = stringResource(R.string.setting_smart_actions_url),
                    subtitle = stringResource(R.string.setting_smart_actions_url_desc),
                    checked = config.url,
                    onCheckedChange = { onTypeChange(SmartActionType.URL, it) }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    icon = Icons.Outlined.Call,
                    title = stringResource(R.string.setting_smart_actions_phone),
                    subtitle = stringResource(R.string.setting_smart_actions_phone_desc),
                    checked = config.phone,
                    onCheckedChange = { onTypeChange(SmartActionType.PHONE, it) }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    icon = Icons.Outlined.LocalShipping,
                    title = stringResource(R.string.setting_smart_actions_tracking),
                    subtitle = stringResource(R.string.setting_smart_actions_tracking_desc),
                    checked = config.tracking,
                    onCheckedChange = { onTypeChange(SmartActionType.TRACKING, it) }
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.smart_actions_group_excluded))
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.smart_actions_excluded_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            SettingsCard {
                if (apps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.smart_actions_no_apps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                } else {
                    apps.forEachIndexed { index, app ->
                        val excluded = app.packageName in config.excludedPackages
                        ExcludedAppRow(
                            app = app,
                            excluded = excluded,
                            onToggle = { onExcludedChange(app.packageName, !excluded) }
                        )
                        if (index < apps.lastIndex) SettingsDivider()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

@Composable
private fun ExcludedAppRow(
    app: AppInfo,
    excluded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.icon)
        Spacer(Modifier.width(16.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Checkbox(checked = excluded, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun AppIcon(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartActionsSettingsPreview() {
    HyperBridgeTheme {
        SmartActionsSettingsContent(
            config = SmartActionsConfig(enabled = true, excludedPackages = setOf("com.bank")),
            apps = listOf(
                AppInfo(name = "Messages", packageName = "com.google.android.apps.messaging", icon = null),
                AppInfo(name = "Bank", packageName = "com.bank", icon = null)
            ),
            onEnabledChange = {},
            onTypeChange = { _, _ -> },
            onExcludedChange = { _, _ -> },
            onBack = {}
        )
    }
}
