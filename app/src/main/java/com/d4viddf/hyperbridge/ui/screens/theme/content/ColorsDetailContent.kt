package com.d4viddf.hyperbridge.ui.screens.theme.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.ColorMode
import com.d4viddf.hyperbridge.ui.components.CustomColorBottomSheet
import com.d4viddf.hyperbridge.ui.components.GradientSlider
import com.d4viddf.hyperbridge.ui.screens.design.ToolbarOption

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorsDetailContent(
    selectedColorHex: String,
    colorMode: ColorMode,
    onColorSelected: (String) -> Unit,
    onColorModeChanged: (ColorMode) -> Unit
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
                        0 -> ColorsPresetsTab(selectedColorHex, colorMode, onColorSelected, onColorModeChanged)
                        1 -> ColorsCustomTab(selectedColorHex, colorMode, onColorSelected, onColorModeChanged)
                    }
                }
            }
        }
    }
}


@Composable
private fun ColorsPresetsTab(
    selectedColorHex: String,
    colorMode: ColorMode,
    onColorSelected: (String) -> Unit,
    onColorModeChanged: (ColorMode) -> Unit
) {
    val presets = listOf("#3DDA82", "#FF3B30", "#007AFF", "#FF9500", "#9333ea", "#e11d48", "#2563eb", "#FFFFFF")

    var activePresetBase by remember {
        mutableStateOf(presets.find { it.equals(selectedColorHex, ignoreCase = true) })
    }

    val selectedColor = safeParseColor(selectedColorHex)
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(selectedColor.toArgb(), hsl)
    val currentLightness = hsl[2]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()) // <-- ADDED SCROLLING HERE
            .padding(bottom = 16.dp), // Added bottom padding for clearance
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.colors_label_presets),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )

        // 1. Preset Bubbles Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(presets) { hex ->
                val color = safeParseColor(hex)
                val isSelected = activePresetBase.equals(hex, ignoreCase = true) && colorMode == ColorMode.CUSTOM

                val shape = if (isSelected) RoundedCornerShape(16.dp) else CircleShape
                val borderColor = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                val borderWidth = if (isSelected) 3.dp else 0.dp

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(shape)
                        .background(color)
                        .clickable {
                            activePresetBase = hex
                            onColorSelected(hex)
                            onColorModeChanged(ColorMode.CUSTOM)
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

        // 2. Animated Lightness Slider Row
        AnimatedVisibility(
            visible = activePresetBase != null && colorMode == ColorMode.CUSTOM,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                val baseColor = safeParseColor(activePresetBase)
                val baseHsl = FloatArray(3)
                ColorUtils.colorToHSL(baseColor.toArgb(), baseHsl)

                val dynamicSliderBrush = remember(baseHsl[0], baseHsl[1]) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color(ColorUtils.HSLToColor(floatArrayOf(baseHsl[0], baseHsl[1], 0.5f))),
                            Color.White
                        )
                    )
                }

                GradientSlider(
                    value = currentLightness,
                    onValueChange = { newLightness ->
                        val newHsl = floatArrayOf(baseHsl[0], baseHsl[1], newLightness)
                        val newColorArgb = ColorUtils.HSLToColor(newHsl)
                        val newColorHex = String.format("#%06X", (0xFFFFFF and newColorArgb))
                        onColorSelected(newColorHex)
                    },
                    valueRange = 0f..1f,
                    brush = dynamicSliderBrush,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Text(
            text = stringResource(R.string.colors_label_dynamic_modes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )

        // 3. App Icon Colors Option (Switch)
        ListItem(
            headlineContent = { Text(stringResource(R.string.colors_label_use_app_colors), fontWeight = FontWeight.Medium) },
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
                    checked = colorMode == ColorMode.APP_ICON,
                    onCheckedChange = { isChecked ->
                        onColorModeChanged(if (isChecked) ColorMode.APP_ICON else ColorMode.CUSTOM)
                    }
                )
            },
            modifier = Modifier.clickable {
                onColorModeChanged(if (colorMode == ColorMode.APP_ICON) ColorMode.CUSTOM else ColorMode.APP_ICON)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        // 4. Material You Option (Switch)
        ListItem(
            headlineContent = { Text(stringResource(R.string.colors_label_material_you), fontWeight = FontWeight.Medium) },
            supportingContent = {
                Text(
                    stringResource(R.string.colors_desc_material_you),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 1,
                    maxLines = 3
                )
            },
            trailingContent = {
                Switch(
                    checked = colorMode == ColorMode.MATERIAL_YOU,
                    onCheckedChange = { isChecked ->
                        onColorModeChanged(if (isChecked) ColorMode.MATERIAL_YOU else ColorMode.CUSTOM)
                    }
                )
            },
            modifier = Modifier.clickable {
                onColorModeChanged(if (colorMode == ColorMode.MATERIAL_YOU) ColorMode.CUSTOM else ColorMode.MATERIAL_YOU)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun ColorsCustomTab(
    selectedColorHex: String,
    colorMode: ColorMode,
    onColorSelected: (String) -> Unit,
    onColorModeChanged: (ColorMode) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    val savedColors = remember {
        mutableStateListOf<String>().apply {
            if (selectedColorHex.isNotEmpty()) add(selectedColorHex)
        }
    }

    val selectedColor = safeParseColor(selectedColorHex)
    val isCustomColorSelected = savedColors.contains(selectedColorHex) && colorMode == ColorMode.CUSTOM

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(selectedColor.toArgb(), hsl)
    val hue = hsl[0]
    val saturation = hsl[1]
    val lightness = hsl[2]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()) // <-- ADDED SCROLLING HERE
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ... (Keep the rest of your ColorsCustomTab exactly the same) ...
        Text(
            stringResource(R.string.colors_label_custom_title),
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Add Button
            FilledTonalIconButton(
                onClick = { showColorPicker = true },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.colors_cd_add_custom)
                )
            }

            // 2. Vertical Separator
            VerticalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // 3. User's Custom Colors
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(savedColors) { hex ->
                    val color = safeParseColor(hex)
                    val isSelected = selectedColorHex.equals(
                        hex,
                        ignoreCase = true
                    ) && colorMode == ColorMode.CUSTOM

                    val shape = if (isSelected) RoundedCornerShape(16.dp) else CircleShape
                    val borderColor =
                        if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                    val borderWidth = if (isSelected) 3.dp else 0.dp

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(shape)
                            .background(color)
                            .clickable {
                                onColorSelected(hex)
                                onColorModeChanged(ColorMode.CUSTOM)
                            }
                            .border(borderWidth, borderColor, shape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = if (color == Color.White) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
        // 4. Custom HSL Bottom Sheet
        if (showColorPicker) {
            CustomColorBottomSheet(
                initialColor = safeParseColor(selectedColorHex),
                onDismiss = { showColorPicker = false },
                onColorAdded = { newColor ->
                    val hex = String.format("#%06X", (0xFFFFFF and newColor.toArgb()))
                    if (!savedColors.contains(hex)) {
                        savedColors.add(0, hex)
                    }
                    onColorSelected(hex)
                    onColorModeChanged(ColorMode.CUSTOM)
                    showColorPicker = false
                }
            )
        }
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