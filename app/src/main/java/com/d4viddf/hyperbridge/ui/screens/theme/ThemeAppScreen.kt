package com.d4viddf.hyperbridge.ui.screens.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.ActionConfig
import com.d4viddf.hyperbridge.models.theme.AppThemeOverride
import com.d4viddf.hyperbridge.models.theme.ResourceType
import com.d4viddf.hyperbridge.models.theme.ThemeResource

// --- APPS CONTENT ---
@Composable
fun AppsDetailContent(viewModel: ThemeViewModel) {
    val overrides by viewModel.appOverrides.collectAsState()
    val apps by viewModel.installedApps.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.apps_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, null) }
        }
        if (overrides.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.apps_empty), color = Color.Gray)
        }
        else LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(overrides.toList()) { (pkg, ov) ->
                val label = apps.find { it.packageName == pkg }?.label ?: pkg
                AppOverrideCard(label, ov) { viewModel.removeAppOverride(pkg) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (showAdd) AppConfigSheet(null, apps, { showAdd = false }, { pkg, ov -> viewModel.updateAppOverride(pkg, ov); showAdd = false }, { k, u -> viewModel.stageAsset(k, u) })
}

@Composable
fun AppOverrideCard(label: String, override: AppThemeOverride, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(12.dp)) {
        ListItem(
            headlineContent = { Text(label, fontWeight = FontWeight.Medium) },
            supportingContent = {
                val color = override.highlightColor ?: stringResource(R.string.apps_default_color)
                val actions = override.actions?.size ?: 0
                Text("$color • " + stringResource(R.string.apps_action_count, actions))
            },
            trailingContent = { IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) } },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigSheet(
    initialOverride: AppThemeOverride?,
    installedApps: List<AppItem>,
    onDismiss: () -> Unit,
    onSave: (String, AppThemeOverride) -> Unit,
    onStageAsset: (String, android.net.Uri) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedApp by remember { mutableStateOf<AppItem?>(null) }
    var highlightColor by remember { mutableStateOf(initialOverride?.highlightColor ?: "") }
    var actions by remember { mutableStateOf(initialOverride?.actions ?: emptyMap()) }
    var newKeyword by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(com.d4viddf.hyperbridge.R.string.apps_sheet_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedApp?.label ?: stringResource(R.string.apps_label_select), onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(R.string.apps_label_app)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    installedApps.forEach { app -> DropdownMenuItem(text = { Text(app.label) }, onClick = { selectedApp = app; expanded = false }) }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = highlightColor, onValueChange = { highlightColor = it }, label = { Text(stringResource(
                com.d4viddf.hyperbridge.R.string.apps_label_highlight)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.apps_group_actions), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newKeyword, onValueChange = { newKeyword = it }, label = { Text(stringResource(
                    com.d4viddf.hyperbridge.R.string.apps_label_keyword)) }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                AssetPickerButton("", Icons.Rounded.Image) { uri ->
                    if (newKeyword.isNotBlank() && selectedApp != null) {
                        val pkg = selectedApp!!.packageName
                        val fileKey = "${pkg}_${newKeyword.lowercase()}_icon"
                        onStageAsset(fileKey, uri)
                        actions = actions + (newKeyword to ActionConfig(icon = ThemeResource(ResourceType.LOCAL_FILE, "icons/$fileKey.png")))
                        newKeyword = ""
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            actions.forEach { (keyword, _) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.apps_desc_button_contains, keyword), modifier = Modifier.weight(1f))
                    IconButton(onClick = { actions = actions - keyword }, modifier = Modifier.size(24.dp)) { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = { selectedApp?.let { onSave(it.packageName, AppThemeOverride(highlightColor.ifBlank { null }, actions.ifEmpty { null })) } }, enabled = selectedApp != null, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(
                R.string.apps_action_save)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}