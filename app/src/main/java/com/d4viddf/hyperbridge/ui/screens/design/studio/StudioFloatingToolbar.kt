package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridOff
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import kotlin.math.hypot

/**
 * Screen dock position for the Studio Floating Toolbar / FAB overlay.
 */
enum class ToolbarDockPosition {
    LEFT,
    RIGHT,
    BOTTOM
}

/**
 * Movable, dockable floating toolbar and FAB overlay for the Studio Design Screen.
 *
 * Can be positioned across the entire screen:
 * - Docks to the right side (vertical, default).
 * - Docks to the left side (vertical).
 * - Docks to the bottom of the screen (horizontal).
 *
 * Offers interactive drag-to-dock, explicit dock switching, collapsible side/bottom handles,
 * and prominent Add Element action so it stays completely clear of editing content on phones.
 */
@Composable
fun StudioFloatingToolbar(
    zoom: Float,
    isGridVisible: Boolean,
    isWireframeMode: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleWireframe: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAddElement: (() -> Unit)? = null,
    maxCanvasHeightDp: Int = 200,
    initialExpanded: Boolean = false,
    initialDockPosition: ToolbarDockPosition = ToolbarDockPosition.RIGHT
) {
    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current

    var dockPosition by remember { mutableStateOf(initialDockPosition) }
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var offsetYDp by remember { mutableFloatStateOf(60f) }
    var offsetXDp by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = modifier) {
        val screenHeightDp = maxHeight.value
        val screenWidthDp = maxWidth.value
        val maxOffsetYDp = (screenHeightDp - 220f).coerceAtLeast(0f)
        val maxOffsetXDp = (screenWidthDp / 2f - 70f).coerceAtLeast(0f)

        val alignment = when (dockPosition) {
            ToolbarDockPosition.LEFT -> Alignment.TopStart
            ToolbarDockPosition.RIGHT -> Alignment.TopEnd
            ToolbarDockPosition.BOTTOM -> Alignment.BottomCenter
        }

        val boxOffsetModifier = when (dockPosition) {
            ToolbarDockPosition.LEFT, ToolbarDockPosition.RIGHT -> {
                Modifier.offset {
                    IntOffset(0, with(density) { offsetYDp.dp.roundToPx() })
                }
            }
            ToolbarDockPosition.BOTTOM -> {
                Modifier
                    .padding(bottom = 12.dp)
                    .offset {
                        IntOffset(with(density) { offsetXDp.dp.roundToPx() }, 0)
                    }
            }
        }

        val cycleDock = {
            dockPosition = when (dockPosition) {
                ToolbarDockPosition.RIGHT -> ToolbarDockPosition.BOTTOM
                ToolbarDockPosition.BOTTOM -> ToolbarDockPosition.LEFT
                ToolbarDockPosition.LEFT -> ToolbarDockPosition.RIGHT
            }
        }

        Box(
            modifier = Modifier
                .align(alignment)
                .then(boxOffsetModifier)
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = 500f)) togetherWith
                        fadeOut(animationSpec = spring(stiffness = 500f))
                },
                label = "StudioFloatingToolbarAnimation"
            ) { expanded ->
                if (expanded) {
                    when (dockPosition) {
                        ToolbarDockPosition.LEFT, ToolbarDockPosition.RIGHT -> {
                            VerticalExpandedToolbar(
                                dockPosition = dockPosition,
                                isGridVisible = isGridVisible,
                                isWireframeMode = isWireframeMode,
                                onCollapse = { isExpanded = false },
                                onCycleDock = cycleDock,
                                onOpenAddElement = onOpenAddElement,
                                onZoomIn = onZoomIn,
                                onZoomOut = onZoomOut,
                                onResetZoom = onResetZoom,
                                onToggleGrid = onToggleGrid,
                                onToggleWireframe = onToggleWireframe,
                                onDrag = { dx, dy ->
                                    offsetYDp = (offsetYDp + dy).coerceIn(0f, maxOffsetYDp)
                                    if (dockPosition == ToolbarDockPosition.RIGHT) {
                                        if (dx < -90f) {
                                            dockPosition = if (dy > 60f) ToolbarDockPosition.BOTTOM else ToolbarDockPosition.LEFT
                                        } else if (dx > 20f) {
                                            isExpanded = false
                                        }
                                    } else {
                                        if (dx > 90f) {
                                            dockPosition = if (dy > 60f) ToolbarDockPosition.BOTTOM else ToolbarDockPosition.RIGHT
                                        } else if (dx < -20f) {
                                            isExpanded = false
                                        }
                                    }
                                }
                            )
                        }
                        ToolbarDockPosition.BOTTOM -> {
                            HorizontalExpandedToolbar(
                                isGridVisible = isGridVisible,
                                isWireframeMode = isWireframeMode,
                                onCollapse = { isExpanded = false },
                                onCycleDock = cycleDock,
                                onOpenAddElement = onOpenAddElement,
                                onZoomIn = onZoomIn,
                                onZoomOut = onZoomOut,
                                onResetZoom = onResetZoom,
                                onToggleGrid = onToggleGrid,
                                onToggleWireframe = onToggleWireframe,
                                onDrag = { dx, dy ->
                                    offsetXDp = (offsetXDp + dx).coerceIn(-maxOffsetXDp, maxOffsetXDp)
                                    if (dy < -80f) {
                                        dockPosition = if (dx < -30f) ToolbarDockPosition.LEFT else ToolbarDockPosition.RIGHT
                                    } else if (dy > 20f) {
                                        isExpanded = false
                                    }
                                }
                            )
                        }
                    }
                } else {
                    CollapsedDockHandle(
                        dockPosition = dockPosition,
                        onExpand = { isExpanded = true },
                        onOpenAddElement = onOpenAddElement,
                        onDrag = { dx, dy ->
                            when (dockPosition) {
                                ToolbarDockPosition.RIGHT -> {
                                    offsetYDp = (offsetYDp + dy).coerceIn(0f, maxOffsetYDp)
                                    if (dx < -70f) {
                                        dockPosition = if (dy > 60f) ToolbarDockPosition.BOTTOM else ToolbarDockPosition.LEFT
                                    }
                                }
                                ToolbarDockPosition.LEFT -> {
                                    offsetYDp = (offsetYDp + dy).coerceIn(0f, maxOffsetYDp)
                                    if (dx > 70f) {
                                        dockPosition = if (dy > 60f) ToolbarDockPosition.BOTTOM else ToolbarDockPosition.RIGHT
                                    }
                                }
                                ToolbarDockPosition.BOTTOM -> {
                                    offsetXDp = (offsetXDp + dx).coerceIn(-maxOffsetXDp, maxOffsetXDp)
                                    if (dy < -60f) {
                                        dockPosition = if (dx < -30f) ToolbarDockPosition.LEFT else ToolbarDockPosition.RIGHT
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalExpandedToolbar(
    dockPosition: ToolbarDockPosition,
    isGridVisible: Boolean,
    isWireframeMode: Boolean,
    onCollapse: () -> Unit,
    onCycleDock: () -> Unit,
    onOpenAddElement: (() -> Unit)?,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleWireframe: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    val density = LocalDensity.current

    Surface(
        modifier = Modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag and collapse header
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .pointerInput(dockPosition) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            with(density) {
                                onDrag(dragAmount.x.toDp().value, dragAmount.y.toDp().value)
                            }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FilledIconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (dockPosition == ToolbarDockPosition.LEFT) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.studio_toolbar_collapse),
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledIconButton(
                    onClick = onCycleDock,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (dockPosition == ToolbarDockPosition.RIGHT) {
                            Icons.Rounded.VerticalAlignBottom
                        } else {
                            Icons.Rounded.ChevronRight
                        },
                        contentDescription = stringResource(R.string.studio_toolbar_dock_cycle),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Primary Add FAB Action
            onOpenAddElement?.let { onAdd ->
                FilledIconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.studio_add_element_title),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            FilledIconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ZoomIn,
                    contentDescription = stringResource(R.string.studio_toolbar_zoom_in),
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledIconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ZoomOut,
                    contentDescription = stringResource(R.string.studio_toolbar_zoom_out),
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledIconButton(
                onClick = onResetZoom,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.CenterFocusStrong,
                    contentDescription = stringResource(R.string.studio_toolbar_zoom_reset),
                    modifier = Modifier.size(18.dp)
                )
            }

            FilledIconButton(
                onClick = onToggleGrid,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isGridVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (isGridVisible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (isGridVisible) Icons.Rounded.GridOn else Icons.Rounded.GridOff,
                    contentDescription = stringResource(R.string.studio_toolbar_grid),
                    modifier = Modifier.size(18.dp)
                )
            }

            FilledIconButton(
                onClick = onToggleWireframe,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isWireframeMode) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (isWireframeMode) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (isWireframeMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(R.string.studio_toolbar_wireframe),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HorizontalExpandedToolbar(
    isGridVisible: Boolean,
    isWireframeMode: Boolean,
    onCollapse: () -> Unit,
    onCycleDock: () -> Unit,
    onOpenAddElement: (() -> Unit)?,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleWireframe: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    val density = LocalDensity.current

    Surface(
        modifier = Modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        with(density) {
                            onDrag(dragAmount.x.toDp().value, dragAmount.y.toDp().value)
                        }
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onCollapse,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = stringResource(R.string.studio_toolbar_collapse),
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledIconButton(
                onClick = onCycleDock,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.studio_toolbar_dock_cycle),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Primary Add FAB Action
            onOpenAddElement?.let { onAdd ->
                FilledIconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.studio_add_element_title),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            FilledIconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ZoomIn,
                    contentDescription = stringResource(R.string.studio_toolbar_zoom_in),
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledIconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ZoomOut,
                    contentDescription = stringResource(R.string.studio_toolbar_zoom_out),
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledIconButton(
                onClick = onResetZoom,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.CenterFocusStrong,
                    contentDescription = stringResource(R.string.studio_toolbar_zoom_reset),
                    modifier = Modifier.size(18.dp)
                )
            }

            FilledIconButton(
                onClick = onToggleGrid,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isGridVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (isGridVisible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (isGridVisible) Icons.Rounded.GridOn else Icons.Rounded.GridOff,
                    contentDescription = stringResource(R.string.studio_toolbar_grid),
                    modifier = Modifier.size(18.dp)
                )
            }

            FilledIconButton(
                onClick = onToggleWireframe,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isWireframeMode) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (isWireframeMode) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (isWireframeMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(R.string.studio_toolbar_wireframe),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CollapsedDockHandle(
    dockPosition: ToolbarDockPosition,
    onExpand: () -> Unit,
    onOpenAddElement: (() -> Unit)?,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current

    val shape = when (dockPosition) {
        ToolbarDockPosition.RIGHT -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        ToolbarDockPosition.LEFT -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        ToolbarDockPosition.BOTTOM -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    }

    Surface(
        modifier = Modifier
            .shadow(elevation = 6.dp, shape = shape)
            .pointerInput(dockPosition) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var change = down
                    var totalX = 0f
                    var totalY = 0f
                    var isDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val current = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!current.pressed) {
                            if (!isDrag) {
                                onExpand()
                            }
                            break
                        }
                        val dx = current.position.x - change.position.x
                        val dy = current.position.y - change.position.y
                        totalX += dx
                        totalY += dy
                        if (hypot(totalX, totalY) > viewConfig.touchSlop) {
                            isDrag = true
                        }
                        if (isDrag) {
                            current.consume()
                            with(density) {
                                onDrag(totalX.toDp().value, totalY.toDp().value)
                            }
                        }
                        change = current
                    }
                }
            },
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        if (dockPosition == ToolbarDockPosition.BOTTOM) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 12.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                )
                onOpenAddElement?.let { onAdd ->
                    FilledIconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.studio_add_element_title),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                FilledIconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = stringResource(R.string.studio_toolbar_expand),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 3.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                )
                onOpenAddElement?.let { onAdd ->
                    FilledIconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.studio_add_element_title),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                FilledIconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (dockPosition == ToolbarDockPosition.LEFT) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft,
                        contentDescription = stringResource(R.string.studio_toolbar_expand),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
