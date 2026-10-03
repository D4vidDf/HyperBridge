package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.TextNode
import com.d4viddf.hyperbridge.models.widget.findNode
import com.d4viddf.hyperbridge.models.widget.parentOf

data class BreadcrumbItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isCurrent: Boolean
)

@Composable
fun StudioBreadcrumb(
    document: CustomWidgetDocument,
    selectedNodeId: String?,
    onSelectNode: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val rootLabel = stringResource(R.string.studio_breadcrumb_root)
    val textLabel = stringResource(R.string.studio_add_text)
    val imageLabel = stringResource(R.string.studio_add_image)
    val progressLabel = stringResource(R.string.studio_add_progress)
    val buttonLabel = stringResource(R.string.studio_add_button)
    val groupLabel = stringResource(R.string.studio_add_container)

    val crumbs = remember(document, selectedNodeId, rootLabel) {
        buildCrumbs(
            document = document,
            selectedNodeId = selectedNodeId,
            rootLabel = rootLabel,
            textLabel = textLabel,
            imageLabel = imageLabel,
            progressLabel = progressLabel,
            buttonLabel = buttonLabel,
            groupLabel = groupLabel
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            crumbs.forEachIndexed { index, crumb ->
                val isLast = index == crumbs.lastIndex
                CrumbChip(
                    item = crumb,
                    onClick = {
                        if (crumb.id == document.root.id) {
                            onSelectNode(null)
                        } else {
                            onSelectNode(crumb.id)
                        }
                    }
                )

                if (!isLast) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CrumbChip(
    item: BreadcrumbItem,
    onClick: () -> Unit
) {
    val backgroundColor = if (item.isCurrent) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (item.isCurrent) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildCrumbs(
    document: CustomWidgetDocument,
    selectedNodeId: String?,
    rootLabel: String,
    textLabel: String,
    imageLabel: String,
    progressLabel: String,
    buttonLabel: String,
    groupLabel: String
): List<BreadcrumbItem> {
    val rootCrumb = BreadcrumbItem(
        id = document.root.id,
        label = document.root.name ?: rootLabel,
        icon = Icons.Rounded.Home,
        isCurrent = selectedNodeId == null || selectedNodeId == document.root.id
    )

    if (selectedNodeId == null || selectedNodeId == document.root.id) {
        return listOf(rootCrumb)
    }

    // Trace from selected node up to root
    val path = mutableListOf<CustomWidgetNode>()
    var current: CustomWidgetNode? = document.findNode(selectedNodeId)
    while (current != null && current.id != document.root.id) {
        path.add(0, current)
        current = document.parentOf(current.id)
    }

    val crumbs = mutableListOf(rootCrumb.copy(isCurrent = false))
    path.forEachIndexed { index, node ->
        val isSelected = index == path.lastIndex
        crumbs.add(
            BreadcrumbItem(
                id = node.id,
                label = node.name ?: fallbackNodeLabel(node, textLabel, imageLabel, progressLabel, buttonLabel, groupLabel),
                icon = nodeIcon(node),
                isCurrent = isSelected
            )
        )
    }

    return crumbs
}

private fun fallbackNodeLabel(
    node: CustomWidgetNode,
    textLabel: String,
    imageLabel: String,
    progressLabel: String,
    buttonLabel: String,
    groupLabel: String
): String = when (node) {
    is TextNode -> if (node.template.isNotBlank()) node.template.take(16) else textLabel
    is ImageNode -> imageLabel
    is ProgressNode -> progressLabel
    is ButtonNode -> if (node.label.isNotBlank()) node.label.take(16) else buttonLabel
    is LayoutContainer -> groupLabel
}

private fun nodeIcon(node: CustomWidgetNode): ImageVector = when (node) {
    is TextNode -> Icons.Rounded.Title
    is ImageNode -> Icons.Rounded.Image
    is ProgressNode -> Icons.Rounded.LinearScale
    is ButtonNode -> Icons.Rounded.SmartButton
    is LayoutContainer -> Icons.Rounded.Folder
}
