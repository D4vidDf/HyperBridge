package com.d4viddf.hyperbridge.ui.screens.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
        onHideOtpChange = { scope.launch { preferences.setSmartActionsHideOtpCode(it) } },
        onExcludedChange = { packageName, excluded ->
            scope.launch { preferences.setSmartActionExcluded(packageName, excluded) }
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
    onHideOtpChange: (Boolean) -> Unit,
    onExcludedChange: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showAppPicker by remember { mutableStateOf(false) }

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
                SettingsDivider()
                SettingsSwitchItem(
                    icon = Icons.Outlined.Directions,
                    title = stringResource(R.string.setting_smart_actions_navigation),
                    subtitle = stringResource(R.string.setting_smart_actions_navigation_desc),
                    checked = config.navigation,
                    onCheckedChange = { onTypeChange(SmartActionType.NAVIGATION, it) }
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.smart_actions_group_privacy))
            Spacer(Modifier.height(12.dp))

            // Hiding the OTP code only matters once Smart Actions (and detection of codes) can
            // actually surface a button, so it follows the same dimmed treatment as Detect above.
            SettingsCard(modifier = Modifier.alpha(if (config.enabled) 1f else 0.55f)) {
                SettingsSwitchItem(
                    icon = Icons.Outlined.VisibilityOff,
                    title = stringResource(R.string.setting_smart_actions_hide_otp),
                    subtitle = stringResource(R.string.setting_smart_actions_hide_otp_desc),
                    checked = config.hideOtpCode,
                    onCheckedChange = onHideOtpChange
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

            val excludedApps = remember(apps, config.excludedPackages) {
                config.excludedPackages.toList().map { pkg -> pkg to apps.find { it.packageName == pkg } }
            }

            SettingsCard {
                if (excludedApps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.smart_actions_excluded_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                } else {
                    excludedApps.forEachIndexed { index, (pkg, app) ->
                        ExcludedAppRow(
                            packageName = pkg,
                            app = app,
                            onRemove = { onExcludedChange(pkg, false) }
                        )
                        if (index < excludedApps.lastIndex) SettingsDivider()
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showAppPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.smart_actions_add_excluded_app))
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAppPicker) {
        ExcludeAppPickerSheet(
            apps = apps,
            excludedPackages = config.excludedPackages,
            onAppSelected = { pkg ->
                onExcludedChange(pkg, true)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcludeAppPickerSheet(
    apps: List<AppInfo>,
    excludedPackages: Set<String>,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    val availableApps = remember(apps, excludedPackages, searchQuery) {
        apps.filter { it.packageName !in excludedPackages }
            .filter {
                searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.smart_actions_pick_app_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.smart_actions_pick_app_search)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Outlined.Close, contentDescription = null) } }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.smart_actions_no_apps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (availableApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.smart_actions_all_apps_excluded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableApps.size) { index ->
                        val app = availableApps[index]
                        PickableAppCard(
                            app = app,
                            onClick = { onAppSelected(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickableAppCard(
    app: AppInfo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(app.icon, modifier = Modifier.size(40.dp))

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
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
    packageName: String,
    app: AppInfo?,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app?.icon, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app?.name ?: packageName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.cd_smart_actions_remove_excluded),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppIcon(bitmap: Bitmap?, modifier: Modifier = Modifier) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )
    } else {
        Surface(
            modifier = modifier.size(40.dp),
            shape = CircleShape,
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
            onHideOtpChange = {},
            onExcludedChange = { _, _ -> },
            onBack = {}
        )
    }
}
