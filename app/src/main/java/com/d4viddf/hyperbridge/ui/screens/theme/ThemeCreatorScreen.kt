package com.d4viddf.hyperbridge.ui.screens.theme

import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.ActionConfig
import com.d4viddf.hyperbridge.models.theme.AppThemeOverride
import com.d4viddf.hyperbridge.models.theme.ResourceType
import com.d4viddf.hyperbridge.models.theme.ThemeResource
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCreatorScreen(
    editThemeId: String? = null,
    onBack: () -> Unit,
    onThemeCreated: () -> Unit
) {
    val viewModel: ThemeViewModel = viewModel()
    val focusManager = LocalFocusManager.current
    val appOverrides by viewModel.appOverrides.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#3DDA82") }
    var isError by remember { mutableStateOf(false) }

    var showAppPicker by remember { mutableStateOf(false) }
    var editingPkg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editThemeId) {
        if (editThemeId != null) {
            val theme = viewModel.getThemeById(editThemeId)
            if (theme != null) {
                name = theme.meta.name
                author = theme.meta.author
                selectedColorHex = theme.global.highlightColor ?: "#3DDA82"
                viewModel.loadThemeForEditing(editThemeId)
            }
        } else {
            viewModel.clearCreatorState()
        }
    }

    val presetColors = listOf("#3DDA82", "#FF3B30", "#007AFF", "#FF9500", "#5856D6", "#FF2D55", "#5AC8FA", "#FFCC00", "#8E8E93", "#FFFFFF")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editThemeId == null) stringResource(R.string.creator_title) else "Edit Theme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Live Preview
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.creator_preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(180.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Black)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.2f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(safeParseColor(selectedColorHex).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = safeParseColor(selectedColorHex),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Box(modifier = Modifier.height(6.dp).width(60.dp).background(Color.White, CircleShape))
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.height(6.dp).width(40.dp).background(Color.Gray, CircleShape))
                            }
                        }
                    }
                }
            }

            // Inputs
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; isError = false },
                label = { Text(stringResource(R.string.creator_label_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            OutlinedTextField(
                value = author,
                onValueChange = { author = it; isError = false },
                label = { Text(stringResource(R.string.creator_label_author)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            // Color Picker
            Column {
                Text(
                    text = stringResource(R.string.creator_label_color),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(presetColors) { hex ->
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(safeParseColor(hex))
                                .clickable { selectedColorHex = hex }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                val c = safeParseColor(hex)
                                val brightness = (0.299*c.red + 0.587*c.green + 0.114*c.blue)
                                val iconColor = if (brightness > 0.5) Color.Black else Color.White
                                Icon(Icons.Rounded.Check, null, tint = iconColor)
                            }
                        }
                    }
                }
            }

            // Asset Picker
            Column {
                Text(
                    text = "Global Assets",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AssetPickerButton(
                        label = "Nav Start",
                        icon = Icons.Rounded.Navigation,
                        onImageSelected = { uri -> viewModel.stageAsset("nav_start", uri) }
                    )
                    AssetPickerButton(
                        label = "Nav End",
                        icon = Icons.Rounded.Flag,
                        onImageSelected = { uri -> viewModel.stageAsset("nav_end", uri) }
                    )
                    AssetPickerButton(
                        label = "Success",
                        icon = Icons.Rounded.CheckCircle,
                        onImageSelected = { uri -> viewModel.stageAsset("tick_icon", uri) }
                    )
                }
            }

            HorizontalDivider() // [FIX] Replaced Divider

            // App Overrides List
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("App Specifics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAppPicker = true; editingPkg = null }) {
                    Icon(Icons.Rounded.Add, null)
                    Text("Add App")
                }
            }

            if (appOverrides.isEmpty()) {
                Text("No app overrides configured.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                appOverrides.forEach { (pkg, override) ->
                    val label = installedApps.find { it.packageName == pkg }?.label ?: pkg
                    AppOverrideItem(
                        label = label,
                        override = override,
                        onEdit = { editingPkg = pkg; showAppPicker = true },
                        onDelete = { viewModel.removeAppOverride(pkg) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isBlank() || author.isBlank()) {
                        isError = true
                    } else {
                        viewModel.saveTheme(editThemeId, name, author, selectedColorHex)
                        onThemeCreated()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isError) stringResource(R.string.creator_err_inputs) else stringResource(R.string.creator_btn_save),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showAppPicker) {
        val initialOverride = if (editingPkg != null) appOverrides[editingPkg] else null

        AppConfigSheet(
            initialOverride = initialOverride,
            installedApps = installedApps,
            onDismiss = { showAppPicker = false },
            onSave = { pkg, override ->
                viewModel.updateAppOverride(pkg, override)
                showAppPicker = false
            },
            onStageAsset = { key, uri -> viewModel.stageAsset(key, uri) }
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
    onStageAsset: (String, Uri) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedApp by remember { mutableStateOf<AppItem?>(null) }
    var highlightColor by remember { mutableStateOf(initialOverride?.highlightColor ?: "") }
    var actions by remember { mutableStateOf(initialOverride?.actions ?: emptyMap()) }
    var newKeyword by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            Text("Configure App", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedApp?.label ?: "Select App",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("App") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth() // [FIX] Added Type
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    installedApps.forEach { app ->
                        DropdownMenuItem(
                            text = { Text(app.label) },
                            onClick = { selectedApp = app; expanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = highlightColor, onValueChange = { highlightColor = it }, label = { Text("Highlight Color (Hex)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))
            HorizontalDivider() // [FIX] Replaced Divider
            Spacer(Modifier.height(16.dp))

            Text("Custom Actions", style = MaterialTheme.typography.titleMedium)
            Text("Replace icons for buttons containing text.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newKeyword, onValueChange = { newKeyword = it }, label = { Text("Keyword (e.g. Reply)") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                AssetPickerButton("", Icons.Rounded.Image) { uri ->
                    if (newKeyword.isNotBlank() && selectedApp != null) {
                        val pkg = selectedApp!!.packageName
                        val fileKey = "${pkg}_${newKeyword.lowercase()}_icon"
                        onStageAsset(fileKey, uri)
                        val newConfig = ActionConfig(icon = ThemeResource(ResourceType.LOCAL_FILE, "icons/$fileKey.png"))
                        actions = actions + (newKeyword to newConfig)
                        newKeyword = ""
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            actions.forEach { (keyword, _) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Button containing '$keyword'", modifier = Modifier.weight(1f))
                    IconButton(onClick = { actions = actions - keyword }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    selectedApp?.let {
                        val override = AppThemeOverride(
                            highlightColor = highlightColor.ifBlank { null },
                            actions = actions.ifEmpty { null }
                        )
                        onSave(it.packageName, override)
                    }
                },
                enabled = selectedApp != null,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Save Configuration") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AppOverrideItem(label: String, override: AppThemeOverride, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                val color = override.highlightColor ?: "Default Color"
                val count = override.actions?.size ?: 0
                Text("$color • $count custom actions", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, null) }
        }
    }
}

@Composable
fun AssetPickerButton(label: String, icon: ImageVector, onImageSelected: (Uri) -> Unit) {
    var hasSelected by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { onImageSelected(uri); hasSelected = true }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = if (hasSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, null, tint = if(hasSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun safeParseColor(hex: String): Color {
    return try { Color(hex.toColorInt()) } catch (e: Exception) { Color.White }
}