package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.widget.*
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

@Composable
fun StudioCanvas(
    root: LayoutContainer,
    canvasHeightDp: Int,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, dxDp: Int, dyDp: Int) -> Unit,
    onResize: (id: String, widthDp: Int, heightDp: Int) -> Unit,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    isGridVisible: Boolean = false,
    isWireframeMode: Boolean = false,
    hiddenNodeIds: Set<String> = emptySet(),
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    context: VariableContext = scenario.toVariableContext()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeightDp.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(StudioCanvasBackground)
            .drawBehind {
                if (isGridVisible) {
                    val stepPx = 16.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.08f)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += stepPx
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += stepPx
                    }
                }
            }
            .pointerInput(root.id) {
                detectTapGestures { onSelect(root.id) }
            }
            .padding(8.dp)
    ) {
        // Enforce LTR Cartesian layout coordinate space so RTL system locales never mirror element coordinates
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                    }
            ) {
                CanvasNode(
                    node = root,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onMove = onMove,
                    onResize = onResize,
                    isWireframeMode = isWireframeMode,
                    hiddenNodeIds = hiddenNodeIds,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun CanvasNode(
    node: CustomWidgetNode,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, dxDp: Int, dyDp: Int) -> Unit,
    onResize: (id: String, widthDp: Int, heightDp: Int) -> Unit,
    draggable: Boolean = false,
    isWireframeMode: Boolean = false,
    hiddenNodeIds: Set<String> = emptySet(),
    context: VariableContext = VariableContext()
) {
    if (hiddenNodeIds.contains(node.id)) return

    val density = LocalDensity.current
    val isSelected = node.id == selectedId
    val canDrag = draggable && !node.locked

    // Evaluate conditional visibility
    val isConditionMet = isWireframeMode || NodeConditionEvaluator.isVisible(node.showIf, context, previewEngine)
    if (!isConditionMet && !isSelected) {
        // Condition not met and element is not currently selected -> do not render on live canvas
        return
    }

    var accumulatedDx by remember(node.id) { mutableFloatStateOf(0f) }
    var accumulatedDy by remember(node.id) { mutableFloatStateOf(0f) }

    var modifier: Modifier = Modifier
    if (node.bounds.widthDp != null) modifier = modifier.width(node.bounds.widthDp!!.dp)
    if (node.bounds.heightDp != null) modifier = modifier.height(node.bounds.heightDp!!.dp)

    modifier = modifier
        .pointerInput(node.id, canDrag) {
            detectTapGestures { onSelect(node.id) }
        }
        .then(
            if (canDrag) {
                Modifier.pointerInput(node.id) {
                    detectDragGestures(
                        onDragStart = {
                            accumulatedDx = 0f
                            accumulatedDy = 0f
                            onSelect(node.id)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        with(density) {
                            accumulatedDx += dragAmount.x.toDp().value
                            accumulatedDy += dragAmount.y.toDp().value
                        }
                        val dx = accumulatedDx.toInt()
                        val dy = accumulatedDy.toInt()
                        if (dx != 0 || dy != 0) {
                            accumulatedDx -= dx
                            accumulatedDy -= dy
                            onMove(node.id, dx, dy)
                        }
                    }
                }
            } else Modifier
        )
        .then(
            if (isSelected) {
                Modifier.border(
                    width = 1.dp,
                    color = if (!isConditionMet) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                )
            } else Modifier
        )
        .then(
            if (!isConditionMet) {
                // If condition not met but selected, render semi-transparent
                Modifier.graphicsLayer { alpha = 0.5f }
            } else Modifier
        )

    Box {
        when (node) {
            is LayoutContainer -> CanvasContainer(
                node = node,
                modifier = modifier,
                selectedId = selectedId,
                onSelect = onSelect,
                onMove = onMove,
                onResize = onResize,
                isWireframeMode = isWireframeMode,
                hiddenNodeIds = hiddenNodeIds,
                context = context
            )
            is TextNode -> {
                val colorFormula = node.bindings[BindableProperty.TEXT_COLOR.key]
                val resolvedColor = if (!isWireframeMode && !colorFormula.isNullOrBlank()) {
                    previewEngine.resolve(colorFormula, context).ifBlank { node.colorHex }
                } else node.colorHex

                Text(
                    text = if (isWireframeMode) node.template else previewEngine.resolve(node.template, context).ifBlank { node.template },
                    color = safeParseColor(resolvedColor),
                    fontSize = TextUnit(node.fontSizeSp.toFloat(), TextUnitType.Sp),
                    fontWeight = if (node.bold) FontWeight.Bold else FontWeight.Normal,
                    maxLines = node.maxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = modifier
                )
            }

            is ImageNode -> {
                val tintFormula = node.bindings[BindableProperty.IMAGE_TINT.key]
                val resolvedTint = if (!isWireframeMode && !tintFormula.isNullOrBlank()) {
                    previewEngine.resolve(tintFormula, context).ifBlank { node.tintHex }
                } else node.tintHex

                @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                val shape = com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId(node.shapeId).toShape()
                val tintColor = resolvedTint?.let { safeParseColor(it) } ?: Color.White

                Box(
                    modifier = modifier
                        .size((node.bounds.widthDp ?: 24).dp)
                        .background(tintColor.copy(alpha = 0.25f), shape)
                        .border(1.dp, tintColor.copy(alpha = 0.5f), shape)
                        .clip(shape),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when (val src = node.source) {
                        is ImageSource.SystemGlyph -> studioGlyphIcon(src.glyphName)
                        is ImageSource.AppIconOf -> Icons.Rounded.Android
                        else -> Icons.Rounded.Image
                    }
                    val iconSize = ((node.bounds.widthDp ?: 24) * 0.65).coerceAtLeast(12.0).dp
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            is ProgressNode -> {
                val value = if (isWireframeMode) 50 else previewEngine.resolve(node.valueTemplate, context).toIntOrNull() ?: 0
                val progressColorFormula = node.bindings[BindableProperty.PROGRESS_COLOR.key]
                val trackColorFormula = node.bindings[BindableProperty.PROGRESS_TRACK_COLOR.key]

                val resolvedProgressColor = if (!isWireframeMode && !progressColorFormula.isNullOrBlank()) {
                    previewEngine.resolve(progressColorFormula, context).ifBlank { node.progressColorHex }
                } else node.progressColorHex

                val resolvedTrackColor = if (!isWireframeMode && !trackColorFormula.isNullOrBlank()) {
                    previewEngine.resolve(trackColorFormula, context).ifBlank { node.trackColorHex }
                } else node.trackColorHex

                LinearProgressIndicator(
                    progress = { (value.toFloat() / node.maxValue.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    color = safeParseColor(resolvedProgressColor),
                    trackColor = safeParseColor(resolvedTrackColor),
                    modifier = modifier.width((node.bounds.widthDp ?: 64).dp)
                )
            }

            is ButtonNode -> {
                val bgFormula = node.bindings[BindableProperty.BUTTON_BACKGROUND.key]
                val textFormula = node.bindings[BindableProperty.BUTTON_TEXT_COLOR.key]

                val resolvedBg = if (!isWireframeMode && !bgFormula.isNullOrBlank()) {
                    previewEngine.resolve(bgFormula, context).ifBlank { node.backgroundHex }
                } else node.backgroundHex

                val resolvedText = if (!isWireframeMode && !textFormula.isNullOrBlank()) {
                    previewEngine.resolve(textFormula, context).ifBlank { node.textColorHex }
                } else node.textColorHex

                Box(
                    modifier = modifier
                        .background(
                            resolvedBg?.let { safeParseColor(it) } ?: MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = node.label, color = safeParseColor(resolvedText))
                }
            }
        }

        // Float-accumulated resize grip for the selected element
        if (isSelected && canDrag) {
            var accumulatedDw by remember(node.id) { mutableFloatStateOf(0f) }
            var accumulatedDh by remember(node.id) { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    .pointerInput(node.id) {
                        detectDragGestures(
                            onDragStart = {
                                accumulatedDw = 0f
                                accumulatedDh = 0f
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            with(density) {
                                accumulatedDw += dragAmount.x.toDp().value
                                accumulatedDh += dragAmount.y.toDp().value
                            }
                            val dw = accumulatedDw.toInt()
                            val dh = accumulatedDh.toInt()
                            if (dw != 0 || dh != 0) {
                                accumulatedDw -= dw
                                accumulatedDh -= dh
                                val width = ((node.bounds.widthDp ?: defaultWidthOf(node)) + dw).coerceAtLeast(MIN_SIZE_DP)
                                val height = ((node.bounds.heightDp ?: defaultHeightOf(node)) + dh).coerceAtLeast(MIN_SIZE_DP)
                                onResize(node.id, width, height)
                            }
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
    onResize: (id: String, widthDp: Int, heightDp: Int) -> Unit,
    isWireframeMode: Boolean = false,
    hiddenNodeIds: Set<String> = emptySet(),
    context: VariableContext
) {
    val freePositioning = node.layout == ContainerLayout.ABSOLUTE || node.layout == ContainerLayout.BOX

    when (node.layout) {
        ContainerLayout.ROW -> Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(node.gapDp.dp)
        ) {
            node.children.forEach {
                CanvasNode(
                    node = it,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onMove = onMove,
                    onResize = onResize,
                    draggable = false,
                    isWireframeMode = isWireframeMode,
                    hiddenNodeIds = hiddenNodeIds,
                    context = context
                )
            }
        }

        ContainerLayout.COLUMN -> Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(node.gapDp.dp)
        ) {
            node.children.forEach {
                CanvasNode(
                    node = it,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onMove = onMove,
                    onResize = onResize,
                    draggable = false,
                    isWireframeMode = isWireframeMode,
                    hiddenNodeIds = hiddenNodeIds,
                    context = context
                )
            }
        }

        ContainerLayout.BOX, ContainerLayout.ABSOLUTE -> Box(modifier = modifier.fillMaxSize()) {
            node.children.forEach { child ->
                val childModifier = if (node.layout == ContainerLayout.ABSOLUTE) {
                    Modifier.absoluteOffset(child.bounds.x.dp, child.bounds.y.dp)
                } else Modifier

                Box(modifier = childModifier) {
                    CanvasNode(
                        node = child,
                        selectedId = selectedId,
                        onSelect = onSelect,
                        onMove = onMove,
                        onResize = onResize,
                        draggable = freePositioning,
                        isWireframeMode = isWireframeMode,
                        hiddenNodeIds = hiddenNodeIds,
                        context = context
                    )
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

private fun studioGlyphIcon(glyphName: String): androidx.compose.ui.graphics.vector.ImageVector = when (glyphName.lowercase()) {
    "notification", "notif" -> Icons.Rounded.Notifications
    "play" -> Icons.Rounded.PlayArrow
    "pause" -> Icons.Rounded.SmartButton
    "message", "mail" -> Icons.Rounded.Email
    "call", "phone" -> Icons.Rounded.Call
    "check" -> Icons.Rounded.Check
    "close" -> Icons.Rounded.Clear
    else -> Icons.Rounded.Image
}

