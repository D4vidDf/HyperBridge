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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    // Load states from ViewModel
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
                .padding(bottom = 48.dp) // Extra bottom padding for navigation bar safety
        ) {
            // =====================================================================
            // HEADER: App Icon and Name
            // =====================================================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Image(
                    bitmap = app.icon.asImageBitmap(),
                    contentDescription = null, // Decorative: Text name is sufficient for TalkBack
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

            // =====================================================================
            // SECTION 1: NOTIFICATION TYPES
            // Toggles specific features (e.g., Disable music island for this app)
            // =====================================================================
            Text(
                text = stringResource(R.string.select_active_notifs),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            NotificationType.entries.forEach { type ->
                val isChecked = typeConfig.contains(type.name)
                val typeLabel = stringResource(type.labelRes)

                // ACCESSIBILITY: Pre-load strings here because we cannot call
                // @Composable functions inside the modifier.semantics block below.
                val switchDesc = if (isChecked)
                    stringResource(R.string.cd_disable_type, typeLabel)
                else
                    stringResource(R.string.cd_enable_type, typeLabel)

                val navEditDesc = stringResource(R.string.cd_nav_edit)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateAppConfig(app.packageName, type, !isChecked) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Special Edit Button for Navigation Type
                    if (type == NotificationType.NAVIGATION) {
                        IconButton(
                            onClick = {
                                onDismiss() // Close sheet before navigating
                                onNavConfigClick()
                            },
                            modifier = Modifier.semantics {
                                contentDescription = navEditDesc
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null, // Handled by modifier above
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Switch(
                        checked = isChecked,
                        onCheckedChange = { viewModel.updateAppConfig(app.packageName, type, it) },
                        modifier = Modifier.semantics {
                            contentDescription = switchDesc
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))

            // =====================================================================
            // SECTION 2: ISLAND APPEARANCE
            // Override global settings (Timeout, Float) for this specific app
            // =====================================================================
            Text(
                text = stringResource(R.string.island_appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Logic: Null means "Inherit from Global". Non-null means "Custom Override".
            val isUsingGlobal = appIslandConfig.isFloat == null

            // ACCESSIBILITY: Resolve strings outside semantics block
            val stateActive = stringResource(R.string.status_active)
            val stateInactive = stringResource(R.string.status_finished) // Reusing "Finished" implies "Not active" context

            // "Use Global Defaults" Checkbox Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isUsingGlobal) {
                            // Switch to Custom: Pre-fill with current global values so the UI doesn't jump
                            viewModel.updateAppIslandConfig(app.packageName, globalConfig)
                        } else {
                            // Switch to Global: Reset to nulls
                            viewModel.updateAppIslandConfig(app.packageName, IslandConfig(null, null, null))
                        }
                    }
                    .padding(vertical = 8.dp)
                    .semantics {
                        // Tell TalkBack if we are using global defaults or custom
                        stateDescription = if (isUsingGlobal) stateActive else stateInactive
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isUsingGlobal, onCheckedChange = null) // decorative, click handled by Row
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.use_global_default), style = MaterialTheme.typography.bodyLarge)
            }

            // Show controls only when NOT using global defaults
            if (!isUsingGlobal) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        // Reusable control logic (Sliders/Switches)
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

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.done))
            }
        }
    }
}