package com.d4viddf.hyperbridge.ui.screens.design.widgets

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.widget.ButtonAction
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.CanvasSize
import com.d4viddf.hyperbridge.models.widget.ContainerLayout
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.CustomWidgetMetadata
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.ImageSource
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.NodeBounds
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.TextGravity
import com.d4viddf.hyperbridge.models.widget.TextNode
import com.d4viddf.hyperbridge.models.widget.WidgetDimensionValidator
import com.d4viddf.hyperbridge.models.widget.addChild
import com.d4viddf.hyperbridge.models.widget.removeNode
import com.d4viddf.hyperbridge.models.widget.replaceNode
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * KWGT-style spatial canvas editor for a [CustomWidgetDocument] (#273). Editing is click-to-select
 * plus a numeric property inspector (bounds/template fields) rather than free drag-and-resize -
 * the preview here is a pure-Compose re-implementation of the AST (Box/Text/Image/Progress with
 * `Modifier.offset`), kept deliberately separate from the RemoteViews
 * [com.d4viddf.hyperbridge.service.widget.CustomWidgetRenderer] used at dispatch time, since
 * RemoteViews has no drag/selection affordances of its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetStudioScreen(
    widgetId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CustomWidgetRepository(context) }
    val scope = rememberCoroutineScope()

    var doc by remember {
        mutableStateOf(
            CustomWidgetDocument(
                id = widgetId ?: UUID.randomUUID().toString(),
                meta = CustomWidgetMetadata(name = "New widget"),
                root = LayoutContainer(id = "root", layout = ContainerLayout.COLUMN, paddingDp = 8, gapDp = 8)
            )
        )
    }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(widgetId) {
        if (widgetId != null) {
            repository.getWidget(widgetId)?.let { doc = it }
        }
    }

    val selectedNode = selectedNodeId?.let { id -> findNode(doc.root, id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doc.meta.name) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val file = repository.exportWidget(doc.id)
                            if (file != null) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooser = Intent.createChooser(intent, context.getString(R.string.widget_studio_export))
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            }
                        }
                    }) {
                        Icon(Icons.Rounded.IosShare, stringResource(R.string.widget_studio_export))
                    }
                    TextButton(onClick = {
                        val validation = WidgetDimensionValidator.validate(doc)
                        doc = validation.clamped
                        validationMessage = if (validation.errors.isNotEmpty()) {
                            context.getString(R.string.widget_studio_validation_errors, validation.errors.size)
                        } else null
                        scope.launch {
                            repository.saveWidget(validation.clamped)
                            onBack()
                        }
                    }) {
                        Text(stringResource(R.string.widget_studio_save))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // --- Live preview ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(doc.canvas.heightDp.dp)
                            .padding(8.dp)
                    ) {
                        WidgetCanvasPreview(
                            node = doc.root,
                            selectedId = selectedNodeId,
                            onSelect = { selectedNodeId = it }
                        )
                    }
                }
            }

            if (validationMessage != null) {
                AssistChip(onClick = { validationMessage = null }, label = { Text(validationMessage!!) }, modifier = Modifier.padding(horizontal = 16.dp))
            }

            // --- Palette ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val targetContainerId = (selectedNode as? LayoutContainer)?.id ?: doc.root.id
                FilterChip(selected = false, onClick = {
                    doc = doc.addChild(targetContainerId, TextNode(id = newId(), template = "New text"))
                }, label = { Text(stringResource(R.string.widget_studio_add_text)) })
                FilterChip(selected = false, onClick = {
                    doc = doc.addChild(targetContainerId, ImageNode(id = newId()))
                }, label = { Text(stringResource(R.string.widget_studio_add_image)) })
                FilterChip(selected = false, onClick = {
                    doc = doc.addChild(targetContainerId, ProgressNode(id = newId()))
                }, label = { Text(stringResource(R.string.widget_studio_add_progress)) })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val targetContainerId = (selectedNode as? LayoutContainer)?.id ?: doc.root.id
                FilterChip(selected = false, onClick = {
                    doc = doc.addChild(targetContainerId, ButtonNode(id = newId(), label = "Button"))
                }, label = { Text(stringResource(R.string.widget_studio_add_button)) })
                FilterChip(selected = false, onClick = {
                    doc = doc.addChild(targetContainerId, LayoutContainer(id = newId(), layout = ContainerLayout.ROW))
                }, label = { Text(stringResource(R.string.widget_studio_add_container)) })
            }

            Spacer(Modifier.height(8.dp))

            // --- Node tree list (selection surface) ---
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                items(flattenTree(doc.root), key = { it.second.id }) { (depth, node) ->
                    NodeRow(
                        depth = depth,
                        node = node,
                        selected = node.id == selectedNodeId,
                        onClick = { selectedNodeId = node.id },
                        onDelete = {
                            if (node.id != doc.root.id) {
                                doc = doc.removeNode(node.id)
                                if (selectedNodeId == node.id) selectedNodeId = null
                            }
                        }
                    )
                }
            }
        }
    }

    if (selectedNode != null && selectedNode.id != doc.root.id) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { selectedNodeId = null }, sheetState = sheetState) {
            NodePropertyInspector(
                node = selectedNode,
                boundPackage = doc.boundPackage,
                onBoundPackageChange = { doc = doc.copy(boundPackage = it) },
                onChange = { updated -> doc = doc.replaceNode(updated.id) { updated } }
            )
        }
    }
}

