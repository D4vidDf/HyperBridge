package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.widget.ButtonAction
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.CanvasSize
import com.d4viddf.hyperbridge.models.widget.ContainerLayout
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.NodeBounds
import com.d4viddf.hyperbridge.models.widget.NodeCondition
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.TextNode

/**
 * The always-visible control panel for whatever element the tab row has selected. KWGT keeps its
 * controls on screen rather than behind a sheet, which is what David asked for in #328.
 */
@Composable
fun StudioInspector(
    node: CustomWidgetNode,
    isRoot: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (CustomWidgetNode) -> Unit,
    onMoveLayer: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StudioSection(stringResource(R.string.studio_section_content)) {
            when (node) {
                is TextNode -> {
                    OutlinedTextField(
                        value = node.template,
                        onValueChange = { onChange(node.copy(template = it)) },
                        label = { Text(stringResource(R.string.studio_property_template)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    VariableTokenRow { token -> onChange(node.copy(template = node.template + token)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = node.fontSizeSp,
                            label = stringResource(R.string.studio_property_font_size),
                            onValueChange = { onChange(node.copy(fontSizeSp = it.coerceIn(6, 96))) },
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = node.maxLines,
                            label = stringResource(R.string.studio_property_max_lines),
                            onValueChange = { onChange(node.copy(maxLines = it.coerceIn(1, 4))) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = node.colorHex,
                        onValueChange = { onChange(node.copy(colorHex = it)) },
                        label = { Text(stringResource(R.string.studio_property_color)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    LabelledSwitch(
                        label = stringResource(R.string.studio_property_bold),
                        checked = node.bold,
                        onCheckedChange = { onChange(node.copy(bold = it)) }
                    )
                }

                is ProgressNode -> {
                    OutlinedTextField(
                        value = node.valueTemplate,
                        onValueChange = { onChange(node.copy(valueTemplate = it)) },
                        label = { Text(stringResource(R.string.studio_property_template)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    VariableTokenRow { token -> onChange(node.copy(valueTemplate = node.valueTemplate + token)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = node.progressColorHex,
                            onValueChange = { onChange(node.copy(progressColorHex = it)) },
                            label = { Text(stringResource(R.string.studio_property_color)) },
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = node.maxValue,
                            label = stringResource(R.string.studio_property_max_value),
                            onValueChange = { onChange(node.copy(maxValue = it.coerceAtLeast(1))) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is ButtonNode -> {
                    OutlinedTextField(
                        value = node.label,
                        onValueChange = { onChange(node.copy(label = it)) },
                        label = { Text(stringResource(R.string.studio_property_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = node.backgroundHex.orEmpty(),
                        onValueChange = { onChange(node.copy(backgroundHex = it.ifBlank { null })) },
                        label = { Text(stringResource(R.string.studio_property_background)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is ImageNode -> {
                    Text(
                        text = node.source.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = node.tintHex.orEmpty(),
                        onValueChange = { onChange(node.copy(tintHex = it.ifBlank { null })) },
                        label = { Text(stringResource(R.string.studio_property_tint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is LayoutContainer -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContainerLayout.entries.forEach { layout ->
                            FilterChip(
                                selected = node.layout == layout,
                                onClick = { onChange(node.copy(layout = layout)) },
                                label = { Text(layout.name) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = node.gapDp,
                            label = stringResource(R.string.studio_property_gap),
                            onValueChange = { onChange(node.copy(gapDp = it.coerceIn(0, 64))) },
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = node.paddingDp,
                            label = stringResource(R.string.studio_property_padding),
                            onValueChange = { onChange(node.copy(paddingDp = it.coerceIn(0, 64))) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (!isRoot) {
            StudioSection(stringResource(R.string.studio_section_position)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        value = node.bounds.x,
                        label = stringResource(R.string.studio_property_x),
                        onValueChange = { onChange(node.withBounds(node.bounds.copy(x = it))) },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        value = node.bounds.y,
                        label = stringResource(R.string.studio_property_y),
                        onValueChange = { onChange(node.withBounds(node.bounds.copy(y = it))) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        value = node.bounds.widthDp ?: 0,
                        label = stringResource(R.string.studio_property_width),
                        onValueChange = { onChange(node.withBounds(node.bounds.copy(widthDp = it.takeIf { v -> v > 0 }))) },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        value = node.bounds.heightDp ?: 0,
                        label = stringResource(R.string.studio_property_height),
                        onValueChange = { onChange(node.withBounds(node.bounds.copy(heightDp = it.takeIf { v -> v > 0 }))) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = stringResource(R.string.studio_layer_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onMoveLayer(1) }, enabled = canMoveUp) {
                        Icon(Icons.Rounded.ArrowUpward, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.studio_layer_forward))
                    }
                    TextButton(onClick = { onMoveLayer(-1) }, enabled = canMoveDown) {
                        Icon(Icons.Rounded.ArrowDownward, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.studio_layer_back))
                    }
                }
            }

            StudioSection(stringResource(R.string.studio_section_visibility)) {
                ConditionEditor(
                    condition = node.showIf,
                    onChange = { onChange(node.withShowIf(it)) }
                )
            }

            StudioSection(stringResource(R.string.studio_section_action)) {
                ActionEditor(
                    action = if (node is ButtonNode) node.action else node.onClick,
                    allowNone = node !is ButtonNode,
                    onChange = { action ->
                        onChange(
                            if (node is ButtonNode) node.copy(action = action ?: ButtonAction.Dismiss)
                            else node.withOnClick(action)
                        )
                    }
                )
            }

            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.studio_delete_element))
            }
        }
    }
}

/** The design's own settings: what it is called, how tall the island is, and when it shows. */
@Composable
fun StudioDesignSettings(
    name: String,
    canvas: CanvasSize,
    notificationType: NotificationType,
    onNameChange: (String) -> Unit,
    onCanvasChange: (CanvasSize) -> Unit,
    onNotificationTypeChange: (NotificationType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StudioSection(stringResource(R.string.studio_section_design)) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.studio_property_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CanvasSize.entries.forEach { size ->
                    FilterChip(
                        selected = canvas == size,
                        onClick = { onCanvasChange(size) },
                        label = { Text("${size.name} · ${size.heightDp}dp") }
                    )
                }
            }
        }

        StudioSection(stringResource(R.string.design_template_type_title)) {
            Text(
                text = stringResource(R.string.design_template_type_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationType.configurableEntries.forEach { type ->
                    FilterChip(
                        selected = notificationType == type,
                        onClick = { onNotificationTypeChange(type) },
                        label = { Text(stringResource(type.labelRes)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConditionEditor(condition: NodeCondition, onChange: (NodeCondition) -> Unit) {
    val kinds = listOf(
        ConditionKind.ALWAYS,
        ConditionKind.NOTIFICATION_ACTION,
        ConditionKind.INLINE_REPLY,
        ConditionKind.SMART_ACTION,
        ConditionKind.PROGRESS,
        ConditionKind.NOT_BLANK,
        ConditionKind.MATCHES
    )
    val current = ConditionKind.of(condition)

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        kinds.forEach { kind ->
            FilterChip(
                selected = current == kind,
                onClick = { onChange(kind.default()) },
                label = { Text(stringResource(kind.labelRes)) }
            )
        }
    }

    when (condition) {
        is NodeCondition.HasNotificationAction -> NumberField(
            value = condition.index,
            label = stringResource(R.string.studio_condition_action_index),
            onValueChange = { onChange(NodeCondition.HasNotificationAction(it.coerceIn(0, 3))) },
            modifier = Modifier.fillMaxWidth()
        )

        is NodeCondition.HasSmartAction -> Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SMART_ACTION_TYPES.forEach { type ->
                FilterChip(
                    selected = condition.type.equals(type, ignoreCase = true),
                    onClick = { onChange(NodeCondition.HasSmartAction(type)) },
                    label = { Text(type) }
                )
            }
        }

        is NodeCondition.NotBlank -> {
            OutlinedTextField(
                value = condition.template,
                onValueChange = { onChange(NodeCondition.NotBlank(it)) },
                label = { Text(stringResource(R.string.studio_property_template)) },
                modifier = Modifier.fillMaxWidth()
            )
            VariableTokenRow { token -> onChange(NodeCondition.NotBlank(condition.template + token)) }
        }

        is NodeCondition.Matches -> {
            OutlinedTextField(
                value = condition.template,
                onValueChange = { onChange(NodeCondition.Matches(it, condition.regex)) },
                label = { Text(stringResource(R.string.studio_property_template)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = condition.regex,
                onValueChange = { onChange(NodeCondition.Matches(condition.template, it)) },
                label = { Text(stringResource(R.string.studio_condition_regex)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        else -> Unit
    }
}

@Composable
private fun ActionEditor(action: ButtonAction?, allowNone: Boolean, onChange: (ButtonAction?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (allowNone) {
            FilterChip(
                selected = action == null,
                onClick = { onChange(null) },
                label = { Text(stringResource(R.string.studio_action_none)) }
            )
        }
        ActionKind.entries.forEach { kind ->
            FilterChip(
                selected = action != null && ActionKind.of(action) == kind,
                onClick = { onChange(kind.default()) },
                label = { Text(stringResource(kind.labelRes)) }
            )
        }
    }

    when (action) {
        is ButtonAction.OpenApp -> OutlinedTextField(
            value = action.packageName,
            onValueChange = { onChange(ButtonAction.OpenApp(it)) },
            label = { Text(stringResource(R.string.studio_action_package)) },
            modifier = Modifier.fillMaxWidth()
        )

        is ButtonAction.DeepLink -> OutlinedTextField(
            value = action.uri,
            onValueChange = { onChange(ButtonAction.DeepLink(it)) },
            label = { Text(stringResource(R.string.studio_action_uri)) },
            modifier = Modifier.fillMaxWidth()
        )

        is ButtonAction.NotificationAction -> NumberField(
            value = action.index,
            label = stringResource(R.string.studio_condition_action_index),
            onValueChange = { onChange(ButtonAction.NotificationAction(it.coerceIn(0, 3))) },
            modifier = Modifier.fillMaxWidth()
        )

        is ButtonAction.SmartAction -> Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SMART_ACTION_TYPES.forEach { type ->
                FilterChip(
                    selected = action.type.equals(type, ignoreCase = true),
                    onClick = { onChange(ButtonAction.SmartAction(type)) },
                    label = { Text(type) }
                )
            }
        }

        is ButtonAction.Broadcast -> {
            OutlinedTextField(
                value = action.action,
                onValueChange = { onChange(action.copy(action = it)) },
                label = { Text(stringResource(R.string.studio_action_broadcast)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = action.packageName.orEmpty(),
                onValueChange = { onChange(action.copy(packageName = it.ifBlank { null })) },
                label = { Text(stringResource(R.string.studio_action_package)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = action.extraKey.orEmpty(),
                    onValueChange = { onChange(action.copy(extraKey = it.ifBlank { null })) },
                    label = { Text(stringResource(R.string.studio_action_extra_key)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = action.extraValue.orEmpty(),
                    onValueChange = { onChange(action.copy(extraValue = it.ifBlank { null })) },
                    label = { Text(stringResource(R.string.studio_action_extra_value)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        else -> Unit
    }
}

@Composable
private fun StudioSection(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun LabelledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NumberField(
    value: Int,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text -> text.toIntOrNull()?.let(onValueChange) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun VariableTokenRow(onTokenSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VARIABLE_TOKENS.forEach { token ->
            AssistChip(onClick = { onTokenSelected(token) }, label = { Text(token) })
        }
    }
}

private val VARIABLE_TOKENS = listOf(
    "{notif.title}", "{notif.text}", "{notif.progress}", "{notif.package}", "{device.battery}", "{time.now}"
)

private val SMART_ACTION_TYPES = listOf("OTP", "URL", "PHONE", "TRACKING")

private enum class ConditionKind(val labelRes: Int) {
    ALWAYS(R.string.studio_condition_always),
    NOTIFICATION_ACTION(R.string.studio_condition_has_action),
    INLINE_REPLY(R.string.studio_condition_has_reply),
    SMART_ACTION(R.string.studio_condition_has_smart_action),
    PROGRESS(R.string.studio_condition_has_progress),
    NOT_BLANK(R.string.studio_condition_not_blank),
    MATCHES(R.string.studio_condition_matches);

    fun default(): NodeCondition = when (this) {
        ALWAYS -> NodeCondition.Always
        NOTIFICATION_ACTION -> NodeCondition.HasNotificationAction(0)
        INLINE_REPLY -> NodeCondition.HasInlineReply
        SMART_ACTION -> NodeCondition.HasSmartAction("OTP")
        PROGRESS -> NodeCondition.HasProgress
        NOT_BLANK -> NodeCondition.NotBlank("{notif.text}")
        MATCHES -> NodeCondition.Matches("{notif.title}", "")
    }

    companion object {
        fun of(condition: NodeCondition): ConditionKind = when (condition) {
            is NodeCondition.Always -> ALWAYS
            is NodeCondition.HasNotificationAction -> NOTIFICATION_ACTION
            is NodeCondition.HasInlineReply -> INLINE_REPLY
            is NodeCondition.HasSmartAction -> SMART_ACTION
            is NodeCondition.HasProgress -> PROGRESS
            is NodeCondition.NotBlank -> NOT_BLANK
            is NodeCondition.Matches -> MATCHES
            // `Not` can only be built by hand in a .hwidget file; the chips leave it alone.
            is NodeCondition.Not -> ALWAYS
        }
    }
}

private enum class ActionKind(val labelRes: Int) {
    DISMISS(R.string.studio_action_dismiss),
    NOTIFICATION_ACTION(R.string.studio_action_notification),
    INLINE_REPLY(R.string.studio_action_reply),
    SMART_ACTION(R.string.studio_action_smart),
    OPEN_APP(R.string.studio_action_open_app),
    DEEP_LINK(R.string.studio_action_deep_link),
    BROADCAST(R.string.studio_action_broadcast_kind);

    fun default(): ButtonAction = when (this) {
        DISMISS -> ButtonAction.Dismiss
        NOTIFICATION_ACTION -> ButtonAction.NotificationAction(0)
        INLINE_REPLY -> ButtonAction.InlineReply
        SMART_ACTION -> ButtonAction.SmartAction("OTP")
        OPEN_APP -> ButtonAction.OpenApp("")
        DEEP_LINK -> ButtonAction.DeepLink("")
        BROADCAST -> ButtonAction.Broadcast("")
    }

    companion object {
        fun of(action: ButtonAction): ActionKind = when (action) {
            is ButtonAction.Dismiss -> DISMISS
            is ButtonAction.NotificationAction -> NOTIFICATION_ACTION
            is ButtonAction.InlineReply -> INLINE_REPLY
            is ButtonAction.SmartAction -> SMART_ACTION
            is ButtonAction.OpenApp -> OPEN_APP
            is ButtonAction.DeepLink -> DEEP_LINK
            is ButtonAction.Broadcast -> BROADCAST
        }
    }
}

/** Typed copies, so the inspector can edit shared node properties without a `when` at every call. */
fun CustomWidgetNode.withBounds(bounds: NodeBounds): CustomWidgetNode = when (this) {
    is TextNode -> copy(bounds = bounds)
    is ImageNode -> copy(bounds = bounds)
    is ProgressNode -> copy(bounds = bounds)
    is ButtonNode -> copy(bounds = bounds)
    is LayoutContainer -> copy(bounds = bounds)
}

fun CustomWidgetNode.withShowIf(condition: NodeCondition): CustomWidgetNode = when (this) {
    is TextNode -> copy(showIf = condition)
    is ImageNode -> copy(showIf = condition)
    is ProgressNode -> copy(showIf = condition)
    is ButtonNode -> copy(showIf = condition)
    is LayoutContainer -> copy(showIf = condition)
}

fun CustomWidgetNode.withOnClick(action: ButtonAction?): CustomWidgetNode = when (this) {
    is TextNode -> copy(onClick = action)
    is ImageNode -> copy(onClick = action)
    is ProgressNode -> copy(onClick = action)
    is ButtonNode -> copy(action = action ?: ButtonAction.Dismiss)
    is LayoutContainer -> copy(onClick = action)
}
