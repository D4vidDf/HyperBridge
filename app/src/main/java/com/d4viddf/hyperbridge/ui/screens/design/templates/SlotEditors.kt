package com.d4viddf.hyperbridge.ui.screens.design.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.composer.ComposerActionButton
import com.d4viddf.hyperbridge.models.composer.FieldBinding
import com.d4viddf.hyperbridge.models.composer.GraphicSource
import com.d4viddf.hyperbridge.models.composer.LeftGraphicSlot
import com.d4viddf.hyperbridge.models.composer.NotificationField
import com.d4viddf.hyperbridge.models.composer.ProgressKind
import com.d4viddf.hyperbridge.models.composer.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.composer.TemplateRule
import com.d4viddf.hyperbridge.models.composer.TextSlotConfig
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.screens.theme.content.ColorPickerSetting

// --- LEFT GRAPHIC ---

@Composable
fun LeftGraphicPicker(value: LeftGraphicSlot, onChange: (LeftGraphicSlot) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.template_left_graphic_source), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                GraphicSource.APP_ICON to R.string.template_left_graphic_app_icon,
                GraphicSource.LARGE_ICON to R.string.template_left_graphic_large_icon,
                GraphicSource.STATIC_ASSET to R.string.template_left_graphic_static
            )
            options.forEachIndexed { index, (source, labelRes) ->
                SegmentedButton(
                    selected = value.source == source,
                    onClick = { onChange(value.copy(source = source)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    // STATIC_ASSET has no asset storage yet (Phase 4 v1 scope cut) — greyed out.
                    enabled = source != GraphicSource.STATIC_ASSET
                ) {
                    Text(stringResource(labelRes), maxLines = 1)
                }
            }
        }

        Text(stringResource(R.string.template_left_graphic_padding), style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value.paddingPercent.toFloat(),
            onValueChange = { onChange(value.copy(paddingPercent = it.toInt())) },
            valueRange = 0f..40f
        )
    }
}

// --- CENTER TEXT ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldBindingDropdown(
    label: String,
    value: FieldBinding,
    availableFields: List<NotificationField>,
    onChange: (FieldBinding) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = fieldLabel(value.field),
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                availableFields.forEach { field ->
                    DropdownMenuItem(
                        text = { Text(fieldLabel(field)) },
                        onClick = {
                            onChange(if (field == NotificationField.STATIC) value.copy(field = field) else FieldBinding(field))
                            expanded = false
                        }
                    )
                }
            }
        }
        if (value.field == NotificationField.STATIC) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = value.staticValue ?: "",
                onValueChange = { onChange(value.copy(staticValue = it)) },
                label = { Text(stringResource(R.string.template_field_static_value)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun fieldLabel(field: NotificationField): String = when (field) {
    NotificationField.TITLE -> stringResource(R.string.template_field_source_title)
    NotificationField.TEXT -> stringResource(R.string.template_field_source_text)
    NotificationField.SUBTEXT -> stringResource(R.string.template_field_source_subtext)
    NotificationField.PROGRESS -> stringResource(R.string.template_field_source_progress)
    NotificationField.STATIC -> stringResource(R.string.template_field_source_static)
    NotificationField.APP_ICON, NotificationField.LARGE_ICON -> stringResource(R.string.template_field_source_static)
}

private val TEXT_FIELDS = listOf(
    NotificationField.TITLE, NotificationField.TEXT, NotificationField.SUBTEXT, NotificationField.STATIC
)

@Composable
fun TextSlotEditor(value: TextSlotConfig, onChange: (TextSlotConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldBindingDropdown(
            label = stringResource(R.string.template_field_title),
            value = value.title,
            availableFields = TEXT_FIELDS,
            onChange = { onChange(value.copy(title = it)) }
        )
        FieldBindingDropdown(
            label = stringResource(R.string.template_field_content),
            value = value.content,
            availableFields = TEXT_FIELDS,
            onChange = { onChange(value.copy(content = it)) }
        )
        ColorPickerSetting(
            title = stringResource(R.string.template_text_title_color),
            subtitle = "",
            colorHex = value.titleColor,
            defaultColor = androidx.compose.ui.graphics.Color.White,
            onReset = { onChange(value.copy(titleColor = null)) },
            onColorChanged = { onChange(value.copy(titleColor = it)) }
        )
        ColorPickerSetting(
            title = stringResource(R.string.template_text_content_color),
            subtitle = "",
            colorHex = value.contentColor,
            defaultColor = androidx.compose.ui.graphics.Color.White,
            onReset = { onChange(value.copy(contentColor = null)) },
            onColorChanged = { onChange(value.copy(contentColor = it)) }
        )
    }
}

// --- PROGRESS ---

@Composable
fun ProgressSlotEditor(value: ProgressSlotConfig, onChange: (ProgressSlotConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.template_progress_kind), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                ProgressKind.NONE to R.string.template_progress_none,
                ProgressKind.LINEAR to R.string.template_progress_linear,
                ProgressKind.CIRCULAR to R.string.template_progress_circular,
                ProgressKind.MULTI_STEP to R.string.template_progress_multi_step
            )
            options.forEachIndexed { index, (kind, labelRes) ->
                SegmentedButton(
                    selected = value.kind == kind,
                    onClick = { onChange(value.copy(kind = kind)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(stringResource(labelRes), maxLines = 1)
                }
            }
        }
        if (value.kind != ProgressKind.NONE) {
            ColorPickerSetting(
                title = stringResource(R.string.template_progress_active_color),
                subtitle = "",
                colorHex = value.activeColor,
                defaultColor = MaterialTheme.colorScheme.primary,
                onReset = { onChange(value.copy(activeColor = null)) },
                onColorChanged = { onChange(value.copy(activeColor = it)) }
            )
            ColorPickerSetting(
                title = stringResource(R.string.template_progress_finished_color),
                subtitle = "",
                colorHex = value.finishedColor,
                defaultColor = androidx.compose.ui.graphics.Color(0xFF34C759),
                onReset = { onChange(value.copy(finishedColor = null)) },
                onColorChanged = { onChange(value.copy(finishedColor = it)) }
            )
        }
    }
}

// --- ACTION BUTTONS ---

@Composable
fun ActionButtonListEditor(
    value: List<ComposerActionButton>,
    maxButtons: Int,
    availableNativeActions: Int,
    onChange: (List<ComposerActionButton>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        value.forEachIndexed { index, button ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = button.label,
                        onValueChange = { newLabel ->
                            onChange(value.toMutableList().also { it[index] = button.copy(label = newLabel) })
                        },
                        label = { Text(stringResource(R.string.template_button_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = { onChange(value.toMutableList().also { it.removeAt(index) }) }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.template_button_remove))
                    }
                }
                if (availableNativeActions > 0) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.template_button_bind_native)) },
                        trailingContent = {
                            Switch(
                                checked = button.useNotificationAction,
                                onCheckedChange = { checked ->
                                    onChange(value.toMutableList().also { it[index] = button.copy(useNotificationAction = checked) })
                                }
                            )
                        }
                    )
                }
                ColorPickerSetting(
                    title = stringResource(R.string.template_button_color),
                    subtitle = "",
                    colorHex = button.bgColor,
                    defaultColor = MaterialTheme.colorScheme.primary,
                    onReset = { onChange(value.toMutableList().also { it[index] = button.copy(bgColor = null) }) },
                    onColorChanged = { color -> onChange(value.toMutableList().also { it[index] = button.copy(bgColor = color) }) }
                )
                HorizontalDivider()
            }
        }
        if (value.size < maxButtons) {
            OutlinedButton(onClick = { onChange(value + ComposerActionButton(label = "")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.template_button_add))
            }
        }
    }
}

