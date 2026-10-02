package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.ContainerLayout
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.NodeBounds
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.TextNode
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor

/**
 * The Studio's canvas: a pure-Compose re-implementation of the AST, deliberately separate from the
 * RemoteViews [com.d4viddf.hyperbridge.service.widget.CustomWidgetRenderer] used at dispatch time,
 * because RemoteViews has no selection, drag or resize affordances of its own.
 *
 * The canvas is always dark. A HyperOS island is dark whatever the phone's theme is, so tying the
 * backdrop to `colorScheme` made the preview light-on-light in dark mode (#328).
 */
val StudioCanvasBackground = Color(0xFF141414)

private val previewEngine = WidgetVariableEngine()

private val PREVIEW_SAMPLE_CONTEXT = VariableContext(
    notifTitle = "Sample title",
    notifText = "Sample text",
    notifProgress = 42,
    deviceBatteryPercent = 77,
    timeNowFormatted = "10:30",
    notificationActionTitles = listOf("Open", "Mute"),
    hasInlineReply = true,
    smartActionTypes = setOf("OTP", "URL")
)

@Composable
fun StudioCanvas(
    root: LayoutContainer,
    canvasHeightDp: Int,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, dxDp: Int, dyDp: Int) -> Unit,
    onResize: (id: String, widthDp: Int, heightDp: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeightDp.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(StudioCanvasBackground)
            .pointerInput(root.id) {
                detectTapGestures { onSelect(root.id) }
            }
            .padding(8.dp)
    ) {
        CanvasNode(
            node = root,
            selectedId = selectedId,
            onSelect = onSelect,
            onMove = onMove,
            onResize = onResize
        )
    }
}

@Composable
private fun CanvasNode(
    node: CustomWidgetNode,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, dxDp: Int, dyDp: Int) -> Unit,
    onResize: (id: String, widthDp: Int, heightDp: Int) -> Unit,
    draggable: Boolean = false
) {
    val density = LocalDensity.current
    val isSelected = node.id == selectedId

    var modifier: Modifier = Modifier
    if (node.bounds.widthDp != null) modifier = modifier.width(node.bounds.widthDp!!.dp)
    if (node.bounds.heightDp != null) modifier = modifier.height(node.bounds.heightDp!!.dp)

    modifier = modifier
        .pointerInput(node.id, draggable) {
            detectTapGestures { onSelect(node.id) }
        }
        .then(
            if (draggable) {
                Modifier.pointerInput(node.id) {
                    detectDragGestures(
                        onDragStart = { onSelect(node.id) }
                    ) { change, dragAmount ->
                        change.consume()
                        val dx = with(density) { dragAmount.x.toDp().value.toInt() }
                        val dy = with(density) { dragAmount.y.toDp().value.toInt() }
                        if (dx != 0 || dy != 0) onMove(node.id, dx, dy)
                    }
                }
            } else Modifier
        )
        .then(
            if (isSelected) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            } else Modifier
        )

    Box {
        when (node) {
            is LayoutContainer -> CanvasContainer(node, modifier, selectedId, onSelect, onMove, onResize)
            is TextNode -> Text(
                text = previewEngine.resolve(node.template, PREVIEW_SAMPLE_CONTEXT).ifBlank { node.template },
                color = safeParseColor(node.colorHex),
                fontSize = TextUnit(node.fontSizeSp.toFloat(), TextUnitType.Sp),
                fontWeight = if (node.bold) FontWeight.Bold else FontWeight.Normal,
                maxLines = node.maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier
            )

            is ImageNode -> Box(
                modifier = modifier
                    .size((node.bounds.widthDp ?: 24).dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
            )

            is ProgressNode -> {
                val value = previewEngine.resolve(node.valueTemplate, PREVIEW_SAMPLE_CONTEXT).toIntOrNull() ?: 0
                LinearProgressIndicator(
                    progress = { (value.toFloat() / node.maxValue.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    color = safeParseColor(node.progressColorHex),
                    trackColor = safeParseColor(node.trackColorHex),
                    modifier = modifier.width((node.bounds.widthDp ?: 64).dp)
                )
            }

            is ButtonNode -> Box(
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

        // Resize grip for the selected element, bottom-right like every canvas editor.
        if (isSelected && draggable) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    .pointerInput(node.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dw = with(density) { dragAmount.x.toDp().value.toInt() }
                            val dh = with(density) { dragAmount.y.toDp().value.toInt() }
                            val width = ((node.bounds.widthDp ?: defaultWidthOf(node)) + dw).coerceAtLeast(MIN_SIZE_DP)
                            val height = ((node.bounds.heightDp ?: defaultHeightOf(node)) + dh).coerceAtLeast(MIN_SIZE_DP)
                            onResize(node.id, width, height)
                        }
                    }
            )
        }
    }
}

@Composable
private fun CanvasContainer(
    node: LayoutContainer,
    modifier: Modifier,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, dxDp: Int, dyDp: Int) -> Unit,
    onResize: (id: String, widthDp: Int, heightDp: Int) -> Unit
) {
    // Children of a free-positioned container can be dragged; in a row or column the layout owns
    // their position, so dragging them would be a lie.
    val freePositioning = node.layout == ContainerLayout.ABSOLUTE || node.layout == ContainerLayout.BOX

    when (node.layout) {
        ContainerLayout.ROW -> Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(node.gapDp.dp)
        ) {
            node.children.forEach { CanvasNode(it, selectedId, onSelect, onMove, onResize, draggable = false) }
        }

        ContainerLayout.COLUMN -> Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(node.gapDp.dp)
        ) {
            node.children.forEach { CanvasNode(it, selectedId, onSelect, onMove, onResize, draggable = false) }
        }

        ContainerLayout.BOX, ContainerLayout.ABSOLUTE -> Box(modifier = modifier.fillMaxSize()) {
            // Later children paint on top: the child order is the layer order (#328).
            node.children.forEach { child ->
                Box(modifier = Modifier.offset(x = child.bounds.x.dp, y = child.bounds.y.dp)) {
                    CanvasNode(child, selectedId, onSelect, onMove, onResize, draggable = freePositioning)
                }
            }
        }
    }
}

private const val MIN_SIZE_DP = 8

private fun defaultWidthOf(node: CustomWidgetNode): Int = when (node) {
    is ImageNode -> 24
    is ProgressNode -> 64
    else -> 80
}

private fun defaultHeightOf(node: CustomWidgetNode): Int = when (node) {
    is ImageNode -> 24
    is ProgressNode -> 8
    else -> 24
}

/** Applies a drag to a node's bounds, keeping it inside the canvas. */
fun NodeBounds.movedBy(dxDp: Int, dyDp: Int, canvasWidthDp: Int, canvasHeightDp: Int): NodeBounds = copy(
    x = (x + dxDp).coerceIn(0, (canvasWidthDp - (widthDp ?: 0)).coerceAtLeast(0)),
    y = (y + dyDp).coerceIn(0, (canvasHeightDp - (heightDp ?: 0)).coerceAtLeast(0))
)