private fun newId(): String = UUID.randomUUID().toString().take(8)

private fun findNode(node: CustomWidgetNode, id: String): CustomWidgetNode? {
    if (node.id == id) return node
    if (node is LayoutContainer) {
        node.children.forEach { child -> findNode(child, id)?.let { return it } }
    }
    return null
}

private fun flattenTree(node: CustomWidgetNode, depth: Int = 0): List<Pair<Int, CustomWidgetNode>> {
    val result = mutableListOf(depth to node)
    if (node is LayoutContainer) {
        node.children.forEach { result += flattenTree(it, depth + 1) }
    }
    return result
}

@Composable
private fun NodeRow(depth: Int, node: CustomWidgetNode, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .padding(start = (16 + depth * 16).dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = nodeLabel(node),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (depth > 0) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, stringResource(R.string.widget_studio_delete_node))
            }
        }
    }
}

private fun nodeLabel(node: CustomWidgetNode): String = when (node) {
    is LayoutContainer -> "Container (${node.layout})"
    is TextNode -> "Text: ${node.template}"
    is ImageNode -> "Image"
    is ProgressNode -> "Progress (${node.style})"
    is ButtonNode -> "Button: ${node.label}"
}

// --- Pure-Compose preview renderer (kept separate from the RemoteViews renderer) ---

private val PREVIEW_SAMPLE_CONTEXT = VariableContext(
    notifTitle = "Sample title",
    notifText = "Sample text",
    notifProgress = 42,
    deviceBatteryPercent = 77,
    timeNowFormatted = "10:30"
)
private val previewEngine = WidgetVariableEngine()

@Composable
private fun WidgetCanvasPreview(node: CustomWidgetNode, selectedId: String?, onSelect: (String) -> Unit) {
    val modifier = Modifier
        .clickable { onSelect(node.id) }
        .then(
            if (node.id == selectedId) {
                Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            } else Modifier
        )
        .then(sizeModifier(node.bounds))

    when (node) {
        is LayoutContainer -> {
            when (node.layout) {
                ContainerLayout.ROW -> Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(node.gapDp.dp)) {
                    node.children.forEach { child -> WidgetCanvasPreview(child, selectedId, onSelect) }
                }
                ContainerLayout.COLUMN -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(node.gapDp.dp)) {
                    node.children.forEach { child -> WidgetCanvasPreview(child, selectedId, onSelect) }
                }
                ContainerLayout.BOX -> Box(modifier = modifier) {
                    node.children.forEach { child -> WidgetCanvasPreview(child, selectedId, onSelect) }
                }
                ContainerLayout.ABSOLUTE -> Box(modifier = modifier) {
                    node.children.forEach { child ->
                        Box(modifier = Modifier.offset(x = child.bounds.x.dp, y = child.bounds.y.dp)) {
                            WidgetCanvasPreview(child, selectedId, onSelect)
                        }
                    }
                }
            }
        }
        is TextNode -> {
            Text(
                text = previewEngine.resolve(node.template, PREVIEW_SAMPLE_CONTEXT).ifBlank { node.template },
                color = safeParseColor(node.colorHex),
                fontSize = androidx.compose.ui.unit.TextUnit(node.fontSizeSp.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp),
                maxLines = node.maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier
            )
        }
        is ImageNode -> {
            Box(
                modifier = modifier
                    .size(if (node.bounds.widthDp != null) node.bounds.widthDp.dp else 24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            )
        }
        is ProgressNode -> {
            val value = previewEngine.resolve(node.valueTemplate, PREVIEW_SAMPLE_CONTEXT).toIntOrNull() ?: 0
            LinearProgressIndicator(
                progress = { (value.toFloat() / node.maxValue.coerceAtLeast(1)).coerceIn(0f, 1f) },
                color = safeParseColor(node.progressColorHex),
                trackColor = safeParseColor(node.trackColorHex),
                modifier = modifier.width(if (node.bounds.widthDp != null) node.bounds.widthDp.dp else 64.dp)
            )
        }
        is ButtonNode -> {
            Box(
                modifier = modifier
                    .background(
                        node.backgroundHex?.let { safeParseColor(it) } ?: MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = node.label, color = safeParseColor(node.textColorHex))
            }
        }
    }
}

