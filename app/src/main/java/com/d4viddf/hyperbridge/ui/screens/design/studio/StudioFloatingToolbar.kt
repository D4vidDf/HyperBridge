package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.GridOff
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R

/**
 * Official Material 3 Expressive [HorizontalFloatingToolbar] with integrated FAB for the Studio Design Screen.
 *
 * Stays permanently visible alongside the primary Add FAB in the screen's FAB position,
 * providing one-tap access to Undo, Redo, Zoom controls, Grid/Wireframe toggles, and Add Element.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    onOpenAddElement: (() -> Unit)? = null
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        floatingActionButton = {
            onOpenAddElement?.let { onAdd ->
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = onAdd
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.studio_add_element_title)
                    )
                }
            }
        },
        content = {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
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

                // Divider
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(width = 1.dp, height = 20.dp)
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
                        .padding(horizontal = 2.dp)
                        .size(width = 1.dp, height = 20.dp)
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

                // Wireframe / Preview toggle
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
    )
}
