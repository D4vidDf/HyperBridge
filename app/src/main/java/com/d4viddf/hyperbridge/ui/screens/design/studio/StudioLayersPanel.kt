package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.NodeCondition
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.TextNode

@Composable
fun StudioLayersPanel(
    rootContainer: LayoutContainer,
    selectedNodeId: String?,
    hiddenNodeIds: Set<String>,
    onSelectNode: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onMoveLayer: (String, Int) -> Unit,
    onMoveToFront: (String) -> Unit,
    onMoveToBack: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onGroup: (String) -> Unit,
    onUngroup: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onAddElement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var renamingNode by remember { mutableStateOf<Pair<String, String>?>(null) }
    var expandedContainers by remember { mutableStateOf<Set<String>>(setOf(rootContainer.id)) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.studio_tab_items),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "${rootContainer.children.size} layers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (rootContainer.children.isEmpty()) {
                Text(
                    text = stringResource(R.string.studio_items_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rootContainer.children.forEachIndexed { index, child ->
                        val canMoveUp = index < rootContainer.children.lastIndex
                        val canMoveDown = index > 0

                        LayerTreeItem(
                            node = child,
                            depth = 0,
                            index = index,
                            isSelected = child.id == selectedNodeId,
                            isExpanded = expandedContainers.contains(child.id),
                            isHiddenInEditor = hiddenNodeIds.contains(child.id),
                            canMoveUp = canMoveUp,
                            canMoveDown = canMoveDown,
                            onSelect = { onSelectNode(child.id) },
                            onToggleExpand = {
                                expandedContainers = if (expandedContainers.contains(child.id)) {
                                    expandedContainers - child.id
                                } else {
                                    expandedContainers + child.id
                                }
                            },
                            onToggleLock = { onToggleLock(child.id) },
                            onToggleVisibility = { onToggleVisibility(child.id) },
                            onMove = { delta -> onMoveLayer(child.id, delta) },
                            onMoveToFront = { onMoveToFront(child.id) },
                            onMoveToBack = { onMoveToBack(child.id) },
                            onDuplicate = { onDuplicate(child.id) },
                            onGroup = { onGroup(child.id) },
                            onUngroup = { onUngroup(child.id) },
                            onStartRename = { renamingNode = Pair(child.id, child.name ?: "") },
                            onDelete = { onDelete(child.id) },
                            onSelectDescendant = onSelectNode,
                            hiddenNodeIds = hiddenNodeIds,
                            selectedNodeId = selectedNodeId,
                            expandedContainers = expandedContainers
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onAddElement(rootContainer.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.studio_items_add))
            }
        }
    }

    renamingNode?.let { (id, currentName) ->
        var inputName by remember(id) { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { renamingNode = null },
            title = { Text(stringResource(R.string.studio_layer_rename)) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(id, inputName)
                        renamingNode = null
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingNode = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun LayerTreeItem(
    node: CustomWidgetNode,
    depth: Int,
    index: Int,
    isSelected: Boolean,
    isExpanded: Boolean,
    isHiddenInEditor: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleVisibility: () -> Unit,
    onMove: (Int) -> Unit,
    onMoveToFront: () -> Unit,
    onMoveToBack: () -> Unit,
    onDuplicate: () -> Unit,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onStartRename: () -> Unit,
    onDelete: () -> Unit,
    onSelectDescendant: (String) -> Unit,
    hiddenNodeIds: Set<String>,
    selectedNodeId: String?,
    expandedContainers: Set<String>
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
    ) {
        val containerColor = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            node.locked -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surface
        }

        val contentColor = when {
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/collapse chevron for groups
                if (node is LayoutContainer && node.children.isNotEmpty()) {
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                    }
                } else {
                    Spacer(Modifier.width(4.dp))
                }

                // Type Icon
                Icon(
                    imageVector = nodeIcon(node, isExpanded),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor
                )

                Spacer(Modifier.width(8.dp))

                // Label and Badges
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name ?: defaultNodeTitle(node),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#$index",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )

                        if (node.bindings.isNotEmpty()) {
                            BadgePill(text = "fx", backgroundColor = MaterialTheme.colorScheme.tertiaryContainer)
                        }

                        if (node.showIf !is NodeCondition.Always) {
                            BadgePill(text = stringResource(R.string.studio_layer_badge_condition), backgroundColor = MaterialTheme.colorScheme.secondaryContainer)
                        }

                        if (node is ButtonNode || node.onClick != null) {
                            BadgePill(text = stringResource(R.string.studio_layer_badge_action), backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                        }
                    }
                }

                // Lock toggle button
                IconButton(onClick = onToggleLock, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (node.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = stringResource(R.string.studio_layer_locked),
                        modifier = Modifier.size(16.dp),
                        tint = if (node.locked) MaterialTheme.colorScheme.error else contentColor.copy(alpha = 0.5f)
                    )
                }

                // Visibility toggle button
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isHiddenInEditor) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = stringResource(if (isHiddenInEditor) R.string.studio_layer_show_editor else R.string.studio_layer_hide_editor),
                        modifier = Modifier.size(16.dp),
                        tint = if (isHiddenInEditor) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else contentColor.copy(alpha = 0.7f)
                    )
                }

                // Overflow / context menu
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = contentColor
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_layer_duplicate)) },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_layer_group)) },
                            leadingIcon = { Icon(Icons.Rounded.Folder, null) },
                            onClick = {
                                showMenu = false
                                onGroup()
                            }
                        )
                        if (node is LayoutContainer) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.studio_layer_ungroup)) },
                                leadingIcon = { Icon(Icons.Rounded.Unarchive, null) },
                                onClick = {
                                    showMenu = false
                                    onUngroup()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_layer_front)) },
                            leadingIcon = { Icon(Icons.Rounded.VerticalAlignTop, null) },
                            onClick = {
                                showMenu = false
                                onMoveToFront()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_layer_back_stack)) },
                            leadingIcon = { Icon(Icons.Rounded.VerticalAlignBottom, null) },
                            onClick = {
                                showMenu = false
                                onMoveToBack()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_layer_rename)) },
                            leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) },
                            onClick = {
                                showMenu = false
                                onStartRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_delete_element), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        // Render nested children when expanded
        if (node is LayoutContainer && isExpanded) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                node.children.forEachIndexed { childIndex, childNode ->
                    val childCanUp = childIndex < node.children.lastIndex
                    val childCanDown = childIndex > 0

                    LayerTreeItem(
                        node = childNode,
                        depth = depth + 1,
                        index = childIndex,
                        isSelected = childNode.id == selectedNodeId,
                        isExpanded = expandedContainers.contains(childNode.id),
                        isHiddenInEditor = hiddenNodeIds.contains(childNode.id),
                        canMoveUp = childCanUp,
                        canMoveDown = childCanDown,
                        onSelect = { onSelectDescendant(childNode.id) },
                        onToggleExpand = {
                            // Handled recursively
                        },
                        onToggleLock = { onToggleLock() },
                        onToggleVisibility = { onToggleVisibility() },
                        onMove = onMove,
                        onMoveToFront = onMoveToFront,
                        onMoveToBack = onMoveToBack,
                        onDuplicate = onDuplicate,
                        onGroup = onGroup,
                        onUngroup = onUngroup,
                        onStartRename = onStartRename,
                        onDelete = onDelete,
                        onSelectDescendant = onSelectDescendant,
                        hiddenNodeIds = hiddenNodeIds,
                        selectedNodeId = selectedNodeId,
                        expandedContainers = expandedContainers
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, backgroundColor: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85f
        )
    }
}

private fun defaultNodeTitle(node: CustomWidgetNode): String = when (node) {
    is TextNode -> if (node.template.isNotBlank()) node.template else "Text"
    is ImageNode -> "Image"
    is ProgressNode -> "Progress"
    is ButtonNode -> if (node.label.isNotBlank()) node.label else "Button"
    is LayoutContainer -> "Group (${node.layout})"
}

private fun nodeIcon(node: CustomWidgetNode, isExpanded: Boolean = false): ImageVector = when (node) {
    is TextNode -> Icons.Rounded.Title
    is ImageNode -> Icons.Rounded.Image
    is ProgressNode -> Icons.Rounded.LinearScale
    is ButtonNode -> Icons.Rounded.SmartButton
    is LayoutContainer -> if (isExpanded) Icons.Rounded.FolderOpen else Icons.Rounded.Folder
}
