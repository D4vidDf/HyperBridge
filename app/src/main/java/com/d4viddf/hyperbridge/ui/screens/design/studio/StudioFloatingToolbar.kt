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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridOff
import androidx.compose.material.icons.rounded.GridOn
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
 * Movable, dockable floating toolbar for the Studio Canvas.
 *
 * Designed to prevent covering canvas content on phones:
 * - Docks into a slim side-handle on the right edge when collapsed.
 * - Expands when tapped or dragged from the side.
 * - Can be repositioned vertically by dragging up/down to stay clear of elements being edited.
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
    maxCanvasHeightDp: Int = 200,
    initialExpanded: Boolean = false
) {
    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val maxOffsetYDp = (maxCanvasHeightDp - 60).coerceAtLeast(0).toFloat()

    var offsetYDp by remember { mutableFloatStateOf(0f) }
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    Box(
        modifier = modifier.offset {
            IntOffset(0, with(density) { offsetYDp.dp.roundToPx() })
        }
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
                Surface(
                    modifier = Modifier
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp)),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag & collapse header
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .pointerInput(maxOffsetYDp) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        with(density) {
                                            offsetYDp = (offsetYDp + dragAmount.y.toDp().value).coerceIn(0f, maxOffsetYDp)
                                        }
                                        if (dragAmount.x > 16f) {
                                            isExpanded = false
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FilledIconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = stringResource(R.string.studio_toolbar_collapse),
                                    modifier = Modifier.size(20.dp)
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
            } else {
                // Docked side handle
                Surface(
                    modifier = Modifier
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                        .pointerInput(maxOffsetYDp) {
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
                                            isExpanded = true
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
                                            offsetYDp = (offsetYDp + dy.toDp().value).coerceIn(0f, maxOffsetYDp)
                                        }
                                        if (totalX < -16f) {
                                            isExpanded = true
                                        }
                                    }
                                    change = current
                                }
                            }
                        },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 12.dp, height = 3.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = stringResource(R.string.studio_toolbar_expand),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
