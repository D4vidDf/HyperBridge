package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.widget.*
import com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor
import com.d4viddf.hyperbridge.ui.screens.translators.TRANSLATOR_OUTLINED_ICONS

/**
 * Tab-oriented KWGT-style Inspector for CustomWidgetNode elements.
 */
@Composable
fun StudioInspector(
    node: CustomWidgetNode,
    isRoot: Boolean,
    selectedTab: StudioTab,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (CustomWidgetNode) -> Unit,
    onMoveLayer: (Int) -> Unit,
    onDelete: () -> Unit,
    onSelectChild: (String) -> Unit = {},
    onAddChild: (String) -> Unit = {},
    onToggleLock: () -> Unit = {},
    modifier: Modifier = Modifier,
    // Root document settings (used when isRoot == true)
    document: CustomWidgetDocument? = null,
    notificationType: NotificationType? = null,
    targetScope: TargetScope = TargetScope.NOTIFICATION_TYPE,
    targetPackages: List<String> = emptyList(),
    boundTranslator: CustomTranslator? = null,
    onNameChange: ((String) -> Unit)? = null,
    onIconChange: ((String) -> Unit)? = null,
    onCanvasChange: ((CanvasSize) -> Unit)? = null,
    onNotificationTypeChange: ((NotificationType) -> Unit)? = null,
    onTargetScopeChange: ((TargetScope) -> Unit)? = null,
    onAddTargetPackage: ((String) -> Unit)? = null,
    onRemoveTargetPackage: ((String) -> Unit)? = null,
    // Stage 2 additions
    selectedNodeId: String? = null,
    hiddenNodeIds: Set<String> = emptySet(),
    onToggleVisibility: (String) -> Unit = {},
    onMoveNodeLayer: (String, Int) -> Unit = { _, _ -> },
    onMoveToFront: (String) -> Unit = {},
    onMoveToBack: (String) -> Unit = {},
    onDuplicate: (String) -> Unit = {},
    onGroup: (String) -> Unit = {},
    onUngroup: (String) -> Unit = {},
    onRename: (String, String) -> Unit = { _, _ -> },
    onDeleteNode: (String) -> Unit = {},
    onToggleNodeLock: (String) -> Unit = {},
    // Stage 4 additions
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    // Stage 5 additions
    onMoveInto: (String, String) -> Unit = { _, _ -> },
    onCreateTranslator: (() -> Unit)? = null,
    onUpdateGlobals: ((CustomWidgetGlobals) -> Unit)? = null,
    onApplyFontToAll: ((TextFontFamily) -> Unit)? = null
) {
    var editingFormulaPropKey by remember { mutableStateOf<String?>(null) }
    var isAppChooserOpen by remember { mutableStateOf(false) }
    var appChooserCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (selectedTab) {
            StudioTab.ITEMS -> {
                if (node is LayoutContainer) {
                    StudioLayersPanel(
                        rootContainer = node,
                        selectedNodeId = selectedNodeId ?: node.id,
                        hiddenNodeIds = hiddenNodeIds,
                        onSelectNode = onSelectChild,
                        onToggleLock = onToggleNodeLock,
                        onToggleVisibility = onToggleVisibility,
                        onMoveLayer = onMoveNodeLayer,
                        onMoveToFront = onMoveToFront,
                        onMoveToBack = onMoveToBack,
                        onDuplicate = onDuplicate,
                        onGroup = onGroup,
                        onUngroup = onUngroup,
                        onRename = onRename,
                        onDelete = onDeleteNode,
                        onAddElement = { onAddChild(it) },
                        onMoveInto = onMoveInto
                    )
                }
            }

            StudioTab.BACKGROUND -> {
                if (node is LayoutContainer) {
                    BackgroundTabContent(
                        container = node,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it }
                    )
                }
            }

            StudioTab.GLOBAL -> {
                if (document != null && onUpdateGlobals != null && onApplyFontToAll != null) {
                    GlobalTabContent(
                        document = document,
                        onUpdateGlobals = onUpdateGlobals,
                        onApplyFontToAll = onApplyFontToAll
                    )
                }
            }

            StudioTab.ITEM, StudioTab.INFO -> {
                if (node is ImageNode) {
                    ImageInfoTabContent(
                        node = node,
                        onChange = onChange
                    )
                } else {
                    ItemTabContent(
                        node = node,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it }
                    )
                }
            }

            StudioTab.EFX -> {
                if (node is TextNode) {
                    EfxTabContent(
                        node = node,
                        onChange = onChange
                    )
                } else if (node is ImageNode) {
                    ImageEfxTabContent(
                        node = node,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it }
                    )
                }
            }

            StudioTab.VISIBILITY -> {
                VisibilityTabContent(
                    node = node,
                    scenario = scenario,
                    onChange = onChange
                )
            }

            StudioTab.POSITION, StudioTab.LAYER -> {
                PositionTabContent(
                    node = node,
                    isRoot = isRoot,
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    scenario = scenario,
                    onChange = onChange,
                    onMoveLayer = onMoveLayer,
                    onMoveToFront = { onMoveToFront(node.id) },
                    onMoveToBack = { onMoveToBack(node.id) },
                    onDuplicate = { onDuplicate(node.id) },
                    onGroup = { onGroup(node.id) },
                    onDelete = onDelete,
                    onRequestFormulaEditor = { editingFormulaPropKey = it }
                )
            }

            StudioTab.PROGRESS -> {
                if (node is ProgressNode) {
                    ProgressTabContent(
                        node = node,
                        scenario = scenario,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it }
                    )
                }
            }

            StudioTab.STYLE -> {
                if (node is ProgressNode) {
                    ProgressStyleTabContent(
                        node = node,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it }
                    )
                }
            }

            StudioTab.COLORS -> {
                if (node is ProgressNode) {
                    ProgressColorTabContent(
                        node = node,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it }
                    )
                } else {
                    ColorsTabContent(
                        node = node,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it }
                    )
                }
            }

            StudioTab.VALUE, StudioTab.IMAGE -> {
                if (node is ProgressNode) {
                    ProgressTabContent(
                        node = node,
                        scenario = scenario,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it }
                    )
                } else {
                    ValueTabContent(
                        node = node,
                        scenario = scenario,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it },
                        onRequestAppChooser = { cb ->
                            appChooserCallback = cb
                            isAppChooserOpen = true
                        }
                    )
                }
            }

            StudioTab.CONTAINER -> {
                if (node is LayoutContainer) {
                    ContainerTabContent(
                        container = node,
                        onChange = onChange,
                        onRequestFormulaEditor = { editingFormulaPropKey = it }
                    )
                }
            }

            StudioTab.DESIGN -> {
                if (document != null && onNameChange != null && onCanvasChange != null) {
                    DesignTabContent(
                        document = document,
                        onNameChange = onNameChange,
                        onIconChange = onIconChange,
                        onCanvasChange = onCanvasChange
                    )
                }
            }

            StudioTab.SCOPE -> {
                ScopeTabContent(
                    boundTranslator = boundTranslator,
                    notificationType = notificationType,
                    targetScope = targetScope,
                    targetPackages = targetPackages,
                    onNotificationTypeChange = onNotificationTypeChange,
                    onTargetScopeChange = onTargetScopeChange,
                    onAddTargetPackage = onAddTargetPackage,
                    onRemoveTargetPackage = onRemoveTargetPackage,
                    onCreateTranslator = onCreateTranslator,
                    onRequestAppChooser = { cb ->
                        appChooserCallback = cb
                        isAppChooserOpen = true
                    }
                )
            }

            StudioTab.ACTIONS -> {
                ActionsTabContent(
                    node = node,
                    onChange = onChange,
                    onRequestAppChooser = { cb ->
                        appChooserCallback = cb
                        isAppChooserOpen = true
                    }
                )
            }

            StudioTab.BINDINGS -> {
                BindingsTabContent(
                    node = node,
                    onChange = onChange,
                    onRequestFormulaEditor = { editingFormulaPropKey = it }
                )
            }
        }
    }

    editingFormulaPropKey?.let { propKey ->
        StudioFormulaDialog(
            propertyName = propKey,
            initialFormula = node.bindings[propKey].orEmpty(),
            scenario = scenario,
            onDismiss = { editingFormulaPropKey = null },
            onApply = { newFormula ->
                onChange(node.withBinding(propKey, newFormula))
                editingFormulaPropKey = null
            }
        )
    }

    if (isAppChooserOpen) {
        StudioAppChooserDialog(
            onDismiss = {
                isAppChooserOpen = false
                appChooserCallback = null
            },
            onAppSelected = { pkg ->
                appChooserCallback?.invoke(pkg)
                isAppChooserOpen = false
                appChooserCallback = null
            }
        )
    }
}

@Composable
private fun ItemsTabContent(
    container: LayoutContainer,
    onSelectChild: (String) -> Unit,
    onAddChild: () -> Unit,
    onChange: (CustomWidgetNode) -> Unit
) {
    StudioSection(stringResource(R.string.studio_tab_items)) {
        if (container.children.isEmpty()) {
            Text(
                text = stringResource(R.string.studio_items_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                container.children.forEachIndexed { index, child ->
                    val canUp = index < container.children.lastIndex
                    val canDown = index > 0

                    ChildLayerCard(
                        child = child,
                        index = index,
                        canMoveUp = canUp,
                        canMoveDown = canDown,
                        onClick = { onSelectChild(child.id) },
                        onToggleLock = {
                            val updatedChild = child.withLocked(!child.locked)
                            val updatedList = container.children.toMutableList()
                            updatedList[index] = updatedChild
                            onChange(container.copy(children = updatedList))
                        },
                        onMove = { delta ->
                            val targetIndex = (index + delta).coerceIn(0, container.children.lastIndex)
                            if (targetIndex != index) {
                                val updatedList = container.children.toMutableList()
                                updatedList.add(targetIndex, updatedList.removeAt(index))
                                onChange(container.copy(children = updatedList))
                            }
                        },
                        onDelete = {
                            val updatedList = container.children.filter { it.id != child.id }
                            onChange(container.copy(children = updatedList))
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onAddChild,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.studio_items_add))
        }
    }
}

@Composable
private fun ChildLayerCard(
    child: CustomWidgetNode,
    index: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onToggleLock: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = nodeIcon(child),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.name ?: defaultNodeTitle(child),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#$index · ${child::class.simpleName?.removeSuffix("Node") ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onToggleLock, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (child.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = stringResource(R.string.studio_layer_locked),
                    modifier = Modifier.size(18.dp),
                    tint = if (child.locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { onMove(1) },
                enabled = canMoveUp,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = stringResource(R.string.studio_layer_forward),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onMove(-1) },
                enabled = canMoveDown,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = stringResource(R.string.studio_layer_back),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.studio_delete_element),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ItemTabContent(
    node: CustomWidgetNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit = {}
) {
    StudioSection(stringResource(R.string.studio_tab_info)) {
        OutlinedTextField(
            value = node.name.orEmpty(),
            onValueChange = { onChange(node.withName(it.ifBlank { null })) },
            label = { Text(stringResource(R.string.studio_layer_name)) },
            placeholder = { Text(defaultNodeTitle(node)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Title,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = nodeIcon(node),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = defaultNodeTitle(node),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "ID: ${node.id}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LabelledSwitch(
            label = stringResource(R.string.studio_layer_locked),
            checked = node.locked,
            onCheckedChange = { onChange(node.withLocked(it)) }
        )

        if (node is TextNode) {
            TextItemDetailsEditor(
                node = node,
                onChange = onChange,
                onRequestFormulaEditor = onRequestFormulaEditor
            )
        }
    }
}

@Composable
private fun TextItemDetailsEditor(
    node: TextNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = node.template,
            onValueChange = { onChange(node.copy(template = it)) },
            label = { Text(stringResource(R.string.studio_property_template)) },
            modifier = Modifier.fillMaxWidth()
        )
        VariableTokenRow { token -> onChange(node.copy(template = node.template + token)) }

        StudioStepper(
            label = stringResource(R.string.studio_property_font_size),
            value = node.fontSizeSp,
            onValueChange = { onChange(node.copy(fontSizeSp = it.coerceIn(6, 96))) },
            unitSuffix = "sp",
            min = 6,
            max = 96,
            boundFormula = node.bindings[BindableProperty.TEXT_FONT_SIZE.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.TEXT_FONT_SIZE.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.TEXT_FONT_SIZE.key) }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.studio_property_font_family),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextFontFamily.entries.forEach { family ->
                    FilterChip(
                        selected = node.fontFamily == family,
                        onClick = { onChange(node.copy(fontFamily = family)) },
                        label = {
                            Text(
                                text = stringResource(fontFamilyLabel(family)),
                                fontFamily = previewComposeFontFamily(family)
                            )
                        }
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.studio_text_sizing_type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextSizingType.entries.forEach { type ->
                    FilterChip(
                        selected = node.sizingType == type,
                        onClick = { onChange(node.copy(sizingType = type)) },
                        label = { Text(stringResource(sizingTypeLabel(type))) }
                    )
                }
            }
        }

        StudioStepper(
            label = stringResource(R.string.studio_text_box_width),
            value = node.boxWidthDp ?: node.bounds.widthDp ?: 100,
            onValueChange = { onChange(node.copy(boxWidthDp = it.coerceIn(20, 500))) },
            unitSuffix = "dp",
            min = 20,
            max = 500
        )

        StudioStepper(
            label = stringResource(R.string.studio_property_max_lines),
            value = node.maxLines,
            onValueChange = { onChange(node.copy(maxLines = it.coerceIn(1, 10))) },
            min = 1,
            max = 10
        )

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.studio_property_gravity),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = node.gravity == TextGravity.START,
                    onClick = { onChange(node.copy(gravity = TextGravity.START)) },
                    label = { Text(stringResource(R.string.studio_gravity_start)) }
                )
                FilterChip(
                    selected = node.gravity == TextGravity.CENTER,
                    onClick = { onChange(node.copy(gravity = TextGravity.CENTER)) },
                    label = { Text(stringResource(R.string.studio_gravity_center)) }
                )
                FilterChip(
                    selected = node.gravity == TextGravity.END,
                    onClick = { onChange(node.copy(gravity = TextGravity.END)) },
                    label = { Text(stringResource(R.string.studio_gravity_end)) }
                )
            }
        }

        LabelledSwitch(
            label = stringResource(R.string.studio_property_bold),
            checked = node.bold,
            onCheckedChange = { onChange(node.copy(bold = it)) }
        )

        LabelledSwitch(
            label = stringResource(R.string.studio_property_italic),
            checked = node.italic,
            onCheckedChange = { onChange(node.copy(italic = it)) }
        )
    }
}

