package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CenterFocusStrong
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R

/**
 * Horizontal floating bottom toolbar and FAB overlay for the Studio Design Screen.
 *
 * Always anchored at the bottom of the screen, grouping Undo, Redo, Canvas view controls
 * (Zoom in/out/reset, Grid, Wireframe), and the primary Add Element FAB together.
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
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onOpenAddElement: (() -> Unit)? = null,
    initialExpanded: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
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
                    HorizontalExpandedToolbar(
                        isGridVisible = isGridVisible,
                        isWireframeMode = isWireframeMode,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        onCollapse = { isExpanded = false },
                        onOpenAddElement = onOpenAddElement,
                        onZoomIn = onZoomIn,
                        onZoomOut = onZoomOut,
                        onResetZoom = onResetZoom,
                        onToggleGrid = onToggleGrid,
                        onToggleWireframe = onToggleWireframe
                    )
                } else {
                    HorizontalCollapsedToolbar(
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        onExpand = { isExpanded = true },
                        onOpenAddElement = onOpenAddElement
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalExpandedToolbar(
    isGridVisible: Boolean,
    isWireframeMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCollapse: () -> Unit,
    onOpenAddElement: (() -> Unit)?,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleWireframe: () -> Unit
) {
    Surface(
        modifier = Modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Collapse button
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

            // Divider
            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // Undo
            FilledIconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(R.string.studio_undo),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Redo
            FilledIconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = stringResource(R.string.studio_redo),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // Zoom Out
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
                    modifier = Modifier.size(18.dp)
                )
            }

            // Zoom Reset
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
                    modifier = Modifier.size(16.dp)
                )
            }

            // Zoom In
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
                    modifier = Modifier.size(18.dp)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // Grid toggle
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

            // Wireframe toggle
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

            // Divider
            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // Primary Add Element FAB
            onOpenAddElement?.let { onAdd ->
                FilledIconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.studio_add_element_title),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalCollapsedToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExpand: () -> Unit,
    onOpenAddElement: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Undo
            FilledIconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(R.string.studio_undo),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Redo
            FilledIconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = stringResource(R.string.studio_redo),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Primary Add Element FAB Action
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

            // Expand button
            FilledIconButton(
                onClick = onExpand,
                modifier = Modifier.size(36.dp),
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
    }
}
