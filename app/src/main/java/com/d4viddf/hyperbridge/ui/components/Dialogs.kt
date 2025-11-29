package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigBottomSheet(
    app: AppInfo,
    viewModel: AppListViewModel,
    onDismiss: () -> Unit,
    onNavConfigClick: () -> Unit
) {
    val typeConfig by viewModel.getAppConfig(app.packageName).collectAsState(initial = emptySet())

    val appIslandConfig by viewModel.getAppIslandConfig(app.packageName).collectAsState(initial = IslandConfig())
    val globalConfig by viewModel.globalConfigFlow.collectAsState(initial = IslandConfig(true, true, 5000L))

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            // --- HEADER ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Image(
                    bitmap = app.icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.configure),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // --- SECTION 1: NOTIFICATION TYPES ---
            Text(
                text = stringResource(R.string.select_active_notifs),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            NotificationType.entries.forEach { type ->
                val isChecked = typeConfig.contains(type.name)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateAppConfig(app.packageName, type, !isChecked) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(type.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Edit Button for Navigation
                    if (type == NotificationType.NAVIGATION) {
                        IconButton(onClick = onNavConfigClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Layout",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Switch(
                        checked = isChecked,
                        onCheckedChange = { viewModel.updateAppConfig(app.packageName, type, it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 2: ISLAND APPEARANCE ---
            Text(
                text = stringResource(R.string.island_appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val isUsingGlobal = appIslandConfig.isFloat == null

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isUsingGlobal) {
                            viewModel.updateAppIslandConfig(app.packageName, globalConfig)
                        } else {
                            viewModel.updateAppIslandConfig(app.packageName, IslandConfig(null, null, null))
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isUsingGlobal, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.use_global_default), style = MaterialTheme.typography.bodyLarge)
            }

            if (!isUsingGlobal) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        IslandSettingsControl(
                            config = appIslandConfig,
                            onUpdate = { newConfig ->
                                viewModel.updateAppIslandConfig(app.packageName, newConfig)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(R.string.done))
            }
        }
    }
}