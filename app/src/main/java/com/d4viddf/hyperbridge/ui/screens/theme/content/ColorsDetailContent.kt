package com.d4viddf.hyperbridge.ui.screens.theme.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.screens.design.ToolbarOption
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorsDetailContent(
    selectedColorHex: String,
    useAppColors: Boolean,
    onColorSelected: (String) -> Unit,
    onUseAppColorsChanged: (Boolean) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            HorizontalFloatingToolbar(
                expanded = true,
                content = {
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ToolbarOption(
                            selected = tabIndex == 0,
                            icon = Icons.Outlined.Palette,
                            text = stringResource(R.string.colors_tab_presets),
                            onClick = { tabIndex = 0 }
                        )

                        ToolbarOption(
                            selected = tabIndex == 1,
                            icon = Icons.Outlined.Colorize,
                            text = stringResource(R.string.colors_tab_custom),
                            onClick = { tabIndex = 1 }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 100.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ColorsTabTransition",
                    modifier = Modifier.padding(vertical = 12.dp)
                ) { selectedTab ->
                    when (selectedTab) {
                        0 -> ColorsPresetsTab(selectedColorHex, useAppColors, onColorSelected, onUseAppColorsChanged)
                        1 -> ColorsCustomTab(selectedColorHex, onColorSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorsPresetsTab(
    selectedColorHex: String,
    useAppColors: Boolean,
    onColorSelected: (String) -> Unit,
    onUseAppColorsChanged: (Boolean) -> Unit
) {
    val presets = listOf("#3DDA82", "#FF3B30", "#007AFF", "#FF9500", "#9333ea", "#e11d48", "#2563eb", "#FFFFFF")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.colors_label_presets),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(72.dp)
        ) {
            items(presets) { hex ->
                val color = safeParseColor(hex)
                val isSelected = selectedColorHex.equals(hex, ignoreCase = true) && !useAppColors

                val shape = if (isSelected) RoundedCornerShape(16.dp) else CircleShape
                val borderColor = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                val borderWidth = if (isSelected) 3.dp else 0.dp

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(shape)
                        .background(color)
                        .clickable {
                            onColorSelected(hex)
                            onUseAppColorsChanged(false)
                        }
                        .border(borderWidth, borderColor, shape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, null, tint = if (color == Color.White) Color.Black else Color.White)
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        ListItem(
            headlineContent = {
                Text(stringResource(R.string.colors_label_use_app_colors), fontWeight = FontWeight.Medium)
            },
            supportingContent = {
                Text(
                    stringResource(R.string.colors_desc_use_app_colors),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 1,
                    maxLines = 3
                )
            },
            trailingContent = {
                Switch(
                    checked = useAppColors,
                    onCheckedChange = onUseAppColorsChanged
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun ColorsCustomTab(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val savedColors = listOf(selectedColorHex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(stringResource(R.string.colors_label_custom_title), style = MaterialTheme.typography.titleMedium)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalIconButton(
                onClick = { showColorPicker = true },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.colors_cd_add_custom))
            }

            Spacer(Modifier.width(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(savedColors) { hex ->
                    val color = safeParseColor(hex)
                    val shape = RoundedCornerShape(16.dp)

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(shape)
                            .background(color)
                            .clickable { onColorSelected(hex) }
                            .border(3.dp, MaterialTheme.colorScheme.onSurface, shape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = if (color == Color.White) Color.Black else Color.White)
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(stringResource(R.string.colors_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.colors_dialog_desc))

                    OutlinedTextField(
                        value = selectedColorHex,
                        onValueChange = { onColorSelected(it) },
                        label = { Text(stringResource(R.string.colors_label_hex)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Outlined.Colorize, null)
                        },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(safeParseColor(selectedColorHex))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text(stringResource(R.string.colors_action_done))
                }
            }
        )
    }
}

fun safeParseColor(hex: String?): Color {
    if (hex.isNullOrEmpty()) return Color.Gray
    return try {
        Color(hex.toColorInt())
    } catch (e: Exception) {
        Color.Gray
    }
}