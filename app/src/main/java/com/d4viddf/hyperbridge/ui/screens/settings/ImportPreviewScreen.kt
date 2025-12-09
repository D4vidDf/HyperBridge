package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.db.SettingsKeys
import com.d4viddf.hyperbridge.data.model.HyperBridgeBackup
import com.d4viddf.hyperbridge.ui.components.ExpressiveGroupCard
import com.d4viddf.hyperbridge.ui.components.ExpressiveSectionTitle
import com.d4viddf.hyperbridge.util.BackupSelection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    backupData: HyperBridgeBackup,
    onBack: () -> Unit,
    onConfirmRestore: (BackupSelection) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Analyze content
    val settingsList = backupData.settings

    val hasBlocklist = settingsList.any { it.key == SettingsKeys.GLOBAL_BLOCKED_TERMS || it.key.endsWith("_blocked") }
    val hasPriorities = settingsList.any { it.key == SettingsKeys.PRIORITY_ORDER }
    val hasSettings = settingsList.any {
        it.key != SettingsKeys.GLOBAL_BLOCKED_TERMS &&
                !it.key.endsWith("_blocked") &&
                it.key != SettingsKeys.PRIORITY_ORDER
    }

    val formattedDate = remember {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(backupData.metadata.timestamp))
    }

    var selection by remember {
        mutableStateOf(BackupSelection(
            includeSettings = hasSettings,
            includeBlocklist = hasBlocklist,
            includePriorities = hasPriorities
        ))
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.backup_restore_title)) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // METADATA CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.backup_info_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(12.dp))

                    MetadataRow(stringResource(R.string.backup_meta_date), formattedDate)
                    MetadataRow(stringResource(R.string.backup_meta_device), backupData.metadata.deviceModel)
                    MetadataRow(stringResource(R.string.backup_meta_version), "${backupData.metadata.versionName} (${backupData.metadata.versionCode})")
                    MetadataRow(stringResource(R.string.backup_meta_keys), "${settingsList.size}")
                }
            }

            ExpressiveSectionTitle(stringResource(R.string.backup_options_subtitle))

            // SELECTION LIST
            ExpressiveGroupCard {
                // Settings
                if (hasSettings) {
                    PreviewCheckboxItem(
                        title = stringResource(R.string.option_app_settings),
                        subtitle = stringResource(R.string.option_app_settings_desc),
                        icon = Icons.Default.Settings,
                        checked = selection.includeSettings,
                        onCheckedChange = { selection = selection.copy(includeSettings = it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
                }

                // Blocklist
                if (hasBlocklist) {
                    PreviewCheckboxItem(
                        title = stringResource(R.string.option_blocklist),
                        subtitle = stringResource(R.string.option_blocklist_desc),
                        icon = Icons.Default.CheckCircle,
                        checked = selection.includeBlocklist,
                        onCheckedChange = { selection = selection.copy(includeBlocklist = it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
                } else {
                    DisabledPreviewItem(stringResource(R.string.option_blocklist) + stringResource(R.string.backup_empty_suffix))
                }

                // Priorities
                if (hasPriorities) {
                    PreviewCheckboxItem(
                        title = stringResource(R.string.option_priorities),
                        subtitle = stringResource(R.string.option_priorities_desc),
                        icon = Icons.Default.CheckCircle,
                        checked = selection.includePriorities,
                        onCheckedChange = { selection = selection.copy(includePriorities = it) }
                    )
                } else {
                    DisabledPreviewItem(stringResource(R.string.option_priorities) + stringResource(R.string.backup_empty_suffix))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // RESTORE BUTTON
            Button(
                onClick = { onConfirmRestore(selection) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = selection.includeSettings || selection.includeBlocklist || selection.includePriorities
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_restore_selected))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.width(100.dp) // Slightly wider for localization
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun PreviewCheckboxItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
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
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun DisabledPreviewItem(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Gray.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, null, tint = Color.Gray.copy(alpha = 0.3f))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
    }
}