private fun sizeModifier(bounds: NodeBounds): Modifier {
    var modifier: Modifier = Modifier
    if (bounds.widthDp != null) modifier = modifier.width(bounds.widthDp.dp)
    if (bounds.heightDp != null) modifier = modifier.height(bounds.heightDp.dp)
    return modifier
}

// --- Property inspector ---

@Composable
private fun NodePropertyInspector(
    node: CustomWidgetNode,
    boundPackage: String?,
    onBoundPackageChange: (String?) -> Unit,
    onChange: (CustomWidgetNode) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
        Text(nodeLabel(node), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        when (node) {
            is TextNode -> {
                OutlinedTextField(
                    value = node.template,
                    onValueChange = { onChange(node.copy(template = it)) },
                    label = { Text(stringResource(R.string.widget_studio_property_template)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                VariableTokenPicker { token -> onChange(node.copy(template = node.template + token)) }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = node.fontSizeSp.toString(),
                    onValueChange = { it.toIntOrNull()?.let { size -> onChange(node.copy(fontSizeSp = size)) } },
                    label = { Text(stringResource(R.string.widget_studio_property_font_size)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = node.colorHex,
                    onValueChange = { onChange(node.copy(colorHex = it)) },
                    label = { Text(stringResource(R.string.widget_studio_property_color)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is ImageNode -> {
                Text("Source: ${node.source}", style = MaterialTheme.typography.bodyMedium)
            }
            is ProgressNode -> {
                OutlinedTextField(
                    value = node.valueTemplate,
                    onValueChange = { onChange(node.copy(valueTemplate = it)) },
                    label = { Text(stringResource(R.string.widget_studio_property_template)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                VariableTokenPicker { token -> onChange(node.copy(valueTemplate = node.valueTemplate + token)) }
            }
            is ButtonNode -> {
                OutlinedTextField(
                    value = node.label,
                    onValueChange = { onChange(node.copy(label = it)) },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { onChange(node.copy(action = ButtonAction.Dismiss)) }, label = { Text("Dismiss") })
                    AssistChip(onClick = { onChange(node.copy(action = ButtonAction.InlineReply)) }, label = { Text("Reply") })
                }
            }
            is LayoutContainer -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContainerLayout.entries.forEach { layout ->
                        FilterChip(
                            selected = node.layout == layout,
                            onClick = { onChange(node.copy(layout = layout)) },
                            label = { Text(layout.name) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.widget_studio_property_x) + " / " + stringResource(R.string.widget_studio_property_y), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = node.bounds.x.toString(),
                onValueChange = { it.toIntOrNull()?.let { x -> onChange(setBounds(node, node.bounds.copy(x = x))) } },
                label = { Text(stringResource(R.string.widget_studio_property_x)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = node.bounds.y.toString(),
                onValueChange = { it.toIntOrNull()?.let { y -> onChange(setBounds(node, node.bounds.copy(y = y))) } },
                label = { Text(stringResource(R.string.widget_studio_property_y)) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = (node.bounds.widthDp ?: 0).toString(),
                onValueChange = { it.toIntOrNull()?.let { w -> onChange(setBounds(node, node.bounds.copy(widthDp = w))) } },
                label = { Text(stringResource(R.string.widget_studio_property_width)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = (node.bounds.heightDp ?: 0).toString(),
                onValueChange = { it.toIntOrNull()?.let { h -> onChange(setBounds(node, node.bounds.copy(heightDp = h))) } },
                label = { Text(stringResource(R.string.widget_studio_property_height)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = boundPackage ?: "",
            onValueChange = { onBoundPackageChange(it.ifBlank { null }) },
            label = { Text(stringResource(R.string.widget_studio_property_bound_package)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun setBounds(node: CustomWidgetNode, bounds: NodeBounds): CustomWidgetNode = when (node) {
    is TextNode -> node.copy(bounds = bounds)
    is ImageNode -> node.copy(bounds = bounds)
    is ProgressNode -> node.copy(bounds = bounds)
    is ButtonNode -> node.copy(bounds = bounds)
    is LayoutContainer -> node.copy(bounds = bounds)
}

private val VARIABLE_TOKENS = listOf(
    "{notif.title}", "{notif.text}", "{notif.progress}", "{device.battery}", "{time.now}"
)

@Composable
private fun VariableTokenPicker(onTokenSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VARIABLE_TOKENS.forEach { token ->
            AssistChip(onClick = { onTokenSelected(token) }, label = { Text(token) })
        }
    }
}