@Composable
private fun ColorsTabContent(
    node: CustomWidgetNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit
) {
    StudioSection(stringResource(R.string.studio_tab_colors)) {
        when (node) {
            is ProgressNode -> {
                StudioColorField(
                    label = stringResource(R.string.studio_progress_color_progress),
                    colorHex = node.progressColorHex,
                    onColorHexChange = { onChange(node.copy(progressColorHex = it)) },
                    boundFormula = node.bindings[BindableProperty.PROGRESS_COLOR.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.PROGRESS_COLOR.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.PROGRESS_COLOR.key) }
                )

                Spacer(Modifier.height(4.dp))

                StudioColorField(
                    label = stringResource(R.string.studio_progress_color_track),
                    colorHex = node.trackColorHex,
                    onColorHexChange = { onChange(node.copy(trackColorHex = it)) },
                    boundFormula = node.bindings[BindableProperty.PROGRESS_TRACK_COLOR.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.PROGRESS_TRACK_COLOR.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.PROGRESS_TRACK_COLOR.key) }
                )
            }

            is TextNode -> {
                StudioColorField(
                    label = stringResource(R.string.studio_property_color),
                    colorHex = node.colorHex,
                    onColorHexChange = { onChange(node.copy(colorHex = it)) },
                    boundFormula = node.bindings[BindableProperty.TEXT_COLOR.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.TEXT_COLOR.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.TEXT_COLOR.key) }
                )

                Spacer(Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.studio_text_filter_mode),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextFilterMode.entries.forEach { mode ->
                            FilterChip(
                                selected = node.filterMode == mode,
                                onClick = { onChange(node.copy(filterMode = mode)) },
                                label = { Text(stringResource(filterModeLabel(mode))) }
                            )
                        }
                    }
                }
            }

            is ButtonNode -> {
                StudioColorField(
                    label = stringResource(R.string.studio_property_color),
                    colorHex = node.textColorHex,
                    onColorHexChange = { onChange(node.copy(textColorHex = it)) },
                    boundFormula = node.bindings[BindableProperty.BUTTON_TEXT_COLOR.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.BUTTON_TEXT_COLOR.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.BUTTON_TEXT_COLOR.key) }
                )

                Spacer(Modifier.height(4.dp))

                StudioColorField(
                    label = stringResource(R.string.studio_property_background),
                    colorHex = node.backgroundHex.orEmpty().ifBlank { "#333333" },
                    onColorHexChange = { onChange(node.copy(backgroundHex = it.ifBlank { null })) },
                    boundFormula = node.bindings[BindableProperty.BUTTON_BACKGROUND.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.BUTTON_BACKGROUND.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.BUTTON_BACKGROUND.key) }
                )
            }

            is ImageNode -> {
                StudioColorField(
                    label = stringResource(R.string.studio_property_tint),
                    colorHex = node.tintHex.orEmpty().ifBlank { "#FFFFFF" },
                    onColorHexChange = { onChange(node.copy(tintHex = it.ifBlank { null })) },
                    boundFormula = node.bindings[BindableProperty.IMAGE_TINT.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.IMAGE_TINT.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.IMAGE_TINT.key) }
                )
            }

            is ShapeNode -> {
                StudioColorField(
                    label = stringResource(R.string.studio_property_shape_fill),
                    colorHex = node.fillColorHex.orEmpty().ifBlank { "#33FFFFFF" },
                    onColorHexChange = { onChange(node.copy(fillColorHex = it.ifBlank { null })) },
                    boundFormula = node.bindings[BindableProperty.SHAPE_FILL.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.SHAPE_FILL.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.SHAPE_FILL.key) }
                )

                Spacer(Modifier.height(4.dp))

                StudioColorField(
                    label = stringResource(R.string.studio_property_shape_stroke),
                    colorHex = node.strokeColorHex.orEmpty().ifBlank { "#FFFFFF" },
                    onColorHexChange = { onChange(node.copy(strokeColorHex = it.ifBlank { null })) },
                    boundFormula = node.bindings[BindableProperty.SHAPE_STROKE.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.SHAPE_STROKE.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.SHAPE_STROKE.key) }
                )
            }

            is LayoutContainer -> {
                StudioColorField(
                    label = stringResource(R.string.studio_property_background),
                    colorHex = node.backgroundHex.orEmpty().ifBlank { if (node.id == "root") "#141414" else "#00000000" },
                    onColorHexChange = { onChange(node.copy(backgroundHex = it.ifBlank { null })) },
                    boundFormula = node.bindings[BindableProperty.CONTAINER_BACKGROUND.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.CONTAINER_BACKGROUND.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.CONTAINER_BACKGROUND.key) }
                )
            }
        }
    }
}