// --- RULE ---

@Composable
fun TemplateRuleEditor(
    value: TemplateRule,
    apps: List<AppInfo>,
    onChange: (TemplateRule) -> Unit
) {
    var advanced by remember { mutableStateOf(!value.packageNameRegex.isNullOrEmpty() && apps.none { it.packageName == value.packageNameRegex }) }
    var appPickerExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.template_rule_advanced_regex), modifier = Modifier.weight(1f))
            Switch(checked = advanced, onCheckedChange = { advanced = it })
        }

        if (advanced) {
            OutlinedTextField(
                value = value.packageNameRegex ?: "",
                onValueChange = { onChange(value.copy(packageNameRegex = it.ifBlank { null })) },
                label = { Text(stringResource(R.string.template_rule_package)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            val selectedApp = apps.find { it.packageName == value.packageNameRegex }
            Box {
                OutlinedButton(onClick = { appPickerExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedApp?.name ?: stringResource(R.string.template_rule_package_any))
                }
                androidx.compose.material3.DropdownMenu(expanded = appPickerExpanded, onDismissRequest = { appPickerExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.template_rule_package_any)) },
                        onClick = { onChange(value.copy(packageNameRegex = null)); appPickerExpanded = false }
                    )
                    apps.take(50).forEach { app ->
                        DropdownMenuItem(
                            text = { Text(app.name) },
                            onClick = {
                                onChange(value.copy(packageNameRegex = Regex.escape(app.packageName)))
                                appPickerExpanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = value.titleRegex ?: "",
            onValueChange = { onChange(value.copy(titleRegex = it.ifBlank { null })) },
            label = { Text(stringResource(R.string.template_rule_title_regex)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = value.textRegex ?: "",
            onValueChange = { onChange(value.copy(textRegex = it.ifBlank { null })) },
            label = { Text(stringResource(R.string.template_rule_text_regex)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Column {
            Text(stringResource(R.string.template_rule_priority, value.priority), style = MaterialTheme.typography.labelMedium)
            Slider(
                value = value.priority.toFloat(),
                onValueChange = { onChange(value.copy(priority = it.toInt())) },
                valueRange = 0f..200f
            )
        }
    }
}
