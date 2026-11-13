package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FirstPage
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Layers
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.d4viddf.hyperbridge.models.widget.ShapeNode
import com.d4viddf.hyperbridge.models.widget.TextNode
import com.d4viddf.hyperbridge.models.widget.findNode

@Composable
fun StudioLayersPanel(
    modifier: Modifier = Modifier,
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
    onAddElement: (String) -> Unit = {},
    onMoveInto: (String, String) -> Unit = { _, _ -> },
    isStandalonePanel: Boolean = false,
    onCollapsePanel: (() -> Unit)? = null

) {
    var renamingNode by remember { mutableStateOf<Pair<String, String>?>(null) }
    var expandedContainers by remember { mutableStateOf(setOf(rootContainer.id)) }
    val allGroups = remember(rootContainer) { findAllContainers(rootContainer) }

    val renderTreeItems: @Composable (Int, Boolean) -> Unit = { baseDepth, isCompactTree ->
        if (rootContainer.children.isEmpty()) {
            Text(
                text = stringResource(R.string.studio_items_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rootContainer.children.forEachIndexed { index, child ->
                    val canMoveUp = index < rootContainer.children.lastIndex
                    val canMoveDown = index > 0

                    LayerTreeItem(
                        node = child,
                        depth = baseDepth,
                        index = index,
                        isSelected = child.id == selectedNodeId,
                        isExpanded = expandedContainers.contains(child.id),
                        isHiddenInEditor = hiddenNodeIds.contains(child.id),
                        canMoveUp = canMoveUp,
                        canMoveDown = canMoveDown,
                        isCompactTree = isCompactTree,
                        rootContainerId = rootContainer.id,
                        allGroups = allGroups,
                        onSelectNode = onSelectNode,
                        onToggleExpand = { containerId ->
                            expandedContainers = if (expandedContainers.contains(containerId)) {
                                expandedContainers - containerId
                            } else {
                                expandedContainers + containerId
                            }
                        },
                        onToggleLock = onToggleLock,
                        onToggleVisibility = onToggleVisibility,
                        onMoveLayer = onMoveLayer,
                        onMoveToFront = onMoveToFront,
                        onMoveToBack = onMoveToBack,
                        onDuplicate = onDuplicate,
                        onGroup = onGroup,
                        onUngroup = onUngroup,
                        onStartRename = { renamingNode = Pair(child.id, child.name ?: "") },
                        onDelete = onDelete,
                        onMoveInto = onMoveInto,
                        hiddenNodeIds = hiddenNodeIds,
                        selectedNodeId = selectedNodeId,
                        expandedContainers = expandedContainers
                    )
                }
            }
        }
    }

    if (isStandalonePanel) {
        val isRootSelected = selectedNodeId == null || selectedNodeId == rootContainer.id
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val isCompactTree = maxWidth < 250.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.studio_element_tree_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.studio_element_tree_count_fmt, rootContainer.children.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { onAddElement(selectedNodeId ?: rootContainer.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.studio_add_element_title),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        onCollapsePanel?.let { onCollapse ->
                            IconButton(
                                onClick = onCollapse,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FirstPage,
                                    contentDescription = stringResource(R.string.studio_element_tree_hide_cd),
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Root Canvas Node (Always selectable at the top of the tree)
                    Surface(
                        selected = isRootSelected,
                        onClick = { onSelectNode(rootContainer.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRootSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isRootSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.studio_root_canvas_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isRootSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (!isCompactTree) {
                                    Text(
                                        text = stringResource(R.string.studio_root_canvas_subtitle),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isRootSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    renderTreeItems(1, isCompactTree)
                }
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                val isCompactTree = maxWidth < 250.dp
                Column(
                    modifier = Modifier.fillMaxWidth(),
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

                    renderTreeItems(0, isCompactTree)
                }
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
    isCompactTree: Boolean,
    rootContainerId: String,
    allGroups: List<LayoutContainer>,
    onSelectNode: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onMoveLayer: (String, Int) -> Unit,
    onMoveToFront: (String) -> Unit,
    onMoveToBack: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onGroup: (String) -> Unit,
    onUngroup: (String) -> Unit,
    onStartRename: () -> Unit,
    onDelete: (String) -> Unit,
    onMoveInto: (String, String) -> Unit,
    hiddenNodeIds: Set<String>,
    selectedNodeId: String?,
    expandedContainers: Set<String>
) {
    var showMenu by remember { mutableStateOf(false) }
    var showTargetGroupDialog by remember { mutableStateOf(false) }
    val candidateGroups = remember(allGroups, node.id) {
        allGroups.filter { it.id != node.id && (node !is LayoutContainer || it.findNode(node.id) == null) }
    }
    val stepIndentDp = if (isCompactTree) 10 else 16

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * stepIndentDp).dp)
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

        Surface(
            selected = isSelected,
            onClick = { onSelectNode(node.id) },
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompactTree) 8.dp else 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/collapse chevron for groups
                if (node is LayoutContainer && node.children.isNotEmpty()) {
                    IconButton(
                        onClick = { onToggleExpand(node.id) },
                        modifier = Modifier.size(if (isCompactTree) 26.dp else 32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = stringResource(if (isExpanded) R.string.studio_layer_collapse else R.string.studio_layer_expand),
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

                        // Compact status indicators when Lock/Hide buttons are moved into the overflow menu
                        if (isCompactTree && node.locked) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = stringResource(R.string.studio_layer_locked),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        if (isCompactTree && isHiddenInEditor) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = stringResource(R.string.studio_layer_show_editor),
                                modifier = Modifier.size(12.dp),
                                tint = contentColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                if (!isCompactTree) {
                    // Quick move buttons when selected
                    if (isSelected && canMoveUp) {
                        IconButton(
                            onClick = { onMoveLayer(node.id, 1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = stringResource(R.string.studio_layer_move_up),
                                modifier = Modifier.size(15.dp),
                                tint = contentColor.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (isSelected && canMoveDown) {
                        IconButton(
                            onClick = { onMoveLayer(node.id, -1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = stringResource(R.string.studio_layer_move_down),
                                modifier = Modifier.size(15.dp),
                                tint = contentColor.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Lock toggle button
                    IconButton(
                        onClick = { onToggleLock(node.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (node.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = stringResource(if (node.locked) R.string.studio_layer_locked else R.string.studio_layer_unlocked),
                            modifier = Modifier.size(16.dp),
                            tint = if (node.locked) MaterialTheme.colorScheme.error else contentColor.copy(alpha = 0.5f)
                        )
                    }

                    // Visibility toggle button
                    IconButton(
                        onClick = { onToggleVisibility(node.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isHiddenInEditor) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = stringResource(if (isHiddenInEditor) R.string.studio_layer_show_editor else R.string.studio_layer_hide_editor),
                            modifier = Modifier.size(16.dp),
                            tint = if (isHiddenInEditor) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else contentColor.copy(alpha = 0.7f)
                        )
                    }
                }

                // Overflow / context menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(if (isCompactTree) 28.dp else 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.studio_layer_more_options),
                            modifier = Modifier.size(16.dp),
                            tint = contentColor
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // In compact tree mode, Lock & Visibility buttons are hidden from the row and shown in the menu
                        if (isCompactTree) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (node.locked) R.string.studio_layer_unlocked else R.string.studio_layer_locked
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (node.locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleLock(node.id)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (isHiddenInEditor) R.string.studio_layer_show_editor else R.string.studio_layer_hide_editor
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isHiddenInEditor) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleVisibility(node.id)
                                }
                            )
                            HorizontalDivider()
                        }

                        if (canMoveUp || canMoveDown) {
                            if (canMoveUp) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.studio_layer_move_up)) },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                                    onClick = {
                                        showMenu = false
                                        onMoveLayer(node.id, 1)
                                    }
                                )
                            }
                            if (canMoveDown) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.studio_layer_move_down)) },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) },
                                    onClick = {
                                        showMenu = false
                                        onMoveLayer(node.id, -1)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.studio_layer_front)) },
                                leadingIcon = { Icon(Icons.Rounded.VerticalAlignTop, null) },
                                onClick = {
                                    showMenu = false
                                    onMoveToFront(node.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.studio_layer_back_stack)) },
                                leadingIcon = { Icon(Icons.Rounded.VerticalAlignBottom, null) },
                                onClick = {
                                    showMenu = false
                                    onMoveToBack(node.id)
                                }
                            )
                            HorizontalDivider()
                        }

                        if (depth > 0) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.studio_layer_move_out_of_group)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                                onClick = {
                                    showMenu = false
                                    onMoveInto(node.id, rootContainerId)
                                }
                            )
                        }
                        if (candidateGroups.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.studio_layer_move_into_group)) },
                                leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) },
                                onClick = {
                                    showMenu = false
                                    if (candidateGroups.size == 1) {
                                        onMoveInto(node.id, candidateGroups.first().id)
                                    } else {
                                        showTargetGroupDialog = true
                                    }
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_layer_group)) },
                            leadingIcon = { Icon(Icons.Rounded.Folder, null) },
                            onClick = {
                                showMenu = false
                                onGroup(node.id)
                            }
                        )
                        if (node is LayoutContainer) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.studio_layer_ungroup)) },
                                leadingIcon = { Icon(Icons.Rounded.Unarchive, null) },
                                onClick = {
                                    showMenu = false
                                    onUngroup(node.id)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_layer_duplicate)) },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                            onClick = {
                                showMenu = false
                                onDuplicate(node.id)
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
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.studio_delete_element), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete(node.id)
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
                        isCompactTree = isCompactTree,
                        rootContainerId = rootContainerId,
                        allGroups = allGroups,
                        onSelectNode = onSelectNode,
                        onToggleExpand = onToggleExpand,
                        onToggleLock = onToggleLock,
                        onToggleVisibility = onToggleVisibility,
                        onMoveLayer = onMoveLayer,
                        onMoveToFront = onMoveToFront,
                        onMoveToBack = onMoveToBack,
                        onDuplicate = onDuplicate,
                        onGroup = onGroup,
                        onUngroup = onUngroup,
                        onStartRename = onStartRename,
                        onDelete = onDelete,
                        onMoveInto = onMoveInto,
                        hiddenNodeIds = hiddenNodeIds,
                        selectedNodeId = selectedNodeId,
                        expandedContainers = expandedContainers
                    )
                }
            }
        }
    }

    if (showTargetGroupDialog) {
        AlertDialog(
            onDismissRequest = { showTargetGroupDialog = false },
            title = { Text(stringResource(R.string.studio_layer_move_into_group)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    candidateGroups.forEach { group ->
                        Surface(
                            onClick = {
                                onMoveInto(node.id, group.id)
                                showTargetGroupDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = group.name ?: "Group (${group.id.take(6)})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTargetGroupDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

private fun findAllContainers(container: LayoutContainer, excludeId: String = ""): List<LayoutContainer> {
    val list = mutableListOf<LayoutContainer>()
    container.children.filterIsInstance<LayoutContainer>().forEach {
        if (it.id != excludeId) {
            list.add(it)
            list.addAll(findAllContainers(it, excludeId))
        }
    }
    return list
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
    is TextNode -> node.template.ifBlank { "Text" }
    is ImageNode -> "Image"
    is ShapeNode -> "Shape"
    is ProgressNode -> "Progress"
    is ButtonNode -> node.label.ifBlank { "Button" }
    is LayoutContainer -> "Group (${node.layout})"
}

private fun nodeIcon(node: CustomWidgetNode, isExpanded: Boolean = false): ImageVector = when (node) {
    is TextNode -> Icons.Rounded.Title
    is ImageNode -> Icons.Rounded.Image
    is ShapeNode -> Icons.Rounded.Category
    is ProgressNode -> Icons.Rounded.LinearScale
    is ButtonNode -> Icons.Rounded.SmartButton
    is LayoutContainer -> if (isExpanded) Icons.Rounded.FolderOpen else Icons.Rounded.Folder
}