@Composable
private fun ValueTabContent(
    node: CustomWidgetNode,
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit,
    onRequestAppChooser: ((String) -> Unit) -> Unit
) {
    val title = if (node is ImageNode) stringResource(R.string.studio_tab_image) else stringResource(R.string.studio_tab_value)
    StudioSection(title) {
        when (node) {
            is ProgressNode -> {
                ProgressTabContent(
                    node = node,
                    scenario = scenario,
                    onChange = onChange,
                    onRequestFormulaEditor = onRequestFormulaEditor
                )
            }

            is TextNode -> {
                TextValueEditor(
                    node = node,
                    scenario = scenario,
                    onChange = onChange,
                    onRequestFormulaEditor = onRequestFormulaEditor
                )
            }

            is ButtonNode -> {
                ButtonValueEditor(
                    node = node,
                    scenario = scenario,
                    onChange = onChange
                )
            }

            is ImageNode -> {
                ImageNodeEditor(
                    node = node,
                    onChange = onChange,
                    onRequestFormulaEditor = onRequestFormulaEditor,
                    onRequestAppChooser = onRequestAppChooser
                )
            }

            is ShapeNode -> {
                ShapeNodeEditor(
                    node = node,
                    onChange = onChange,
                    onRequestFormulaEditor = onRequestFormulaEditor
                )
            }

            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressTabContent(
    node: ProgressNode,
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    StudioSection(stringResource(R.string.studio_tab_progress)) {
        Text(
            text = stringResource(R.string.studio_progress_linked_to),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        val isBattery = node.valueTemplate == "{device.battery}"
        val isNotif = node.valueTemplate == "{notif.progress}"
        val isMedia = node.valueTemplate == "{media.progress}"

        val linkedIcon = when {
            isBattery -> Icons.Rounded.BatteryChargingFull
            isNotif -> Icons.Rounded.Notifications
            isMedia -> Icons.Rounded.MusicNote
            else -> Icons.Rounded.Tune
        }

        val linkedTitle = when {
            isBattery -> stringResource(R.string.studio_progress_link_battery)
            isNotif -> stringResource(R.string.studio_progress_link_notif)
            isMedia -> stringResource(R.string.studio_progress_link_media)
            else -> stringResource(R.string.studio_progress_link_custom)
        }

        // Linked data summary box
        Card(
            onClick = { showBottomSheet = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = linkedIcon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = linkedTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = node.valueTemplate,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.studio_progress_select_source),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress Mode selector (Line, Circle, Divided, Wave)
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.studio_progress_mode),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressIndicatorMode.entries.forEach { mode ->
                val labelRes = when (mode) {
                    ProgressIndicatorMode.LINE -> R.string.studio_progress_mode_line
                    ProgressIndicatorMode.CIRCLE -> R.string.studio_progress_mode_circle
                    ProgressIndicatorMode.DIVIDED -> R.string.studio_progress_mode_divided
                    ProgressIndicatorMode.WAVE -> R.string.studio_progress_mode_wave
                }
                FilterChip(
                    selected = node.mode == mode,
                    onClick = {
                        val newStyle = if (mode == ProgressIndicatorMode.CIRCLE) ProgressStyle.RING else ProgressStyle.LINEAR
                        val currentH = node.bounds.heightDp ?: 8
                        val currentW = node.bounds.widthDp ?: 64
                        val newBounds = when (mode) {
                            ProgressIndicatorMode.CIRCLE -> {
                                if (currentH < 24 || currentW < 24) {
                                    node.bounds.copy(widthDp = maxOf(currentW, 36), heightDp = maxOf(currentH, 36))
                                } else node.bounds
                            }
                            ProgressIndicatorMode.WAVE -> {
                                if (currentH < 16) {
                                    node.bounds.copy(heightDp = 16)
                                } else node.bounds
                            }
                            else -> node.bounds
                        }
                        onChange(node.copy(mode = mode, style = newStyle, bounds = newBounds))
                    },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }

        // Live Resolved Value Preview Card
        val previewEngine = remember { WidgetVariableEngine() }
        val resolvedString = previewEngine.resolve(node.valueTemplate, scenario.toVariableContext())
        val resolvedValue = resolvedString.toFloatOrNull() ?: 0f
        val fraction = (resolvedValue / node.maxValue.coerceAtLeast(1)).coerceIn(0f, 1f)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.studio_progress_preview_label,
                            resolvedString.ifBlank { "0" },
                            node.maxValue,
                            (fraction * 100).toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                val previewColorList: List<Color> = when (node.colorMode) {
                    ProgressColorMode.FLAT -> listOf(safeParseColor(node.progressColorHex))
                    ProgressColorMode.GRADIENT -> listOf(safeParseColor(node.progressColorHex), safeParseColor(node.gradientEndColorHex))
                    ProgressColorMode.CURRENT -> {
                        val cur = when (node.currentSource) {
                            "notification" -> Color(0xFF38BDF8)
                            "media" -> Color(0xFFA855F7)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        listOf(cur)
                    }
                    ProgressColorMode.MULTICOLOR -> {
                        if (node.multiColorsHex.isNotEmpty()) {
                            node.multiColorsHex.map { safeParseColor(it) }
                        } else {
                            listOf(Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFFF44336))
                        }
                    }
                }
                val previewTrackColor = safeParseColor(node.trackColorHex)
                val previewBrush = if (previewColorList.size > 1) {
                    Brush.horizontalGradient(previewColorList)
                } else {
                    SolidColor(previewColorList.first())
                }
                val previewThumbColor = node.thumbColorHex?.let { safeParseColor(it) } ?: previewColorList.first()

                when (node.mode) {
                    ProgressIndicatorMode.CIRCLE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val strokeWidthPx = node.strokeWidthDp.dp.coerceAtLeast(2.dp)
                            Canvas(modifier = Modifier.size(48.dp)) {
                                val strokePx = strokeWidthPx.toPx()
                                val thumbRadiusPx = if (node.thumbType != ProgressIndicatorThumb.NONE) (node.thumbSizeDp.dp / 2f).toPx() else 0f
                                val arcRadius = (minOf(size.width, size.height) - maxOf(strokePx, thumbRadiusPx * 2f)) / 2f
                                val strokeCap = if (node.roundCaps) androidx.compose.ui.graphics.StrokeCap.Round else androidx.compose.ui.graphics.StrokeCap.Butt
                                drawArc(
                                    color = previewTrackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx, cap = strokeCap)
                                )
                                if (fraction > 0f) {
                                    val ringBrush = if (previewColorList.size > 1) {
                                        Brush.sweepGradient(previewColorList + previewColorList.first())
                                    } else {
                                        SolidColor(previewColorList.first())
                                    }
                                    drawArc(
                                        brush = ringBrush,
                                        startAngle = -90f,
                                        sweepAngle = fraction * 360f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx, cap = strokeCap)
                                    )
                                }
                                if (node.thumbType != ProgressIndicatorThumb.NONE) {
                                    val currentAngleDeg = -90f + fraction * 360f
                                    val angleRad = Math.toRadians(currentAngleDeg.toDouble())
                                    val cx = (size.width / 2f) + arcRadius * kotlin.math.cos(angleRad).toFloat()
                                    val cy = (size.height / 2f) + arcRadius * kotlin.math.sin(angleRad).toFloat()
                                    drawCircle(
                                        color = if (node.thumbType == ProgressIndicatorThumb.CUSTOM_PIC) Color(0xFF1E293B) else previewThumbColor,
                                        radius = thumbRadiusPx,
                                        center = Offset(cx, cy)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = thumbRadiusPx,
                                        center = Offset(cx, cy),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                    )
                                    if (node.thumbType == ProgressIndicatorThumb.CUSTOM_PIC) {
                                        drawCircle(
                                            color = previewThumbColor,
                                            radius = (thumbRadiusPx * 0.55f).coerceAtLeast(1f),
                                            center = Offset(cx, cy)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    ProgressIndicatorMode.DIVIDED -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(node.bounds.heightDp?.dp ?: 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val totalSegments = 10
                                val filledSegments = (fraction * totalSegments).toInt()
                                val segmentShape = if (node.roundCaps) RoundedCornerShape(2.dp) else androidx.compose.ui.graphics.RectangleShape
                                for (i in 0 until totalSegments) {
                                    val segColor = if (i < filledSegments) {
                                        when {
                                            previewColorList.size == 1 -> previewColorList.first()
                                            node.colorMode == ProgressColorMode.GRADIENT -> {
                                                val ratio = i.toFloat() / (totalSegments - 1).coerceAtLeast(1)
                                                lerp(previewColorList.first(), previewColorList.last(), ratio)
                                            }
                                            else -> previewColorList[i % previewColorList.size]
                                        }
                                    } else previewTrackColor
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(segmentShape)
                                            .background(segColor)
                                    )
                                }
                            }
                            if (node.thumbType != ProgressIndicatorThumb.NONE) {
                                val thumbSize = node.thumbSizeDp.dp
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val thumbRadiusPx = (thumbSize / 2f).toPx()
                                    val totalSegments = 10
                                    val filledSegments = (fraction * totalSegments).toInt()
                                    if (filledSegments > 0) {
                                        val spacingPx = 4.dp.toPx()
                                        val segW = (size.width - spacingPx * (totalSegments - 1)) / totalSegments
                                        val segRight = (filledSegments - 1) * (segW + spacingPx) + segW
                                        val cx = segRight.coerceIn(thumbRadiusPx, size.width - thumbRadiusPx)
                                        val cy = size.height / 2f
                                        drawCircle(
                                            color = if (node.thumbType == ProgressIndicatorThumb.CUSTOM_PIC) Color(0xFF1E293B) else previewThumbColor,
                                            radius = thumbRadiusPx,
                                            center = Offset(cx, cy)
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = thumbRadiusPx,
                                            center = Offset(cx, cy),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                        )
                                        if (node.thumbType == ProgressIndicatorThumb.CUSTOM_PIC) {
                                            drawCircle(
                                                color = previewThumbColor,
                                                radius = (thumbRadiusPx * 0.55f).coerceAtLeast(1f),
                                                center = Offset(cx, cy)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ProgressIndicatorMode.WAVE -> {
                        WavyProgressCanvas(
                            fraction = fraction,
                            progressBrush = previewBrush,
                            trackColor = previewTrackColor,
                            strokeWidth = node.strokeWidthDp.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(node.bounds.heightDp?.dp?.coerceAtLeast(12.dp) ?: 12.dp),
                            roundCaps = node.roundCaps,
                            thumbType = node.thumbType,
                            thumbSizeDp = node.thumbSizeDp,
                            thumbColor = previewThumbColor
                        )
                    }
                    ProgressIndicatorMode.LINE -> {
                        val barRadius = if (node.roundCaps) ((node.bounds.heightDp ?: 8).dp / 2f) else 0.dp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(node.bounds.heightDp?.dp ?: 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(barRadius))
                                    .background(previewTrackColor)
                            ) {
                                if (fraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction)
                                            .clip(RoundedCornerShape(barRadius))
                                            .background(previewBrush)
                                    )
                                }
                            }
                            if (node.thumbType != ProgressIndicatorThumb.NONE) {
                                val thumbSize = node.thumbSizeDp.dp
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val thumbRadiusPx = (thumbSize / 2f).toPx()
                                    val usableW = size.width - thumbRadiusPx * 2f
                                    val cx = if (usableW > 0) thumbRadiusPx + usableW * fraction else size.width * fraction
                                    val cy = size.height / 2f
                                    drawCircle(
                                        color = if (node.thumbType == ProgressIndicatorThumb.CUSTOM_PIC) Color(0xFF1E293B) else previewThumbColor,
                                        radius = thumbRadiusPx,
                                        center = Offset(cx, cy)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = thumbRadiusPx,
                                        center = Offset(cx, cy),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                    )
                                    if (node.thumbType == ProgressIndicatorThumb.CUSTOM_PIC) {
                                        drawCircle(
                                            color = previewThumbColor,
                                            radius = (thumbRadiusPx * 0.55f).coerceAtLeast(1f),
                                            center = Offset(cx, cy)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.studio_progress_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Battery option
                LinkedDataSheetItem(
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = stringResource(R.string.studio_progress_link_battery),
                    description = stringResource(R.string.studio_progress_link_battery_desc),
                    selected = node.valueTemplate == "{device.battery}",
                    onClick = {
                        onChange(node.copy(valueTemplate = "{device.battery}", maxValue = 100))
                        showBottomSheet = false
                    }
                )

                // Notification progress option
                LinkedDataSheetItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.studio_progress_link_notif),
                    description = stringResource(R.string.studio_progress_link_notif_desc),
                    selected = node.valueTemplate == "{notif.progress}",
                    onClick = {
                        onChange(node.copy(valueTemplate = "{notif.progress}", maxValue = 100))
                        showBottomSheet = false
                    }
                )

                // Media progress option
                LinkedDataSheetItem(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.studio_progress_link_media),
                    description = stringResource(R.string.studio_progress_link_media_desc),
                    selected = node.valueTemplate == "{media.progress}",
                    onClick = {
                        onChange(node.copy(valueTemplate = "{media.progress}", maxValue = 100))
                        showBottomSheet = false
                    }
                )

                // Custom template option
                LinkedDataSheetItem(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.studio_progress_link_custom),
                    description = stringResource(R.string.studio_progress_link_custom_desc),
                    selected = node.valueTemplate != "{device.battery}" && node.valueTemplate != "{notif.progress}" && node.valueTemplate != "{media.progress}",
                    onClick = {
                        showBottomSheet = false
                        onRequestFormulaEditor(BindableProperty.PROGRESS_VALUE.key)
                    }
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProgressColorTabContent(
    node: ProgressNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit
) {
    StudioSection(stringResource(R.string.studio_tab_colors)) {
        // Mode: plano, gradiente, actual, multicolor
        Text(
            text = stringResource(R.string.studio_progress_color_mode),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressColorMode.entries.forEach { mode ->
                val labelRes = when (mode) {
                    ProgressColorMode.FLAT -> R.string.studio_progress_color_mode_flat
                    ProgressColorMode.GRADIENT -> R.string.studio_progress_color_mode_gradient
                    ProgressColorMode.CURRENT -> R.string.studio_progress_color_mode_current
                    ProgressColorMode.MULTICOLOR -> R.string.studio_progress_color_mode_multicolor
                }
                FilterChip(
                    selected = node.colorMode == mode,
                    onClick = { onChange(node.copy(colorMode = mode)) },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Color mode specific controls
        when (node.colorMode) {
            ProgressColorMode.FLAT -> {
                StudioColorField(
                    label = stringResource(R.string.studio_progress_fg_color),
                    colorHex = node.progressColorHex,
                    onColorHexChange = { onChange(node.copy(progressColorHex = it)) },
                    boundFormula = node.bindings[BindableProperty.PROGRESS_COLOR.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.PROGRESS_COLOR.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.PROGRESS_COLOR.key) }
                )
            }
            ProgressColorMode.GRADIENT -> {
                StudioColorField(
                    label = stringResource(R.string.studio_progress_color_start),
                    colorHex = node.progressColorHex,
                    onColorHexChange = { onChange(node.copy(progressColorHex = it)) },
                    boundFormula = node.bindings[BindableProperty.PROGRESS_COLOR.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.PROGRESS_COLOR.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.PROGRESS_COLOR.key) }
                )
                Spacer(Modifier.height(4.dp))
                StudioColorField(
                    label = stringResource(R.string.studio_progress_color_end),
                    colorHex = node.gradientEndColorHex,
                    onColorHexChange = { onChange(node.copy(gradientEndColorHex = it)) },
                    boundFormula = node.bindings[BindableProperty.PROGRESS_GRADIENT_END_COLOR.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.PROGRESS_GRADIENT_END_COLOR.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.PROGRESS_GRADIENT_END_COLOR.key) }
                )
            }
            ProgressColorMode.CURRENT -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.studio_progress_current_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.studio_progress_current_source),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sources = listOf(
                        "system" to R.string.studio_progress_current_system,
                        "notification" to R.string.studio_progress_current_notif,
                        "media" to R.string.studio_progress_current_media
                    )
                    sources.forEach { (srcKey, srcLabelRes) ->
                        FilterChip(
                            selected = node.currentSource == srcKey,
                            onClick = { onChange(node.copy(currentSource = srcKey)) },
                            label = { Text(stringResource(srcLabelRes)) }
                        )
                    }
                }
            }
            ProgressColorMode.MULTICOLOR -> {
                MulticolorPaletteEditor(
                    colors = node.multiColorsHex,
                    onChange = { onChange(node.copy(multiColorsHex = it)) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // FndColor
        StudioColorField(
            label = stringResource(R.string.studio_progress_fnd_color),
            colorHex = node.trackColorHex,
            onColorHexChange = { onChange(node.copy(trackColorHex = it)) },
            boundFormula = node.bindings[BindableProperty.PROGRESS_TRACK_COLOR.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.PROGRESS_TRACK_COLOR.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.PROGRESS_TRACK_COLOR.key) }
        )

        Spacer(Modifier.height(4.dp))

        // Filter Mode
        Text(
            text = stringResource(R.string.studio_text_filter_mode),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextFilterMode.entries.forEach { filter ->
                FilterChip(
                    selected = node.filterMode == filter,
                    onClick = { onChange(node.copy(filterMode = filter)) },
                    label = { Text(stringResource(filterModeLabel(filter))) }
                )
            }
        }
    }
}

@Composable
private fun MulticolorPaletteEditor(
    colors: List<String>,
    onChange: (List<String>) -> Unit
) {
    var selectedIndex by remember(colors.size) {
        mutableIntStateOf(0.coerceAtMost((colors.size - 1).coerceAtLeast(0)))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.studio_progress_multicolor_palette),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        // Palette Swatches Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEachIndexed { index, colorHex ->
                val parsedColor = safeParseColor(colorHex)
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { selectedIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        val iconTint = if (parsedColor.luminance() > 0.5f) Color.Black else Color.White
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (colors.size < 8) {
                IconButton(
                    onClick = {
                        val defaultPalette = listOf("#4CAF50", "#FFEB3B", "#FF9800", "#F44336", "#2196F3", "#9C27B0", "#00BCD4", "#E91E63")
                        val nextColor = defaultPalette.firstOrNull { it !in colors } ?: "#00E5FF"
                        val updated = colors + nextColor
                        onChange(updated)
                        selectedIndex = updated.lastIndex
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.studio_progress_add_color),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Selected Color Editor
        if (selectedIndex in colors.indices) {
            val currentColor = colors[selectedIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StudioColorField(
                        label = "${stringResource(R.string.studio_progress_multicolor_palette)} #${selectedIndex + 1}",
                        colorHex = currentColor,
                        onColorHexChange = { newHex ->
                            val updated = colors.toMutableList()
                            updated[selectedIndex] = newHex
                            onChange(updated)
                        }
                    )
                }
                if (colors.size > 2) {
                    IconButton(
                        onClick = {
                            val updated = colors.toMutableList()
                            updated.removeAt(selectedIndex)
                            onChange(updated)
                            selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.studio_progress_remove_color),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                R.string.studio_progress_preset_traffic to listOf("#4CAF50", "#FFEB3B", "#FF9800", "#F44336"),
                R.string.studio_progress_preset_rainbow to listOf("#E91E63", "#9C27B0", "#2196F3", "#4CAF50", "#FFEB3B", "#FF9800"),
                R.string.studio_progress_preset_neon to listOf("#00F0FF", "#7000FF", "#FF007A", "#FFE600"),
                R.string.studio_progress_preset_sunset to listOf("#F72585", "#7209B7", "#3A0CA3", "#4361EE", "#4CC9F0")
            )

            presets.forEach { (labelRes, presetColors) ->
                SuggestionChip(
                    onClick = {
                        onChange(presetColors)
                        selectedIndex = 0
                    },
                    label = { Text(stringResource(labelRes)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ProgressStyleTabContent(
    node: ProgressNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit
) {
    StudioSection(stringResource(R.string.studio_tab_style)) {
        // Lineal or Circular style selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = node.style == ProgressStyle.LINEAR,
                onClick = {
                    val newMode = if (node.mode == ProgressIndicatorMode.CIRCLE) ProgressIndicatorMode.LINE else node.mode
                    onChange(node.copy(style = ProgressStyle.LINEAR, mode = newMode))
                },
                label = { Text(stringResource(R.string.studio_progress_style_lineal)) }
            )
            FilterChip(
                selected = node.style == ProgressStyle.RING,
                onClick = {
                    val currentH = node.bounds.heightDp ?: 8
                    val currentW = node.bounds.widthDp ?: 64
                    val newBounds = if (currentH < 24 || currentW < 24) {
                        node.bounds.copy(widthDp = maxOf(currentW, 36), heightDp = maxOf(currentH, 36))
                    } else node.bounds
                    onChange(node.copy(style = ProgressStyle.RING, mode = ProgressIndicatorMode.CIRCLE, bounds = newBounds))
                },
                label = { Text(stringResource(R.string.studio_progress_style_circular)) }
            )
        }

        // Size / Thickness
        StudioStepper(
            label = stringResource(R.string.studio_progress_stroke_size),
            value = node.strokeWidthDp,
            onValueChange = { onChange(node.copy(strokeWidthDp = it.coerceIn(1, 32))) },
            unitSuffix = "dp",
            min = 1,
            max = 32,
            boundFormula = node.bindings[BindableProperty.PROGRESS_STROKE_WIDTH.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.PROGRESS_STROKE_WIDTH.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.PROGRESS_STROKE_WIDTH.key) }
        )

        // Height
        StudioStepper(
            label = stringResource(R.string.studio_property_height),
            value = node.bounds.heightDp ?: 8,
            onValueChange = { onChange(node.withBounds(node.bounds.copy(heightDp = it.coerceAtLeast(2)))) },
            unitSuffix = "dp",
            min = 2,
            max = 200,
            boundFormula = node.bindings[BindableProperty.BOUNDS_HEIGHT.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.BOUNDS_HEIGHT.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.BOUNDS_HEIGHT.key) }
        )

        // Rounded ends (start and end)
        LabelledSwitch(
            label = stringResource(R.string.studio_progress_round_caps),
            checked = node.roundCaps,
            onCheckedChange = { onChange(node.copy(roundCaps = it)) }
        )

        // Position Indicator (Thumb)
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.studio_progress_indicator_header),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val thumbOptions = listOf(
                ProgressIndicatorThumb.NONE to stringResource(R.string.studio_progress_thumb_none),
                ProgressIndicatorThumb.ROUNDED to stringResource(R.string.studio_progress_thumb_rounded),
                ProgressIndicatorThumb.CUSTOM_PIC to stringResource(R.string.studio_progress_thumb_custom_pic)
            )
            thumbOptions.forEach { (thumbType, label) ->
                FilterChip(
                    selected = node.thumbType == thumbType,
                    onClick = {
                        onChange(
                            node.copy(
                                thumbType = thumbType,
                                thumbImageSource = if (thumbType == ProgressIndicatorThumb.CUSTOM_PIC && node.thumbImageSource == null) ImageSource.NotifMedia("album_art") else node.thumbImageSource
                            )
                        )
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (node.thumbType != ProgressIndicatorThumb.NONE) {
            // Thumb size
            StudioStepper(
                label = stringResource(R.string.studio_progress_thumb_size),
                value = node.thumbSizeDp,
                onValueChange = { onChange(node.copy(thumbSizeDp = it.coerceIn(4, 48))) },
                unitSuffix = "dp",
                min = 4,
                max = 48,
                boundFormula = node.bindings[BindableProperty.PROGRESS_THUMB_SIZE.key],
                onFormulaChange = { onChange(node.withBinding(BindableProperty.PROGRESS_THUMB_SIZE.key, it)) },
                onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.PROGRESS_THUMB_SIZE.key) }
            )

            if (node.thumbType == ProgressIndicatorThumb.ROUNDED) {
                // Thumb color
                StudioColorField(
                    label = stringResource(R.string.studio_progress_thumb_color),
                    colorHex = node.thumbColorHex ?: node.progressColorHex,
                    onColorHexChange = { onChange(node.copy(thumbColorHex = it)) },
                    boundFormula = node.bindings[BindableProperty.PROGRESS_THUMB_COLOR.key],
                    onFormulaChange = { onChange(node.withBinding(BindableProperty.PROGRESS_THUMB_COLOR.key, it)) },
                    onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.PROGRESS_THUMB_COLOR.key) }
                )
            } else if (node.thumbType == ProgressIndicatorThumb.CUSTOM_PIC) {
                // Preset source chips for Custom Pic
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.studio_progress_thumb_source),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                val picPresets = listOf<Pair<ImageSource, String>>(
                    ImageSource.NotifMedia("album_art") to stringResource(R.string.studio_image_notif_media_album_art),
                    ImageSource.NotifMedia("avatar") to stringResource(R.string.studio_image_notif_media_avatar),
                    ImageSource.NotifMedia("picture") to stringResource(R.string.studio_image_notif_media_picture),
                    ImageSource.NotifMedia("small_icon") to stringResource(R.string.studio_image_notif_media_small_icon),
                    ImageSource.AppIconOf("{notif.package}") to stringResource(R.string.studio_image_source_app),
                    ImageSource.SystemGlyph("music") to stringResource(R.string.studio_image_source_glyph)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    picPresets.forEach { (src, label) ->
                        val isSelected = when (src) {
                            is ImageSource.SystemGlyph -> node.thumbImageSource is ImageSource.SystemGlyph
                            else -> node.thumbImageSource == src
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onChange(node.copy(thumbImageSource = src)) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedDataSheetItem(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TextValueEditor(
    node: TextNode,
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = node.template,
            onValueChange = { onChange(node.copy(template = it)) },
            label = { Text(stringResource(R.string.studio_property_template)) },
            modifier = Modifier.fillMaxWidth()
        )
        VariableTokenRow { token -> onChange(node.copy(template = node.template + token)) }

        StudioStepper(
            label = stringResource(R.string.studio_property_font_size),
            value = node.fontSizeSp,
            onValueChange = { onChange(node.copy(fontSizeSp = it.coerceIn(6, 96))) },
            unitSuffix = "sp",
            min = 6,
            max = 96,
            boundFormula = node.bindings[BindableProperty.TEXT_FONT_SIZE.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.TEXT_FONT_SIZE.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.TEXT_FONT_SIZE.key) }
        )

        StudioStepper(
            label = stringResource(R.string.studio_property_max_lines),
            value = node.maxLines,
            onValueChange = { onChange(node.copy(maxLines = it.coerceIn(1, 10))) },
            min = 1,
            max = 10
        )

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.studio_property_gravity),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = node.gravity == TextGravity.START,
                    onClick = { onChange(node.copy(gravity = TextGravity.START)) },
                    label = { Text(stringResource(R.string.studio_gravity_start)) }
                )
                FilterChip(
                    selected = node.gravity == TextGravity.CENTER,
                    onClick = { onChange(node.copy(gravity = TextGravity.CENTER)) },
                    label = { Text(stringResource(R.string.studio_gravity_center)) }
                )
                FilterChip(
                    selected = node.gravity == TextGravity.END,
                    onClick = { onChange(node.copy(gravity = TextGravity.END)) },
                    label = { Text(stringResource(R.string.studio_gravity_end)) }
                )
            }
        }

        LabelledSwitch(
            label = stringResource(R.string.studio_property_bold),
            checked = node.bold,
            onCheckedChange = { onChange(node.copy(bold = it)) }
        )

        LabelledSwitch(
            label = stringResource(R.string.studio_property_italic),
            checked = node.italic,
            onCheckedChange = { onChange(node.copy(italic = it)) }
        )
    }
}

@Composable
private fun ButtonValueEditor(
    node: ButtonNode,
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    onChange: (CustomWidgetNode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = node.label,
            onValueChange = { onChange(node.copy(label = it)) },
            label = { Text(stringResource(R.string.studio_property_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        VariableTokenRow { token -> onChange(node.copy(label = node.label + token)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageNodeEditor(
    node: ImageNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit,
    onRequestAppChooser: ((String) -> Unit) -> Unit
) {
    var showSourceSheet by remember { mutableStateOf(false) }
    var showSubtypeSheet by remember { mutableStateOf(false) }

    val currentSource = node.source
    val selectedKind = ImageSourceKind.of(currentSource)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Source selector header & card
        Text(
            text = stringResource(R.string.studio_image_source),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            onClick = { showSourceSheet = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = selectedKind.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(selectedKind.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(selectedKind.descRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showSourceSheet = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.studio_image_change_source))
                }
            }
        }

        // 2. Subtype Card (if current source kind supports subtypes)
        when (currentSource) {
            is ImageSource.NotifMedia -> {
                val currentSubtype = NOTIF_MEDIA_SUBTYPES.find { it.key == currentSource.mediaType }
                    ?: NOTIF_MEDIA_SUBTYPES.first()

                Text(
                    text = stringResource(R.string.studio_image_subtype_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    onClick = { showSubtypeSheet = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = currentSubtype.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(currentSubtype.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(currentSubtype.descRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showSubtypeSheet = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.studio_image_change_subtype))
                        }
                    }
                }
            }

            is ImageSource.SystemGlyph -> {
                Text(
                    text = stringResource(R.string.studio_image_subtype_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    onClick = { showSubtypeSheet = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = studioGlyphIcon(currentSource.glyphName),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSource.glyphName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = stringResource(R.string.studio_image_source_glyph_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showSubtypeSheet = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.studio_image_change_subtype))
                        }
                    }
                }

                OutlinedTextField(
                    value = currentSource.glyphName,
                    onValueChange = { onChange(node.copy(source = ImageSource.SystemGlyph(it))) },
                    label = { Text(stringResource(R.string.studio_image_source_glyph)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_GLYPHS.forEach { glyph ->
                        AssistChip(
                            onClick = { onChange(node.copy(source = ImageSource.SystemGlyph(glyph))) },
                            label = { Text(glyph) }
                        )
                    }
                }
            }

            is ImageSource.AppIconOf -> {
                val isDynamic = currentSource.packageTemplate == "{notif.package}"

                Text(
                    text = stringResource(R.string.studio_image_subtype_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    onClick = { showSubtypeSheet = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Android,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDynamic) {
                                    stringResource(R.string.studio_image_app_dynamic)
                                } else {
                                    currentSource.packageTemplate
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isDynamic) {
                                    stringResource(R.string.studio_image_app_dynamic_desc)
                                } else {
                                    stringResource(R.string.studio_image_app_specific_desc)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showSubtypeSheet = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.studio_image_change_subtype))
                        }
                    }
                }

                OutlinedTextField(
                    value = currentSource.packageTemplate,
                    onValueChange = { onChange(node.copy(source = ImageSource.AppIconOf(it))) },
                    label = { Text(stringResource(R.string.studio_action_package)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { onChange(node.copy(source = ImageSource.AppIconOf("{notif.package}"))) },
                        label = { Text("{notif.package}", fontFamily = FontFamily.Monospace) }
                    )
                    OutlinedButton(
                        onClick = {
                            onRequestAppChooser { selectedPkg ->
                                onChange(node.copy(source = ImageSource.AppIconOf(selectedPkg)))
                            }
                        }
                    ) {
                        Text(stringResource(R.string.studio_image_choose_app))
                    }
                }
            }

            is ImageSource.SourceIcon -> {
                Text(
                    text = stringResource(R.string.studio_image_subtype_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    onClick = { showSubtypeSheet = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSource.sourceId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.studio_image_source_source_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showSubtypeSheet = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.studio_image_change_subtype))
                        }
                    }
                }

                OutlinedTextField(
                    value = currentSource.sourceId,
                    onValueChange = { onChange(node.copy(source = ImageSource.SourceIcon(it))) },
                    label = { Text(stringResource(R.string.studio_image_source_source)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("weather", "music", "system").forEach { src ->
                        AssistChip(
                            onClick = { onChange(node.copy(source = ImageSource.SourceIcon(src))) },
                            label = { Text(src) }
                        )
                    }
                }
            }

            is ImageSource.ContactAvatarOf -> {
                OutlinedTextField(
                    value = currentSource.numberOrNameTemplate,
                    onValueChange = { onChange(node.copy(source = ImageSource.ContactAvatarOf(it))) },
                    label = { Text(stringResource(R.string.studio_image_source_contact)) },
                    modifier = Modifier.fillMaxWidth()
                )
                VariableTokenRow { token ->
                    onChange(node.copy(source = ImageSource.ContactAvatarOf(currentSource.numberOrNameTemplate + token)))
                }
            }

            is ImageSource.CustomAsset -> {
                OutlinedTextField(
                    value = currentSource.fileName,
                    onValueChange = { onChange(node.copy(source = ImageSource.CustomAsset(it))) },
                    label = { Text(stringResource(R.string.studio_image_source_asset)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // 2. Mode (Bitmap / SVG)
        Text(
            text = stringResource(R.string.studio_image_mode),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = node.mode == ImageMode.BITMAP,
                onClick = { onChange(node.copy(mode = ImageMode.BITMAP)) },
                label = { Text(stringResource(R.string.studio_image_mode_bitmap)) }
            )
            FilterChip(
                selected = node.mode == ImageMode.SVG,
                onClick = { onChange(node.copy(mode = ImageMode.SVG)) },
                label = { Text(stringResource(R.string.studio_image_mode_svg)) }
            )
        }

        Spacer(Modifier.height(4.dp))

        // 3. Shape & Custom Rounded Borders
        Text(
            text = stringResource(R.string.studio_image_shape),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        val shapes = listOf("circle", "square", "rounded", "cookie", "arch", "clover8")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            shapes.forEach { shapeId ->
                val isSelected = node.shapeId.equals(shapeId, ignoreCase = true)
                @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                val shape = when (shapeId) {
                    "rectangle", "square" -> androidx.compose.ui.graphics.RectangleShape
                    "rounded", "rounded_rect" -> RoundedCornerShape(node.cornerRadiusDp.dp)
                    "circle", "ellipse" -> CircleShape
                    else -> getShapeFromId(shapeId).toShape()
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(shape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = shape
                        )
                        .clickable { onChange(node.copy(shapeId = shapeId)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (shapeId == "rounded") "RD" else shapeId.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (node.shapeId == "rounded" || node.shapeId == "rounded_rect") {
            StudioStepper(
                label = stringResource(R.string.studio_property_shape_corner_radius),
                value = node.cornerRadiusDp,
                onValueChange = { onChange(node.copy(cornerRadiusDp = it.coerceIn(0, 100))) },
                unitSuffix = "dp",
                min = 0,
                max = 100
            )
        }

        Spacer(Modifier.height(4.dp))

        // 3. Escalado (Scale Type)
        Text(
            text = stringResource(R.string.studio_image_scale),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = node.scaleType == ImageScaleType.FIT_WIDTH,
                onClick = { onChange(node.copy(scaleType = ImageScaleType.FIT_WIDTH)) },
                label = { Text(stringResource(R.string.studio_image_scale_fit_width)) }
            )
            FilterChip(
                selected = node.scaleType == ImageScaleType.FIT_HEIGHT,
                onClick = { onChange(node.copy(scaleType = ImageScaleType.FIT_HEIGHT)) },
                label = { Text(stringResource(R.string.studio_image_scale_fit_height)) }
            )
            FilterChip(
                selected = node.scaleType == ImageScaleType.FIT_CENTER,
                onClick = { onChange(node.copy(scaleType = ImageScaleType.FIT_CENTER)) },
                label = { Text(stringResource(R.string.studio_image_scale_fit_center)) }
            )
            FilterChip(
                selected = node.scaleType == ImageScaleType.CENTER_CROP,
                onClick = { onChange(node.copy(scaleType = ImageScaleType.CENTER_CROP)) },
                label = { Text(stringResource(R.string.studio_image_scale_center_crop)) }
            )
        }
    }

    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.studio_image_select_source),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                ImageSourceKind.entries.forEach { kind ->
                    val isSelected = selectedKind == kind
                    Card(
                        onClick = {
                            if (!isSelected) {
                                onChange(node.copy(source = kind.defaultSource()))
                            }
                            showSourceSheet = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = kind.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(kind.labelRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(kind.descRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        onChange(node.copy(source = kind.defaultSource()))
                                    }
                                    showSourceSheet = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showSubtypeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSubtypeSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.studio_image_select_subtype),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                when (currentSource) {
                    is ImageSource.NotifMedia -> {
                        NOTIF_MEDIA_SUBTYPES.forEach { subtype ->
                            val isSelected = currentSource.mediaType == subtype.key
                            Card(
                                onClick = {
                                    onChange(node.copy(source = ImageSource.NotifMedia(subtype.key)))
                                    showSubtypeSheet = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = subtype.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(subtype.titleRes),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(subtype.descRes),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            onChange(node.copy(source = ImageSource.NotifMedia(subtype.key)))
                                            showSubtypeSheet = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is ImageSource.SystemGlyph -> {
                        QUICK_GLYPHS.chunked(3).forEach { rowGlyphs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowGlyphs.forEach { glyph ->
                                    val isSelected = currentSource.glyphName == glyph
                                    Card(
                                        onClick = {
                                            onChange(node.copy(source = ImageSource.SystemGlyph(glyph)))
                                            showSubtypeSheet = false
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainer
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = studioGlyphIcon(glyph),
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = glyph,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is ImageSource.AppIconOf -> {
                        val isDynamic = currentSource.packageTemplate == "{notif.package}"
                        Card(
                            onClick = {
                                onChange(node.copy(source = ImageSource.AppIconOf("{notif.package}")))
                                showSubtypeSheet = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDynamic) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Notifications, null)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.studio_image_app_dynamic),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.studio_image_app_dynamic_desc),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                RadioButton(selected = isDynamic, onClick = {
                                    onChange(node.copy(source = ImageSource.AppIconOf("{notif.package}")))
                                    showSubtypeSheet = false
                                })
                            }
                        }

                        Card(
                            onClick = {
                                showSubtypeSheet = false
                                onRequestAppChooser { selectedPkg ->
                                    onChange(node.copy(source = ImageSource.AppIconOf(selectedPkg)))
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isDynamic) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Android, null)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.studio_image_app_specific),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.studio_image_app_specific_desc),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                RadioButton(selected = !isDynamic, onClick = {
                                    showSubtypeSheet = false
                                    onRequestAppChooser { selectedPkg ->
                                        onChange(node.copy(source = ImageSource.AppIconOf(selectedPkg)))
                                    }
                                })
                            }
                        }
                    }

                    is ImageSource.SourceIcon -> {
                        val presets = listOf(
                            "weather" to R.string.studio_image_source_weather,
                            "music" to R.string.studio_image_source_music,
                            "system" to R.string.studio_image_source_system
                        )
                        presets.forEach { (src, label) ->
                            val isSelected = currentSource.sourceId == src
                            Card(
                                onClick = {
                                    onChange(node.copy(source = ImageSource.SourceIcon(src)))
                                    showSubtypeSheet = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Widgets, null)
                                    Text(
                                        text = stringResource(label),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    RadioButton(selected = isSelected, onClick = {
                                        onChange(node.copy(source = ImageSource.SourceIcon(src)))
                                        showSubtypeSheet = false
                                    })
                                }
                            }
                        }
                    }

                    else -> {}
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ShapeNodeEditor(
    node: ShapeNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.studio_property_shape_geometry),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        val shapes = listOf("circle", "rounded", "rectangle", "cookie", "arch", "clover8")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            shapes.forEach { shapeId ->
                val isSelected = node.shapeId == shapeId
                val label = when (shapeId) {
                    "circle" -> stringResource(R.string.studio_shape_circle)
                    "rounded" -> stringResource(R.string.studio_shape_rounded)
                    "rectangle" -> stringResource(R.string.studio_shape_rectangle)
                    "cookie" -> stringResource(R.string.studio_shape_cookie)
                    "arch" -> stringResource(R.string.studio_shape_arch)
                    "clover8" -> stringResource(R.string.studio_shape_clover)
                    else -> shapeId
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onChange(node.copy(shapeId = shapeId)) },
                    label = { Text(label) }
                )
            }
        }

        if (node.shapeId == "rounded" || node.shapeId == "rounded_rect") {
            StudioStepper(
                label = stringResource(R.string.studio_property_shape_corner_radius),
                value = node.cornerRadiusDp,
                onValueChange = { onChange(node.copy(cornerRadiusDp = it.coerceIn(0, 100))) },
                unitSuffix = "dp",
                min = 0,
                max = 100
            )
        }

        // Fill color
        StudioColorField(
            label = stringResource(R.string.studio_property_shape_fill),
            colorHex = node.fillColorHex.orEmpty().ifBlank { "#33FFFFFF" },
            onColorHexChange = { onChange(node.copy(fillColorHex = it.ifBlank { null })) },
            boundFormula = node.bindings[BindableProperty.SHAPE_FILL.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.SHAPE_FILL.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.SHAPE_FILL.key) }
        )

        // Stroke color
        StudioColorField(
            label = stringResource(R.string.studio_property_shape_stroke),
            colorHex = node.strokeColorHex.orEmpty().ifBlank { "#FFFFFF" },
            onColorHexChange = { onChange(node.copy(strokeColorHex = it.ifBlank { null })) },
            boundFormula = node.bindings[BindableProperty.SHAPE_STROKE.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.SHAPE_STROKE.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.SHAPE_STROKE.key) }
        )

        // Stroke width
        StudioStepper(
            label = stringResource(R.string.studio_property_shape_stroke_width),
            value = node.strokeWidthDp,
            onValueChange = { onChange(node.copy(strokeWidthDp = it.coerceIn(0, 32))) },
            unitSuffix = "dp",
            min = 0,
            max = 32,
            boundFormula = node.bindings[BindableProperty.SHAPE_STROKE_WIDTH.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.SHAPE_STROKE_WIDTH.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.SHAPE_STROKE_WIDTH.key) }
        )
    }
}

@Composable
private fun PositionTabContent(
    node: CustomWidgetNode,
    isRoot: Boolean,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    onChange: (CustomWidgetNode) -> Unit,
    onMoveLayer: (Int) -> Unit = {},
    onMoveToFront: () -> Unit = {},
    onMoveToBack: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onGroup: () -> Unit = {},
    onDelete: () -> Unit,
    onRequestFormulaEditor: (String) -> Unit = {}
) {
    StudioSection(stringResource(R.string.studio_tab_position)) {
        StudioStepper(
            label = stringResource(R.string.studio_property_opacity),
            value = (node.opacity * 100).toInt(),
            onValueChange = { onChange(node.withOpacity(it.coerceIn(0, 100) / 100f)) },
            unitSuffix = "%",
            min = 0,
            max = 100,
            boundFormula = node.bindings[BindableProperty.OPACITY.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.OPACITY.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.OPACITY.key) }
        )

        if (!isRoot) {
            StudioStepper(
                label = stringResource(R.string.studio_property_x),
                value = node.bounds.x,
                onValueChange = { onChange(node.withBounds(node.bounds.copy(x = it))) },
                unitSuffix = "dp",
                min = -1000,
                max = 1000
            )

            StudioStepper(
                label = stringResource(R.string.studio_property_y),
                value = node.bounds.y,
                onValueChange = { onChange(node.withBounds(node.bounds.copy(y = it))) },
                unitSuffix = "dp",
                min = -1000,
                max = 1000
            )

            StudioStepper(
                label = stringResource(R.string.studio_property_width),
                value = node.bounds.widthDp ?: 0,
                onValueChange = { onChange(node.withBounds(node.bounds.copy(widthDp = it.takeIf { v -> v > 0 }))) },
                unitSuffix = "dp",
                min = 0,
                max = 1000,
                boundFormula = node.bindings[BindableProperty.BOUNDS_WIDTH.key],
                onFormulaChange = { onChange(node.withBinding(BindableProperty.BOUNDS_WIDTH.key, it)) },
                onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.BOUNDS_WIDTH.key) }
            )

            StudioStepper(
                label = stringResource(R.string.studio_property_height),
                value = node.bounds.heightDp ?: 0,
                onValueChange = { onChange(node.withBounds(node.bounds.copy(heightDp = it.takeIf { v -> v > 0 }))) },
                unitSuffix = "dp",
                min = 0,
                max = 1000,
                boundFormula = node.bindings[BindableProperty.BOUNDS_HEIGHT.key],
                onFormulaChange = { onChange(node.withBinding(BindableProperty.BOUNDS_HEIGHT.key, it)) },
                onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.BOUNDS_HEIGHT.key) }
            )

            StudioStepper(
                label = stringResource(R.string.studio_property_rotation),
                value = node.bounds.rotation.toInt(),
                onValueChange = { onChange(node.withBounds(node.bounds.copy(rotation = it.toFloat()))) },
                unitSuffix = "°",
                min = 0,
                max = 360,
                boundFormula = node.bindings[BindableProperty.ROTATION.key],
                onFormulaChange = { onChange(node.withBinding(BindableProperty.ROTATION.key, it)) },
                onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.ROTATION.key) }
            )

            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Rounded.Delete, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.studio_delete_element))
            }
        }
    }
}

@Composable
private fun VisibilityTabContent(
    node: CustomWidgetNode,
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    onChange: (CustomWidgetNode) -> Unit
) {
    StudioSection(stringResource(R.string.studio_tab_visibility)) {
        ConditionEditor(
            condition = node.showIf,
            scenario = scenario,
            onChange = { onChange(node.withShowIf(it)) }
        )
    }
}

@Composable
private fun ImageEfxTabContent(
    node: ImageNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit
) {
    StudioSection(stringResource(R.string.studio_tab_efx)) {
        StudioStepper(
            label = stringResource(R.string.studio_property_opacity),
            value = (node.opacity * 100).toInt(),
            onValueChange = { onChange(node.copy(opacity = it.coerceIn(0, 100) / 100f)) },
            unitSuffix = "%",
            min = 0,
            max = 100,
            boundFormula = node.bindings[BindableProperty.OPACITY.key],
            onFormulaChange = { onChange(node.withBinding(BindableProperty.OPACITY.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.OPACITY.key) }
        )

        LabelledSwitch(
            label = stringResource(R.string.studio_image_tint_enable),
            checked = node.tintEnabled,
            onCheckedChange = { isEnabled ->
                onChange(
                    node.copy(
                        tintEnabled = isEnabled,
                        tintHex = if (isEnabled && node.tintHex.isNullOrBlank()) "#FFFFFF" else node.tintHex
                    )
                )
            }
        )

        if (node.tintEnabled) {
            StudioColorField(
                label = stringResource(R.string.studio_property_tint),
                colorHex = node.tintHex.orEmpty().ifBlank { "#FFFFFF" },
                onColorHexChange = { onChange(node.copy(tintHex = it.ifBlank { null })) },
                boundFormula = node.bindings[BindableProperty.IMAGE_TINT.key],
                onFormulaChange = { onChange(node.withBinding(BindableProperty.IMAGE_TINT.key, it)) },
                onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.IMAGE_TINT.key) }
            )
        }

        Text(
            text = stringResource(R.string.studio_image_filter),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextFilterMode.entries.forEach { mode ->
                FilterChip(
                    selected = node.filterMode == mode,
                    onClick = { onChange(node.copy(filterMode = mode)) },
                    label = { Text(stringResource(filterModeLabel(mode))) }
                )
            }
        }

        StudioStepper(
            label = stringResource(R.string.studio_image_blur),
            value = node.blurRadius,
            onValueChange = { onChange(node.copy(blurRadius = it.coerceIn(0, 100))) },
            unitSuffix = "dp",
            min = 0,
            max = 100
        )

        StudioStepper(
            label = stringResource(R.string.studio_image_attenuation),
            value = node.attenuation,
            onValueChange = { onChange(node.copy(attenuation = it.coerceIn(0, 100))) },
            unitSuffix = "%",
            min = 0,
            max = 100
        )
    }
}

@Composable
private fun ImageInfoTabContent(
    node: ImageNode,
    onChange: (CustomWidgetNode) -> Unit
) {
    StudioSection(stringResource(R.string.studio_tab_info)) {
        OutlinedTextField(
            value = node.name.orEmpty(),
            onValueChange = { onChange(node.copy(name = it.ifBlank { null })) },
            label = { Text(stringResource(R.string.studio_layer_name)) },
            placeholder = { Text(defaultNodeTitle(node)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Title,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = nodeIcon(node),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = defaultNodeTitle(node),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "ID: ${node.id}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LabelledSwitch(
            label = stringResource(R.string.studio_layer_locked),
            checked = node.locked,
            onCheckedChange = { onChange(node.copy(locked = it)) }
        )
    }
}

@Composable
private fun LayerTabContent(
    node: CustomWidgetNode,
    isRoot: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    onChange: (CustomWidgetNode) -> Unit,
    onMoveLayer: (Int) -> Unit,
    onMoveToFront: () -> Unit = {},
    onMoveToBack: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onGroup: () -> Unit = {},
    onDelete: () -> Unit,
    onRequestFormulaEditor: (String) -> Unit = {}
) = PositionTabContent(
    node = node,
    isRoot = isRoot,
    canMoveUp = canMoveUp,
    canMoveDown = canMoveDown,
    scenario = scenario,
    onChange = onChange,
    onMoveLayer = onMoveLayer,
    onMoveToFront = onMoveToFront,
    onMoveToBack = onMoveToBack,
    onDuplicate = onDuplicate,
    onGroup = onGroup,
    onDelete = onDelete,
    onRequestFormulaEditor = onRequestFormulaEditor
)

@Composable
private fun EfxTabContent(
    node: TextNode,
    onChange: (CustomWidgetNode) -> Unit
) {
    StudioSection(stringResource(R.string.studio_tab_efx)) {
        // --- 1. MASK ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.studio_efx_mask),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = node.efx.mask == TextMaskType.NONE,
                        onClick = { onChange(node.copy(efx = node.efx.copy(mask = TextMaskType.NONE))) },
                        label = { Text(stringResource(R.string.studio_efx_mask_none)) }
                    )
                    FilterChip(
                        selected = node.efx.mask == TextMaskType.BLUR_BACKGROUND,
                        onClick = { onChange(node.copy(efx = node.efx.copy(mask = TextMaskType.BLUR_BACKGROUND))) },
                        label = { Text(stringResource(R.string.studio_efx_mask_blur_bg)) }
                    )
                }

                if (node.efx.mask == TextMaskType.BLUR_BACKGROUND) {
                    StudioStepper(
                        label = stringResource(R.string.studio_efx_mask_blur),
                        value = node.efx.maskBlurRadius,
                        onValueChange = { onChange(node.copy(efx = node.efx.copy(maskBlurRadius = it.coerceIn(1, 100)))) },
                        min = 1,
                        max = 100
                    )
                    StudioStepper(
                        label = stringResource(R.string.studio_efx_mask_attenuation),
                        value = node.efx.maskAttenuation,
                        onValueChange = { onChange(node.copy(efx = node.efx.copy(maskAttenuation = it.coerceIn(0, 100)))) },
                        unitSuffix = "%",
                        min = 0,
                        max = 100
                    )
                }
            }
        }

        // --- 2. TEXTURE ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.studio_efx_texture),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    if (uri != null) {
                        onChange(node.copy(efx = node.efx.copy(textureBitmapUri = uri.toString())))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextTextureType.entries.forEach { type ->
                        FilterChip(
                            selected = node.efx.texture == type,
                            onClick = {
                                val updatedColor = if (type != TextTextureType.NONE && node.efx.textureColorHex == null) {
                                    "#FF5722"
                                } else {
                                    node.efx.textureColorHex
                                }
                                onChange(node.copy(efx = node.efx.copy(texture = type, textureColorHex = updatedColor)))
                            },
                            label = { Text(stringResource(textureTypeLabel(type))) }
                        )
                    }
                }

                if (node.efx.texture != TextTextureType.NONE) {
                    val previewStart = safeParseColor(node.colorHex)
                    val previewEnd = safeParseColor(node.efx.textureColorHex ?: "#FF5722")
                    val previewColors = if (node.efx.textureParallel) {
                        listOf(previewEnd, previewStart)
                    } else {
                        listOf(previewStart, previewEnd)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (node.efx.texture) {
                                    TextTextureType.HORIZONTAL_GRADIENT -> androidx.compose.ui.graphics.Brush.horizontalGradient(previewColors)
                                    TextTextureType.VERTICAL_GRADIENT -> androidx.compose.ui.graphics.Brush.verticalGradient(previewColors)
                                    TextTextureType.RADIAL_GRADIENT -> androidx.compose.ui.graphics.Brush.radialGradient(previewColors)
                                    TextTextureType.SWEEP_GRADIENT -> androidx.compose.ui.graphics.Brush.sweepGradient(listOf(previewColors[0], previewColors[1], previewColors[0]))
                                    else -> androidx.compose.ui.graphics.Brush.linearGradient(previewColors)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (node.template.isNotBlank()) node.template else stringResource(textureTypeLabel(node.efx.texture)),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    StudioColorField(
                        label = stringResource(R.string.studio_efx_texture_color),
                        colorHex = node.efx.textureColorHex ?: "#FF5722",
                        onColorHexChange = { onChange(node.copy(efx = node.efx.copy(textureColorHex = it))) }
                    )

                    StudioStepper(
                        label = stringResource(R.string.studio_efx_texture_width),
                        value = node.efx.textureWidthDp ?: 100,
                        onValueChange = { onChange(node.copy(efx = node.efx.copy(textureWidthDp = it.coerceIn(10, 500)))) },
                        unitSuffix = "dp",
                        min = 10,
                        max = 500
                    )

                    StudioStepper(
                        label = stringResource(R.string.studio_efx_texture_height),
                        value = node.efx.textureHeightDp ?: 100,
                        onValueChange = { onChange(node.copy(efx = node.efx.copy(textureHeightDp = it.coerceIn(10, 500)))) },
                        unitSuffix = "dp",
                        min = 10,
                        max = 500
                    )

                    LabelledSwitch(
                        label = stringResource(R.string.studio_efx_texture_parallel),
                        checked = node.efx.textureParallel,
                        onCheckedChange = { onChange(node.copy(efx = node.efx.copy(textureParallel = it))) }
                    )

                    if (node.efx.texture == TextTextureType.BITMAP) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = node.efx.textureBitmapUri.orEmpty(),
                                onValueChange = { onChange(node.copy(efx = node.efx.copy(textureBitmapUri = it.ifBlank { null }))) },
                                label = { Text(stringResource(R.string.studio_efx_texture_bitmap_uri)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = { imageLauncher.launch("image/*") }
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- 3. SHADOW ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.studio_efx_shadow),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                LabelledSwitch(
                    label = stringResource(R.string.studio_efx_shadow_enable),
                    checked = node.efx.shadow.enabled,
                    onCheckedChange = { isEnabled ->
                        onChange(node.copy(efx = node.efx.copy(shadow = node.efx.shadow.copy(enabled = isEnabled))))
                    }
                )

                if (node.efx.shadow.enabled) {
                    StudioStepper(
                        label = stringResource(R.string.studio_efx_shadow_blur),
                        value = node.efx.shadow.blurRadius,
                        onValueChange = { onChange(node.copy(efx = node.efx.copy(shadow = node.efx.shadow.copy(blurRadius = it.coerceIn(0, 50))))) },
                        min = 0,
                        max = 50
                    )

                    StudioStepper(
                        label = stringResource(R.string.studio_efx_shadow_direction),
                        value = node.efx.shadow.direction,
                        onValueChange = { onChange(node.copy(efx = node.efx.copy(shadow = node.efx.shadow.copy(direction = it.coerceIn(0, 360))))) },
                        unitSuffix = "°",
                        min = 0,
                        max = 360,
                        step = 15,
                        fastStep = 45
                    )

                    StudioStepper(
                        label = stringResource(R.string.studio_efx_shadow_distance),
                        value = node.efx.shadow.distance,
                        onValueChange = { onChange(node.copy(efx = node.efx.copy(shadow = node.efx.shadow.copy(distance = it.coerceIn(0, 50))))) },
                        unitSuffix = "dp",
                        min = 0,
                        max = 50
                    )

                    StudioColorField(
                        label = stringResource(R.string.studio_efx_shadow_color),
                        colorHex = node.efx.shadow.colorHex,
                        onColorHexChange = { onChange(node.copy(efx = node.efx.copy(shadow = node.efx.shadow.copy(colorHex = it)))) }
                    )
                }
            }
        }
    }
}

private fun sizingTypeLabel(type: TextSizingType): Int = when (type) {
    TextSizingType.FIXED_FONT_HEIGHT -> R.string.studio_text_sizing_fixed_font_height
    TextSizingType.FIT_WIDTH -> R.string.studio_text_sizing_fit_width
    TextSizingType.FIXED_WIDTH -> R.string.studio_text_sizing_fixed_width
    TextSizingType.FIT_BOX -> R.string.studio_text_sizing_fit_box
}

private fun filterModeLabel(mode: TextFilterMode): Int = when (mode) {
    TextFilterMode.NORMAL -> R.string.studio_filter_normal
    TextFilterMode.CLEAR -> R.string.studio_filter_clear
    TextFilterMode.SRC -> R.string.studio_filter_src
    TextFilterMode.DST -> R.string.studio_filter_dst
    TextFilterMode.XOR -> R.string.studio_filter_xor
    TextFilterMode.DARKEN -> R.string.studio_filter_darken
    TextFilterMode.LIGHTEN -> R.string.studio_filter_lighten
    TextFilterMode.SCREEN -> R.string.studio_filter_screen
    TextFilterMode.ADD -> R.string.studio_filter_add
    TextFilterMode.OVERLAY -> R.string.studio_filter_overlay
    TextFilterMode.MULTIPLY -> R.string.studio_filter_multiply
}

private fun textureTypeLabel(type: TextTextureType): Int = when (type) {
    TextTextureType.NONE -> R.string.studio_efx_texture_none
    TextTextureType.HORIZONTAL_GRADIENT -> R.string.studio_efx_texture_horizontal_gradient
    TextTextureType.VERTICAL_GRADIENT -> R.string.studio_efx_texture_vertical_gradient
    TextTextureType.RADIAL_GRADIENT -> R.string.studio_efx_texture_radial_gradient
    TextTextureType.SWEEP_GRADIENT -> R.string.studio_efx_texture_sweep_gradient
    TextTextureType.BITMAP -> R.string.studio_efx_texture_bitmap
}

private fun previewComposeFontFamily(family: TextFontFamily): androidx.compose.ui.text.font.FontFamily = when (family) {
    TextFontFamily.DEFAULT -> androidx.compose.ui.text.font.FontFamily.Default
    TextFontFamily.SANS_SERIF -> androidx.compose.ui.text.font.FontFamily.SansSerif
    TextFontFamily.SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
    TextFontFamily.MONOSPACE -> androidx.compose.ui.text.font.FontFamily.Monospace
    TextFontFamily.CURSIVE -> androidx.compose.ui.text.font.FontFamily.Cursive
    TextFontFamily.CASUAL -> androidx.compose.ui.text.font.FontFamily(android.graphics.Typeface.create("casual", android.graphics.Typeface.NORMAL))
    TextFontFamily.CONDENSED -> androidx.compose.ui.text.font.FontFamily(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL))
}

private fun fontFamilyLabel(family: TextFontFamily): Int = when (family) {
    TextFontFamily.DEFAULT -> R.string.studio_font_family_default
    TextFontFamily.SANS_SERIF -> R.string.studio_font_family_sans_serif
    TextFontFamily.SERIF -> R.string.studio_font_family_serif
    TextFontFamily.MONOSPACE -> R.string.studio_font_family_monospace
    TextFontFamily.CURSIVE -> R.string.studio_font_family_cursive
    TextFontFamily.CASUAL -> R.string.studio_font_family_casual
    TextFontFamily.CONDENSED -> R.string.studio_font_family_condensed
}

@Composable
private fun ContainerTabContent(
    container: LayoutContainer,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit = {}
) {
    StudioSection(stringResource(R.string.studio_tab_container)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContainerLayout.entries.forEach { layout ->
                FilterChip(
                    selected = container.layout == layout,
                    onClick = {
                        val withNewLayout = container.copy(layout = layout)
                        onChange(
                            withNewLayout.copy(
                                bounds = withNewLayout.bounds.copy(
                                    widthDp = withNewLayout.adaptedContentWidth() ?: withNewLayout.bounds.widthDp,
                                    heightDp = withNewLayout.adaptedContentHeight() ?: withNewLayout.bounds.heightDp
                                )
                            )
                        )
                    },
                    label = { Text(layout.name) }
                )
            }
        }

        StudioStepper(
            label = stringResource(R.string.studio_property_gap),
            value = container.gapDp,
            onValueChange = {
                val withGap = container.copy(gapDp = it.coerceIn(0, 64))
                onChange(
                    withGap.copy(
                        bounds = withGap.bounds.copy(
                            widthDp = withGap.adaptedContentWidth() ?: withGap.bounds.widthDp,
                            heightDp = withGap.adaptedContentHeight() ?: withGap.bounds.heightDp
                        )
                    )
                )
            },
            unitSuffix = "dp",
            min = 0,
            max = 64,
            boundFormula = container.bindings["gapDp"],
            onFormulaChange = { onChange(container.withBinding("gapDp", it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor("gapDp") }
        )

        StudioStepper(
            label = stringResource(R.string.studio_property_padding),
            value = container.paddingDp,
            onValueChange = {
                val withPad = container.copy(paddingDp = it.coerceIn(0, 64))
                onChange(
                    withPad.copy(
                        bounds = withPad.bounds.copy(
                            widthDp = withPad.adaptedContentWidth() ?: withPad.bounds.widthDp,
                            heightDp = withPad.adaptedContentHeight() ?: withPad.bounds.heightDp
                        )
                    )
                )
            },
            unitSuffix = "dp",
            min = 0,
            max = 64,
            boundFormula = container.bindings["paddingDp"],
            onFormulaChange = { onChange(container.withBinding("paddingDp", it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor("paddingDp") }
        )

        StudioColorField(
            label = stringResource(R.string.studio_property_background),
            colorHex = container.backgroundHex.orEmpty().ifBlank { if (container.id == "root") "#141414" else "#00000000" },
            onColorHexChange = { onChange(container.copy(backgroundHex = it.ifBlank { null })) },
            boundFormula = container.bindings[BindableProperty.CONTAINER_BACKGROUND.key],
            onFormulaChange = { onChange(container.withBinding(BindableProperty.CONTAINER_BACKGROUND.key, it)) },
            onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.CONTAINER_BACKGROUND.key) }
        )
    }
}

@Composable
private fun BackgroundTabContent(
    container: LayoutContainer,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit = {}
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onChange(
                container.copy(
                    backgroundType = ContainerBackgroundType.PICTURE,
                    backgroundImageUri = uri.toString(),
                    backgroundImageSource = null
                )
            )
        }
    }

    StudioSection(stringResource(R.string.studio_tab_background)) {
        Text(
            text = stringResource(R.string.studio_background_type),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = container.backgroundType == ContainerBackgroundType.SOLID,
                onClick = {
                    onChange(container.copy(backgroundType = ContainerBackgroundType.SOLID))
                },
                leadingIcon = {
                    if (container.backgroundType == ContainerBackgroundType.SOLID) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                },
                label = { Text(stringResource(R.string.studio_background_type_solid)) }
            )
            FilterChip(
                selected = container.backgroundType == ContainerBackgroundType.PICTURE,
                onClick = {
                    onChange(
                        container.copy(
                            backgroundType = ContainerBackgroundType.PICTURE,
                            backgroundImageSource = container.backgroundImageSource ?: ImageSource.NotifMedia("picture")
                        )
                    )
                },
                leadingIcon = {
                    if (container.backgroundType == ContainerBackgroundType.PICTURE) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                },
                label = { Text(stringResource(R.string.studio_background_type_picture)) }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (container.backgroundType == ContainerBackgroundType.SOLID) {
            StudioColorField(
                label = stringResource(R.string.studio_property_background),
                colorHex = container.backgroundHex.orEmpty().ifBlank { if (container.id == "root") "#141414" else "#00000000" },
                onColorHexChange = { onChange(container.copy(backgroundHex = it.ifBlank { null })) },
                boundFormula = container.bindings[BindableProperty.CONTAINER_BACKGROUND.key],
                onFormulaChange = { onChange(container.withBinding(BindableProperty.CONTAINER_BACKGROUND.key, it)) },
                onRequestFormulaEditor = { onRequestFormulaEditor(BindableProperty.CONTAINER_BACKGROUND.key) }
            )
        } else {
            Text(
                text = stringResource(R.string.studio_background_picture_source),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val isCustomPicture = !container.backgroundImageUri.isNullOrBlank()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isCustomPicture,
                    onClick = {
                        onChange(
                            container.copy(
                                backgroundImageUri = null,
                                backgroundImageSource = container.backgroundImageSource ?: ImageSource.NotifMedia("picture")
                            )
                        )
                    },
                    label = { Text(stringResource(R.string.studio_background_notif_media)) }
                )
                FilterChip(
                    selected = isCustomPicture,
                    onClick = {
                        if (container.backgroundImageUri.isNullOrBlank()) {
                            photoPickerLauncher.launch("image/*")
                        }
                    },
                    label = { Text(stringResource(R.string.studio_background_custom_picture)) }
                )
            }

            if (!isCustomPicture) {
                val currentMediaKey = (container.backgroundImageSource as? ImageSource.NotifMedia)?.mediaType ?: "picture"
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NOTIF_MEDIA_SUBTYPES.forEach { subtype ->
                        val isSelected = currentMediaKey == subtype.key
                        Card(
                            onClick = {
                                onChange(
                                    container.copy(
                                        backgroundImageSource = ImageSource.NotifMedia(subtype.key),
                                        backgroundImageUri = null
                                    )
                                )
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = subtype.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(subtype.titleRes),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(subtype.descRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onChange(
                                            container.copy(
                                                backgroundImageSource = ImageSource.NotifMedia(subtype.key),
                                                backgroundImageUri = null
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.studio_background_custom_picture),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = container.backgroundImageUri.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { photoPickerLauncher.launch("image/*") }
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.studio_background_select_picture))
                            }
                            OutlinedButton(
                                onClick = {
                                    onChange(
                                        container.copy(
                                            backgroundImageUri = null,
                                            backgroundImageSource = ImageSource.NotifMedia("picture")
                                        )
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.studio_background_remove_picture))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalTabContent(
    document: CustomWidgetDocument,
    onUpdateGlobals: (CustomWidgetGlobals) -> Unit,
    onApplyFontToAll: (TextFontFamily) -> Unit
) {
    val globals = document.globals

    StudioSection(stringResource(R.string.studio_tab_global)) {
        StudioColorField(
            label = stringResource(R.string.studio_global_primary_color),
            colorHex = globals.primaryColorHex,
            onColorHexChange = { onUpdateGlobals(globals.copy(primaryColorHex = it)) }
        )

        StudioColorField(
            label = stringResource(R.string.studio_global_accent_color),
            colorHex = globals.accentColorHex,
            onColorHexChange = { onUpdateGlobals(globals.copy(accentColorHex = it)) }
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.studio_global_font_family),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextFontFamily.entries.forEach { family ->
                FilterChip(
                    selected = globals.fontFamily == family,
                    onClick = { onUpdateGlobals(globals.copy(fontFamily = family)) },
                    label = {
                        Text(
                            text = stringResource(fontFamilyLabel(family)),
                            fontFamily = previewComposeFontFamily(family)
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onApplyFontToAll(globals.fontFamily) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Title, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.studio_global_apply_to_all))
        }
    }
}

@Composable
private fun DesignTabContent(
    document: CustomWidgetDocument,
    onNameChange: (String) -> Unit,
    onIconChange: ((String) -> Unit)? = null,
    onCanvasChange: (CanvasSize) -> Unit
) {
    StudioSection(stringResource(R.string.studio_section_design)) {
        OutlinedTextField(
            value = document.meta.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.studio_property_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.studio_design_icon),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TRANSLATOR_OUTLINED_ICONS.forEach { option ->
                FilterChip(
                    selected = document.meta.icon.equals(option.id, ignoreCase = true),
                    onClick = { onIconChange?.invoke(option.id) },
                    leadingIcon = {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text(option.label) }
                )
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CanvasSize.entries.forEach { size ->
                FilterChip(
                    selected = document.canvas == size,
                    onClick = { onCanvasChange(size) },
                    label = { Text("${size.name} · ${size.heightDp}dp") }
                )
            }
        }
    }
}

@Composable
private fun ScopeTabContent(
    boundTranslator: CustomTranslator?,
    notificationType: NotificationType?,
    targetScope: TargetScope,
    targetPackages: List<String>,
    onNotificationTypeChange: ((NotificationType) -> Unit)?,
    onTargetScopeChange: ((TargetScope) -> Unit)?,
    onAddTargetPackage: ((String) -> Unit)?,
    onRemoveTargetPackage: ((String) -> Unit)?,
    onCreateTranslator: (() -> Unit)? = null,
    onRequestAppChooser: ((String) -> Unit) -> Unit
) {
    // 1. Translator Usage Status Card
    StudioSection(stringResource(R.string.studio_scope_translator_status)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (boundTranslator != null) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (boundTranslator != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (boundTranslator != null) Icons.Rounded.CheckCircle else Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (boundTranslator != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (boundTranslator != null) {
                            stringResource(R.string.studio_scope_translator_active)
                        } else {
                            stringResource(R.string.studio_scope_translator_inactive)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (boundTranslator != null) {
                            stringResource(R.string.studio_scope_translator_active_desc, boundTranslator.meta.name)
                        } else {
                            stringResource(R.string.studio_scope_translator_inactive_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (boundTranslator == null && onCreateTranslator != null) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCreateTranslator,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.studio_scope_create_translator))
            }
        }
    }

    // 2. Where It Applies
    StudioSection(stringResource(R.string.studio_scope_title)) {
        Text(
            text = stringResource(R.string.studio_scope_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TargetScope.entries.forEach { scope ->
                val label = when (scope) {
                    TargetScope.NOTIFICATION_TYPE -> stringResource(R.string.studio_scope_notif_type)
                    TargetScope.SPECIFIC_APPS -> stringResource(R.string.studio_scope_specific_apps)
                    TargetScope.GLOBAL -> stringResource(R.string.studio_scope_global)
                    TargetScope.SYSTEM_APPS -> stringResource(R.string.studio_scope_system_apps)
                }
                FilterChip(
                    selected = targetScope == scope,
                    onClick = { onTargetScopeChange?.invoke(scope) },
                    label = { Text(label) }
                )
            }
        }

        when (targetScope) {
            TargetScope.NOTIFICATION_TYPE -> {
                Text(
                    text = stringResource(R.string.design_template_type_desc),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NotificationType.configurableEntries.forEach { type ->
                        FilterChip(
                            selected = notificationType == type,
                            onClick = { onNotificationTypeChange?.invoke(type) },
                            label = { Text(stringResource(type.labelRes)) }
                        )
                    }
                }
            }

            TargetScope.SPECIFIC_APPS, TargetScope.SYSTEM_APPS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            onRequestAppChooser { selectedPkg ->
                                onAddTargetPackage?.invoke(selectedPkg)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = { Text(stringResource(R.string.studio_scope_add_app)) }
                    )

                    targetPackages.forEach { pkg ->
                        FilterChip(
                            selected = true,
                            onClick = { onRemoveTargetPackage?.invoke(pkg) },
                            trailingIcon = {
                                Icon(Icons.Rounded.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(pkg.substringAfterLast('.')) }
                        )
                    }
                }

                if (targetPackages.isEmpty()) {
                    Text(
                        text = stringResource(R.string.studio_scope_no_apps),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TargetScope.GLOBAL -> {
                Text(
                    text = stringResource(R.string.translator_target_scope_global_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionsTabContent(
    node: CustomWidgetNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestAppChooser: ((String) -> Unit) -> Unit = {}
) {
    StudioSection(stringResource(R.string.studio_tab_actions)) {
        ActionEditor(
            action = if (node is ButtonNode) node.action else node.onClick,
            allowNone = node !is ButtonNode,
            onChange = { action ->
                onChange(
                    if (node is ButtonNode) node.copy(action = action ?: ButtonAction.NotificationAction(0))
                    else node.withOnClick(action)
                )
            },
            onRequestAppChooser = onRequestAppChooser
        )
    }
}

@Composable
private fun BindingsTabContent(
    node: CustomWidgetNode,
    onChange: (CustomWidgetNode) -> Unit,
    onRequestFormulaEditor: (String) -> Unit = {}
) {
    val bindableProps = when (node) {
        is TextNode -> listOf(BindableProperty.TEXT_TEMPLATE, BindableProperty.TEXT_COLOR, BindableProperty.TEXT_FONT_SIZE, BindableProperty.OPACITY)
        is ProgressNode -> listOf(
            BindableProperty.PROGRESS_VALUE,
            BindableProperty.PROGRESS_COLOR,
            BindableProperty.PROGRESS_GRADIENT_END_COLOR,
            BindableProperty.PROGRESS_TRACK_COLOR,
            BindableProperty.PROGRESS_STROKE_WIDTH,
            BindableProperty.PROGRESS_ROUND_CAPS,
            BindableProperty.PROGRESS_THUMB_SIZE,
            BindableProperty.PROGRESS_THUMB_COLOR,
            BindableProperty.OPACITY
        )
        is ButtonNode -> listOf(BindableProperty.BUTTON_LABEL, BindableProperty.BUTTON_TEXT_COLOR, BindableProperty.BUTTON_BACKGROUND, BindableProperty.OPACITY)
        is ImageNode -> listOf(BindableProperty.IMAGE_TINT, BindableProperty.BOUNDS_WIDTH, BindableProperty.BOUNDS_HEIGHT, BindableProperty.OPACITY)
        is ShapeNode -> listOf(BindableProperty.SHAPE_FILL, BindableProperty.SHAPE_STROKE, BindableProperty.SHAPE_STROKE_WIDTH, BindableProperty.BOUNDS_WIDTH, BindableProperty.BOUNDS_HEIGHT, BindableProperty.OPACITY)
        is LayoutContainer -> listOf(BindableProperty.CONTAINER_BACKGROUND, BindableProperty.BOUNDS_WIDTH, BindableProperty.BOUNDS_HEIGHT, BindableProperty.OPACITY)
    }

    StudioSection(stringResource(R.string.studio_tab_bindings)) {
        Text(
            text = stringResource(R.string.studio_binding_formula_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        bindableProps.forEach { prop ->
            val currentFormula = node.bindings[prop.key].orEmpty()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prop.key,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onRequestFormulaEditor(prop.key) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Calculate,
                                contentDescription = stringResource(R.string.studio_bind_formula),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (currentFormula.isNotBlank()) {
                            IconButton(
                                onClick = { onChange(node.withBinding(prop.key, null)) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = currentFormula,
                    onValueChange = { onChange(node.withBinding(prop.key, it)) },
                    placeholder = { Text("{var}") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                VariableTokenRow { token ->
                    onChange(node.withBinding(prop.key, currentFormula + token))
                }
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
private fun ConditionEditor(
    condition: NodeCondition,
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    onChange: (NodeCondition) -> Unit
) {
    val isConditionMet = NodeConditionEvaluator.isVisible(condition, scenario.toVariableContext())
    val isCompound = condition is NodeCondition.All || condition is NodeCondition.Any

    // Live Scenario Evaluation Status Badge
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isConditionMet) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isConditionMet) Icons.Rounded.CheckCircle else Icons.Rounded.VisibilityOff,
                contentDescription = null,
                tint = if (isConditionMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isConditionMet) stringResource(R.string.studio_condition_status_met)
                       else stringResource(R.string.studio_condition_status_unmet),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (isConditionMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Mode Selector (Single Rule vs Compound)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = !isCompound,
            onClick = {
                if (isCompound) {
                    val first = when (condition) {
                        is NodeCondition.All -> condition.conditions.firstOrNull() ?: NodeCondition.Always
                        is NodeCondition.Any -> condition.conditions.firstOrNull() ?: NodeCondition.Always
                        else -> condition
                    }
                    onChange(first)
                }
            },
            label = { Text(stringResource(R.string.studio_condition_mode_simple)) }
        )
        FilterChip(
            selected = isCompound,
            onClick = {
                if (!isCompound) {
                    val existing = if (condition is NodeCondition.Always) NodeCondition.NotBlank("{notif.text}") else condition
                    onChange(NodeCondition.All(listOf(existing, NodeCondition.NotBlank("{notif.title}"))))
                }
            },
            label = { Text(stringResource(R.string.studio_condition_mode_compound)) }
        )
    }

    if (!isCompound) {
        // Quick Presets
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.studio_condition_presets),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { onChange(NodeCondition.HasSmartAction("OTP")) },
                    label = { Text(stringResource(R.string.studio_condition_preset_otp)) }
                )
                AssistChip(
                    onClick = { onChange(NodeCondition.HasProgress) },
                    label = { Text(stringResource(R.string.studio_condition_preset_progress)) }
                )
                AssistChip(
                    onClick = { onChange(NodeCondition.HasInlineReply) },
                    label = { Text(stringResource(R.string.studio_condition_preset_reply)) }
                )
                AssistChip(
                    onClick = { onChange(NodeCondition.Matches("{device.battery}", "^(1[0-9]|[0-9])$")) },
                    label = { Text(stringResource(R.string.studio_condition_preset_low_battery)) }
                )
            }
        }

        // Invert (NOT) toggle
        val isInverted = condition is NodeCondition.Not
        val baseCondition = if (condition is NodeCondition.Not) condition.condition else condition
        LabelledSwitch(
            label = stringResource(R.string.studio_condition_invert),
            checked = isInverted,
            onCheckedChange = { checked ->
                if (checked) {
                    onChange(NodeCondition.Not(baseCondition))
                } else {
                    onChange(baseCondition)
                }
            }
        )

        SingleConditionFieldEditor(
            condition = baseCondition,
            onChange = { updated ->
                if (isInverted) onChange(NodeCondition.Not(updated)) else onChange(updated)
            }
        )
    } else {
        // Compound Mode (AND / OR)
        val isAll = condition is NodeCondition.All
        val subConditions = when (condition) {
            is NodeCondition.All -> condition.conditions
            is NodeCondition.Any -> condition.conditions
            else -> emptyList()
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isAll,
                onClick = { onChange(NodeCondition.All(subConditions)) },
                label = { Text(stringResource(R.string.studio_condition_match_all)) }
            )
            FilterChip(
                selected = !isAll,
                onClick = { onChange(NodeCondition.Any(subConditions)) },
                label = { Text(stringResource(R.string.studio_condition_match_any)) }
            )
        }

        // List of Sub-Rules
        subConditions.forEachIndexed { index, subRule ->
            val subInverted = subRule is NodeCondition.Not
            val baseSub = if (subRule is NodeCondition.Not) subRule.condition else subRule

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.studio_condition_rule_title, index + 1),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (subConditions.size > 1) {
                            IconButton(
                                onClick = {
                                    val updated = subConditions.toMutableList().also { it.removeAt(index) }
                                    onChange(if (isAll) NodeCondition.All(updated) else NodeCondition.Any(updated))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.studio_condition_delete_rule),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    LabelledSwitch(
                        label = stringResource(R.string.studio_condition_invert),
                        checked = subInverted,
                        onCheckedChange = { checked ->
                            val updatedRule = if (checked) NodeCondition.Not(baseSub) else baseSub
                            val updatedList = subConditions.toMutableList().also { it[index] = updatedRule }
                            onChange(if (isAll) NodeCondition.All(updatedList) else NodeCondition.Any(updatedList))
                        }
                    )

                    SingleConditionFieldEditor(
                        condition = baseSub,
                        onChange = { updatedBase ->
                            val updatedRule = if (subInverted) NodeCondition.Not(updatedBase) else updatedBase
                            val updatedList = subConditions.toMutableList().also { it[index] = updatedRule }
                            onChange(if (isAll) NodeCondition.All(updatedList) else NodeCondition.Any(updatedList))
                        }
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                val updated = subConditions + NodeCondition.NotBlank("{notif.text}")
                onChange(if (isAll) NodeCondition.All(updated) else NodeCondition.Any(updated))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.studio_condition_add_rule))
        }
    }
}

@Composable
private fun SingleConditionFieldEditor(
    condition: NodeCondition,
    onChange: (NodeCondition) -> Unit
) {
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
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
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
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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
            VariableTokenRow { token -> onChange(NodeCondition.Matches(condition.template + token, condition.regex)) }
        }

        else -> Unit
    }
}

@Composable
private fun ActionEditor(
    action: ButtonAction?,
    allowNone: Boolean,
    onChange: (ButtonAction?) -> Unit,
    onRequestAppChooser: ((String) -> Unit) -> Unit = {}
) {
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
        is ButtonAction.OpenApp -> {
            OutlinedTextField(
                value = action.packageName,
                onValueChange = { onChange(ButtonAction.OpenApp(it)) },
                label = { Text(stringResource(R.string.studio_action_package)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onChange(ButtonAction.OpenApp("{notif.package}")) },
                    label = { Text("{notif.package}", fontFamily = FontFamily.Monospace) }
                )
                OutlinedButton(
                    onClick = {
                        onRequestAppChooser { selectedPkg ->
                            onChange(ButtonAction.OpenApp(selectedPkg))
                        }
                    }
                ) {
                    Text(stringResource(R.string.studio_action_choose_app))
                }
            }
        }

        is ButtonAction.DeepLink -> {
            OutlinedTextField(
                value = action.uri,
                onValueChange = { onChange(ButtonAction.DeepLink(it)) },
                label = { Text(stringResource(R.string.studio_action_uri)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("{notif.url}", "{source.weather.url}").forEach { token ->
                    AssistChip(
                        onClick = { onChange(ButtonAction.DeepLink(token)) },
                        label = { Text(token, fontFamily = FontFamily.Monospace) }
                    )
                }
            }
        }

        is ButtonAction.NotificationAction -> StudioStepper(
            label = stringResource(R.string.studio_condition_action_index),
            value = action.index,
            onValueChange = { onChange(ButtonAction.NotificationAction(it.coerceIn(0, 3))) },
            min = 0,
            max = 3
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
            VariableTokenRow { token ->
                onChange(action.copy(extraValue = (action.extraValue.orEmpty()) + token))
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

private fun defaultNodeTitle(node: CustomWidgetNode): String = when (node) {
    is TextNode -> if (node.template.isNotBlank()) node.template else "Text"
    is ImageNode -> "Image"
    is ProgressNode -> "Progress"
    is ButtonNode -> if (node.label.isNotBlank()) node.label else "Button"
    is ShapeNode -> "Shape"
    is LayoutContainer -> "Group"
}

private fun nodeIcon(node: CustomWidgetNode): ImageVector = when (node) {
    is TextNode -> Icons.Rounded.Title
    is ImageNode -> Icons.Rounded.Image
    is ProgressNode -> Icons.Rounded.LinearScale
    is ButtonNode -> Icons.Rounded.SmartButton
    is ShapeNode -> Icons.Rounded.Category
    is LayoutContainer -> Icons.Rounded.Folder
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
            is NodeCondition.Not -> of(condition.condition)
            is NodeCondition.All -> condition.conditions.firstOrNull()?.let { of(it) } ?: ALWAYS
            is NodeCondition.Any -> condition.conditions.firstOrNull()?.let { of(it) } ?: ALWAYS
        }
    }
}

private enum class ActionKind(val labelRes: Int) {
    NOTIFICATION_ACTION(R.string.studio_action_notification),
    OPEN_APP(R.string.studio_action_open_app),
    INLINE_REPLY(R.string.studio_action_reply),
    SMART_ACTION(R.string.studio_action_smart),
    DEEP_LINK(R.string.studio_action_deep_link),
    BROADCAST(R.string.studio_action_broadcast_kind),
    DISMISS(R.string.studio_action_dismiss);

    fun default(): ButtonAction = when (this) {
        NOTIFICATION_ACTION -> ButtonAction.NotificationAction(0)
        OPEN_APP -> ButtonAction.OpenApp("")
        INLINE_REPLY -> ButtonAction.InlineReply
        SMART_ACTION -> ButtonAction.SmartAction("OTP")
        DEEP_LINK -> ButtonAction.DeepLink("")
        BROADCAST -> ButtonAction.Broadcast("")
        DISMISS -> ButtonAction.Dismiss
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

private enum class ImageSourceKind(
    val labelRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val hasSubtypes: Boolean = false
) {
    NOTIF_MEDIA(
        R.string.studio_image_source_notif_media,
        R.string.studio_image_source_notif_media_desc,
        Icons.Rounded.Notifications,
        hasSubtypes = true
    ),
    APP_ICON(
        R.string.studio_image_source_app,
        R.string.studio_image_source_app_desc,
        Icons.Rounded.Android,
        hasSubtypes = true
    ),
    SYSTEM_GLYPH(
        R.string.studio_image_source_glyph,
        R.string.studio_image_source_glyph_desc,
        Icons.Rounded.Category,
        hasSubtypes = true
    ),
    SOURCE_ICON(
        R.string.studio_image_source_source,
        R.string.studio_image_source_source_desc,
        Icons.Rounded.Widgets,
        hasSubtypes = true
    ),
    CUSTOM_ASSET(
        R.string.studio_image_source_asset,
        R.string.studio_image_source_asset_desc,
        Icons.Rounded.Image,
        hasSubtypes = false
    ),
    CONTACT_AVATAR(
        R.string.studio_image_source_contact,
        R.string.studio_image_source_contact_desc,
        Icons.Rounded.Call,
        hasSubtypes = false
    );

    fun defaultSource(): ImageSource = when (this) {
        NOTIF_MEDIA -> ImageSource.NotifMedia("avatar")
        APP_ICON -> ImageSource.AppIconOf("{notif.package}")
        SYSTEM_GLYPH -> ImageSource.SystemGlyph("notification")
        SOURCE_ICON -> ImageSource.SourceIcon("weather")
        CUSTOM_ASSET -> ImageSource.CustomAsset("icon.png")
        CONTACT_AVATAR -> ImageSource.ContactAvatarOf("{notif.title}")
    }

    companion object {
        fun of(source: ImageSource): ImageSourceKind = when (source) {
            is ImageSource.NotifMedia -> NOTIF_MEDIA
            is ImageSource.AppIconOf -> APP_ICON
            is ImageSource.SystemGlyph -> SYSTEM_GLYPH
            is ImageSource.SourceIcon -> SOURCE_ICON
            is ImageSource.CustomAsset -> CUSTOM_ASSET
            is ImageSource.ContactAvatarOf -> CONTACT_AVATAR
        }
    }
}

private data class NotifMediaSubtype(
    val key: String,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)

private val NOTIF_MEDIA_SUBTYPES = listOf(
    NotifMediaSubtype(
        key = "avatar",
        titleRes = R.string.studio_image_notif_media_avatar,
        descRes = R.string.studio_image_notif_media_avatar_desc,
        icon = Icons.Rounded.Notifications
    ),
    NotifMediaSubtype(
        key = "picture",
        titleRes = R.string.studio_image_notif_media_picture,
        descRes = R.string.studio_image_notif_media_picture_desc,
        icon = Icons.Rounded.Image
    ),
    NotifMediaSubtype(
        key = "album_art",
        titleRes = R.string.studio_image_notif_media_album_art,
        descRes = R.string.studio_image_notif_media_album_art_desc,
        icon = Icons.Rounded.MusicNote
    ),
    NotifMediaSubtype(
        key = "small_icon",
        titleRes = R.string.studio_image_notif_media_small_icon,
        descRes = R.string.studio_image_notif_media_small_icon_desc,
        icon = Icons.Rounded.Notifications
    )
)

private val QUICK_GLYPHS = listOf(
    "notification", "play", "pause", "message", "call", "info", "settings", "check", "close"
)

